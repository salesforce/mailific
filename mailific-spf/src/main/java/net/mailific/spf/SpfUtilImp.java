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
import net.mailific.spf.dns.DnsFail;
import net.mailific.spf.dns.InvalidName;
import net.mailific.spf.dns.NameNotFound;
import net.mailific.spf.dns.NameResolver;
import net.mailific.spf.dns.RuntimeDnsFail;
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

  private int lookupsUsed = 0;
  private int voidLookupsUsed = 0;
  private NameResolver resolver;
  private Settings settings;

  public SpfUtilImp(NameResolver resolver, Settings settings) {
    this.resolver = resolver;
    this.settings = settings;
  }

  public Result checkHost(InetAddress ip, String domain, String sender, String ehloParam) {
    try {
      verifyDomain(domain);
      String spfRecord = lookupSpfRecord(domain);
      SpfPolicy parser =
          new SpfPolicy(
              new ByteArrayInputStream(spfRecord.getBytes(StandardCharsets.US_ASCII)), "US-ASCII");
      parser.setExplainPrefix(settings.getExplainPrefix());
      Policy policy = parser.policy();
      Result result = null;
      for (Directive directive : policy.getDirectives()) {
        result = directive.evaluate(this, ip, domain, sender, ehloParam);
        if (result != null) {
          break;
        }
      }
      if (result == null && policy.getRedirect() != null) {
        incLookupCounter();
        String redirectDomain =
            policy.getRedirect().getDomainSpec().expand(this, ip, domain, sender, ehloParam);
        result = checkHost(ip, redirectDomain, sender, ehloParam);
        if (result.getCode() == ResultCode.None) {
          result = new Result(ResultCode.Permerror, result.getExplanation());
        }
      } else if (result != null
          && result.getCode() == ResultCode.Fail
          && policy.getExplanation() != null) {
        String explanation = policy.getExplanation().expand(this, ip, domain, sender, ehloParam);
        if (explanation != null) {
          result = new Result(result.getCode(), explanation);
        }
      }
      if (result == null) {
        result = new Result(ResultCode.Neutral, "No directives matched.");
      }
      return result;
    } catch (ParseException | PolicySyntaxException e) {
      return new Result(ResultCode.Permerror, "Invalid spf record syntax.");
    } catch (Abort e) {
      return e.result;
    }
  }

  private static boolean hasSpfVersion(String s) {
    return s != null && s.length() >= 6 && s.substring(0, 6).equalsIgnoreCase("v=spf1");
  }

  public String lookupSpfRecord(String domain) throws Abort {
    try {
      List<String> txtRecords = resolver.resolveTxtRecords(domain);
      txtRecords =
          txtRecords.stream().filter(SpfUtilImp::hasSpfVersion).collect(Collectors.toList());
      if (txtRecords.size() < 1) {
        throw new Abort(ResultCode.None, "No SPF record found for: " + domain);
      }
      if (txtRecords.size() > 1) {
        throw new Abort(ResultCode.Permerror, "Multiple SPF records found for: " + domain);
      }
      return txtRecords.get(0);
    } catch (NameNotFound e) {
      throw new Abort(ResultCode.None, "No SPF record found for: " + domain);
    } catch (InvalidName e) {
      throw new Abort(ResultCode.None, e.getMessage());
    } catch (DnsFail e) {
      throw new Abort(ResultCode.Temperror, e.getMessage());
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
    for (int i = 0; i < labels.length; i++) {
      if (labels[i].isEmpty() && i != labels.length - 1) {
        throw new Abort(ResultCode.None, "Domain contains 0-length label: " + domain);
      }
      if (labels[i].length() > 63) {
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
   * @throws DnsFail
   */
  @Override
  public String validatedHostForIp(InetAddress ip, String domain, boolean requireMatch)
      throws DnsFail, Abort {
    String ptrName = ptrName(ip);
    List<String> results = null;
    try {
      results = getNameResolver().resolvePtrRecords(ptrName);
    } catch (NameNotFound e) {
      // Do nothing -- leave results null
    }
    if (results == null || results.isEmpty()) {
      incVoidLookupCounter();
      return null;
    }
    String match = null;
    Set<String> subdomains = new HashSet<>();
    Set<String> fallbacks = new HashSet<>();
    int i = 0;
    for (String result : results) {
      if (++i > 10) {
        break;
      }
      if (result.equalsIgnoreCase(domain)) {
        match = result;
      } else if (result.endsWith(domain)) {
        subdomains.add(result);
      } else {
        fallbacks.add(result);
      }
    }
    try {
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
    } catch (RuntimeAbort e) {
      throw e.getAbort();
    }
  }

  private boolean nameHasIp(String name, InetAddress ip) throws RuntimeAbort {
    try {
      List<InetAddress> ips = getIpsByHostnameRte(name, ip instanceof Inet4Address);
      return ips.stream().anyMatch(i -> i.equals(ip));
    } catch (RuntimeDnsFail e) {
      // If a DNS error occurs while doing an A RR lookup,
      // then that domain name is skipped and the search continues.
      return false;
    }
  }

  public int incLookupCounter() throws Abort {
    if (++lookupsUsed > settings.getLookupLimit()) {
      throw new Abort(ResultCode.Permerror, "Maximum total DNS lookups exceeded.");
    }
    return settings.getLookupLimit() - lookupsUsed;
  }

  public int incVoidLookupCounter() throws Abort {
    if (++voidLookupsUsed > settings.getVoidLookupLimit()) {
      throw new Abort(ResultCode.Permerror, "Maximum DNS void lookups exceeded.");
    }
    return settings.getVoidLookupLimit() - voidLookupsUsed;
  }

  @Override
  public List<InetAddress> getIpsByMxName(String name, boolean ip4) throws DnsFail, Abort {
    List<String> names = null;
    try {
      names = getNameResolver().resolveMXRecords(name);
    } catch (NameNotFound e) {
      // Do nothing
    }
    if (names == null || names.isEmpty()) {
      incVoidLookupCounter();
      return Collections.emptyList();
    }

    Set<String> nameSet = new HashSet<>(names);
    if (nameSet.size() > settings.getLookupLimit()) {
      throw new Abort(ResultCode.Permerror, "More than 10 MX records for " + name);
    }
    try {
      return names.stream()
          .flatMap(n -> getIpsByHostnameRte(n, ip4).stream())
          .distinct()
          .collect(Collectors.toList());
    } catch (RuntimeDnsFail e) {
      throw e.getDnsFail();
    } catch (RuntimeAbort e) {
      throw e.getAbort();
    }
  }

  private List<InetAddress> getIpsByHostnameRte(String name, boolean ip4)
      throws RuntimeDnsFail, RuntimeAbort {
    try {
      return getIpsByHostname(name, ip4);
    } catch (DnsFail e) {
      throw new RuntimeDnsFail(e);
    } catch (Abort e) {
      throw new RuntimeAbort(e);
    }
  }

  public List<InetAddress> getIpsByHostname(String name, boolean ip4) throws DnsFail, Abort {
    List<InetAddress> rv = null;
    try {
      if (ip4) {
        rv = getNameResolver().resolveARecords(name);
      } else {
        rv = getNameResolver().resolveAAAARecords(name);
      }
    } catch (NameNotFound e) {
      // Do nothing
    }
    if (rv == null || rv.isEmpty()) {
      incVoidLookupCounter();
      return Collections.emptyList();
    }
    return rv;
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

  @Override
  public List<String> resolveTxtRecords(String name) throws DnsFail, Abort {
    List<String> rv = null;
    try {
      rv = getNameResolver().resolveTxtRecords(name);
    } catch (NameNotFound e) {
      // Do nothing
    }
    if (rv == null || rv.isEmpty()) {
      incVoidLookupCounter();
      return Collections.emptyList();
    }
    return rv;
  }

  @Override
  public String getHostDomain() {
    return settings.getHostDomain();
  }
}
