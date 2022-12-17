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
package net.mailific.server.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.EnumSet;
import net.mailific.server.MailObject;
import net.mailific.server.MailObjectFactory;
import net.mailific.server.extension.auth.TransitionMatcher;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MailTest {

  @Mock MailObjectFactory mailObjectFactory;
  @Mock MailObject mailObject;
  @Mock SmtpSession session;

  Mail it;

  private AutoCloseable closeable;

  @Before
  public void setUp() {
    closeable = MockitoAnnotations.openMocks(this);

    when(mailObjectFactory.newMailObject(session)).thenReturn(mailObject);

    it = new Mail(mailObjectFactory);
  }

  @After
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  @Test
  public void incompleteCommand() {
    Transition t = it.handleValidCommand(session, "MAIL");
    assertThat(t, TransitionMatcher.with(Reply._500_UNRECOGNIZED, SessionState.NO_STATE_CHANGE));
  }

  @Test
  public void syntaxError() {
    Transition t = it.handleValidCommand(session, "MAIL MAIL:<joe@example.com>");
    assertThat(t, TransitionMatcher.with(Reply._500_UNRECOGNIZED, SessionState.NO_STATE_CHANGE));
  }

  @Test
  public void happyPath() {
    when(mailObject.mailFrom(ArgumentMatchers.any())).thenReturn(Reply._250_OK);

    Transition t = it.handleValidCommand(session, "MAIL FROM:<joe@example.com>");

    verify(session).newMailObject(mailObject);
    assertThat(t, TransitionMatcher.with(Reply._250_OK, StandardStates.AFTER_MAIL));
  }

  @Test
  public void unparsable_Path() {
    Transition t = it.handleValidCommand(session, "MAIL FROM:!notavalidpath!");

    verify(mailObject).dispose();
    assertThat(t, TransitionMatcher.with(Reply._501_BAD_ARGS, StandardStates.NO_STATE_CHANGE));
  }

  @Test
  public void mailObjectDisapproves() {
    Reply reply = new Reply(550, "Bad sender");
    when(mailObject.mailFrom(ArgumentMatchers.any())).thenReturn(reply);

    Transition t = it.handleValidCommand(session, "MAIL FROM:<joe@example.com>");

    verify(mailObject).dispose();
    assertThat(t, TransitionMatcher.with(reply, StandardStates.NO_STATE_CHANGE));
  }

  @Test
  public void unexpectedException() {
    when(mailObject.mailFrom(ArgumentMatchers.any())).thenThrow(NullPointerException.class);

    assertThrows(
        NullPointerException.class,
        () -> it.handleValidCommand(session, "MAIL FROM:<joe@example.com>"));
    verify(mailObject).dispose();
  }

  @Test
  public void validForState() {
    for (StandardStates state : EnumSet.allOf(StandardStates.class)) {
      switch (state) {
        case AFTER_EHLO:
          assertTrue(it.validForState(state));
          break;
        default:
          assertFalse(it.validForState(state));
      }
    }
  }

  @Test
  public void command() {
    assertEquals("MAIL", it.verb());
  }

  @Test
  public void parseCommandLine_validNoParams() throws Exception {
    final String line = "MAIL FROM:<joe@example.com>";

    ParsedCommandLine actual = it.parseCommandLine(line);

    assertEquals("MAIL", actual.getCommand());
    assertEquals(line, actual.getLine());
    assertEquals("joe@example.com", actual.getPath());
    assertThat(actual.getParameters().getParameterNames(), empty());
  }

  @Test
  public void parseCommandLine_validWithParams() throws Exception {
    final String line = "MAIL FROM:<joe@example.com> foo=bar";

    ParsedCommandLine actual = it.parseCommandLine(line);

    assertEquals("MAIL", actual.getCommand());
    assertEquals(line, actual.getLine());
    assertEquals("joe@example.com", actual.getPath());
    assertEquals("bar", actual.getParameters().get("Foo"));
  }

  @Test
  public void parseCommandLine_invalid() {
    assertThrows(
        ParseException.class,
        () -> {
          it.parseCommandLine("MAIL FROM:not a valid address");
        });
  }
}
