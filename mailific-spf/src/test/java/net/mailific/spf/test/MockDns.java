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

package net.mailific.spf.test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import net.mailific.spf.dns.DnsFail;
import net.mailific.spf.dns.NameNotFound;
import net.mailific.spf.dns.NameResolver;

public class MockDns implements NameResolver {

  Map<String, List<Object>> a = new HashMap<>();
  Map<String, List<Object>> aaaa = new HashMap<>();
  Map<String, List<Object>> txt = new HashMap<>();
  Map<String, List<Object>> mx = new HashMap<>();
  Map<String, List<Object>> ptr = new HashMap<>();

  public MockDns a(String name, Object ipOrException) {
    a.compute(dot(name), def).add(ipOrException);
    return this;
  }

  private BiFunction<String, List<Object>, List<Object>> def =
      (k, v) -> (v == null) ? new ArrayList<>() : v;

  public MockDns aaaa(String name, Object ipOrException) {
    aaaa.compute(dot(name), def).add(ipOrException);
    return this;
  }

  public MockDns txt(String name, Object stringOrException) {
    txt.compute(dot(name), def).add(stringOrException);
    return this;
  }

  public MockDns mx(String name, Object mxOrException) {
    mx.compute(dot(name), def).add(mxOrException);
    return this;
  }

  public MockDns ptr(String name, Object mxOrException) {
    ptr.compute(dot(name), def).add(mxOrException);
    return this;
  }

  @Override
  public List<String> resolveTxtRecords(String name) throws DnsFail, NameNotFound {
    return resolve(txt, name);
  }

  public List<String> resolve(Map<String, List<Object>> map, String name)
      throws DnsFail, NameNotFound {
    List<Object> things = map.get(dot(name));
    if (things == null) {
      return Collections.emptyList();
    }
    List<String> rv = new ArrayList<>();
    for (Object o : things) {
      throwIfException(o);
      rv.add(o.toString());
    }
    return rv;
  }

  private void throwIfException(Object o) throws DnsFail, NameNotFound {
    if (o instanceof DnsFail) {
      throw (DnsFail) o;
    }
    if (o instanceof NameNotFound) {
      throw (NameNotFound) o;
    }
    if (o instanceof RuntimeException) {
      throw (RuntimeException) o;
    }
    if (o instanceof Exception) {
      throw new RuntimeException((Exception) o);
    }
  }

  public List<InetAddress> resolveIp(Map<String, List<Object>> map, String name)
      throws DnsFail, NameNotFound {
    List<Object> things = map.get(dot(name));
    if (things == null) {
      return Collections.emptyList();
    }
    List<InetAddress> rv = new ArrayList<>();
    for (Object o : things) {
      throwIfException(o);
      if (o instanceof InetAddress) {
        rv.add((InetAddress) o);
      }
      try {
        rv.add(InetAddress.getByName(o.toString()));
      } catch (UnknownHostException e) {
        throw new RuntimeException("Can't get ip from " + o);
      }
    }
    return rv;
  }

  @Override
  public List<InetAddress> resolveARecords(String name) throws DnsFail, NameNotFound {
    return resolveIp(a, name);
  }

  @Override
  public List<InetAddress> resolveAAAARecords(String name) throws DnsFail, NameNotFound {
    return resolveIp(aaaa, name);
  }

  @Override
  public List<String> resolveMXRecords(String name) throws DnsFail, NameNotFound {
    return resolve(mx, name);
  }

  @Override
  public List<String> resolvePtrRecords(String name) throws DnsFail, NameNotFound {
    return resolve(ptr, name);
  }

  public String dot(String s) {
    return (s.endsWith(".")) ? s : s + ".";
  }
}
