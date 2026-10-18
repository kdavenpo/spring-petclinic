# Modifications to Spring PetClinic

This fork (`kdavenpo/spring-petclinic`, branch `subject-system`) is the **`clinic-core`** half of the starting system for a CS 8674 master's project. The other half, the **`vets`** service, is in `kdavenpo/spring-petclinic-microservices`, branch `subject-system`. The two share one MySQL schema.

Upstream: [spring-projects/spring-petclinic](https://github.com/spring-projects/spring-petclinic), Apache License 2.0 (see `LICENSE.txt`). This file is the statement of changes that the license's §4(b) asks for. Every change below is on top of upstream commit `4a021e4ccf7a48c8d1b5a68485418a49e1fed606` ("Upgraded databases", Spring Boot 3.4.2).

## Commits

| Commit | Change |
|---|---|
| `2f378ff` | Shared database |
| `dd3b058` | `clinic-core`: the monolith with the vet package moved out |
| `0dd0ee7` | **Modification A** — each visit records its attending vet |
| `157b065` | **Modification B** — the owner list fetches each owner's visits one at a time |
| `eeb94ca` | Data collection: endpoint-template access log (settings only) |

## 1. Shared database (`2f378ff`)

- **Added** `subject-system-compose.yml`: one MySQL container (`mysql:9.3`, pinned by digest `sha256:b9d8b7ec6e6aced08b1cfe50776f8e323c0a625adf4e10e69f90fc686ea10807`). On first start it runs upstream's `db/mysql/schema.sql` then `data.sql`. Neither service creates tables. `docker compose -f subject-system-compose.yml down` deletes the data on purpose; the next `up -d` rebuilds it from the scripts.

## 2. `clinic-core` (`dd3b058`)

The vet functionality now belongs to the separate `vets` service.

- **Removed:** `vet/VetController.java`, `vet/VetRepository.java`, `vet/Vets.java`, `vet/Specialty.java`, `templates/vets/vetList.html`, and the Veterinarians menu item in `templates/fragments/layout.html`. Removed endpoints: `GET /vets.html`, `GET /vets`.
- **Kept and trimmed:** `vet/Vet.java` is now a read-only (`@Immutable`) mapping of the shared `vets` table; still extends `Person`; no specialties. It exists only so that modification A can give a visit its vet.
- **Added** `application-subject.properties` (`--spring.profiles.active=subject`): port 8080, datasource `localhost:3306/petclinic`, `spring.sql.init.mode=never`.
- **Tests:** vet tests and the vet parts of four other test classes removed; `vet/VetTests.java` kept.

## 3. Modification A — each visit records its attending vet (`0dd0ee7`)

*Anti-pattern or omission corrected:* artificial decoupling. A veterinary visit has an attending vet; the published code omits it because that repository was written already decomposed.

- `owner/Visit.java`: `@ManyToOne` field `vet`, column `vet_id`, with getter and setter. **This is the new Visit → Vet edge.**
- `owner/OwnerRepository.java`: `findVets()` (JPQL, same pattern as upstream's `findPetTypes()`), for the vet dropdown.
- `owner/VisitController.java`: `populateVets()` puts the vets on the form; a new visit with no vet is rejected (`required`).
- `templates/pets/createOrUpdateVisitForm.html`: vet dropdown, and a Vet column in Previous Visits. `templates/owners/ownerDetails.html`: Vet column in each pet's visits.
- `db/{mysql,h2,hsqldb,postgres}/schema.sql`: `visits.vet_id` with a foreign key to `vets(id)` (in H2 and HSQLDB, `visits` is now dropped first). `db/{mysql,h2,hsqldb}/data.sql`: the four sample visits get `NULL` vet. (`postgres/data.sql` names its columns and needed no change.)
- `messages/messages*.properties`: keys `vet`, `chooseVet` in every locale.
- `VisitControllerTests.java`: success test sends a vet; new test checks that a visit without a vet is rejected.

## 4. Modification B — chatty owner list (`157b065`)

*Anti-pattern:* N + 1 / chatty cross-domain read.

- `owner/OwnerRepository.java`: `findVisitsByOwnerId(ownerId)`.
- `owner/OwnerController.java`: `addPaginationModel` calls it **once per owner on the page** to fill a visit count. The owners' visits are already loaded with the owners, so every one of these queries is redundant — that is the anti-pattern. Static footprint: one call edge. Runtime footprint: one extra query per owner listed (5 per page).
- `templates/owners/ownersList.html`: Visits column (`visitCounts.get(owner.id)`).
- `messages/messages*.properties`: key `visits` in every locale.
- `OwnerControllerTests.java`: new test verifies one visit query per listed owner.

## 5. Data collection (`eeb94ca`) — settings only, no code

- `application-subject.properties`: Tomcat access log to `./logs/access-clinic-core.<date>.log`, one line per request: `%t "%r" %s %D tmpl=<endpoint template>`. `%D` is **microseconds** in Tomcat 10. The template is Spring's `HandlerMapping.bestMatchingPattern` attribute; `tmpl=-` means the request matched no controller (static files).
- `.gitignore`: `logs/`.
- Endpoint inventory: upstream already exposes all actuator endpoints, so `GET /actuator/mappings` lists every endpoint with its controller class and method. Nothing added.

## What is unchanged

- No other Java class was added, removed, or edited.
- Thymeleaf templates are not part of the class graph (static analysis reads Java only), so the template edits in A and B change what a user sees but add no edges.

## Endpoints (15 in `clinic-core`)

`GET /` · `GET /oups` · `GET /owners/new` · `POST /owners/new` · `GET /owners/find` · `GET /owners` · `GET /owners/{ownerId}` · `GET /owners/{ownerId}/edit` · `POST /owners/{ownerId}/edit` · `GET /owners/{ownerId}/pets/new` · `POST /owners/{ownerId}/pets/new` · `GET /owners/{ownerId}/pets/{petId}/edit` · `POST /owners/{ownerId}/pets/{petId}/edit` · `GET /owners/{ownerId}/pets/{petId}/visits/new` · `POST /owners/{ownerId}/pets/{petId}/visits/new`

The `vets` service adds `GET /vets`: 16 endpoints in the starting system.

## Run

```
docker compose -f subject-system-compose.yml up -d
.\mvnw.cmd spring-javaformat:apply package
java -jar target\spring-petclinic-3.4.0-SNAPSHOT.jar --spring.profiles.active=subject
```
