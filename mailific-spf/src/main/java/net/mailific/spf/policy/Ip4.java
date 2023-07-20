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
import net.mailific.spf.SpfUtil;

public class Ip4 implements Mechanism {
  private final String ip4Network;
  private final int cidrLength;

  public Ip4(String ip4Network, int cidrLength) {
    this.ip4Network = ip4Network;
    this.cidrLength = cidrLength;
  }

  public String toString() {
    return "ip4:" + ip4Network + (cidrLength > -1 ? "/" + cidrLength : "");
  }

  @Override
  public boolean causesLookup() {
    return false;
  }

  @Override
  public boolean matches(
      SpfUtil spf, InetAddress ip, String domain, String sender, String ehloParam) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'matches'");
  }
}
