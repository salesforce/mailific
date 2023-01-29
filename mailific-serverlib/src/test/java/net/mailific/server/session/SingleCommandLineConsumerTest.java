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

package net.mailific.server.session;

import static org.junit.Assert.assertEquals;

import net.mailific.server.Line;
import net.mailific.server.commands.CommandHandler;
import net.mailific.test.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SingleCommandLineConsumerTest {

  CommandHandler handler;
  Transition transition;
  Line line;

  @Mock SmtpSession session;

  SingleCommandLineConsumer it;

  private AutoCloseable closeable;

  @Before
  public void setup() {
    closeable = MockitoAnnotations.openMocks(this);
    transition = new Transition(new Reply(223, "OK foo"), StandardStates.AFTER_EHLO);
    line = new Line("foo bar");
    handler = TestUtil.mockCommandHandler(session, line, transition);
    it = new SingleCommandLineConsumer(handler);
  }

  @After
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  @Test
  public void commandHandled() throws Exception {
    Transition actual = it.consume(session, line);
    assertEquals(223, actual.getReply().getCode());
  }

  @Test
  public void commandUnhandled() throws Exception {
    Transition actual = it.consume(session, new Line("bar"));
    assertEquals(actual, Transition.UNHANDLED);
  }

  @Test
  public void connect() {
    Transition actual = it.connect(session);
    assertEquals(actual, Transition.UNHANDLED);
  }
}
