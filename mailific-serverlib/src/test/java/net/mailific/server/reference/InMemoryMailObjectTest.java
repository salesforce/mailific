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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import net.mailific.server.MailObject;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SmtpSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class InMemoryMailObjectTest {

  MailObject it;
  byte[] data;
  Reply aReply = new Reply(250, "Yep.");

  private AutoCloseable closeable;
  @Mock SmtpSession session;

  @Before
  public void setUp() throws Exception {
    closeable = MockitoAnnotations.openMocks(this);

    it =
        new InMemoryMailObject(256) {

          @Override
          protected Reply processFinished(byte[] messageBytes) {
            data = Arrays.copyOf(messageBytes, messageBytes.length);
            return aReply;
          }
        };
  }

  @After
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  @Test
  public void happyPath() throws IOException {
    byte[] line1 = "Subject: hi\r\n".getBytes(StandardCharsets.UTF_8);
    byte[] line2 = "\r\n".getBytes(StandardCharsets.UTF_8);
    byte[] line3 = "junk Hi.\r\n more junk".getBytes(StandardCharsets.UTF_8);

    it.prepareForData(session);
    it.writeLine(line1, 0, line1.length);
    it.writeLine(line2, 0, line2.length);
    it.writeLine(line3, 5, 5);
    Reply actual = it.complete(session);

    assertArrayEquals("Subject: hi\r\n\r\nHi.\r\n".getBytes(StandardCharsets.UTF_8), data);
    assertEquals(actual, aReply);
  }
}
