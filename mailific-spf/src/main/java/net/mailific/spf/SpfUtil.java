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
import net.mailific.spf.dns.NameNotFound;
import net.mailific.spf.dns.NameResolutionException;
import net.mailific.spf.dns.NameResolver;

public interface SpfUtil extends Spf {

  // String expand(InetAddress ip, String domain, String sender);

  NameResolver getNameResolver();

  String dotFormatIp(InetAddress ip);

  // String ptrName(InetAddress ip);

  String validateIp(InetAddress ip, String domain)
      throws NameResolutionException, NameNotFound, Abort;

  public int incLookupCounter() throws Abort;
}
