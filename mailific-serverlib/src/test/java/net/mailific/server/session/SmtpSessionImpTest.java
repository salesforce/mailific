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

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLSession;
import net.mailific.server.Line;
import net.mailific.server.LineArgMatcher;
import net.mailific.server.LineConsumer;
import net.mailific.server.MailObject;
import net.mailific.server.commands.ParsedCommandLine;
import net.mailific.server.extension.Extension;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SmtpSessionImpTest {

  @Mock MailObject oldMailObject;

  @Mock MailObject newMailObject;

  @Mock LineConsumer lineConsumer1;
  Line line = new Line("LINE1");
  Transition transition1 = new Transition(new Reply(8, ""), StandardStates.AFTER_RCPT);

  @Mock LineConsumer lineConsumer2;
  Transition transition2 = new Transition(new Reply(9, ""), StandardStates.AFTER_MAIL);

  @Mock Extension extension;

  @Mock LineConsumer commandMap;

  @Mock SSLSession sslSession;

  SmtpSessionImp it;

  final InetSocketAddress remoteAddress = new InetSocketAddress(25);

  private AutoCloseable closeable;

  @Before
  public void setup() {
    closeable = MockitoAnnotations.openMocks(this);
    it = new SmtpSessionImp(remoteAddress, commandMap, null);

    when(lineConsumer1.consume(same(it), argThat(new LineArgMatcher(line))))
        .thenReturn(transition1);
    when(lineConsumer2.consume(same(it), argThat(new LineArgMatcher(line))))
        .thenReturn(transition2);
  }

  @After
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  @Test
  public void newMailObject_disposesOld() {
    // setup
    it.newMailObject(oldMailObject);

    // act
    it.newMailObject(newMailObject);

    // verify
    verify(oldMailObject).dispose();
    assertSame(newMailObject, it.getMailObject());
  }

  @Test
  public void clearMailObject_disposes() {
    // setup
    it.newMailObject(oldMailObject);

    // act
    it.completeMailObject();

    // verify
    verify(oldMailObject).dispose();
    assertNull(it.getMailObject());
  }

  @Test
  public void clearMailObject_null() {
    it.completeMailObject();
  }

  @Test
  public void getSetAndClearProperty() {
    Object bar = new Object();
    it.setProperty("foo", bar);
    assertEquals(bar, it.getProperty("foo"));
    it.clearProperty("foo");
    assertNull(it.getProperty("foo"));
  }

  @Test
  public void gettersAndSetters() {
    assertEquals(remoteAddress, it.getRemoteAddress());

    assertEquals(StandardStates.BEFORE_CONNECT, it.getConnectionState());
    it.setConnectionState(StandardStates.AFTER_EHLO);
    assertEquals(StandardStates.AFTER_EHLO, it.getConnectionState());

    assertNull(it.getEhloCommandLine());
    final ParsedCommandLine ehloLine = new ParsedCommandLine("EHLO foo.com", "EHLO", null, null);
    it.setEhloCommandLine(ehloLine);
    assertEquals(ehloLine, it.getEhloCommandLine());

    assertFalse(it.isTlsStarted());
    it.setSslSession(sslSession);
    assertEquals(sslSession, it.getSslSession());
    assertTrue(it.isTlsStarted());
    it.setSslSession(null);
    assertFalse(it.isTlsStarted());
    assertNull(it.getSslSession());
  }

  @Test
  public void connect_success() {
    final Reply reply = new Reply(1, "foo");
    Transition transition = new Transition(reply, StandardStates.ENDING_SESSION);
    when(commandMap.connect(it)).thenReturn(transition);

    final Reply actual = it.connect();

    assertEquals(reply, actual);
    assertEquals(StandardStates.ENDING_SESSION, it.getConnectionState());
  }

  @Test
  public void connect_unhandled() {
    when(commandMap.connect(it)).thenReturn(Transition.UNHANDLED);

    assertEquals(Reply._554_SERVER_ERROR, it.connect());
    assertEquals(StandardStates.BEFORE_CONNECT, it.getConnectionState());
  }

  @Test
  public void consumeLine_stateChange() {
    Reply reply = new Reply(1, "foo");
    Transition transition = new Transition(reply, StandardStates.ENDING_SESSION);
    when(commandMap.consume(any(), any())).thenReturn(transition);

    Reply actual = it.consumeLine("bar\r\n".getBytes());

    assertEquals(reply, actual);
    assertEquals(StandardStates.ENDING_SESSION, it.getConnectionState());
  }

  @Test
  public void consumeLine_noStateChange() {
    Reply reply = new Reply(1, "foo");
    Transition transition = new Transition(reply, StandardStates.NO_STATE_CHANGE);
    when(commandMap.consume(any(), any())).thenReturn(transition);

    assertEquals(reply, it.consumeLine("bar\r\n".getBytes()));
    assertNotEquals(StandardStates.NO_STATE_CHANGE, it.getConnectionState());
  }

  @Test
  public void extension_nullLineConsumer() {
    when(extension.getLineConsumer()).thenReturn(null);
    List<Extension> extensions = Arrays.asList(extension);
    it = new SmtpSessionImp(remoteAddress, commandMap, extensions);

    MatcherAssert.assertThat(it.getSupportedExtensions(), contains(extension));
  }

  @Test
  public void extension_withLineConsumer() {
    MockHandler handler = new MockHandler("FOO");
    when(extension.getLineConsumer()).thenReturn(new SingleCommandLineConsumer(handler));
    List<Extension> extensions = Arrays.asList(extension);
    it = new SmtpSessionImp(remoteAddress, commandMap, extensions);

    it.consumeLine("FOO\r\n".getBytes(StandardCharsets.UTF_8));

    assertTrue(handler.called());
  }

  @Test
  public void addRemoveConsumers() {
    when(commandMap.consume(any(), any())).thenReturn(Transition.UNHANDLED);

    // No consumers
    assertEquals(Reply._500_UNRECOGNIZED, it.consumeLine(line.getLine()));

    // Add one consumer for line and verify it gets called
    String selector1 = "selector1";
    it.addLineConsumer(selector1, lineConsumer1);
    assertEquals(transition1.getReply(), it.consumeLine(line.getLine()));

    // Add a second filter for the same line, and verify it gets called instead
    String selector2 = "selector2";
    it.addLineConsumer(selector2, lineConsumer2);
    assertEquals(transition2.getReply(), it.consumeLine(line.getLine()));

    // Remove the second filter and verify the first gets called
    it.removeLineConsumer(selector2);
    assertEquals(transition1.getReply(), it.consumeLine(line.getLine()));

    // Remove the remaining filter and verify the no-filter behavior
    it.removeLineConsumer(selector1);
    assertEquals(Reply._500_UNRECOGNIZED, it.consumeLine(line.getLine()));
  }
}
