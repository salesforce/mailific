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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import net.mailific.spf.dns.DnsFail;
import net.mailific.spf.dns.InvalidName;
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
    it = new SpfImp(dns, new Settings(10, 2, "rcpt.com", "%{d} explained: "));
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
  public void noTxtRecords() {
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.None, result.getCode());
  }

  // 4.5
  @Test
  public void noSpfRecords() {
    dns.txt("foo.com", "foo").txt("foo.com", "v=spf2.0 -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.None, result.getCode());
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

  // 4.6.4, 5.5 If [limit is] exceeded, processing is terminated and
  // the mechanism does not match.
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
    dns.txt("foo.com", "v=spf1 a:no1.com a:no2.com mx:no3.com +all");
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
        .mx("foo.com", "no3.com");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  // 4.6.4
  @Test
  public void voidLookups_ptr_exists() {
    dns.txt("foo.com", "v=spf1 ptr exists:no1.com exists:no2.com +all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  // 4.6.4
  @Test
  public void voidLookups_ptr_subqueries() {
    dns.txt("foo.com", "v=spf1 ptr +all")
        .ptr("4.3.2.1.in-addr.arpa", "no1.foo.com")
        .ptr("4.3.2.1.in-addr.arpa", "no2.foo.com")
        .ptr("4.3.2.1.in-addr.arpa", "no3.foo.com");
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

  @Test
  public void ptrNotFound() {
    dns.txt("foo.com", "v=spf1 ~ptr -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
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

  @Test
  public void cidr_mx_match() {
    dns.txt("foo.com", "v=spf1 ~mx:bar.baz/16 -all")
        .mx("bar.baz", "baz.quux")
        .a("baz.quux", "1.2.254.205");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void cidr_mx_no_match() {
    dns.txt("foo.com", "v=spf1 ~mx:bar.baz/17 -all")
        .mx("bar.baz", "baz.quux")
        .a("baz.quux", "1.2.254.205");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
  }

  @Test
  public void cidr_zero() {
    dns.txt("foo.com", "v=spf1 ~ip4:100.1.1.0/0 -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void cidr_ip4_oor() {
    dns.txt("foo.com", "v=spf1 ~ip4:1.2.254.205/60 -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  @Test
  public void cidr_ip6_oor() {
    dns.txt("foo.com", "v=spf1 ~ip6:1234:5678:1234::/190 -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  @Test
  public void cidr_a_oor() {
    dns.txt("foo.com", "v=spf1 a:foo.bar/50 -all").a("foo.bar", ip);
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  ///////////////////
  // 5. a vs aaaa  //
  ///////////////////

  @Test
  public void a_ip4() {
    dns.txt("foo.com", "v=spf1 ~a:baz.com -all").a("baz.com", "1.2.3.4");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void a_ip6() {
    dns.txt("foo.com", "v=spf1 ~a:baz.com -all").aaaa("baz.com", ip6);
    Result result = it.checkHost(ip6, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void mx_ip4() {
    dns.txt("foo.com", "v=spf1 ~mx:baz.com -all")
        .mx("baz.com", "mail.baz.com")
        .a("mail.baz.com", ip);
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void mx_ip6() {
    dns.txt("foo.com", "v=spf1 ~mx:baz.com -all")
        .mx("baz.com", "mail.baz.com")
        .aaaa("mail.baz.com", ip6);
    Result result = it.checkHost(ip6, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void ptr_ip4() {
    dns.txt("foo.com", "v=spf1 ~ptr -all").ptr("4.3.2.1.in-addr.arpa", "foo.com").a("foo.com", ip);
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void ptr_ip6() {
    dns.txt("foo.com", "v=spf1 ~ptr -all")
        .ptr("f.3.d.c.b.a.0.9.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.7.6.5.4.3.2.1.ip6.arpa", "foo.com")
        .aaaa("foo.com", ip6);
    Result result = it.checkHost(ip6, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  ////////////////
  // DNS Errors //
  ////////////////

  @Test
  public void include_dnsError() {
    dns.txt("foo.com", "v=spf1 ~include:bar.com -all")
        .txt("bar.com", new DnsFail("Some DNS error"));

    Result result = it.checkHost(ip6, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Temperror, result.getCode());
  }

  @Test
  public void a_dnsError() {
    dns.txt("foo.com", "v=spf1 ~a:bar.com -all").a("bar.com", new DnsFail("Some DNS error"));

    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Temperror, result.getCode());
  }

  @Test
  public void exists_dnsError() {
    dns.txt("foo.com", "v=spf1 ~exists:bar.com -all").a("bar.com", new DnsFail("Some DNS error"));
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Temperror, result.getCode());
  }

  @Test
  public void mx_dnsError() {
    dns.txt("foo.com", "v=spf1 ~mx:bar.com -all").mx("bar.com", new DnsFail("Some DNS error"));
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Temperror, result.getCode());
  }

  @Test
  public void mx_notFound() {
    dns.txt("foo.com", "v=spf1 ~mx:bar.com -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
  }

  @Test
  public void mx_subquery_dnsError() {
    dns.txt("foo.com", "v=spf1 ~mx:bar.com -all")
        .mx("bar.com", "mail.bar.com")
        .a("mail.bar.com", new DnsFail("Some DNS error"));
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Temperror, result.getCode());
  }

  // 5.5  DNS error during PTR lookup = no match
  @Test
  public void ptr_dnsError() {
    dns.txt("foo.com", "v=spf1 ~ptr -all")
        .ptr("4.3.2.1.in-addr.arpa", new DnsFail("Some DNS error"));
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
  }

  // 5.5 DNS error during A subquery ... continue
  @Test
  public void ptr_subquery_dnsError() {
    dns.txt("foo.com", "v=spf1 ~ptr -all")
        .ptr("4.3.2.1.in-addr.arpa", "x.foo.com")
        .ptr("4.3.2.1.in-addr.arpa", "y.foo.com")
        .a("x.foo.com", new DnsFail("Some DNS error"))
        .a("y.foo.com", "1.2.3.4");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  // 5 if the DNS server returns an error ... the topmost check_host()
  // returns "temperror".
  @Test
  public void dnsErrorBubblesUp() {
    dns.txt("foo.com", "v=spf1 include:foo1.com -all")
        .txt("foo1.com", "v=spf1 include:foo2.com -all")
        .txt("foo2.com", "v=spf1 ip4:4.3.2.1 redirect=foo3.com")
        .txt("foo3.com", "v=spf1 a:quux.com -all")
        .a("quux.com", new DnsFail("Some DNS error"));

    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Temperror, result.getCode());
  }

  // 5.1 -- mechanisms after all never tested
  @Test
  public void mechanismsAfterAllNeverTested() {
    dns.txt("foo.com", "v=spf1 ~all +a:bar.com").a("bar.com", new RuntimeException("boom"));

    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  // 5.1
  @Test
  public void redirectIgnoredIfAllPresent() {
    dns.txt("foo.com", "v=spf1 redirect=bar.com -all").txt("bar.com", "v=spf1 +ip4:1.2.3.4");

    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
  }

  /////////////////
  // 5.2 Include //
  /////////////////

  @Test
  public void include_domainSpecExpanded() {
    dns.txt("foo.com", "v=spf1 ~include:bar.%{d} -all").txt("bar.foo.com", "v=spf1 +all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void include_domainSpecBecomesDomain() {
    dns.txt("foo.com", "v=spf1 ~include:bar.com -all")
        .txt("bar.com", "v=spf1 a:%{dr} -all")
        .a("com.bar", ip);
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void include_senderRemainsSame() {
    dns.txt("foo.com", "v=spf1 ~include:bar.com -all")
        .txt("bar.com", "v=spf1 a:%{o}.%{l} -all")
        .a("foo.com.sender", ip);
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  // Also tests that included pass is a match
  @Test
  public void include_defaultResult() {
    dns.txt("foo.com", "v=spf1 include:bar.com -all").txt("bar.com", "v=spf1 +all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void include_resultIsQualifierNotIncludedResult() {
    dns.txt("foo.com", "v=spf1 -include:bar.com +all").txt("bar.com", "v=spf1 +all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
  }

  @Test
  public void include_failIsNoMatch() {
    dns.txt("foo.com", "v=spf1 +include:bar.com ~all").txt("bar.com", "v=spf1 -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void include_softfailIsNoMatch() {
    dns.txt("foo.com", "v=spf1 +include:bar.com ~all").txt("bar.com", "v=spf1 ~all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void include_neutralIsNoMatch() {
    dns.txt("foo.com", "v=spf1 +include:bar.com ~all").txt("bar.com", "v=spf1 ?all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void include_includedTemperrorIsTemperror() {
    dns.txt("foo.com", "v=spf1 +include:bar.com ~all")
        .txt("bar.com", "v=spf1 a:baz.com +all")
        .a("baz.com", new DnsFail("Some DNS error"));

    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Temperror, result.getCode());
  }

  @Test
  public void include_includedPermerrorIsPermerror() {
    dns.txt("foo.com", "v=spf1 +include:bar.com ~all")
        .txt("bar.com", "v=spf1 include:baz.com +all")
        .txt("baz.com", "v=spf1 bad syntax.");

    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  @Test
  public void include_includedNoneIsPermerror() {
    dns.txt("foo.com", "v=spf1 +include:bar.com ~all")
        .txt("bar.com", "v=spf1 include:baz.com +all");

    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  @Test
  public void include_nonHostname() {
    dns.txt("foo.com", "v=spf1 +include:_spf.bar.com ~all").txt("_spf.bar.com", "v=spf1 +all");

    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void domainWithEndDot() {
    dns.txt("foo.com.", "v=spf1 +all");
    Result result = it.checkHost(ip, "foo.com.", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  ///////////
  // 5.3 a //
  ///////////

  @Test
  public void multipleARecords() {
    dns.txt("foo.com", "v=spf1 a -all")
        .a("foo.com", "1.1.1.1")
        .a("foo.com", ip)
        .a("foo.com", "5.5.5.5");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void multipleAAAARecords() {
    dns.txt("foo.com", "v=spf1 a -all")
        .aaaa("foo.com", "ab:cd::ef:12")
        .aaaa("foo.com", ip6)
        .aaaa("foo.com", "fedc:ba98::7654:3210");
    Result result = it.checkHost(ip6, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  ////////////
  // 5.4 MX //
  ////////////
  @Test
  public void mxWithMultipleARecords() {
    dns.txt("foo.com", "v=spf1 mx -all")
        .mx("foo.com", "mail.foo.com")
        .a("mail.foo.com", "1.1.1.1")
        .a("mail.foo.com", ip)
        .a("mail.foo.com", "5.5.5.5");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void mxNoImplicitLookup() {
    dns.txt("foo.com", "v=spf1 mx:bar.com -all").a("bar.com", ip);
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
  }

  // 5.6
  @Test
  public void ip4WithIp6Addr() {
    dns.txt("foo.com", "v=spf1 ip4:abcd::1234 -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  @Test
  public void ip6WithIp4Addr() {
    dns.txt("foo.com", "v=spf1 ip6:1.2.3.4 -all");
    Result result = it.checkHost(ip6, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  @Test
  public void ip4IpIp6Mechanism() {
    dns.txt("foo.com", "v=spf1 ip6:abcd::1234 -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
  }

  @Test
  public void noOmittedQuads() {
    dns.txt("foo.com", "v=spf1 ip4:10.10.100 -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  // 5.7 - exists
  @Test
  public void aLookupEvenWhenIpIsIp6() {
    dns.txt("foo.com", "v=spf1 exists:bar.baz -all").a("bar.baz", "127.0.0.1");
    Result result = it.checkHost(ip6, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  // 6

  // when executing a "redirect" modifier, an "exp" modifier from the original domain MUST NOT be
  // used
  @Test
  public void expBeforeRedirect() {
    dns.txt("foo.com", "v=spf1 exp=bar.com redirect=baz.com")
        .txt("bar.com", "Because I said so.")
        .txt("baz.com", "v=spf1 -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
    assertEquals("Matched -all.", result.getExplanation());
  }

  @Test
  public void redirectBeforeExp() {
    dns.txt("foo.com", "v=spf1 redirect=baz.com exp=bar.com")
        .txt("bar.com", "Because I said so.")
        .txt("baz.com", "v=spf1 -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
    assertEquals("Matched -all.", result.getExplanation());
  }

  @Test
  public void modifiersMixedIn() {
    dns.txt("foo.com", "v=spf1 ip4:8.8.8.8 redirect=baz.com ip4:2.4.8.3 exp=bar.com ip4:9.9.9.9")
        .txt("bar.com", "Because I said so.")
        .txt("baz.com", "v=spf1 -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
    assertEquals("Matched -all.", result.getExplanation());
  }

  @Test
  public void twoExps() {
    dns.txt("foo.com", "v=spf1 exp=bar.com exp=baz.com -all")
        .txt("bar.com", "Because I said so.")
        .txt("baz.com", "Two exps not allowed");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void twoRedirects() {
    dns.txt("foo.com", "v=spf1 redirect=bar.com redirect=baz.com -all")
        .txt("bar.com", "v=spf1 ?all")
        .txt("baz.com", "v=spf1 +all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void unknownModifiers() {
    dns.txt("foo.com", "v=spf1 a9-_.=bar%{l} ~all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Softfail, result.getCode());
  }

  @Test
  public void unknownModifiersRepeated() {
    dns.txt("foo.com", "v=spf1 a9=bar a9=bar ip4:1.2.3.4 ~all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  // 6.1
  @Test
  public void redirectStringBecomesDomain() {
    dns.txt("foo.com", "v=spf1 redirect=bar.com")
        .txt("bar.com", "v=spf1 a:%{i}.%{o}.%{l}.%{d} -all")
        .a("1.2.3.4.foo.com.sender.bar.com", "1.2.3.4")
        .a("1.2.3.4.foo.com.sender.bar.foo", "9.9.9.9");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void redirectNoRecordPermError() {
    dns.txt("foo.com", "v=spf1 redirect=bar.com");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  @Test
  public void stackedRedirect() {
    dns.txt("foo.com", "v=spf1 redirect=bar.com")
        .txt("bar.com", "v=spf1 redirect=baz.com")
        .txt("baz.com", "v=spf1 ?all");

    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Neutral, result.getCode());
  }

  // 6.2
  @Test
  public void expMacroExpanded() {
    dns.txt("foo.com", "v=spf1 exp=%{l}.com -all").txt("sender.com", "Because I said so.");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
    assertEquals("foo.com explained: Because I said so.", result.getExplanation());
  }

  @Test
  public void expDnsError() {
    dns.txt("foo.com", "v=spf1 exp=%{l}.com -all")
        .txt("sender.com", new DnsFail("something happened"));
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
    assertEquals("Matched -all.", result.getExplanation());
  }

  @Test
  public void expNoRecords() {
    dns.txt("foo.com", "v=spf1 exp=%{l}.com -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
    assertEquals("Matched -all.", result.getExplanation());
  }

  @Test
  public void expTwoRecords() {
    dns.txt("foo.com", "v=spf1 exp=%{l}.com -all")
        .txt("sender.com", "foo")
        .txt("sender.com", "bar");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
    assertEquals("Matched -all.", result.getExplanation());
  }

  @Test
  public void expResultExpanded() {
    dns.txt("foo.com", "v=spf1 exp=bar.com -all").txt("bar.com", "No such mailbox %{l} at %{d}.");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
    assertEquals("foo.com explained: No such mailbox sender at foo.com.", result.getExplanation());
  }

  // 6.2
  @Test
  public void includedExpNotUsed() {
    dns.txt("foo.com", "v=spf1 -include:baz.com -all")
        .txt("baz.com", "v=spf1 exp=bar.com -all")
        .txt("bar.com", "Nope.");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
    assertEquals("Matched -all.", result.getExplanation());
  }

  @Test
  public void redirectWithExp() {
    dns.txt("foo.com", "v=spf1 exp=bar.com redirect=baz.com")
        .txt("baz.com", "v=spf1 -all")
        .txt("bar.com", "Nope.");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
    assertEquals("Matched -all.", result.getExplanation());
  }

  // TODO syntax tests based on bnf and comments in bnf

  // 7.2
  @Test
  public void cNotAllowedInInclude() {
    dns.txt("foo.com", "v=spf1 include:%{c}.foo.com -all").txt("1.2.3.4.foo.com", "v=spf1 +all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void cNotAllowedInA() {
    dns.txt("foo.com", "v=spf1 a:%{c}.foo.com -all").a("1.2.3.4.foo.com", ip);
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void cNotAllowedInMx() {
    dns.txt("foo.com", "v=spf1 mx:%{c}.foo.com -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void cNotAllowedInPtr() {
    dns.txt("foo.com", "v=spf1 ptr:%{c}.foo.com -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void cNotAllowedInExists() {
    dns.txt("foo.com", "v=spf1 exists:%{c}.foo.com -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void cNotAllowedInRedirect() {
    dns.txt("foo.com", "v=spf1 redirect=%{c}.foo.com -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void cNotAllowedInExpModifier() {
    dns.txt("foo.com", "v=spf1 exp=%{c}.foo.com -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void cAllowedInExpText() {
    dns.txt("foo.com", "v=spf1 exp=exp.foo.com -all").txt("exp.foo.com", "%{c} says no.");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
    assertEquals("foo.com explained: 1.2.3.4 says no.", result.getExplanation());
  }

  @Test
  public void cIpv6() {
    dns.txt("foo.com", "v=spf1 exp=exp.foo.com -all").txt("exp.foo.com", "%{c} says no.");
    Result result = it.checkHost(ip6, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
    assertEquals(
        "foo.com explained: 1234:5678:0:0:0:0:90ab:cd3f says no.", result.getExplanation());
  }

  @Test
  public void rNotAllowedInInclude() {
    dns.txt("foo.com", "v=spf1 include:%{r}.foo.com -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void rNotAllowedInA() {
    dns.txt("foo.com", "v=spf1 a:%{r} -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void rNotAllowedInMx() {
    dns.txt("foo.com", "v=spf1 mx:%{r} -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void rNotAllowedInPtr() {
    dns.txt("foo.com", "v=spf1 ptr:%{r} -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void rNotAllowedInExists() {
    dns.txt("foo.com", "v=spf1 exists:%{r} -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void rNotAllowedInRedirect() {
    dns.txt("foo.com", "v=spf1 redirect=%{r}.com -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void rNotAllowedInExpModifier() {
    dns.txt("foo.com", "v=spf1 exp=%{r}.com -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void rAllowedInExpText() {
    dns.txt("foo.com", "v=spf1 exp=exp.foo.com -all").txt("exp.foo.com", "%{r} says no.");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
    assertEquals("foo.com explained: rcpt.com says no.", result.getExplanation());
  }

  @Test
  public void tNotAllowedInInclude() {
    dns.txt("foo.com", "v=spf1 include:t%{t}.foo.com -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void tNotAllowedInA() {
    dns.txt("foo.com", "v=spf1 a:t%{t}.com -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void tNotAllowedInMx() {
    dns.txt("foo.com", "v=spf1 mx:t%{t}.com -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void tNotAllowedInPtr() {
    dns.txt("foo.com", "v=spf1 ptr:t%{t}.com -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void tNotAllowedInExists() {
    dns.txt("foo.com", "v=spf1 exists:t%{t}.com -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void tNotAllowedInRedirect() {
    dns.txt("foo.com", "v=spf1 redirect=t%{t}.com -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void tNotAllowedInExpModifier() {
    dns.txt("foo.com", "v=spf1 exp=t%{t}.com -all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
    assertEquals("Invalid spf record syntax.", result.getExplanation());
  }

  @Test
  public void tAllowedInExpText() {
    dns.txt("foo.com", "v=spf1 exp=exp.foo.com -all").txt("exp.foo.com", "not at %{t}.");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Fail, result.getCode());
    assertThat(result.getExplanation(), matchesPattern("foo.com explained: not at [0-9]+[.]"));
  }

  // 7.3
  @Test
  public void percentNotFollowedByBrace() {
    dns.txt("foo.com", "v=spf1 -exists:%(ir).sbl.example.org");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Permerror, result.getCode());
  }

  @Test
  public void delimSplitsJoinsWithDots() {
    dns.txt("foo.com", "v=spf1 exists:%{l+} -all").a("a.b.com", "9.9.9.9");
    Result result = it.checkHost(ip, "foo.com", "a+b+com@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void digitsAlone() {
    dns.txt("foo.bar.com", "v=spf1 exists:%{d2} -all").a("bar.com", "9.9.9.9");
    Result result = it.checkHost(ip, "foo.bar.com", "sender@foo.bar.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void digitsAndReverse() {
    dns.txt("foo.bar.com", "v=spf1 exists:%{d2r} -all").a("bar.foo", "9.9.9.9");
    Result result = it.checkHost(ip, "foo.bar.com", "sender@foo.bar.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void digitsReverseAndDelim() {
    dns.txt("foo.com", "v=spf1 exists:%{l2r-} -all").a("bar.foo", "9.9.9.9");
    Result result = it.checkHost(ip, "foo.com", "foo-bar-com@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void digitsAndDelim() {
    dns.txt("foo.com", "v=spf1 exists:%{l2-} -all").a("bar.com", "9.9.9.9");
    Result result = it.checkHost(ip, "foo.com", "foo-bar-com@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void reverseAlone() {
    dns.txt("foo.com", "v=spf1 exists:%{dr} -all").a("com.foo", "9.9.9.9");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void reverseAndDelim() {
    dns.txt("foo.com", "v=spf1 exists:%{lr-} -all").a("com.foo.bar", "9.9.9.9");
    Result result = it.checkHost(ip, "foo.com", "bar-foo-com@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void multidigits() {
    String l = "";
    for (int i = 0; i < 127; i++) {
      l += SpfUtilImp.HEX[i % 16];
      if (i < 126) {
        l += ".";
      }
    }
    dns.txt("foo.com", "v=spf1 exists:%{l128} -all").a(l, "9.9.9.9");
    Result result = it.checkHost(ip, "foo.com", l + "@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void multiDelims() {
    dns.txt("foo.com", "v=spf1 exists:%{l.+} -all").a("foo.bar.baz", "9.9.9.9");
    Result result = it.checkHost(ip, "foo.com", "foo+bar.baz@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void delimsEverywhere() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all").txt("exp.com", "%{l/}");
    Result result = it.checkHost(ip, "foo.com", "//foo///bar/baz/@foo.com", "bar.baz");
    assertEquals("foo.com explained: ..foo...bar.baz.", result.getExplanation());
  }

  @Test
  public void digitAfterReversal() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all").txt("exp.com", "%{l2r}");
    Result result = it.checkHost(ip, "foo.com", "a.b.c.d@foo.com", "bar.baz");
    assertEquals("foo.com explained: b.a", result.getExplanation());
  }

  @Test
  public void digitsGTparts() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all").txt("exp.com", "%{l105}");
    Result result = it.checkHost(ip, "foo.com", "a.b.c.d@foo.com", "bar.baz");
    assertEquals("foo.com explained: a.b.c.d", result.getExplanation());
  }

  @Test
  public void senderMacro() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all").txt("exp.com", "%{s}");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals("foo.com explained: sender@foo.com", result.getExplanation());
  }

  //////////////////////
  // Macro expansions //
  //////////////////////

  @Test
  public void senderNoLocalPartSupplied() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all").txt("exp.com", "%{s}");
    Result result = it.checkHost(ip, "foo.com", "foo.com", "bar.baz");
    assertEquals("foo.com explained: postmaster@foo.com", result.getExplanation());
  }

  @Test
  public void senderNoSenderSupplied() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all").txt("exp.com", "%{s}");
    Result result = it.checkHost(ip, "foo.com", null, "bar.baz");
    assertEquals("foo.com explained: postmaster@foo.com", result.getExplanation());
  }

  @Test
  public void iMacroIp4() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all").txt("exp.com", "%{i}");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals("foo.com explained: 1.2.3.4", result.getExplanation());
  }

  @Test
  public void iMacroIp6() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all").txt("exp.com", "%{i}");
    Result result = it.checkHost(ip6, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(
        "foo.com explained: 1.2.3.4.5.6.7.8.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.9.0.a.b.c.d.3.f",
        result.getExplanation());
  }

  @Test
  public void cMacroIp4() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all").txt("exp.com", "%{c}");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals("foo.com explained: 1.2.3.4", result.getExplanation());
  }

  @Test
  public void cMacroIp6() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all").txt("exp.com", "%{c}");
    Result result = it.checkHost(ip6, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals("foo.com explained: 1234:5678:0:0:0:0:90ab:cd3f", result.getExplanation());
  }

  // p 7.1 / 5.5
  @Test
  public void pNameIsPresent() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all")
        .txt("exp.com", "%{p}")
        .ptr("4.3.2.1.in-addr.arpa", "x.foo.com")
        .ptr("4.3.2.1.in-addr.arpa", "foo.com")
        .ptr("4.3.2.1.in-addr.arpa", "baz.bar")
        .a("x.foo.com", "1.2.3.4")
        .a("foo.com", "1.2.3.4")
        .a("baz.bar", "1.2.3.4");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals("foo.com explained: foo.com", result.getExplanation());
  }

  @Test
  public void psubdomainIsPresent() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all")
        .txt("exp.com", "%{p}")
        .ptr("4.3.2.1.in-addr.arpa", "x.y.foo.com")
        .ptr("4.3.2.1.in-addr.arpa", "baz.bar")
        .a("x.y.foo.com", "1.2.3.4")
        .a("baz.bar", "1.2.3.4");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals("foo.com explained: x.y.foo.com", result.getExplanation());
  }

  @Test
  public void pnoDomainOrSubdomain() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all")
        .txt("exp.com", "%{p}")
        .ptr("4.3.2.1.in-addr.arpa", "baz.bar")
        .a("baz.bar", "1.2.3.4");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals("foo.com explained: baz.bar", result.getExplanation());
  }

  @Test
  public void pnoPtrs() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all").txt("exp.com", "%{p}");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals("foo.com explained: unknown", result.getExplanation());
  }

  @Test
  public void pPtrsDontValidate() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all")
        .txt("exp.com", "%{p}")
        .ptr("4.3.2.1.in-addr.arpa", "x.foo.com")
        .ptr("4.3.2.1.in-addr.arpa", "foo.com")
        .ptr("4.3.2.1.in-addr.arpa", "baz.bar")
        .a("x.foo.com", "1.2.3.5")
        .a("foo.com", "1.2.3.5")
        .a("baz.bar", "1.2.3.5");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals("foo.com explained: unknown", result.getExplanation());
  }

  @Test
  public void pDnsErrorOnPtrLookup() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all")
        .txt("exp.com", "%{p}")
        .ptr("4.3.2.1.in-addr.arpa", new DnsFail("Some DNS error"));
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals("foo.com explained: unknown", result.getExplanation());
  }

  @Test
  public void pDnsErrorOnRR() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all")
        .txt("exp.com", "%{p}")
        .ptr("4.3.2.1.in-addr.arpa", "x.foo.com")
        .ptr("4.3.2.1.in-addr.arpa", "foo.com")
        .a("x.foo.com", "1.2.3.4")
        .a("foo.com", new DnsFail("Some DNS error"));
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals("foo.com explained: x.foo.com", result.getExplanation());
  }

  @Test
  public void hMacro() {
    dns.txt("foo.com", "v=spf1 a:%{hr}.foo -all").a("baz.bar.foo", ip);
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void rMacroNullHost() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all").txt("exp.com", "%{r}");
    it = new SpfImp(dns, new Settings(10, 2, null, "zoinks: "));
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals("zoinks: unknown", result.getExplanation());
  }

  @Test
  public void tMacro() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all").txt("exp.com", "%{t}");
    it = new SpfImp(dns, new Settings(10, 2, null, null));
    String pattern = (System.currentTimeMillis() / 100000) + "[0-9][0-9]";
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertThat(result.getExplanation(), matchesPattern(pattern));
  }

  // 7.3 truncate long domain for query
  @Test
  public void includeDsTruncated() {
    String domain = "foo.";
    while (domain.length() < 128) {
      domain += domain;
    }
    dns.txt(domain, "v=spf1 include:%{d}%{d} -all")
        .txt(domain.substring(4) + domain, "v=spf1 +all");
    Result result = it.checkHost(ip, domain, "sender@" + domain, "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void aDsTruncated() {
    String ehlo = "foo.";
    while (ehlo.length() < 128) {
      ehlo += ehlo;
    }
    ehlo = ehlo.substring(0, ehlo.length() - 1);
    dns.txt("foo.com", "v=spf1 a:%{h}.%{h} -all").a(ehlo.substring(4) + "." + ehlo, ip);
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", ehlo);
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void mxDsTruncated() {
    String ehlo = "foo.";
    while (ehlo.length() < 128) {
      ehlo += ehlo;
    }
    ehlo = ehlo.substring(0, ehlo.length() - 1);
    String truncated = ehlo.substring(4) + "." + ehlo;
    dns.txt("foo.com", "v=spf1 mx:%{h}.%{h} -all").mx(truncated, "a.com").a("a.com", ip);
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", ehlo);
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void ptrDsTruncated() {
    String ehlo = "foo.";
    while (ehlo.length() < 128) {
      ehlo += ehlo;
    }
    ehlo = ehlo.substring(0, ehlo.length() - 1);
    String truncated = ehlo.substring(4) + "." + ehlo;
    dns.txt("foo.com", "v=spf1 ptr:%{h}.%{h} -all")
        .ptr("4.3.2.1.in-addr.arpa", truncated)
        .a(truncated, ip);
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", ehlo);
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void existsDsTruncated() {
    String ehlo = "foo.";
    while (ehlo.length() < 128) {
      ehlo += ehlo;
    }
    ehlo = ehlo.substring(0, ehlo.length() - 1);
    String truncated = ehlo.substring(4) + "." + ehlo;
    dns.txt("foo.com", "v=spf1 exists:%{h}.%{h} -all").a(truncated, ip);
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", ehlo);
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void redirectDsTruncated() {
    String ehlo = "foo.";
    while (ehlo.length() < 128) {
      ehlo += ehlo;
    }
    ehlo = ehlo.substring(0, ehlo.length() - 1);
    String truncated = ehlo.substring(4) + "." + ehlo;
    dns.txt("foo.com", "v=spf1 redirect=%{h}.%{h}").txt(truncated, "v=spf1 +all");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", ehlo);
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void explanationDsTruncated() {
    String ehlo = "foo.";
    while (ehlo.length() < 128) {
      ehlo += ehlo;
    }
    ehlo = ehlo.substring(0, ehlo.length() - 1);
    String truncated = ehlo.substring(4) + "." + ehlo;
    dns.txt("foo.com", "v=spf1 exp=%{h}.%{h} -all").txt(truncated, "nope");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", ehlo);
    assertEquals("foo.com explained: nope", result.getExplanation());
  }

  @Test
  public void explanationTextNotTruncated() {
    String ehlo = "foo.";
    while (ehlo.length() < 128) {
      ehlo += ehlo;
    }
    dns.txt("foo.com", "v=spf1 exp=exp.com -all").txt("exp.com", "%{h}%{h}");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", ehlo);
    assertEquals("foo.com explained: " + ehlo + ehlo, result.getExplanation());
  }

  // 4.6.1 mechanism and modifier names are case insensitve
  // Actually, everything is case insensitive, per the ABNF
  @Test
  public void caseInsensitive() {
    dns.txt("foo.com", "v=SPF1 a:FoO.foo mX:BaR.CoM RediRect=BAR.COM")
        .txt("bar.com", "V=SPF1 INCLUDE:bar.baz IP4:8.8.8.8 +AlL")
        .txt("bar.baz", "V=spf1 Ip6:ABCD::ef12 ExistS:foo.foo -ALL")
        .mx("bar.com", "mx1.bar")
        .a("mx1.bar", "9.9.9.9")
        .a("foo.foo", "9.9.9.9");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  // % escaped chars
  @Test
  public void escapedCharsInDomainSpec() {
    // For some reason these are allowed in domain spec :shrug:
    // This test is a bit bogus because a real DNS lookup would fail,
    // but it proves the expansion "works"
    dns.txt("foo.com", "v=spf1 a:f%_o%-o%%.bar.com -all").a("f o%20o%.bar.com", "1.2.3.4");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void escapedCharsInExplanation() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all").txt("exp.com", "foo%%bar%_baz%-quux");
    Result result = it.checkHost(ip, "foo.com", "sender@foo.com", "bar.baz");
    assertEquals("foo.com explained: foo%bar baz%20quux", result.getExplanation());
  }

  // upper-case macro letters
  @Test
  public void upperCaseMacroInDS() {
    // For some reason these are allowed in domain spec :shrug:
    dns.txt("foo.com", "v=spf1 a:%{L} -all").a("foo%2bbar.baz", "1.2.3.4");
    Result result = it.checkHost(ip, "foo.com", "foo+bar.baz@foo.com", "bar.baz");
    assertEquals(ResultCode.Pass, result.getCode());
  }

  @Test
  public void upperCaseMacroInExp() {
    dns.txt("foo.com", "v=spf1 exp=exp.com -all").txt("exp.com", "%{L2R}");
    Result result = it.checkHost(ip, "foo.com", "foo#bar.baz.quux@foo.com", "bar.baz");
    assertEquals("foo.com explained: baz.foo%23bar", result.getExplanation());
  }

  @Test
  public void examplesInRfc() throws Exception {
    runExample("%{s}", "strong-bad@email.example.com", true);
    runExample("%{o}", "email.example.com", true);
    runExample("%{d}", "email.example.com", true);
    runExample("%{d4}", "email.example.com", true);
    runExample("%{d3}", "email.example.com", true);
    runExample("%{d2}", "example.com", true);
    runExample("%{d1}", "com", true);
    runExample("%{dr}", "com.example.email", true);
    runExample("%{d2r}", "example.email", true);
    runExample("%{l}", "strong-bad", true);
    runExample("%{l-}", "strong.bad", true);
    runExample("%{lr}", "strong-bad", true);
    runExample("%{lr-}", "bad.strong", true);
    runExample("%{l1r-}", "strong", true);
    runExample("%{ir}.%{v}._spf.%{d2}", "3.2.0.192.in-addr._spf.example.com", true);
    runExample("%{lr-}.lp._spf.%{d2}", "bad.strong.lp._spf.example.com", true);
    runExample(
        "%{lr-}.lp.%{ir}.%{v}._spf.%{d2}",
        "bad.strong.lp.3.2.0.192.in-addr._spf.example.com", true);
    runExample(
        "%{ir}.%{v}.%{l1r-}.lp._spf.%{d2}", "3.2.0.192.in-addr.strong.lp._spf.example.com", true);
    runExample(
        "%{d2}.trusted-domains.example.net", "example.com.trusted-domains.example.net", true);
    runExample(
        "%{ir}.%{v}._spf.%{d2}",
        "1.0.b.c.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.b.d.0.1.0.0.2.ip6._spf.example.com",
        false);
  }

  private void runExample(String macro, String expected, boolean ip4) throws Exception {
    dns = new MockDns();
    dns.txt("email.example.com", "v=spf1 exp=exp.com -all")
        .txt("exp.com", macro)
        .ptr("3.2.0.192.in-addr.arpa", "mx.example.org");
    InetAddress addr = InetAddress.getByName(ip4 ? "192.0.2.3" : "2001:db8::cb01");
    it = new SpfImp(dns, new Settings(10, 2, null, null));
    Result result =
        it.checkHost(addr, "email.example.com", "strong-bad@email.example.com", "baz.quux");
    assertEquals("Failed example:" + macro, expected, result.getExplanation());
  }
}
