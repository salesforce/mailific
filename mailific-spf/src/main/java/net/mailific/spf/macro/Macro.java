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

public abstract class Macro {

  private final int rightParts;
  private final boolean reverse;
  private final String delimiter;

  public static Macro macro(String type, int rightParts, boolean reverse, String delimiter)
      throws PolicySyntaxException {
    switch (type) {
      case "s":
        return new Sender(rightParts, reverse, delimiter);
      default:
        throw new PolicySyntaxException("Not a macro type: " + type);
    }
  }

  protected Macro(int rightParts, boolean reverse, String delimiter) {
    this.rightParts = rightParts;
    this.reverse = reverse;
    this.delimiter = delimiter;
  }

  public int getRightParts() {
    return rightParts;
  }

  public boolean isReverse() {
    return reverse;
  }

  public abstract String expand(SpfUtil spf, InetAddress ip, String domain, String sender);

  public String getDelimiter() {
    return delimiter;
  }

  public String modify(String s, int digits, boolean reverse, String delimiter) {
    if (reverse || digits != 0 || delimiter != null) {
      if (delimiter == null) {
        delimiter = "[.]";
      } else {
        // The spec allows repeats, so strip down to one copy of each character
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < 7; i++) {
          char c = "-.+,/_=".charAt(i);
          if (delimiter.indexOf(c) > -1) {
            sb.append(c);
          }
        }
        sb.append(']');
        delimiter = sb.toString();
      }
    }
    return null;
  }
}
