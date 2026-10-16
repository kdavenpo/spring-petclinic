/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.vet;

import org.hibernate.annotations.Immutable;
import org.springframework.samples.petclinic.model.Person;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Read-only mapping of the shared {@code vets} table.
 * <p>
 * CS 8674 starting system: the vet package was moved out of clinic-core into the separate
 * vets service, which owns this table. This class is the one piece kept, so that
 * modification A can give each visit an attending vet. It has no specialties and is never
 * written by clinic-core.
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Arjen Poutsma
 */
@Entity
@Immutable
@Table(name = "vets")
public class Vet extends Person {

}
