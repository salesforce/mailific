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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import net.mailific.spf.SpfUtil;
import net.mailific.spf.test.LiveTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(LiveTests.class)
public class JndiResolverTest {

  String baseDomain = "smtpqa.com";
  JndiResolver it;

  @Before
  public void setup() {
    it = new JndiResolver();
  }

  @Test
  public void singleTxtRecord() throws Exception {
    List<String> actual = it.resolveTxtRecords("orange." + baseDomain);
    assertThat(actual, contains("orange"));
  }

  @Test
  public void twoTxtRecords() throws Exception {
    List<String> actual = it.resolveTxtRecords("fruit." + baseDomain);
    assertThat(actual, containsInAnyOrder("apples", "oranges"));
  }

  @Test
  public void existingNameWithNoTxtRecords() throws Exception {
    List<String> actual = it.resolveTxtRecords("other." + baseDomain);
    assertEquals(0, actual.size());
  }

  @Test
  public void nameNotFoundTxt() throws Exception {
    List<String> actual = it.resolveTxtRecords("nosuchname." + baseDomain);
    assertEquals(0, actual.size());
  }

  @Test
  public void singleARecord() throws Exception {
    List<InetAddress> actual = it.resolveARecords("orange." + baseDomain);
    assertThat(actual, contains(InetAddress.getByName("100.121.20.1")));
  }

  @Test
  public void twoARecords() throws Exception {
    List<InetAddress> actual = it.resolveARecords("fruit." + baseDomain);
    assertThat(
        actual,
        containsInAnyOrder(
            InetAddress.getByName("100.121.20.1"), InetAddress.getByName("100.121.20.2")));
  }

  @Test
  public void existingNameWithNoARecords() throws Exception {
    List<InetAddress> actual = it.resolveARecords("mx." + baseDomain);
    assertEquals(0, actual.size());
  }

  @Test
  public void nameNotFoundA() throws Exception {
    List<InetAddress> actual = it.resolveARecords("nosuchname." + baseDomain);
    assertEquals(0, actual.size());
  }

  @Test
  public void singleAAAARecord() throws Exception {
    List<InetAddress> actual = it.resolveAAAARecords("orange." + baseDomain);
    assertThat(actual, contains(InetAddress.getByName("2001:db8::1")));
  }

  @Test
  public void twoAAAARecords() throws Exception {
    List<InetAddress> actual = it.resolveAAAARecords("fruit." + baseDomain);
    assertThat(
        actual,
        containsInAnyOrder(
            InetAddress.getByName("2001:db8::1"), InetAddress.getByName("2001:db8::2")));
  }

  @Test
  public void existingNameWithNoAAAARecords() throws Exception {
    List<InetAddress> actual = it.resolveAAAARecords("mx." + baseDomain);
    assertEquals(0, actual.size());
  }

  @Test
  public void nameNotFoundAAAA() throws Exception {
    List<InetAddress> actual = it.resolveAAAARecords("nosuchname." + baseDomain);
    assertEquals(0, actual.size());
  }

  @Test
  public void singleMXRecord() throws Exception {
    List<String> actual = it.resolveMXRecords("orange." + baseDomain);
    assertThat(actual, contains("orange." + baseDomain));
  }

  @Test
  public void twoMXRecords() throws Exception {
    List<String> actual = it.resolveMXRecords("fruit." + baseDomain);
    assertThat(actual, containsInAnyOrder("orange." + baseDomain, "apple." + baseDomain));
  }

  @Test
  public void existingNameWithNoMXRecords() throws Exception {
    List<String> actual = it.resolveMXRecords("apple." + baseDomain);
    assertEquals(0, actual.size());
  }

  @Test
  public void nameNotFoundMX() throws Exception {
    List<String> actual = it.resolveMXRecords("nosuchname." + baseDomain);
    assertEquals(0, actual.size());
  }

  @Test
  public void singleIp4PtrRecord() throws Exception {
    // I can't create PTRs for my domain.
    List<InetAddress> ips = it.resolveARecords("google.com");
    String name = SpfUtil.ptrName(ips.get(0));
    List<String> actual = it.resolvePtrRecords(name);
    assertEquals(1, actual.size());
  }

  @Test
  public void specifiedNameServer() throws Exception {
    it = new JndiResolver(List.of("1.1.1.1"));
    List<String> actual = it.resolveTxtRecords("orange." + baseDomain);
    assertThat(actual, contains("orange"));
  }

  @Test
  public void emptyNameServerList() throws Exception {
    it = new JndiResolver(Collections.emptyList());
    List<String> actual = it.resolveTxtRecords("orange." + baseDomain);
    assertThat(actual, contains("orange"));
  }

  @Test
  public void initializationError() throws Exception {
    try {
      it = new JndiResolver(List.of("~"));
      fail("Should have thrown RuntimeException");
    } catch (RuntimeException e) {
      assertEquals("Failed to initialized JNDI DNS context.", e.getMessage());
    }
  }

  @Test
  public void namingExceptionDuringLookup() throws Exception {
    try {
      it.resolve("example.com", new String[] {"foo"}, Object::toString);
      fail("expected some exception");
    } catch (TempDnsFail e) {
      assertThat(e.getMessage(), startsWith("Failure looking up \"example.com\""));
    }
  }

  @Test
  public void undotNull() {
    assertNull(JndiResolver.undot(null));
  }

  @Test
  public void undot() {
    assertEquals("foo.com", JndiResolver.undot("foo.com."));
  }

  @Test
  public void undotNoDot() {
    assertEquals("foo.com", JndiResolver.undot("foo.com"));
  }
}
