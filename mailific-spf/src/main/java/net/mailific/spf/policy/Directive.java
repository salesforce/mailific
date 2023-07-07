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

import java.net.Inet4Address;
import net.mailific.spf.Abort;
import net.mailific.spf.LookupCount;
import net.mailific.spf.Result;
import net.mailific.spf.SpfImp;

public class Directive {

  private final Qualifier qualifier;
  private final Mechanism mechanism;

  public Directive(Qualifier qualifier, Mechanism mechanism) {
    this.qualifier = qualifier == null ? Qualifier.PASS : qualifier;
    this.mechanism = mechanism;
  }

  public Qualifier getQualifier() {
    return qualifier;
  }

  public Mechanism getMechanism() {
    return mechanism;
  }

  public String toString() {
    return qualifier.getSymbol() + mechanism;
  }

  /**
   * @return a result if the mechanism matched, or otherwise led to a final disposition. Otherwise
   *     null.
   * @throws Abort
   */
  public Result evaluate(
      SpfImp spf, Inet4Address ip, String domain, String sender, LookupCount lookupCount)
      throws Abort {
    if (mechanism.causesLookup()) {
      lookupCount.inc();
    }
    if (mechanism.matches(spf, ip, domain, sender, lookupCount)) {
      return new Result(qualifier.getResultCode(), "matched " + toString());
    }
    return null;
  }
}
