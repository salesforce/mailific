package net.mailific.server.extension.starttls;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import net.mailific.server.Line;
import net.mailific.server.extension.auth.TransitionMatcher;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class StartTlsCommandHandlerTest {

  @Mock SmtpSession session;

  StartTlsCommandHandler it;

  final InetSocketAddress remoteAddress = new InetSocketAddress(25);

  private AutoCloseable closeable;

  @Before
  public void setup() {
    closeable = MockitoAnnotations.openMocks(this);
    it = new StartTlsCommandHandler();
  }

  @After
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  @Test
  public void handleCommand_clearMailObject() {
    it.handleCommand(session, new Line("STARTTLS"));
    verify(session).clearMailObject();
  }

  @Test
  public void handleCommand_callsPrepare() {
    AtomicReference<Boolean> called = new AtomicReference<>(false);
    it =
        new StartTlsCommandHandler() {
          @Override
          protected void prepareForHandshake(SmtpSession session) {
            called.set(true);
          }
        };

    it.handleCommand(session, new Line("STARTTLS"));
    assertTrue(called.get());
  }

  @Test
  public void handleCommand_returnValue() {
    Transition actual = it.handleCommand(session, new Line("STARTTLS"));
    assertThat(actual, TransitionMatcher.with(StartTls._220_READY, StandardStates.CONNECTED));
  }

  @Test
  public void handleCommand_tlsAlreadyStarted() {
    when(session.isTlsStarted()).thenReturn(true);
    Transition actual = it.handleCommand(session, new Line("STARTTLS"));
    assertThat(
        actual, TransitionMatcher.with(Reply._503_BAD_SEQUENCE, StandardStates.NO_STATE_CHANGE));
  }

  @Test
  public void command() {
    assertEquals(StartTls.NAME, it.verb());
  }
}
