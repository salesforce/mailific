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
import net.mailific.spf.LookupCount;

public class Exists implements Mechanism {

  private final String domainSpec;

  public Exists(String domainSpec) {
    this.domainSpec = domainSpec;
  }

  public String toString() {
    return "exists:" + domainSpec;
  }

  @Override
  public boolean causesLookup() {
    return true;
  }

  @Override
  public boolean matches(Inet4Address ip, String domain, String sender, LookupCount lookupCount) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'matches'");
  }
}
