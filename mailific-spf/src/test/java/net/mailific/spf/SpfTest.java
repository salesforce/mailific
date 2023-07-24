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

import java.net.InetAddress;
import net.mailific.spf.dns.InvalidName;
import net.mailific.spf.dns.NameNotFound;
import net.mailific.spf.dns.TempDnsFail;
import net.mailific.spf.test.MockDns;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class SpfTest {

  MockDns dns = new MockDns();
  Spf it;
  InetAddress ip;
  InetAddress ip2;

  InetAddress ip6;

  private AutoCloseable mocks;

  @Before
  public void setup() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    it = new SpfImp(dns, 10, 2);
    ip = InetAddress.getByName("1.2.3.4");
    ip2 = InetAddress.getByName("10.20.30.40");
    ip6 = InetAddress.getByName("1234:5678::90ab:cd3f");
  }

  @After
  public void releaseMocks() throws Exception {
    mocks.close();
  }

  @Test
  public void plusAll() throws Exception {
    dns.txt("foo.com", "v=spf1 +all");
    Result result = it.checkHost(ip, "foo.com", "joe@foo.com", "foo.com");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void minusAll() throws Exception {
    dns.txt("foo.com", "v=spf1 -all");
    Result result = it.checkHost(ip, "foo.com", "joe@foo.com", "foo.com");
    assertEquals(ResultCode.Fail, result.getCode());
  }

  @Test
  public void neutralAll() throws Exception {
    dns.txt("foo.com", "v=spf1 ?all");
    Result result = it.checkHost(ip, "foo.com", "joe@foo.com", "foo.com");
    assertEquals(ResultCode.Neutral, result.getCode());
  }

  @Test
  public void softfailAll() throws Exception {
    dns.txt("foo.com", "v=spf1 ~all");
    Result result = it.checkHost(ip, "foo.com", "joe@foo.com", "foo.com");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void defaultAll() throws Exception {
    dns.txt("foo.com", "v=spf1 all");
    Result result = it.checkHost(ip, "foo.com", "joe@foo.com", "foo.com");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  // 2.4
  @Test
  public void noSender() throws Exception {
    dns.txt("foo.com", "v=spf1 exists:%{l}.%{o} -all").a("postmaster.foo.com", "1.1.1.1");
    Result result = it.checkHost(ip, "foo.com", null, "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());

    it.checkHost(ip, "foo.com", "", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());

    it.checkHost(ip, "foo.com", " 	", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());

    it.checkHost(ip, "foo.com", "<>", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  // 4.3
  @Test
  public void badDomainsReturnNone() {
    String[] domains = {
      "0123456789012345678901234567890123456789012345678901234567891234.com",
      "foo..bar",
      "baz",
      "badsomehow.com"
    };
    dns.txt("badsomehow.com", new InvalidName("badsomehow.com", "Yuck."));
    for (String domain : domains) {
      dns.txt(domain, "v=spf1 all");
      Result result = it.checkHost(ip, domain, "sender@foo.com", "bar.baz");
      assertEquals(domain + " check should have returned None.", ResultCode.None, result.getCode());
    }
  }

  // 4.4
  @Test
  public void spfPolicyLookupErrors() {
    dns.txt("foo.com", new TempDnsFail("Something happened"));
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(result.getCode(), ResultCode.Temperror);
  }

  // 4.5
  @Test
  public void nameNotFound() {
    dns.txt("foo.com", new NameNotFound("foo.com"));
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(result.getCode(), ResultCode.None);
  }

  // 4.5
  @Test
  public void noTxtRecords() {
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(result.getCode(), ResultCode.None);
  }

  // 4.5
  @Test
  public void noSpfRecords() {
    dns.txt("foo.com", "foo").txt("foo.com", "v=spf2.0 -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(result.getCode(), ResultCode.None);
  }

  // 4.5
  @Test
  public void dosRecords() {
    dns.txt("foo.com", "v=spf1 -all").txt("foo.com", "v=spf1 ~all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  // 4.6
  @Test
  public void badSyntax() {
    dns.txt("foo.com", "v=spf1 some nonsense!!!");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  // 4.6.2
  @Test
  public void leftToRight() {
    dns.txt("foo.com", "v=spf1 -ip4:1.2.3.4 +all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
  }

  // 4.6.2
  // 4.7
  @Test
  public void noMatch() {
    dns.txt("foo.com", "v=spf1 ip4:1.2.3.8");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Neutral, result.getCode());
  }

  // 4.6.4
  @Test
  public void lookupLimitA() {
    dns.a("x1.foo.bar", "100.2.3.1")
        .a("x2.foo.bar", "100.2.3.2")
        .a("x3.foo.bar", "100.2.3.3")
        .a("x4.foo.bar", "100.2.3.4")
        .a("x5.foo.bar", "100.2.3.5")
        .a("x6.foo.bar", "100.2.3.6")
        .a("x7.foo.bar", "100.2.3.7")
        .a("x8.foo.bar", "100.2.3.8")
        .a("x9.foo.bar", "100.2.3.9")
        .a("x10.foo.bar", "100.2.3.10")
        .a("x11.foo.bar", "100.2.3.11")
        .txt(
            "foo.com",
            "v=spf1 a:x1.foo.bar a:x2.foo.bar a:x3.foo.bar a:x4.foo.bar a:x5.foo.bar "
                + "a:x6.foo.bar a:x7.foo.bar a:x8.foo.bar"
                + " a:x9.foo.bar a:x10.foo.bar a:x11.foo.bar");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  // 4.6.4
  @Test
  public void lookupLimitMx() {
    dns.a("x1.bar", "100.2.3.1")
        .a("x2.bar", "100.2.3.2")
        .a("x3.bar", "100.2.3.3")
        .a("x4.bar", "100.2.3.4")
        .a("x5.bar", "100.2.3.5")
        .a("x6.bar", "100.2.3.6")
        .a("x7.bar", "100.2.3.7")
        .a("x8.bar", "100.2.3.8")
        .a("x9.bar", "100.2.3.9")
        .a("x10.bar", "100.2.3.10")
        .a("x11.bar", "100.2.3.11")
        .mx("mx1.bar", "x1.bar")
        .mx("mx2.bar", "x2.bar")
        .mx("mx3.bar", "x3.bar")
        .mx("mx4.bar", "x4.bar")
        .mx("mx5.bar", "x5.bar")
        .mx("mx6.bar", "x6.bar")
        .mx("mx7.bar", "x7.bar")
        .mx("mx8.bar", "x8.bar")
        .mx("mx9.bar", "x9.bar")
        .mx("mx10.bar", "x10.bar")
        .mx("mx11.bar", "x11.bar")
        .txt(
            "foo.com",
            "v=spf1 mx:mx1.bar mx:mx2.bar mx:mx3.bar mx:mx4.bar mx:mx5.bar mx:mx6.bar mx:mx7.bar"
                + " mx:mx8.bar mx:mx9.bar mx:mx10.bar mx:mx11.bar");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  // 4.6.4
  @Test
  public void lookupLimitInclude() {
    dns.txt("foo.com", "v=spf1 include:x1.com")
        .txt("x1.com", "v=spf1 include:x2.com")
        .txt("x2.com", "v=spf1 include:x3.com")
        .txt("x3.com", "v=spf1 include:x4.com")
        .txt("x4.com", "v=spf1 include:x5.com")
        .txt("x5.com", "v=spf1 include:x6.com")
        .txt("x6.com", "v=spf1 include:x7.com")
        .txt("x7.com", "v=spf1 include:x8.com")
        .txt("x8.com", "v=spf1 include:x9.com")
        .txt("x9.com", "v=spf1 include:x10.com")
        .txt("x10.com", "v=spf1 include:x11.com")
        .txt("x11.com", "v=spf1 +all");

    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  @Test
  public void underLookupLimitInclude() {
    dns.txt("foo.com", "v=spf1 include:x1.com")
        .txt("x1.com", "v=spf1 include:x2.com")
        .txt("x2.com", "v=spf1 include:x3.com")
        .txt("x3.com", "v=spf1 include:x4.com")
        .txt("x4.com", "v=spf1 include:x5.com")
        .txt("x5.com", "v=spf1 include:x6.com")
        .txt("x6.com", "v=spf1 include:x7.com")
        .txt("x7.com", "v=spf1 include:x8.com")
        .txt("x8.com", "v=spf1 include:x9.com")
        .txt("x9.com", "v=spf1 include:x10.com")
        .txt("x10.com", "v=spf1 +all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  // 4.6.4
  @Test
  public void lookupLimitPtr() {
    dns.txt(
        "foo.com",
        "v=spf1 ptr:p1.bar ptr:p2.bar ptr:p3.bar ptr:p4.bar"
            + " ptr:p5.bar ptr:p6.bar ptr:p7.bar"
            + " ptr:p8.bar ptr:p9.bar ptr:p10.bar ptr:p11.bar");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  // 4.6.4
  @Test
  public void lookupLimitExists() {
    // You can't do 11 exists because of the 2-null-lookup limit,
    // So do 10 ptrs and an exist.
    dns.a("foo.bar", "1.2.3.200")
        .txt(
            "foo.com",
            "v=spf1 ptr:p1.bar ptr:p2.bar ptr:p3.bar ptr:p4.bar"
                + " ptr:p5.bar ptr:p6.bar ptr:p7.bar"
                + " ptr:p8.bar ptr:p9.bar ptr:p10.bar exists:foo.bar");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  // 4.6.4
  @Test
  public void lookupLimitRedirect() {
    // You can't do 11 exists because of the 2-null-lookup limit,
    // So do 10 ptrs and an exist.
    dns.txt("foo.bar", "v=spf1 +all")
        .txt(
            "foo.com",
            "v=spf1 ptr:p1.bar ptr:p2.bar ptr:p3.bar ptr:p4.bar"
                + " ptr:p5.bar ptr:p6.bar ptr:p7.bar"
                + " ptr:p8.bar ptr:p9.bar ptr:p10.bar redirect=foo.bar");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  // 4.6.4
  @Test
  public void lookupLimitMx_10As() {
    dns.mx("mx1.bar", "a1.bar")
        .mx("mx1.bar", "a2.bar")
        .mx("mx1.bar", "a3.bar")
        .mx("mx1.bar", "a4.bar")
        .mx("mx1.bar", "a5.bar")
        .mx("mx1.bar", "a6.bar")
        .mx("mx1.bar", "a7.bar")
        .mx("mx1.bar", "a8.bar")
        .mx("mx1.bar", "a9.bar")
        .mx("mx1.bar", "a10.bar")
        .a("a1.bar", "11.2.3.4")
        .a("a2.bar", "12.2.3.4")
        .a("a3.bar", "13.2.3.4")
        .a("a4.bar", "14.2.3.4")
        .a("a5.bar", "15.2.3.4")
        .a("a6.bar", "16.2.3.4")
        .a("a7.bar", "17.2.3.4")
        .a("a8.bar", "18.2.3.4")
        .a("a9.bar", "19.2.3.4")
        .a("a10.bar", "1.2.3.4")
        .txt("foo.com", "v=spf1 mx:mx1.bar -all");

    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  // 4.6.4
  @Test
  public void lookupLimitMx_11As() {
    dns.mx("mx1.bar", "a1.bar")
        .mx("mx1.bar", "a2.bar")
        .mx("mx1.bar", "a3.bar")
        .mx("mx1.bar", "a4.bar")
        .mx("mx1.bar", "a5.bar")
        .mx("mx1.bar", "a6.bar")
        .mx("mx1.bar", "a7.bar")
        .mx("mx1.bar", "a8.bar")
        .mx("mx1.bar", "a9.bar")
        .mx("mx1.bar", "a10.bar")
        .mx("mx1.bar", "a11.bar")
        .a("a1.bar", "11.2.3.4")
        .a("a2.bar", "12.2.3.4")
        .a("a3.bar", "13.2.3.4")
        .a("a4.bar", "14.2.3.4")
        .a("a5.bar", "15.2.3.4")
        .a("a6.bar", "16.2.3.4")
        .a("a7.bar", "17.2.3.4")
        .a("a8.bar", "18.2.3.4")
        .a("a9.bar", "19.2.3.4")
        .a("a10.bar", "1.2.3.4")
        .a("a11.bar", "1.2.3.5")
        .txt("foo.com", "v=spf1 mx:mx1.bar -all");

    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  // 4.6.4
  @Test
  public void lookupLimit_10_lookups_per_mx() {
    dns.mx("mx1.bar", "a1.bar")
        .mx("mx1.bar", "a2.bar")
        .mx("mx1.bar", "a3.bar")
        .mx("mx1.bar", "a4.bar")
        .mx("mx1.bar", "a5.bar")
        .mx("mx1.bar", "a6.bar")
        .a("a1.bar", "11.2.3.4")
        .a("a2.bar", "12.2.3.4")
        .a("a3.bar", "13.2.3.4")
        .a("a4.bar", "14.2.3.4")
        .a("a5.bar", "15.2.3.4")
        .a("a6.bar", "16.2.3.4")
        //
        .mx("mx2.bar", "b1.bar")
        .mx("mx2.bar", "b2.bar")
        .mx("mx2.bar", "b3.bar")
        .mx("mx2.bar", "b4.bar")
        .mx("mx2.bar", "b5.bar")
        .mx("mx2.bar", "b6.bar")
        .a("b1.bar", "11.2.3.4")
        .a("b2.bar", "12.2.3.4")
        .a("b3.bar", "13.2.3.4")
        .a("b4.bar", "14.2.3.4")
        .a("b5.bar", "15.2.3.4")
        .a("b6.bar", "16.2.3.4")
        //
        .txt("foo.com", "v=spf1 mx:mx2.bar mx:mx2.bar ptr +all");

    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  // 4.6.4
  @Test
  public void lookupLimit_10_ptrs() {
    dns.ptr("4.3.2.1.in-addr.arpa", "baz1.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz2.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz3.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz4.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz5.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz6.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz7.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz8.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz9.quux")
        .ptr("4.3.2.1.in-addr.arpa", "foo.com")
        .a("foo.com", "1.2.3.4")
        .txt("foo.com", "v=spf1 ptr -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  // 4.6.4
  @Test
  public void lookupLimit_11_ptrs_miss() {
    dns.ptr("4.3.2.1.in-addr.arpa", "baz1.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz2.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz3.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz4.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz5.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz6.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz7.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz8.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz9.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz10.quux")
        .ptr("4.3.2.1.in-addr.arpa", "foo.com")
        .a("foo.com", "1.2.3.4")
        .txt("foo.com", "v=spf1 ptr -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
  }

  // 4.6.4
  @Test
  public void lookupLimit_11_ptrs_hit() {
    dns.ptr("4.3.2.1.in-addr.arpa", "baz1.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz2.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz3.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz4.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz5.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz6.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz7.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz8.quux")
        .ptr("4.3.2.1.in-addr.arpa", "baz9.quux")
        .ptr("4.3.2.1.in-addr.arpa", "foo.com")
        .ptr("4.3.2.1.in-addr.arpa", "baz10.quux")
        .a("foo.com", "1.2.3.4")
        .txt("foo.com", "v=spf1 ptr -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  // 4.6.4
  @Test
  public void voidLookups_A_MX() {
    dns.txt("foo.com", "v=spf1 a:no1.com a:no2.com mx:no3.com +all")
        .a("no2.com", new NameNotFound("no2.com"));
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  // Void lookup with include does not need to be tested because
  // that's automatically a permerror.

  // 4.6.4
  @Test
  public void voidLookups_MX_subqueries() {
    dns.txt("foo.com", "v=spf1 mx +all")
        .mx("foo.com", "no1.com")
        .mx("foo.com", "no2.com")
        .mx("foo.com", "no3.com")
        .a("no2.com", new NameNotFound("no2.com"));
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  // 4.6.4
  @Test
  public void voidLookups_ptr_exists() {
    dns.txt("foo.com", "v=spf1 ptr exists:no1.com exists:no2.com +all")
        .a("no2.com", new NameNotFound("no2.com"));
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  // 4.6.4
  @Test
  public void voidLookups_ptr_subqueries() {
    dns.txt("foo.com", "v=spf1 ptr +all")
        .ptr("4.3.2.1.in-addr.arpa", "no1.foo.com")
        .ptr("4.3.2.1.in-addr.arpa", "no2.foo.com")
        .ptr("4.3.2.1.in-addr.arpa", "no3.foo.com")
        .a("no2.com", new NameNotFound("no2.com"));
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  // 4.6.4
  @Test
  public void voidLookups_recursive() {
    dns.txt("foo.com", "v=spf1 a:no1.com include:foo2.com +all")
        .txt("foo2.com", "v=spf1 a:no2.com include:foo3.com +all")
        .txt("foo3.com", "v=spf1 a:no3.com +all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  ////////////////////////////////////////////////////
  // 4.8 When no domain-spec provided, use <domain> //
  ////////////////////////////////////////////////////

  @Test
  public void aNoDs() {
    dns.txt("foo.com", "v=spf1 ~a -all").a("foo.com", "1.2.3.4");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void mxNoDs() {
    dns.txt("foo.com", "v=spf1 ~mx -all").mx("foo.com", "mx.foo.com").a("mx.foo.com", "1.2.3.4");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void ptrNoDs() {
    dns.txt("foo.com", "v=spf1 ~ptr -all")
        .ptr("4.3.2.1.in-addr.arpa", "foo.com")
        .a("foo.com", "1.2.3.4");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  // 5 - CIDR prefixes

  @Test
  public void cidr_ip4_match() {
    dns.txt("foo.com", "v=spf1 ~ip4:1.2.254.205/16 -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void cidr_ip4_no_match() {
    dns.txt("foo.com", "v=spf1 ~ip4:1.2.254.205/17 -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
  }

  @Test
  public void cidr_ip6_match() {
    dns.txt("foo.com", "v=spf1 ~ip6:1234:5678:1234::/32 -all");
    Result result = it.checkHost(ip6, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void cidr_ip6_no_match() {
    dns.txt("foo.com", "v=spf1 ~ip6:1234:5678:1234::/48 -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
  }

  /**
   * @Test public void cidr_mx_match() { dns.txt("foo.com", "v=spf1 ~mx:bar.baz/16 -all")
   * .mx("bar.baz", "baz.quux") .a("baz.quux", "1.2.254.205"); Result result = it.checkHost(ip,
   * "foo.com", "sender@foo.com", "bar.baz"); assertEquals(ResultCode.Softfail, result.getCode());
   * } @Test public void cidr_mx_no_match() { dns.txt("foo.com", "v=spf1 ~ip4:1.2.254.205/17 -all");
   * Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
   * assertEquals(ResultCode.Fail, result.getCode()); }
   */
  ///////////////////
  // 5. a vs aaaa  //
  ///////////////////

  @Test
  public void a_ip4() {
    dns.txt("foo.com", "v=spf1 ~a:baz.com -all").a("baz.com", "1.2.3.4");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }
}
