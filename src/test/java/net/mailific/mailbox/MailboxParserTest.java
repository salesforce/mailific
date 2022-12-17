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
package net.mailific.mailbox;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import org.junit.Assert;
import org.junit.Test;

public class MailboxParserTest {

  @Test
  public void simplest() throws ParseException {
    MailboxParser p = new MailboxParser("<joe@example.com>", 0);

    assertEquals("joe@example.com", p.getMailbox());
    assertEquals(1, p.getStart());
    assertEquals(16, p.getEnd());
    assertEquals(17, p.getPathEnd());
  }

  @Test
  public void offset() throws ParseException {
    MailboxParser p = new MailboxParser("mail from:<foo@bar.com>", 10);

    assertEquals("foo@bar.com", p.getMailbox());
    assertEquals(11, p.getStart());
    assertEquals(22, p.getEnd());
    assertEquals(23, p.getPathEnd());
  }

  @Test
  public void offsetBeforeBracket() throws ParseException {
    MailboxParser p = new MailboxParser("mail from:<foo@bar.com>", 3);

    assertEquals("foo@bar.com", p.getMailbox());
    assertEquals(11, p.getStart());
    assertEquals(22, p.getEnd());
    assertEquals(23, p.getPathEnd());
  }

  @Test
  public void noLeftBracket() {
    Assert.assertThrows(ParseException.class, () -> new MailboxParser("joe.example.com>", 0));
  }

  @Test
  public void noRightBracket() {
    Assert.assertThrows(ParseException.class, () -> new MailboxParser("<joe.example.com", 0));
  }

  @Test
  public void leftBeforeRight() {
    Assert.assertThrows(ParseException.class, () -> new MailboxParser(">joe.example.com<", 0));
  }

  @Test
  public void offsetAfterBracket() {
    Assert.assertThrows(ParseException.class, () -> new MailboxParser("<joe.example.com>", 3));
  }

  @Test
  public void offsetOutOfBounds() {
    Assert.assertThrows(ParseException.class, () -> new MailboxParser("<joe.example.com>", 800));
  }
}
