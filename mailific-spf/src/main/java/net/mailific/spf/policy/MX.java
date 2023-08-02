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
import java.net.InetAddress;
import java.util.List;
import net.mailific.spf.Abort;
import net.mailific.spf.ResultCode;
import net.mailific.spf.SpfUtil;
import net.mailific.spf.dns.DnsFail;
import net.mailific.spf.macro.DomainSpec;

public class MX implements Mechanism {
  private final DomainSpec domainSpec;
  private final int cidrLength;

  public MX(DomainSpec domainSpec, int cidrLength) {
    this.domainSpec = domainSpec;
    this.cidrLength = cidrLength;
  }

  public String toString() {
    return "mx"
        + (domainSpec == null ? "" : ":" + domainSpec)
        + (cidrLength > -1 ? "/" + cidrLength : "");
  }

  @Override
  public boolean causesLookup() {
    return true;
  }

  @Override
  public boolean matches(
      SpfUtil spf, InetAddress ip, String domain, String sender, String ehloParam) throws Abort {
    String name = domain;
    if (domainSpec != null) {
      name = domainSpec.expand(spf, ip, domain, sender, ehloParam);
    }
    List<InetAddress> ips;
    try {
      ips = spf.getIpsByMxName(name, ip instanceof Inet4Address);
      return ips.stream().anyMatch(i -> spf.cidrMatch(i, ip, cidrLength));
    } catch (DnsFail e) {
      throw new Abort(ResultCode.Temperror, e.getMessage());
    }
  }
}
