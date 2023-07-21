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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
      List<String> txtRecords = resolver.resolveTxtRecords(domain);
      txtRecords =
          txtRecords.stream()
              .filter(s -> s != null && (s.equals("v=spf1") || s.startsWith("v=spf1 ")))
              .collect(Collectors.toList());
      if (txtRecords.size() < 1) {
        throw new Abort(ResultCode.None, "No SPF record found for: " + domain);
      }
      if (txtRecords.size() > 1) {
        throw new Abort(ResultCode.Permerror, "Multiple SPF records found for: " + domain);
      }
      return txtRecords.get(0);
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
  public String validatedHostForIp(InetAddress ip, String domain, boolean requireMatch)
      throws NameResolutionException, NameNotFound, Abort {
    String ptrName = ptrName(ip);
    String[] results = getNameResolver().resolvePtrRecords(ptrName);
    if (results == null || results.length == 0) {
      return null;
    }
    String match = null;
    Set<String> subdomains = new HashSet<>();
    Set<String> fallbacks = new HashSet<>();
    for (String result : results) {
      if (result.equalsIgnoreCase(domain)) {
        match = result;
      } else if (result.endsWith(domain)) {
        subdomains.add(result);
      } else {
        fallbacks.add(result);
      }
    }
    if (match != null && nameHasIp(match, ip)) {
      return match;
    }
    match = subdomains.stream().filter(s -> nameHasIp(s, ip)).findAny().orElse(null);
    if (match != null) {
      return match;
    }
    if (requireMatch) {
      return null;
    }
    return fallbacks.stream().filter(s -> nameHasIp(s, ip)).findAny().orElse(null);
  }

  private boolean nameHasIp(String name, InetAddress ip) {
    try {
      List<InetAddress> ips = getIpsByHostname(name, ip instanceof Inet4Address);
      return ips.stream().anyMatch(i -> i.equals(ip));
    } catch (NameResolutionException e) {
      // If a DNS error occurs while doing an A RR lookup,
      // then that domain name is skipped and the search continues.
      return false;
    }
  }

  public int incLookupCounter() throws Abort {
    if (++lookupsUsed > lookupLimit) {
      throw new Abort(ResultCode.Permerror, "Maximum total DNS lookups exceeded.");
    }
    return lookupLimit - lookupsUsed;
  }

  @Override
  public List<InetAddress> getIpsByMxName(String name, boolean ip4)
      throws NameResolutionException, Abort {
    List<String> names;
    try {
      names = getNameResolver().resolveMXRecords(name);
    } catch (NameNotFound e) {
      return Collections.emptyList();
    }

    Set<String> nameSet = new HashSet<>(names);
    if (nameSet.size() > 10) {
      throw new Abort(ResultCode.Permerror, "More than 10 MX records for " + name);
    }
    try {
      return names.stream()
          .flatMap(
              (n) -> {
                try {
                  return getIpsByHostname(n, ip4).stream();
                } catch (NameResolutionException e) {
                  throw new RuntimeException(e);
                }
              })
          .distinct()
          .collect(Collectors.toList());
    } catch (RuntimeException e) {
      if (e.getCause() != null && e.getCause() instanceof NameResolutionException) {
        throw (NameResolutionException) e.getCause();
      }
      throw e;
    }
  }

  public List<InetAddress> getIpsByHostname(String name, boolean ip4)
      throws NameResolutionException {
    try {
      if (ip4) {
        return getNameResolver().resolveARecords(name);
      } else {
        return getNameResolver().resolveAAAARecords(name);
      }
    } catch (NameNotFound e) {
      return Collections.emptyList();
    }
  }

  private static final int[] MASKS = {
    0, 0b10000000, 0b11000000, 0b11100000, 0b11110000, 0b11111000, 0b11111100, 0b11111110
  };

  @Override
  public boolean cidrMatch(InetAddress ip1, InetAddress ip2, int bits) {
    // TODO: null checks
    if (bits < 0) {
      return ip1.equals(ip2);
    }
    byte[] ip1Bytes = ip1.getAddress();
    byte[] ip2Bytes = ip2.getAddress();
    if (ip1Bytes.length != ip2Bytes.length) {
      return false;
    }
    if (bits == 0) {
      return true;
    }
    if (bits > ip1Bytes.length * 8) {
      // TODO: probably should throw?
      return false;
    }
    int i = 0;
    while (bits >= 8) {
      if (ip1Bytes[i] != ip2Bytes[i]) {
        return false;
      }
      ++i;
      bits -= 8;
    }
    if (bits > 0) {
      int mask = MASKS[bits];
      if ((ip1Bytes[i] & mask) != (ip2Bytes[i] & mask)) {
        return false;
      }
    }
    return true;
  }
}
