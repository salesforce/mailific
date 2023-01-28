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

package net.mailific.server.reference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;
import net.mailific.server.MailObject;
import net.mailific.server.commands.ParsedCommandLine;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SmtpSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BaseMailObjectTest {

  private AutoCloseable closeable;

  String PATH1 = "joe@example.com";
  String PATH2 = "notjoe@example.com";

  @Mock ParsedCommandLine fromLine;

  @Mock ParsedCommandLine rcpt1;

  @Mock ParsedCommandLine rcpt2;

  @Mock SmtpSession session;

  MailObject it;

  @Before
  public void setup() {
    closeable = MockitoAnnotations.openMocks(this);

    when(fromLine.getPath()).thenReturn(PATH1);
    when(rcpt1.getPath()).thenReturn(PATH1);
    when(rcpt2.getPath()).thenReturn(PATH2);

    it = new BaseMailObjectFactory().newMailObject(session);
  }

  @After
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  @Test
  public void mailFrom_null() {
    assertThrows(
        NullPointerException.class,
        () -> {
          it.mailFrom(null);
        });
  }

  @Test
  public void mailFrom() {
    Reply actual = it.mailFrom(fromLine);

    assertEquals(250, actual.getCode());
    assertEquals(false, actual.isImmediate());
    assertEquals(fromLine, it.getMailFromLine());
  }

  @Test
  public void reversePath_nullMailFrom() {
    assertNull(it.getReversePathMailbox());
  }

  @Test
  public void reversePath() {

    when(fromLine.getPath()).thenReturn(PATH1);
    it.mailFrom(fromLine);

    assertEquals(PATH1, it.getReversePathMailbox());
  }

  @Test
  public void noRcptTos() {
    assertThat(it.getAcceptedRcptToLines(), empty());
    assertThat(it.getForwardPathMailBoxes(), empty());
    assertThat(it.getDistinctForwardPathMailboxes(String::toString), empty());
  }

  @Test
  public void addRcptTos() {
    Reply reply = it.rcptTo(rcpt1);
    assertEquals(250, reply.getCode());

    reply = it.rcptTo(rcpt2);
    assertEquals(250, reply.getCode());

    assertThat(it.getAcceptedRcptToLines(), contains(rcpt1, rcpt2));
    assertThat(it.getForwardPathMailBoxes(), contains(PATH1, PATH2));
    assertThat(it.getDistinctForwardPathMailboxes(String::toString), contains(PATH1, PATH2));
  }

  @Test
  public void rcptTo_callsOffer() {
    it =
        new BaseMailObject() {
          @Override
          public Reply offerRecipient(ParsedCommandLine rcpt) {
            if (rcpt.getPath().startsWith("j")) {
              return Reply._250_OK;
            }
            return Reply._501_BAD_ARGS;
          }
        };

    Reply reply = it.rcptTo(rcpt1);
    assertEquals(Reply._250_OK, reply);

    reply = it.rcptTo(rcpt2);
    assertEquals(Reply._501_BAD_ARGS, reply);

    assertThat(it.getAcceptedRcptToLines(), contains(rcpt1));
    assertThat(it.getForwardPathMailBoxes(), contains(PATH1));
    assertThat(it.getDistinctForwardPathMailboxes(String::toString), contains(PATH1));
  }

  @Test
  public void rcptTo_notDistinct() {
    Reply reply = it.rcptTo(rcpt1);
    assertEquals(250, reply.getCode());

    reply = it.rcptTo(rcpt2);
    assertEquals(250, reply.getCode());

    assertThat(it.getAcceptedRcptToLines(), contains(rcpt1, rcpt2));
    assertThat(it.getForwardPathMailBoxes(), contains(PATH1, PATH2));

    Collection<String> distinctPaths = it.getDistinctForwardPathMailboxes(s -> "foo");
    assertEquals(1, distinctPaths.size());
    assertThat(distinctPaths, contains(PATH1));
  }

  @Test
  public void getOutputStream() throws IOException {
    // Doesn't verify anything, but exercises it a little
    byte[] line = "hi\r\n".getBytes("UTF-8");
    it.writeLine(line, 0, line.length);
  }

  @Test
  public void complete() throws Exception {
    assertEquals(BaseMailObject.COMPLETE_MAIL_OK, it.complete());
  }

  @Test
  public void extensionMaterial() {
    final String key = "foo";
    final Object material = new Object();

    assertNull(it.getExtensionMaterial(key));

    it.putExtensionMaterial(key, material);

    assertEquals(material, it.getExtensionMaterial(key));
  }

  // Test do-nothing methods, just for coverage

  @Test
  public void doNothingMethods() throws Exception {
    it.dispose();
    it.prepareForData();
  }
}
