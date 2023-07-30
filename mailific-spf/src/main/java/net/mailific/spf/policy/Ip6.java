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
import java.net.UnknownHostException;
import net.mailific.spf.SpfUtil;

public class Ip6 implements Mechanism {

  private final InetAddress ip6;
  private final int cidrLength;

  public Ip6(String ip6Network, int cidrLength) throws PolicySyntaxException {
    try {
      this.ip6 = InetAddress.getByName(ip6Network);
    } catch (UnknownHostException e) {
      throw new PolicySyntaxException("Bad ip6 network: " + ip6Network);
    }
    if (cidrLength > 128) {
      throw new PolicySyntaxException("ip6 cidr length > 128");
    }
    this.cidrLength = cidrLength;
  }

  public String toString() {
    return "ip6:" + ip6 + (cidrLength > -1 ? "/" + cidrLength : "");
  }

  @Override
  public boolean causesLookup() {
    return false;
  }

  @Override
  public boolean matches(
      SpfUtil spf, InetAddress ip, String domain, String sender, String ehloParam) {
    return spf.cidrMatch(ip6, ip, cidrLength);
  }
}
