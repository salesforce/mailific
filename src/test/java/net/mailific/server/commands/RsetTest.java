package net.mailific.server.commands;

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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.EnumSet;
import net.mailific.server.extension.auth.TransitionMatcher;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RsetTest {

  @Mock SmtpSession session;

  Rset it;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    it = new Rset();
  }

  @Test
  public void happyPath() {
    Transition t = it.handleValidCommand(session, "RSET");

    verify(session).clearMailObject();
    MatcherAssert.assertThat(t, TransitionMatcher.with(Reply._250_OK, StandardStates.AFTER_EHLO));
  }

  @Test
  public void validForState() {
    for (StandardStates state : EnumSet.allOf(StandardStates.class)) {
      assertTrue(it.validForState(state));
    }
  }

  @Test
  public void command() {
    assertEquals("RSET", it.verb());
  }
}
