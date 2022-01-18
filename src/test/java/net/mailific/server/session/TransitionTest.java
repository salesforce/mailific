package net.mailific.server.session;

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
import static org.junit.Assert.assertThrows;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class TransitionTest {

  Transition it;

  Reply reply = new Reply(234, "OK dude");

  SessionState state = StandardStates.AFTER_EHLO;

  @Before
  public void setup() {
    it = new Transition(reply, state);
  }

  @Test
  public void nullArgs() {
    assertThrows(
        NullPointerException.class,
        () -> {
          new Transition(null, state);
        });
    assertThrows(
        NullPointerException.class,
        () -> {
          new Transition(reply, null);
        });
  }

  @Test
  public void getters() {
    assertEquals(reply, it.getReply());
    assertEquals(state, it.getNextState());
  }

  @Test
  public void testToString() {
    assertThat(it.toString(), CoreMatchers.containsString("reply=Reply [234, OK dude]"));
    assertThat(it.toString(), CoreMatchers.containsString("nextState=AFTER_EHLO"));
  }
}
