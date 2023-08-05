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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

public class JndiResolver implements NameResolver {

  // TODO: IC provider for spring. Reuse ICs.

  private static final String DNSURL = "dns://%s";
  private static final String[] TXT = {"TXT"};
  private static final String[] A = {"A"};
  private static final String[] AAAA = {"AAAA"};
  private static final String[] MX = {"MX"};
  private static final String[] PTR = {"PTR"};

  private InitialDirContext ic;

  public JndiResolver() {
    this(null);
  }

  public JndiResolver(List<String> nameServers) {
    Hashtable<Object, Object> env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");

    if (nameServers != null && !nameServers.isEmpty()) {
      String dnsUrl =
          nameServers.stream().map(n -> String.format(DNSURL, n)).collect(Collectors.joining(" "));
      env.put(Context.PROVIDER_URL, dnsUrl);
    }

    try {
      ic = new InitialDirContext(env);
    } catch (NamingException e) {
      throw new RuntimeException("Failed to initialized JNDI DNS context.");
    }
  }

  @Override
  public List<String> resolveTxtRecords(String name) throws DnsFail {
    return resolve(name, TXT, JndiResolver::stripQuotes);
  }

  public <T> List<T> resolve(String name, String[] type, Function<Object, T> mapper)
      throws DnsFail {
    List<T> rv = new ArrayList<>();
    try {
      Attributes atts = ic.getAttributes(name, type);
      for (NamingEnumeration<? extends Attribute> all = atts.getAll(); all.hasMore(); ) {
        Attribute att = all.next();
        for (NamingEnumeration<?> values = att.getAll(); values.hasMore(); ) {
          rv.add(mapper.apply(values.next()));
        }
      }
    } catch (NameNotFoundException e) {
      // Do nothing -- return empty
    } catch (NamingException e) {
      mapToDnsFail(e, name);
    }
    return rv;
  }

  private static InetAddress mapFromLookup(Object o) {
    try {
      return InetAddress.getByName(String.valueOf(o));
    } catch (UnknownHostException e) {
      throw new ShouldNotOccur(String.valueOf(o));
    }
  }

  @Override
  public List<InetAddress> resolveARecords(String name) throws DnsFail {
    try {
      return resolve(name, A, JndiResolver::mapFromLookup);
    } catch (ShouldNotOccur e) {
      throw new DnsFail("Bad A record: " + e.getMessage());
    }
  }

  @Override
  public List<InetAddress> resolveAAAARecords(String name) throws DnsFail {
    try {
      return resolve(name, AAAA, JndiResolver::mapFromLookup);
    } catch (ShouldNotOccur e) {
      throw new DnsFail("Bad AAAA record: " + e.getMessage());
    }
  }

  @Override
  public List<String> resolveMXRecords(String name) throws DnsFail {
    return resolve(name, MX, o -> undot(o).split(" ")[1]);
  }

  @Override
  public List<String> resolvePtrRecords(String name) throws DnsFail {
    return resolve(name, PTR, JndiResolver::undot);
  }

  private void mapToDnsFail(NamingException namingException, String name) throws DnsFail {
    try {
      throw namingException;
    } catch (InvalidNameException e) {
      throw new InvalidName(name, e.getMessage());
    } catch (NamingException e) {
      String explanation = String.format("Failure looking up \"%s\": %s", name, e.getMessage());
      throw new TempDnsFail(explanation);
    }
  }

  private static final class ShouldNotOccur extends RuntimeException {

    public ShouldNotOccur(String name) {
      super(name);
    }
  }

  public static final String undot(Object hostname) {
    if (hostname == null) {
      return null;
    }
    String s = String.valueOf(hostname);
    return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
  }

  public static final String stripQuotes(Object o) {
    if (o == null) {
      return null;
    }
    String s = String.valueOf(o);
    if (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
      return s.substring(1, s.length() - 1);
    }
    return s;
  }
}
