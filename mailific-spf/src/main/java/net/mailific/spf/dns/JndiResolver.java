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
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

public class JndiResolver implements NameResolver {

  // TODO: IC provider for spring. Reuse ICs.

  private static final String DNSURL = "dns://%s/example.com";
  private static final String[] TXT = {"TXT"};

  private InitialDirContext ic;

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
    try {
      Attributes atts = ic.getAttributes(name, TXT);

    } catch (NamingException e) {
      mapToDnsFail(e, name);
    }
    return null;
  }

  @Override
  public List<InetAddress> resolveARecords(String name) throws DnsFail, NameNotFound {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'resolveARecords'");
  }

  @Override
  public List<InetAddress> resolveAAAARecords(String name) throws DnsFail, NameNotFound {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'resolveAAAARecords'");
  }

  @Override
  public List<String> resolveMXRecords(String name) throws DnsFail, NameNotFound {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'resolveMXRecords'");
  }

  @Override
  public List<String> resolvePtrRecords(String name) throws DnsFail, NameNotFound {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'resolvePtrRecords'");
  }

  private void mapToDnsFail(NamingException namingException, String name) throws DnsFail {
    try {
      throw namingException;
    } catch (NameNotFoundException e) {
      throw new NameNotFound(name);
    } catch (InvalidNameException e) {
      throw new InvalidName(name, e.getMessage());
    } catch (NamingException e) {
      String explanation = String.format("Failure looking up \"%s\": %s", name, e.getMessage());
      throw new TempDnsFail(explanation);
    }
  }
}
