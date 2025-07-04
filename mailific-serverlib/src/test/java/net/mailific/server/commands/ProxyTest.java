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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.EnumSet;
import net.mailific.server.extension.auth.TransitionMatcher;
import net.mailific.server.session.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ProxyTest {

    @Mock SmtpSession session;

    Proxy it;

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        it = new Proxy();
    }

    @After
    public void releaseMocks() throws Exception {
        closeable.close();
    }

    @Test
    public void happyPath() {
        Transition t = it.handleValidCommand(session, "PROXY TCP6 5::ffff d::ffff 5555 2222");

        verify(session).setProperty(Proxy.SESSION_CLIENTIP_PROPERTY, "5::ffff");
        assertThat(t, TransitionMatcher.with(Reply.DO_NOT_REPLY, SessionState.NO_STATE_CHANGE));
    }

    @Test
    public void badParams() {
        Transition t = it.handleValidCommand(session, "PROXY TCP6 5::ffff d::ffff 5555");
        assertThat(t, TransitionMatcher.with(Reply.DO_NOT_REPLY, SessionState.NO_STATE_CHANGE));
    }

    @Test
    public void validForState() {
        for (StandardStates state : EnumSet.allOf(StandardStates.class)) {
            switch (state) {
                case CONNECTED:
                    assertTrue(it.validForState(state));
                    break;
                default:
                    assertFalse(it.validForState(state));
            }
        }
    }

    @Test
    public void command() {
        assertEquals("PROXY", it.verb());
    }
}
