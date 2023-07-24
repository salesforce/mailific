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

import java.net.InetAddress;
import java.util.List;
import net.mailific.spf.dns.DnsFail;
import net.mailific.spf.dns.NameNotFound;
import net.mailific.spf.dns.NameResolver;

public interface SpfUtil extends Spf {

  NameResolver getNameResolver();

  String dotFormatIp(InetAddress ip);

  List<String> resolveTxtRecords(String name) throws DnsFail, NameNotFound, Abort;

  String validatedHostForIp(InetAddress ip, String domain, boolean requireMatch)
      throws DnsFail, NameNotFound, Abort;

  int incLookupCounter() throws Abort;

  List<InetAddress> getIpsByHostname(String name, boolean ip4) throws DnsFail, Abort;

  List<InetAddress> getIpsByMxName(String name, boolean ip4) throws DnsFail, Abort;

  boolean cidrMatch(InetAddress ip1, InetAddress ip2, int bits);
}
