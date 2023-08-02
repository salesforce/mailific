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

package net.mailific.spf.macro;

import java.net.InetAddress;
import net.mailific.spf.Abort;
import net.mailific.spf.SpfUtil;

public class DomainSpec extends MacroString {

  @Override
  public String expand(SpfUtil spf, InetAddress ip, String domain, String sender, String ehloParam)
      throws Abort {
    return truncate(super.expand(spf, ip, domain, sender, ehloParam));
  }

  public static String truncate(String domain) {
    if (domain == null) {
      return null;
    }
    int l = domain.length();
    int i = -1;
    while (l > 253) {
      i = domain.indexOf('.', i + 1);
      if (i == -1) {
        return "";
      }
      l = domain.length() - (i + 1);
    }
    return (i > -1) ? domain.substring(i + 1) : domain;
  }
}
