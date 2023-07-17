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
import java.util.ArrayList;
import java.util.List;
import net.mailific.spf.Abort;
import net.mailific.spf.LookupCount;
import net.mailific.spf.SpfUtil;

public class MacroString implements Expandable {

  private final List<Expandable> tokens = new ArrayList<>();

  public void add(Expandable e) {
    tokens.add(e);
  }

  public boolean isEmpty() {
    return tokens.isEmpty();
  }

  @Override
  public String expand(
      SpfUtil spf, InetAddress ip, String domain, String sender, LookupCount lookupCount)
      throws Abort {
    StringBuilder sb = new StringBuilder();
    for (Expandable token : tokens) {
      sb.append(token.expand(spf, ip, domain, sender, lookupCount));
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    tokens.stream()
        .forEach(
            (token) -> {
              sb.append(token.toString());
            });
    return sb.toString();
  }
}
