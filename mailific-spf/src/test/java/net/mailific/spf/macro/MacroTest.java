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

package net.mailific.spf.macro;

import static org.junit.Assert.assertEquals;

import net.mailific.spf.policy.PolicySyntaxException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MacroTest {

  Macro it;

  @Before
  public void setup() throws Exception {
    it = Macro.macro("s", 0, false, null);
  }

  @Test
  public void testL_digits() {
    Assert.assertThrows(
        PolicySyntaxException.class,
        () -> {
          Macro.macro("l", 1, false, null);
        });
    Assert.assertThrows(
        PolicySyntaxException.class,
        () -> {
          Macro.macro("l", 2, false, null);
        });
  }

  @Test
  public void patternFromDelimeter_null() {
    assertEquals("[.]", it.patternFromDelimiter(null));
  }

  @Test
  public void patternFromDelimeter_empty() {
    assertEquals("[.]", it.patternFromDelimiter(""));
  }

  @Test
  public void patternFromDelimeter_dot() {
    assertEquals("[.]", it.patternFromDelimiter("."));
  }

  @Test
  public void patternFromDelimeter_leadingHyphen() {
    assertEquals("[-.]", it.patternFromDelimiter("-."));
  }

  @Test
  public void patternFromDelimeter_Hy() {
    assertEquals("[-]", it.patternFromDelimiter("-"));
  }

  @Test
  public void patternFromDelimeter_HyphenMoved() {
    assertEquals("[-+/]", it.patternFromDelimiter("+-/"));
  }

  @Test
  public void patternFromDelimeter_All() {
    assertEquals("[-.+,/_=]", it.patternFromDelimiter(".+-,/_="));
  }

  @Test
  public void patternFromDelimeter_Dupes() {
    assertEquals("[-..+,+]", it.patternFromDelimiter("..-+,+"));
  }

  @Test
  public void transform_noModifier() {
    String s = "a.b.c.d.e.f";
    String actual = it.transform(s, 0, false, null);
    assertEquals(s, actual);
  }

  @Test
  public void transform_right1() {
    String s = "a.b.c.d.e.f";
    String actual = it.transform(s, 1, false, null);
    assertEquals("f", actual);
  }

  @Test
  public void transform_right3() {
    String s = "a.b.c.d.e.f";
    String actual = it.transform(s, 3, false, null);
    assertEquals("d.e.f", actual);
  }

  @Test
  public void transform_right6() {
    String s = "a.b.c.d.e.f";
    String actual = it.transform(s, 6, false, null);
    assertEquals(s, actual);
  }

  @Test
  public void transform_right7() {
    String s = "a.b.c.d.e.f";
    String actual = it.transform(s, 7, false, null);
    assertEquals(s, actual);
  }

  @Test
  public void transform_reverse() {
    String s = "a.b.c.d.e.f";
    String actual = it.transform(s, 0, true, null);
    assertEquals("f.e.d.c.b.a", actual);
  }

  @Test
  public void transform_right1_reverse() {
    String s = "a.b.c.d.e.f";
    String actual = it.transform(s, 1, true, null);
    assertEquals("a", actual);
  }

  @Test
  public void transform_right3_reverse() {
    String s = "a.b.c.d.e.f";
    String actual = it.transform(s, 3, true, null);
    assertEquals("c.b.a", actual);
  }

  @Test
  public void transform_right6_reverse() {
    String s = "a.b.c.d.e.f";
    String actual = it.transform(s, 6, true, null);
    assertEquals("f.e.d.c.b.a", actual);
  }

  @Test
  public void transform_right7_reverse() {
    String s = "a.b.c.d.e.f";
    String actual = it.transform(s, 7, true, null);
    assertEquals("f.e.d.c.b.a", actual);
    ;
  }

  @Test
  public void transform_otherDelim() {
    String s = "a-b-c";
    String actual = it.transform(s, 2, true, "-");
    assertEquals("b.a", actual);
  }

  @Test
  public void transform_multiDelim() {
    String s = "a-b+c,d/e_f=g";
    String actual = it.transform(s, 0, false, ".+-+,=/_=");
    assertEquals("a.b.c.d.e.f.g", actual);
  }
}