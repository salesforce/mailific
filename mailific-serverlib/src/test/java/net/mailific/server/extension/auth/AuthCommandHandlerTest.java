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

package net.mailific.server.extension.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import net.mailific.server.Line;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AuthCommandHandlerTest {

  @Mock Auth authExtension;

  @Mock Mechanism fooMech;

  @Mock SmtpSession session;

  @Mock SaslServer saslServer;

  AuthCommandHandler it;

  private AutoCloseable closeable;

  @Before
  public void setUp() {
    closeable = MockitoAnnotations.openMocks(this);

    when(session.getConnectionState()).thenReturn(StandardStates.AFTER_EHLO);

    when(fooMech.available(session)).thenReturn(true);
    when(fooMech.getName()).thenReturn("MECHNAME");

    when(authExtension.getMechanism("MECHNAME")).thenReturn(fooMech);

    it =
        new AuthCommandHandler(
            authExtension,
            new AuthCommandHandler.SaslServerCreator() {
              @Override
              public SaslServer createServer(Mechanism mech, SmtpSession session, String serverName)
                  throws SaslException {
                return saslServer;
              }
            });
  }

  @After
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  @Test
  public void command() {
    assertEquals("AUTH", it.verb());
  }

  @Test
  public void standardStates() {
    for (StandardStates state : EnumSet.allOf(StandardStates.class)) {
      boolean valid = false;
      switch (state) {
        case CONNECTED:
        case AFTER_EHLO:
          valid = true;
          break;
        default:
          break;
      }

      when(session.getConnectionState()).thenReturn(state);

      Transition transition = it.handleCommand(session, new Line("foo"));

      if (valid) {
        assertNotEquals(
            "State " + state + "should be allowed", Reply._503_BAD_SEQUENCE, transition.getReply());
      } else {
        assertEquals(
            "State " + state + "should not be allowed",
            Reply._503_BAD_SEQUENCE,
            transition.getReply());
      }
    }
  }

  @Test
  public void customState() {
    when(session.getConnectionState())
        .thenReturn(
            new SessionState() {

              @Override
              public String name() {
                return "baz";
              }
            });

    Transition transition = it.handleCommand(session, new Line("foo"));

    assertEquals(Reply._503_BAD_SEQUENCE, transition.getReply());
  }

  @Test
  public void onlyOneSuccessfulAuthAllowed() {
    when(session.getProperty(Auth.AUTH_RESULTS_PROPERTY)).thenReturn("bar");
    Transition transition = it.handleCommand(session, new Line("foo"));
    assertEquals(Reply._503_BAD_SEQUENCE, transition.getReply());
  }

  @Test
  public void bareAuthCommand() {
    Transition transition = it.handleCommand(session, new Line("AUTH"));
    assertEquals(504, transition.getReply().getCode());
    assertEquals(SessionState.NO_STATE_CHANGE, transition.getNextState());
  }

  @Test
  public void commandHasTooManyTokens() {
    Transition transition = it.handleCommand(session, new Line("AUTH PLAIN abcdefg hijklmnop"));
    assertEquals(504, transition.getReply().getCode());
    assertEquals(SessionState.NO_STATE_CHANGE, transition.getNextState());
  }

  @Test
  public void unknownMechanism() {
    Transition transition = it.handleCommand(session, new Line("AUTH FOOBAR"));
    assertEquals(504, transition.getReply().getCode());
    assertEquals(SessionState.NO_STATE_CHANGE, transition.getNextState());
  }

  @Test
  public void unavailableMechanism() {
    when(fooMech.available(session)).thenReturn(false);

    Transition transition = it.handleCommand(session, new Line("AUTH MECHNAME"));
    assertEquals(504, transition.getReply().getCode());
    assertEquals(SessionState.NO_STATE_CHANGE, transition.getNextState());
  }

  @Test
  public void zeroLengthInitialResponse() throws Exception {
    // --------- SETUP
    String challengeString = "a challenge";
    final byte[] challenge = challengeString.getBytes("UTF-8");
    Reply reply = new Reply(123, challengeString);
    when(authExtension.challengeToReply(challenge)).thenReturn(reply);

    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    when(saslServer.evaluateResponse(ArgumentMatchers.any())).thenReturn(challenge);

    // --------- ACT
    Transition t = it.handleCommand(session, new Line("AUTH MECHNAME ="));

    // --------- VERIFY
    verify(saslServer).evaluateResponse(captor.capture());
    assertEquals("SaslServer not called with empty byte[]", 0, captor.getValue().length);

    verify(session).setProperty(Auth.SASL_SERVER_PROPERTY, saslServer);
    verify(session)
        .addLineConsumer(
            ArgumentMatchers.eq(Auth.AUTH_LINE_CONSUMER_SELECTOR),
            ArgumentMatchers.any(AuthLineConsumer.class));

    assertEquals(reply, t.getReply());
  }

  @Test
  public void nullInitialResponse() throws Exception {
    // --------- SETUP
    String challengeString = "a challenge";
    final byte[] challenge = challengeString.getBytes("UTF-8");
    Reply reply = new Reply(123, challengeString);
    when(authExtension.challengeToReply(challenge)).thenReturn(reply);

    when(saslServer.evaluateResponse(null)).thenReturn(challenge);

    // --------- ACT
    Transition t = it.handleCommand(session, new Line("AUTH MECHNAME"));

    // --------- VERIFY
    verify(saslServer).evaluateResponse(null);

    verify(session).setProperty(Auth.SASL_SERVER_PROPERTY, saslServer);
    verify(session)
        .addLineConsumer(
            ArgumentMatchers.eq(Auth.AUTH_LINE_CONSUMER_SELECTOR),
            ArgumentMatchers.any(AuthLineConsumer.class));

    assertEquals(reply, t.getReply());
  }

  @Test
  public void badBase64InInitialResponse() {
    Transition t = it.handleCommand(session, new Line("AUTH MECHNAME x&"));
    assertEquals(Reply._501_BAD_ARGS, t.getReply());
  }

  @Test
  public void validInitialResponsePassedToServer() throws Exception {
    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
    when(authExtension.challengeToReply(any())).thenReturn(Auth._235_AUTH_SUCCESS);

    it.handleCommand(session, new Line("AUTH MECHNAME YmFyYmF6")); // barbaz in base64

    verify(saslServer).evaluateResponse(captor.capture());
    assertEquals("barbaz", new String(captor.getValue(), "UTF-8"));
  }

  // The other branch was tested in nullInitialResponse()
  @Test
  public void serverCompletes_returnValueOfAuthExtSaslCompleted() throws Exception {
    when(saslServer.isComplete()).thenReturn(true);
    Transition expected = new Transition(new Reply(1, "foo"), SessionState.NO_STATE_CHANGE);
    when(authExtension.saslCompleted(session, fooMech, saslServer)).thenReturn(expected);

    Transition actual = it.handleCommand(session, new Line("AUTH MECHNAME YmFyYmF6"));

    verify(authExtension).saslCompleted(session, fooMech, saslServer);
    assertEquals(expected, actual);
  }

  @Test
  public void exceptionCreatingServer() {
    it =
        new AuthCommandHandler(
            authExtension,
            new AuthCommandHandler.SaslServerCreator() {
              @Override
              public SaslServer createServer(Mechanism mech, SmtpSession session, String serverName)
                  throws SaslException {
                throw new SaslException();
              }
            });

    Transition actual = it.handleCommand(session, new Line("AUTH MECHNAME YmFyYmF6"));

    assertEquals(501, actual.getReply().getCode());
  }

  @Test
  public void oneArgConstructorWorks() {
    // Exercising just for coverage
    it = new AuthCommandHandler(authExtension);
    Transition actual = it.handleCommand(session, new Line("AUTH MECHNAME"));
    Assert.assertNotNull(actual);
    Assert.assertNotNull(actual.getReply());
    assertEquals(501, actual.getReply().getCode());
  }
}
