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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import net.mailific.spf.dns.DnsFail;
import net.mailific.spf.dns.NameNotFound;
import net.mailific.spf.dns.NameResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SpfUtilImplTest {

  @Mock NameResolver resolver;
  InetAddress ip;
  private SpfUtilImp it;

  private AutoCloseable mocks;

  @Before
  public void setup() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    ip = InetAddress.getByName("1.2.3.4");
    it = new SpfUtilImp(resolver, new Settings());
  }

  @After
  public void releaseMocks() throws Exception {
    mocks.close();
  }

  @Test
  public void cidrMatchIp4() throws Exception {
    assertCidrMatch("1.23.45.67", "1.23.255.255", 16, true);
    assertCidrMatch("1.23.45.67", "1.23.255.255", 17, false);
    assertCidrMatch("1.23.45.67", "2.23.255.255", 8, false);
    assertCidrMatch("1.23.45.67", "1.23.45.67", -1, true);
    assertCidrMatch("1.23.45.67", "1.23.255.255", -1, false);
    assertCidrMatch("1.23.45.67", "1.23.45.67", 32, true);
    assertCidrMatch("1.23.45.67", "1.23.45.67", 90, true);
    assertCidrMatch("1.23.45.67", "1.23.45.68", 32, false);
    assertCidrMatch("1.23.45.67", "1.23.45.68", 90, false);
    assertCidrMatch("1.23.45.67", "1.23.45.67", 0, true);
    assertCidrMatch("0.0.0.0", "255.255.255.255", 0, true);
  }

  @Test
  public void cidrMatchIp6() throws Exception {
    String ip1 = "1234:5678:90ab:cdef:1234:5678:90ab:cdef";
    assertCidrMatch(ip1, "1234:5678:90ab:cdef::", 64, true);
    assertCidrMatch("1234:5678:90ab:cdef:ff34:5678:90ab:cdef", "1234:5678:90ab:cdef::", 65, false);

    assertCidrMatch(ip1, "::1234:5678:90ab:cdef", 8, false);
    assertCidrMatch(ip1, "1:1:1:1:1:1:1:1", 0, true);
    assertCidrMatch(ip1, ip1, 128, true);
    assertCidrMatch(ip1, "1234:5678:90ab:cdef:1234:5678:90ab:cdee", 128, false);
    assertCidrMatch(ip1, ip1, -1, true);
    assertCidrMatch(ip1, "1234:5678:90ab:cdef:1234:5678:90ab:cdee", -1, false);
    assertCidrMatch(ip1, ip1, 222, true);
    assertCidrMatch(ip1, "1234:5678:90ab:cdef:1234:5678:90ab:cdee", 222, false);
  }

  void assertCidrMatch(String ip1, String ip2, int bits, boolean match) throws Exception {
    InetAddress address1 = InetAddress.getByName(ip1);
    InetAddress address2 = InetAddress.getByName(ip2);
    assertCidrMatch(address1, address2, bits, match);
  }

  void assertCidrMatch(byte[] ip1, byte[] ip2, int bits, boolean match) throws Exception {
    InetAddress address1 = InetAddress.getByAddress(ip1);
    InetAddress address2 = InetAddress.getByAddress(ip2);
    assertCidrMatch(address1, address2, bits, match);
  }

  void assertCidrMatch(InetAddress ip1, InetAddress ip2, int bits, boolean match) throws Exception {
    assertEquals(match, it.cidrMatch(ip1, ip2, bits));
  }

  @Test
  public void nullSpfRecord() throws NameNotFound, DnsFail {
    List<String> records = new ArrayList<>();
    records.add(null);
    when(resolver.resolveTxtRecords("foo.com")).thenReturn(records);
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.None, result.getCode());
  }

  @Test
  public void nullDomain() throws Exception {
    Result result = it.checkHost(ip, null, "sender@foo.com", "bar.baz");
    assertResult(ResultCode.None, "Null domain", result);
  }

  @Test
  public void bigDomain() throws Exception {
    String domain = "foo.";
    while (domain.length() < 252) {
      domain += domain;
    }
    domain += ".com";
    Result result = it.checkHost(ip, domain, "sender@foo.com", "bar.baz");
    assertResult(ResultCode.None, "Domain too long: " + domain, result);
  }

  private void assertResult(ResultCode code, String explanation, Result result) {
    assertEquals(code, result.getCode());
    assertEquals(explanation, result.getExplanation());
  }

  @Test
  public void zeroLengthLabel() {
    Result result = it.checkHost(ip, "foo..bar", "sender@foo..bar", "bar.baz");
    assertResult(ResultCode.None, "Domain contains 0-length label: foo..bar", result);
  }

  @Test
  public void finalDot() throws Exception {
    when(resolver.resolveTxtRecords("foo.bar.")).thenReturn(List.of("v=spf1 +all"));
    Result result = it.checkHost(ip, "foo.bar.", "sender@foo.bar", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }
}
