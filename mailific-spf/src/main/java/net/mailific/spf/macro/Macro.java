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

import static net.mailific.spf.SpfUtilImp.HEX;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.Collectors;
import net.mailific.spf.Abort;
import net.mailific.spf.SpfUtil;
import net.mailific.spf.policy.PolicySyntaxException;

public abstract class Macro implements Expandable {

  private static final BitSet unreserved = new BitSet(127);

  static {
    for (char c = 'A'; c <= 'Z'; c++) {
      unreserved.set(c);
    }
    for (char c = 'a'; c <= 'z'; c++) {
      unreserved.set(c);
    }
    for (char c = '0'; c <= '9'; c++) {
      unreserved.set(c);
    }
    unreserved.set('-');
    unreserved.set('.');
    unreserved.set('_');
    unreserved.set('~');
  }

  private final int rightParts;
  private final boolean reverse;
  private final String delimiter;
  private final boolean escape;

  public static Macro macro(String type, int rightParts, boolean reverse, String delimiter)
      throws PolicySyntaxException {
    String lcType = type.toLowerCase();
    boolean escape = !(type.equals(lcType));
    switch (lcType) {
      case "s":
        return new Sender(rightParts, reverse, delimiter, escape);
      case "l":
        return new SenderLocal(rightParts, reverse, delimiter, escape);
      case "o":
        return new SenderDomain(rightParts, reverse, delimiter, escape);
      case "d":
        return new Domain(rightParts, reverse, delimiter, escape);
      case "i":
        return new Ip(rightParts, reverse, delimiter, escape);
      case "p":
        return new NameOfIp(rightParts, reverse, delimiter, escape);
      case "h":
        return new Helo(rightParts, reverse, delimiter, escape);
      case "r":
        return new Receiver(rightParts, reverse, delimiter, escape);
      case "c":
        return new ReadableIp(rightParts, reverse, delimiter, escape);
      case "%":
      case "_":
      case "-":
        return new Escape(type);
      default:
        throw new PolicySyntaxException("Not a macro type: " + type);
    }
  }

  protected Macro(int rightParts, boolean reverse, String delimiter, boolean escape) {
    this.rightParts = rightParts;
    this.reverse = reverse;
    this.delimiter = delimiter;
    this.escape = escape;
  }

  public int getRightParts() {
    return rightParts;
  }

  public boolean isReverse() {
    return reverse;
  }

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

  public abstract String innerExpand(
      SpfUtil spf, InetAddress ip, String domain, String sender, String ehloParam) throws Abort;

  public String expand(SpfUtil spf, InetAddress ip, String domain, String sender, String ehloParam)
      throws Abort {
    String expanded = innerExpand(spf, ip, domain, sender, ehloParam);
    if (escape) {
      return urlEscape(expanded);
    } else {
      return expanded;
    }
  }

  public String urlEscape(String s) {
    boolean escaped = false;
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (unreserved.get(c)) {
        if (escaped) {
          sb.append(c);
        }
      } else {
        if (!escaped) {
          // First unreserved char. Bite the bullet.
          sb.append(s.substring(0, i));
          escaped = true;
        }
        sb.append('%').append(HEX[(c & 0xf0) >> 4]).append(HEX[c & 0xF]);
      }
    }
    if (escaped) {
      return sb.toString();
    }
    return s;
  }

  @Override
  public String toString() {
    return "Macro [rightParts="
        + rightParts
        + ", reverse="
        + reverse
        + ", delimiter="
        + delimiter
        + "]";
  }
}
