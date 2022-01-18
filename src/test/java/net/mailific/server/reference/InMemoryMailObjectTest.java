package net.mailific.server.reference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import net.mailific.server.MailObject;
import net.mailific.server.session.Reply;
import org.junit.Before;
import org.junit.Test;

public class InMemoryMailObjectTest {

  MailObject it;
  byte[] data;
  Reply aReply = new Reply(250, "Yep.");

  @Before
  public void setUp() throws Exception {
    it =
        new InMemoryMailObject(256) {

          @Override
          protected Reply processFinished(byte[] messageBytes) {
            data = Arrays.copyOf(messageBytes, messageBytes.length);
            return aReply;
          }
        };
  }

  @Test
  public void happyPath() throws IOException {
    byte[] line1 = "Subject: hi\r\n".getBytes(StandardCharsets.UTF_8);
    byte[] line2 = "\r\n".getBytes(StandardCharsets.UTF_8);
    byte[] line3 = "junk Hi.\r\n more junk".getBytes(StandardCharsets.UTF_8);

    it.prepareForData();
    it.writeLine(line1, 0, line1.length);
    it.writeLine(line2, 0, line2.length);
    it.writeLine(line3, 5, 5);
    Reply actual = it.complete();

    assertArrayEquals("Subject: hi\r\n\r\nHi.\r\n".getBytes(StandardCharsets.UTF_8), data);
    assertEquals(actual, aReply);
  }
}
