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

import java.net.Inet4Address;
import java.net.InetAddress;
import net.mailific.spf.SpfUtil;

public class Arpa extends Macro {

  public Arpa(int rightParts, boolean reverse, String delimiter, boolean escape) {
    super(rightParts, reverse, delimiter, escape);
  }

  @Override
  public String innerExpand(
      SpfUtil spf, InetAddress ip, String domain, String sender, String ehloParam) {
    String arpa = ip instanceof Inet4Address ? "in-addr" : "ip6";
    return transform(arpa, getRightParts(), isReverse(), getDelimiter());
  }

  @Override
  public String toString() {
    return "%{v"
        + (getRightParts() > 0 ? getRightParts() : "")
        + (isReverse() ? "r" : "")
        + (getDelimiter() == null ? "" : getDelimiter())
        + "}";
  }
}
