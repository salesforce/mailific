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

package net.mailific.spf;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.List;
import net.mailific.spf.dns.DnsFail;
import net.mailific.spf.dns.NameResolver;

public interface SpfUtil extends Spf {

  public static final char[] HEX = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };

  public static String ptrName(InetAddress ip) {
    StringBuilder sb = new StringBuilder();
    String[] bites = dotFormatIp(ip).split("[.]");
    for (int i = bites.length - 1; i >= 0; i--) {
      sb.append(bites[i]).append('.');
    }
    sb.append(ip instanceof Inet4Address ? "in-addr.arpa." : "ip6.arpa.");
    return sb.toString();
  }

  public static String dotFormatIp(InetAddress ip) {
    if (ip instanceof Inet4Address) {
      return ip.getHostAddress();
    }
    byte[] bytes = ip.getAddress();
    StringBuilder sb = new StringBuilder(62);
    sb.append(HEX[(bytes[0] & 0xf0) >> 4]).append('.').append(HEX[bytes[0] & 0xF]);
    for (int i = 1; i < bytes.length; i++) {
      sb.append('.').append(HEX[(bytes[i] & 0xf0) >> 4]).append('.').append(HEX[bytes[i] & 0xF]);
    }
    return sb.toString();
  }

  NameResolver getNameResolver();

  List<String> resolveTxtRecords(String name) throws DnsFail, Abort;

  String validatedHostForIp(InetAddress ip, String domain, boolean requireMatch)
      throws DnsFail, Abort;

  int incLookupCounter() throws Abort;

  /**
   * @return non-null
   */
  List<InetAddress> getIpsByHostname(String name, boolean ip4) throws DnsFail, Abort;

  /**
   * @return non-null
   */
  List<InetAddress> getIpsByMxName(String name, boolean ip4) throws DnsFail, Abort;

  boolean cidrMatch(InetAddress ip1, InetAddress ip2, int bits);

  String getHostDomain();
}
