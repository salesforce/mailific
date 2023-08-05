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

package net.mailific.spf.dns;

import java.net.InetAddress;
import java.util.List;

public interface NameResolver {

  /**
   * @param name The name to look up in DNS
   * @return A list of Strings. Each returned String corresponds to one TXT record found. If a TXT
   *     record had multiple Strings, those strings are concatenated, without adding any space
   *     between them, and the results added to the list as a single String.
   *     <p>If the name does not exist, or it has no txt records, an empty list will be returned.
   * @throws DnsFail
   */
  List<String> resolveTxtRecords(String name) throws DnsFail;

  List<InetAddress> resolveARecords(String name) throws DnsFail;

  List<InetAddress> resolveAAAARecords(String name) throws DnsFail;

  List<String> resolveMXRecords(String name) throws DnsFail;

  /**
   * @param name The name of the PTR record (e.g. 4.3.2.1.in-addr.arpa)
   */
  List<String> resolvePtrRecords(String name) throws DnsFail;
}
