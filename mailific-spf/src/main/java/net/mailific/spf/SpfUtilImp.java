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

import java.io.ByteArrayInputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.mailific.spf.dns.NameNotFound;
import net.mailific.spf.dns.NameResolutionException;
import net.mailific.spf.dns.NameResolver;
import net.mailific.spf.parser.ParseException;
import net.mailific.spf.parser.SpfPolicy;
import net.mailific.spf.policy.Directive;
import net.mailific.spf.policy.Policy;
import net.mailific.spf.policy.PolicySyntaxException;

/** Stateful class that tracks the current SPF check and provides needed utilities. */
public class SpfUtilImp implements SpfUtil {

  public static final char[] HEX = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };

  private int lookupLimit = 10;
  private int lookupsUsed = 0;

  private NameResolver resolver;

  // String expand(InetAddress ip, String domain, String sender);

  public Result checkHost(InetAddress ip, String domain, String sender, String ehloParam) {

    try {
      verifyDomain(domain);
      String spfRecord = lookupSpfRecord(domain);
      SpfPolicy parser =
          new SpfPolicy(
              new ByteArrayInputStream(spfRecord.getBytes(StandardCharsets.US_ASCII)), "US_ASCII");
      Policy policy = parser.policy();
      for (Directive directive : policy.getDirectives()) {
        Result result = directive.evaluate(this, ip, domain, sender, ehloParam);
        if (result != null) {
          // TODO exp
          return result;
        }
      }
    } catch (ParseException | PolicySyntaxException e) {
      return new Result(ResultCode.Permerror, "Invalid spf record syntax.");
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

  @Override
  public NameResolver getNameResolver() {
    return resolver;
  }

  @Override
  public String dotFormatIp(InetAddress ip) {
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

  // @Override
  public String ptrName(InetAddress ip) {
    StringBuilder sb = new StringBuilder();
    String[] bites = dotFormatIp(ip).split("[.]");
    for (int i = bites.length - 1; i >= 0; i--) {
      sb.append(bites[i]).append('.');
    }
    sb.append(ip instanceof Inet4Address ? "in-addr.arpa." : "ip6.arpa.");
    return sb.toString();
  }

  /**
   * @param ip
   * @param domain
   * @return null if not validated
   * @throws NameNotFound
   * @throws NameResolutionException
   */
  @Override
  public String validateIp(InetAddress ip, String domain)
      throws NameResolutionException, NameNotFound, Abort {
    incLookupCounter();
    String ptrName = ptrName(ip);
    String[] results = getNameResolver().resolvePtrRecords(ptrName);
    if (results == null || results.length == 0) {
      return null;
    }
    String candidate = null;
    for (String result : results) {
      if (result.equalsIgnoreCase(domain)) {
        return result;
      }
      if (candidate == null && result.endsWith(domain)) {
        candidate = result;
      }
    }
    if (candidate == null) {
      candidate = results[0];
    }
    return candidate;
  }

  public int incLookupCounter() throws Abort {
    if (++lookupsUsed > lookupLimit) {
      throw new Abort(ResultCode.Permerror, "Maximum total DNS lookups exceeded.");
    }
    return lookupLimit - lookupsUsed;
  }
}
