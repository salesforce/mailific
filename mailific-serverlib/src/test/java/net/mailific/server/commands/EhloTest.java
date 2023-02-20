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
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.EnumSet;
import net.mailific.server.extension.Extension;
import net.mailific.server.extension.auth.TransitionMatcher;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class EhloTest {

  @Mock SmtpSession session;

  @Mock Extension extension1;
  @Mock Extension extension2;
  @Mock Extension extension3;

  Ehlo it;

  private AutoCloseable closeable;

  @Before
  public void setUp() {
    closeable = MockitoAnnotations.openMocks(this);

    when(extension1.getEhloAdvertisment(session)).thenReturn("ext1");
    when(extension1.available(session)).thenReturn(true);

    when(extension2.getEhloAdvertisment(session)).thenReturn("ext2");
    when(extension2.available(session)).thenReturn(false);

    when(extension3.getEhloAdvertisment(session)).thenReturn("ext3");
    when(extension3.available(session)).thenReturn(true);

    it = new Ehlo("foo");
  }

  @After
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  @Test
  public void happyPath() {
    ArgumentCaptor<ParsedCommandLine> pclArgument =
        ArgumentCaptor.forClass(ParsedCommandLine.class);

    Transition t = it.handleValidCommand(session, "EHLO example.com");

    verify(session).clearMailObject();
    verify(session).setEhloCommandLine(pclArgument.capture());
    assertEquals("example.com", pclArgument.getValue().getPath());
    MatcherAssert.assertThat(t, TransitionMatcher.with(250, is("foo"), StandardStates.AFTER_EHLO));
  }

  @Test
  public void withExtensions() {
    when(session.getSupportedExtensions())
        .thenReturn(Arrays.asList(extension1, extension2, extension3));

    Transition t = it.handleValidCommand(session, "EHLO example.com");

    verify(session).clearMailObject();
    assertThat(
        t,
        TransitionMatcher.with(
            is("250-foo\r\n250-ext1\r\n250 ext3\r\n"), StandardStates.AFTER_EHLO));
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
    assertEquals("EHLO", it.verb());
  }

  @Test
  public void greeting() {
    it = new Ehlo("domain", "greeting");

    Transition t = it.handleValidCommand(session, "EHLO example.com");

    assertEquals("250 domain greeting\r\n", t.getReply().replyString());
  }

  @Test
  public void withParams() {
    it = new Ehlo("domain", "greeting");
    ArgumentCaptor<ParsedCommandLine> pclArgument =
        ArgumentCaptor.forClass(ParsedCommandLine.class);

    it.handleValidCommand(session, "EHLO example.com foo=bar");

    verify(session).setEhloCommandLine(pclArgument.capture());
    assertEquals("bar", pclArgument.getValue().getParameters().get("foo"));
  }

  @Test
  public void noDomainGiven() {
    it = new Ehlo("domain", "greeting");

    Transition t = it.handleValidCommand(session, "EHLO");

    assertEquals("501 Syntax error in parameters or arguments\r\n", t.getReply().replyString());
  }
}
