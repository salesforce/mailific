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
import net.mailific.spf.dns.NameResolver;

public class SpfImp implements Spf {

  private final NameResolver resolver;
  private final int lookupLimit;
  private int voidLookupLimit;

  public SpfImp(NameResolver resolver, int lookupLimit, int voidLookupLimit) {
    this.resolver = resolver;
    this.lookupLimit = lookupLimit;
    this.voidLookupLimit = voidLookupLimit;
  }

  // TODO: overload for sender/domain

  @Override
  public Result checkHost(InetAddress ip, String domain, String sender, String ehloParam) {
    if (sender == null || sender.isBlank() || sender.strip().equals("<>")) {
      sender = "postmaster@" + domain;
    }
    return new SpfUtilImp(resolver, lookupLimit, voidLookupLimit)
        .checkHost(ip, domain, sender, ehloParam);
  }
}
