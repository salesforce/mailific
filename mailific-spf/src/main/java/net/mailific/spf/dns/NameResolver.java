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

  List<String> resolveTxtRecords(String name) throws NameResolutionException, NameNotFound;

  List<InetAddress> resolveARecords(String name) throws NameResolutionException, NameNotFound;

  List<InetAddress> resolveAAAARecords(String name) throws NameResolutionException, NameNotFound;

  List<String> resolveMXRecords(String name) throws NameResolutionException, NameNotFound;

  /**
   * @param name The name of the PTR record (e.g. 4.3.2.1.in-addr.arpa)
   */
  String[] resolvePtrRecords(String name) throws NameResolutionException, NameNotFound;
}
