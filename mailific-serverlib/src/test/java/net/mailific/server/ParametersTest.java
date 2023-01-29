/*-
 * Mailific SMTP Server Library
 *
 * Copyright (C) 2021-2022 Joe Humphreys
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

package net.mailific.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParametersTest {

  @Test
  public void test_none() {
    Parameters it = new Parameters("MAIL FROM:<joe@example.com>", 27);
    assertThat(it.getParameterNames(), empty());
  }

  @Test
  public void test_pair() {
    Parameters it = new Parameters("MAIL FROM:<joe@example.com> foo=bar", 27);
    assertTrue(it.exists("foo"));
    assertEquals("bar", it.get("foo"));
    assertThat(it.getParameterNames(), containsInAnyOrder("FOO"));
  }

  @Test
  public void test_caseInsensitive() {
    Parameters it = new Parameters("MAIL FROM:<joe@example.com> foo=bar", 27);
    assertTrue(it.exists("foO"));
    assertEquals("bar", it.get("FOO"));
  }

  @Test
  public void test_noValue() {
    Parameters it = new Parameters("MAIL FROM:<joe@example.com> foo", 27);
    assertTrue(it.exists("foo"));
    assertEquals("", it.get("foo"));
    assertThat(it.getParameterNames(), containsInAnyOrder("FOO"));
  }

  @Test
  public void test_mixedValues() {
    Parameters it =
        new Parameters("MAIL FROM:<joe@example.com> FOO BAR=baz quux=Frobozz frobnitz", 27);
    assertTrue(it.exists("foo"));
    assertEquals("baz", it.get("BAR"));
    assertEquals("Frobozz", it.get("quux"));
    assertTrue(it.exists("FROBNITZ"));
    assertThat(it.getParameterNames(), containsInAnyOrder("FOO", "BAR", "QUUX", "FROBNITZ"));
  }

  @Test
  public void test_notPresent() {
    Parameters it = new Parameters("MAIL FROM:<joe@example.com> FOO quux=Frobozz", 27);
    assertFalse(it.exists("BAR"));
    assertNull(it.get("BAR"));
  }
}
