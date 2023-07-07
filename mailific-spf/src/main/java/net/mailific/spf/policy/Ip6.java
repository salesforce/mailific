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
import net.mailific.spf.Spf;

public class Ip6 implements Mechanism {
  private final String ip6Network;
  private final String cidrLength;

  public Ip6(String ip6Network, String cidrLength) {
    this.ip6Network = ip6Network;
    this.cidrLength = cidrLength;
  }

  public String toString() {
    return "ip6:" + ip6Network + (cidrLength == null ? "" : cidrLength);
  }

  @Override
  public boolean causesLookup() {
    return false;
  }

  @Override
  public boolean matches(
      Spf spf, Inet4Address ip, String domain, String sender, LookupCount lookupCount) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'matches'");
  }
}
