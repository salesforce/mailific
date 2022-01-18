package net.mailific.util;

/*-
 * #%L
 * Mailific SMTP Server Library
 * %%
 * Copyright (C) 2021 - 2022 Joe Humphreys
 * %%
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
 * #L%
 */

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import net.mailific.main.SimpleArgParser;
import org.junit.Before;
import org.junit.Test;

public class SimpleArgParserTest {

  SimpleArgParser it;

  @Before
  public void setup() {
    it = new SimpleArgParser("-a -b somebar --bar somebar -c");
  }

  @Test
  public void testHappyPath() {
    String[] expectedRemainder = new String[] {"baz", "quux"};

    it.parseArgs("-c -b bar baz quux".split("\\s"));

    assertTrue(it.getFlag("-c"));
    assertEquals("bar", it.getString("-b", "--bar"));
    assertArrayEquals(expectedRemainder, it.getRemainder().toArray(new String[0]));
  }

  @Test
  public void nullDef() {
    assertThrows(
        NullPointerException.class,
        () -> {
          new SimpleArgParser(null);
        });
  }

  @Test
  public void expectedDash() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              new SimpleArgParser("foo -x");
            });
    assertThat(
        e.getMessage(), containsString("Argument value 'foo' appears in template without arg"));

    e =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              new SimpleArgParser("-x foo bar");
            });
    assertThat(
        e.getMessage(), containsString("Argument value 'bar' appears in template without arg"));
  }

  @Test
  public void duplicateArgs() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              it.parseArgs("-c -b bar -b foo".split("\\s"));
            });
    assertThat(e.getMessage(), containsString("-b appeared more than once"));
  }

  @Test
  public void optionAfterRemainder() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              it.parseArgs("bar -b foo".split("\\s"));
            });
    assertThat(e.getMessage(), containsString("Received option -b after positional args bar"));
  }

  @Test
  public void duplicateFlags() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              it.parseArgs("-c -b foo -c bar".split("\\s"));
            });
    assertThat(e.getMessage(), containsString("-c appeared more than once"));
  }

  @Test
  public void unexpectedOption() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              it.parseArgs("-c -d foo".split("\\s"));
            });
    assertThat(e.getMessage(), containsString("Unexpected option -d"));
  }

  @Test
  public void missingValue() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              it.parseArgs("-c -b".split("\\s"));
            });
    assertThat(e.getMessage(), containsString("-b requires a value"));
  }

  @Test
  public void getFlag_None() {
    it.parseArgs("-c --bar foo".split("\\s"));
    assertFalse(it.getFlag());
  }

  @Test
  public void getFlag_Single() {
    it.parseArgs("-c --bar foo".split("\\s"));
    assertTrue(it.getFlag("-c"));
  }

  @Test
  public void getFlag_twoHits() {
    it.parseArgs("-c --bar foo -a".split("\\s"));
    assertTrue(it.getFlag("-c", "-a"));
  }

  @Test
  public void getFlag_miss() {
    it.parseArgs("-c --bar foo".split("\\s"));
    assertFalse(it.getFlag("-a"));
  }

  @Test
  public void getFlag_mixed() {
    it.parseArgs("-c --bar foo -a".split("\\s"));
    assertTrue(it.getFlag("-d", "-a"));
  }

  @Test
  public void getString_none() {
    it.parseArgs("-c --bar foo -a".split("\\s"));
    assertNull(it.getString("-d", "-a"));
  }

  @Test
  public void getString_hit() {
    it.parseArgs("-c --bar foo -b bar".split("\\s"));
    assertEquals("foo", it.getString("--bar"));
  }

  @Test
  public void getString_miss() {
    it.parseArgs("-c --bar foo -b bar".split("\\s"));
    assertNull(it.getString("--baz"));
  }

  @Test
  public void getString_mixed() {
    it.parseArgs("-c --bar foo -b bar".split("\\s"));
    assertEquals("foo", it.getString("--bar", "-a"));
  }

  @Test
  public void getString_ambiguous() {
    it.parseArgs("-c --bar foo -b bar".split("\\s"));
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              it.getString("-b", "--bar");
            });
    assertEquals(e.getMessage(), "Passed more than one synonym for --bar");
  }

  @Test
  public void getInt_happyPath() {
    String argDef = "-i 5";
    SimpleArgParser parser = new SimpleArgParser(argDef);

    parser.parseArgs("-i 8".split(" "));

    assertEquals(8, parser.getInt(9, "-i"));
  }

  @Test
  public void getInt_default() {
    String argDef = "-i 5";
    SimpleArgParser parser = new SimpleArgParser(argDef);

    parser.parseArgs(new String[0]);

    assertEquals(6, parser.getInt(6, "-i"));
  }

  @Test
  public void getInt_nan() {
    String argDef = "-i 5";
    it = new SimpleArgParser(argDef);

    it.parseArgs("-i foo".split(" "));

    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              it.getInt(3, "-i");
            });
    assertEquals(e.getMessage(), "Can't parse foo as an int.");
  }
}
