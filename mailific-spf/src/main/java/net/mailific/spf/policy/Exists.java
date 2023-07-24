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
import java.util.List;
import net.mailific.spf.Abort;
import net.mailific.spf.ResultCode;
import net.mailific.spf.RuntimeAbort;
import net.mailific.spf.SpfUtil;
import net.mailific.spf.dns.DnsFail;
import net.mailific.spf.macro.MacroString;

public class Exists implements Mechanism {

  private final MacroString domainSpec;

  public Exists(MacroString domainSpec) {
    this.domainSpec = domainSpec;
    // TODO: null check
  }

  public String toString() {
    return "exists:" + domainSpec;
  }

  @Override
  public boolean causesLookup() {
    return true;
  }

  @Override
  public boolean matches(
      SpfUtil spf, InetAddress ip, String domain, String sender, String ehloParam) throws Abort {
    String expandedDomain = domainSpec.expand(spf, ip, domain, sender, ehloParam);
    try {
      List<InetAddress> results = spf.getIpsByHostname(expandedDomain, true);
      return !results.isEmpty();
    } catch (DnsFail e) {
      throw new Abort(ResultCode.Temperror, e.getMessage());
    } catch (RuntimeAbort e) {
      throw e.getAbort();
    }
  }
}
