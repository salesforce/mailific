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

package net.mailific.spf.policy;

import static org.junit.Assert.assertEquals;

import net.mailific.spf.test.LiveTests;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PolicyTest {

  Policy it;

  @Test
  public void testToString() throws Exception {
    it = Policy.parse("v=spf1 mx -all", null);
    assertEquals("v=spf1 +mx -all", it.toString());
  }

  @Test
  public void testBigPolicyString() throws Exception {
    it = Policy.parse("v=spf1 ip6:abcd::1234/120 ~ip6:1234::abcd ?mx -all", null);
    assertEquals(
        "v=spf1 +ip6:abcd:0:0:0:0:0:0:1234/120 ~ip6:1234:0:0:0:0:0:0:abcd ?mx -all", it.toString());
  }

  @Test
  public void testToStringWithRedirect() throws Exception {
    it = Policy.parse("v=spf1 mx redirect=foo.com", null);
    assertEquals("v=spf1 +mx redirect=foo.com", it.toString());
  }

  @Test
  public void testToStringWithExplanation() throws Exception {
    it = Policy.parse("v=spf1 mx exp=foo.com", null);
    assertEquals("v=spf1 +mx exp=foo.com", it.toString());
  }

  @Test
  public void testToStringWithUnknownMods() throws Exception {
    it = Policy.parse("v=spf1 mx mod=%{l}.com mod2=foo -all", null);
    assertEquals("v=spf1 +mx -all mod=%{l}.com mod2=foo", it.toString());
  }

  @Test
  public void testNullModifiers() throws PolicySyntaxException {
    // Will never happen if you create the policy by parsing, but just in case
    it = new Policy("v=spf1", null, null);
    assertEquals("v=spf1", it.toString());
  }

  @Category(LiveTests.class)
  @Test
  public void liveTest() {
    Assert.fail("Live test.");
  }
}
