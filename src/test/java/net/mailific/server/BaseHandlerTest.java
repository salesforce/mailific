package net.mailific.server;

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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import net.mailific.server.commands.BaseHandler;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BaseHandlerTest {

  @Mock SmtpSession session;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(session.getConnectionState()).thenReturn(StandardStates.AFTER_EHLO);
  }

  @Test
  public void handleCommand_invalidForSession() {
    BaseHandler subject =
        new BaseHandler() {
          @Override
          public String verb() {
            return "foo";
          }

          @Override
          protected boolean validForSession(SmtpSession session) {
            return false;
          }
        };

    Transition actual = subject.handleCommand(session, new Line("foo"));

    assertEquals(Reply._503_BAD_SEQUENCE, actual.getReply());
    assertEquals(SessionState.NO_STATE_CHANGE, actual.getNextState());
  }

  @Test
  public void handleCommand_happyPath() {
    BaseHandler subject =
        new BaseHandler() {
          @Override
          public String verb() {
            return "foo";
          }
        };

    Transition actual = subject.handleCommand(session, new Line("foo"));

    assertEquals(Reply._250_OK, actual.getReply());
    assertEquals(SessionState.NO_STATE_CHANGE, actual.getNextState());
  }

  @Test
  public void handleCommand_callsHandleValidCommand() {
    final Transition transition = new Transition(Reply._501_BAD_ARGS, StandardStates.AFTER_MAIL);
    BaseHandler subject =
        new BaseHandler() {
          @Override
          public String verb() {
            return "foo";
          }

          @Override
          protected Transition handleValidCommand(SmtpSession session, String commandLine) {
            return transition;
          }
        };

    Transition actual = subject.handleCommand(session, new Line("foo"));

    assertEquals(transition, actual);
  }

  @Test
  public void handleCommand_defaultImplChecksState() {
    BaseHandler subject =
        new BaseHandler() {
          @Override
          public String verb() {
            return "foo";
          }

          @Override
          protected boolean validForState(SessionState state) {
            return false;
          }
        };

    Transition actual = subject.handleCommand(session, new Line("foo"));

    Assert.assertEquals(Reply._503_BAD_SEQUENCE, actual.getReply());
    assertEquals(SessionState.NO_STATE_CHANGE, actual.getNextState());
  }

  @Test
  public void handleCommand_null() {
    Transition transition = new Transition(new Reply(220, "Hi"), StandardStates.CONNECTED);
    BaseHandler subject =
        new BaseHandler() {
          @Override
          public String verb() {
            return "foo";
          }

          @Override
          protected Transition handleValidCommand(SmtpSession session, String commandLine) {
            return transition;
          }
        };

    Transition actual = subject.handleCommand(session, null);

    assertEquals(transition, actual);
  }
}
