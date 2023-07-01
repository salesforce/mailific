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
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.mailific.spf.dns.NameNotFound;
import net.mailific.spf.dns.NameResolutionException;
import net.mailific.spf.dns.NameResolver;

public class SpfImp implements Spf {

  private int lookupLimit = 10;
  private NameResolver resolver;

  public SpfImp(NameResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public Result checkHost(Inet4Address ip, String domain, String sender) {
    return checkHost(ip, domain, sender, lookupLimit, 0);
  }

  private Result checkHost(
      Inet4Address ip, String domain, String sender, int lookupLimit, int lookups) {
    try {
      verifyDomain(domain);
      String spfRecord = lookupSpfRecord(domain);

    } catch (Abort e) {
      return e.result;
    }

    return new Result(ResultCode.Permerror, "NOT IMPLEMENTED");
  }

  public String lookupSpfRecord(String domain) throws Abort {
    try {
      String[] txtRecords = resolver.resolveTxtRecords(domain);
      txtRecords =
          Stream.of(txtRecords)
              .filter(s -> s != null && (s.equals("v=spf1") || s.startsWith("v=spf1 ")))
              .toArray(String[]::new);
      if (txtRecords.length < 1) {
        throw new Abort(ResultCode.None, "No SPF record found for: " + domain);
      }
      if (txtRecords.length > 1) {
        throw new Abort(ResultCode.Permerror, "Multiple SPF records found for: " + domain);
      }
      return txtRecords[0];
    } catch (NameResolutionException e) {
      throw new Abort(ResultCode.Temperror, e.getMessage());
    } catch (NameNotFound e) {
      throw new Abort(ResultCode.None, "No SPF record found for: " + domain);
    }
  }

  private static final Pattern DOMAIN_CHARS_PATTERN = Pattern.compile("^[-A-Za-z0-9.]+$");

  /* Probably we could just skip this, or limit it to the small number of checks that
   * would not always result in a lookup failure.
   */
  private void verifyDomain(String domain) throws Abort {
    if (domain == null) {
      throw new Abort(ResultCode.None, "Null domain");
    }
    if (domain.length() > 255) {
      throw new Abort(ResultCode.None, "Domain too long: " + domain);
    }
    String[] labels = domain.split("[.]");
    if (labels.length < 2) {
      throw new Abort(ResultCode.None, "Domain not FQDN: " + domain);
    }
    for (String label : labels) {
      if (label.isEmpty()) {
        throw new Abort(ResultCode.None, "Domain contains 0-length label: " + domain);
      }
      if (label.length() > 63) {
        throw new Abort(ResultCode.None, "Domain label > 63 chars: " + domain);
      }
    }
    if (!DOMAIN_CHARS_PATTERN.matcher(domain).matches()) {
      throw new Abort(ResultCode.None, "Illegal domain characters: " + domain);
    }
  }

  private static class Abort extends Exception {

    Result result;

    Abort(ResultCode code, String detail) {
      super(detail);
      this.result = new Result(code, detail);
    }
  }
}
