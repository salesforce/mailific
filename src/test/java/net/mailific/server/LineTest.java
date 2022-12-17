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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;

public class LineTest {

  byte[] b1 = "foo bar\r\n".getBytes(StandardCharsets.UTF_8);
  byte[] b2 = "baz quux\r\n".getBytes(StandardCharsets.UTF_8);

  Line it = new Line(b1);

  @Test
  public void stringConstructor() {
    it = new Line("foo bar\r\n");
    assertArrayEquals(b1, it.getOriginalLine());
  }

  @Test
  public void stringConstructor_noCRLF() {
    it = new Line("foo bar");
    assertArrayEquals(b1, it.getOriginalLine());
  }

  @Test
  public void getLine_notChanged() {
    assertEquals(b1, it.getLine());
  }

  @Test
  public void getLine_changed() {
    it.setLine(b2);
    Assert.assertEquals(b2, it.getLine());
  }

  @Test
  public void originalLine_notChanged() {
    Assert.assertEquals(b1, it.getOriginalLine());
  }

  @Test
  public void originalLine_changed() {
    it.setLine(b2);
    Assert.assertEquals(b1, it.getOriginalLine());
  }

  @Test
  public void setLine_null() {
    Assert.assertThrows(
        NullPointerException.class,
        () -> {
          it.setLine(null);
        });
  }

  @Test
  public void getStripped() {
    assertEquals("foo bar", it.getStripped());
    // Call again, just to excercise the memoized path
    assertEquals("foo bar", it.getStripped());
  }

  @Test
  public void getStripped_changed() {
    assertEquals("foo bar", it.getStripped());
    it.setLine(b2);
    assertEquals("baz quux", it.getStripped());
  }

  @Test
  public void getVerb() {
    assertEquals("foo", it.getVerb());
    // Call again, just to excercise the memoized path
    assertEquals("foo", it.getVerb());
  }

  @Test
  public void getVerb_changed() {
    assertEquals("foo", it.getVerb());
    it.setLine(b2);
    assertEquals("baz", it.getVerb());
  }

  @Test
  public void strippedAndVerb_noArgs() {
    it.setLine("frobozz\r\n".getBytes(StandardCharsets.UTF_8));
    assertEquals("frobozz", it.getStripped());
    assertEquals("frobozz", it.getVerb());
  }
}
