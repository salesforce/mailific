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

import java.net.InetAddress;
import net.mailific.spf.LookupCount;
import net.mailific.spf.SpfUtil;
import net.mailific.spf.macro.MacroString;

public class MX implements Mechanism {
  private final MacroString domainSpec;
  private final String cidrLength;

  public MX(MacroString domainSpec, String cidrLength) {
    this.domainSpec = domainSpec;
    this.cidrLength = cidrLength;
  }

  public String toString() {
    return "mx"
        + (domainSpec == null ? "" : ":" + domainSpec)
        + (cidrLength == null ? "" : cidrLength);
  }

  @Override
  public boolean causesLookup() {
    return true;
  }

  @Override
  public boolean matches(
      SpfUtil spf, InetAddress ip, String domain, String sender, LookupCount lookupCount) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'matches'");
  }
}
