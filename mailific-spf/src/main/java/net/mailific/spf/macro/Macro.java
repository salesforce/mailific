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
import java.util.Arrays;
import java.util.stream.Collectors;
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

  public String patternFromDelimiter(String delimiter) {
    String pattern = "[.]";
    if (delimiter != null && !delimiter.isBlank()) {
      StringBuilder sb = new StringBuilder();
      sb.append('[');
      if (delimiter.contains("-")) {
        // Move hyphen to the beginning, so it's not interpreted as a range
        sb.append('-');
        sb.append(delimiter.replace("-", ""));
      } else {
        sb.append(delimiter);
      }
      sb.append(']');
      pattern = sb.toString();
    }
    return pattern;
  }

  public String transform(String s, int rightParts, boolean reverse, String delimiter) {
    if (reverse || rightParts != 0 || delimiter != null) {

      String pattern = patternFromDelimiter(delimiter);

      String[] splits = s.split(pattern, -1);
      if (splits.length == 1) {
        return splits[0];
      }
      String[] parts = splits;

      int count = rightParts > 0 ? Math.min(splits.length, rightParts) : splits.length;
      if (reverse) {
        parts = new String[count];
        for (int i = 0, j = count - 1; i < count; i++, j--) {
          parts[i] = splits[j];
        }
      } else if (count < splits.length) {
        parts = new String[count];
        for (int i = 0, j = splits.length - count; i < count; i++, j++) {
          parts[i] = splits[j];
        }
      }

      return Arrays.stream(parts).collect(Collectors.joining("."));
    }
    return s;
  }
}
