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
import net.mailific.spf.Abort;
import net.mailific.spf.Result;
import net.mailific.spf.ResultCode;
import net.mailific.spf.SpfUtil;
import net.mailific.spf.macro.DomainSpec;

public class Include implements Mechanism {

  private final DomainSpec domainSpec;

  public Include(DomainSpec domainSpec) throws PolicySyntaxException {
    if (domainSpec == null || domainSpec.isEmpty()) {
      throw new PolicySyntaxException("Include domainSpec can't be empty.");
    }
    this.domainSpec = domainSpec;
  }

  public String toString() {
    return "include:" + domainSpec;
  }

  @Override
  public boolean causesLookup() {
    return true;
  }

  @Override
  public boolean matches(
      SpfUtil spf, InetAddress ip, String domain, String sender, String ehloParam) throws Abort {
    String expandedDomain = domainSpec.expand(spf, ip, domain, sender, ehloParam);
    Result result = spf.checkHost(ip, expandedDomain, sender, ehloParam);
    switch (result.getCode()) {
      case Pass:
        return true;
      case Fail:
      case Softfail:
      case Neutral:
        return false;
      case Temperror:
        throw new Abort(result);
      case None:
        throw new Abort(ResultCode.Permerror, "Include returned None.");
      default:
        throw new Abort(result);
    }
  }
}
