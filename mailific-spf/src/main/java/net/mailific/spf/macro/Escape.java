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
import net.mailific.spf.SpfUtil;
import net.mailific.spf.policy.PolicySyntaxException;

public class Escape extends Macro {

  private final String value;
  private final String expanded;

  public Escape(String value) throws PolicySyntaxException {
    super(0, false, null, false);
    this.value = value;
    switch (value) {
      case "%":
        expanded = "%";
        break;
      case "_":
        expanded = " ";
        break;
      case "-":
        expanded = "%20";
        break;
      default:
        throw new PolicySyntaxException("Unexpected escape %" + value);
    }
  }

  @Override
  public String innerExpand(
      SpfUtil spf, InetAddress ip, String domain, String sender, String ehloParam) {
    return expanded;
  }

  public String toString() {
    return "%" + value;
  }
}
