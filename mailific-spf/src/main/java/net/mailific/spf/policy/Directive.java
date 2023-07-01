/*-
 * Mailific SMTP Server Library
 *
 * Copyright (C) 2023 Joe Humphreys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.mailific.spf.policy;

public class Directive extends Term {

  private final Qualifier qualifier;
  private final Mechanism mechanism;

  public Directive(Qualifier qualifier, Mechanism mechanism) {
    super("foo");
    this.qualifier = qualifier == null ? Qualifier.PASS : qualifier;
    this.mechanism = mechanism;
  }

  public Qualifier getQualifier() {
    return qualifier;
  }

  public Mechanism getMechanism() {
    return mechanism;
  }
}
