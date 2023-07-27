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

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.mailific.spf.Abort;
import net.mailific.spf.SpfUtil;
import net.mailific.spf.dns.DnsFail;
import net.mailific.spf.macro.Expandable;
import net.mailific.spf.macro.MacroString;
import net.mailific.spf.parser.ParseException;
import net.mailific.spf.parser.SpfPolicy;

public class Explanation extends Modifier implements Expandable {

  private final MacroString domainSpec;
  private String prefix;

  public Explanation(MacroString domainSpec, String prefix) {
    this.domainSpec = domainSpec;
    this.prefix = prefix;
  }

  public String toString() {
    return "exp=" + domainSpec;
  }

  @Override
  public String expand(SpfUtil spf, InetAddress ip, String domain, String sender, String ehloParam)
      throws Abort {
    String name = domainSpec.expand(spf, ip, domain, sender, ehloParam);
    try {
      List<String> records = spf.resolveTxtRecords(name);
      if (records.size() != 1) {
        return null;
      }
      String unparsed = records.get(0);
      if (prefix != null) {
        unparsed = prefix + unparsed;
      }
      SpfPolicy parser =
          new SpfPolicy(
              new ByteArrayInputStream(unparsed.getBytes(StandardCharsets.US_ASCII)), "US-ASCII");
      MacroString ms = parser.explainString();
      return ms.expand(spf, ip, domain, sender, ehloParam);
    } catch (DnsFail | ParseException | PolicySyntaxException e) {
      return null;
    }
  }
}
