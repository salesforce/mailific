package net.mailific.server.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.mailific.server.Line;
import net.mailific.server.MailObject;
import net.mailific.server.extension.auth.TransitionMatcher;
import net.mailific.server.reference.JavaUtilLogger;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class DataLineConsumerTest {

  @Mock SmtpSession session;

  @Mock MailObject mailObject;

  ByteArrayOutputStream dataSink = new ByteArrayOutputStream();

  DataLineConsumer it = new DataLineConsumer();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    // TODO verify logging
    when(session.getEventLogger()).thenReturn(new JavaUtilLogger());
    when(session.getMailObject()).thenReturn(mailObject);
  }

  @Test
  public void consumeDataLine() throws Exception {
    final byte[] line = "foo\r\n".getBytes("UTF-8");

    Transition actual = it.consume(session, new Line(line));

    assertThat(actual, TransitionMatcher.with(Reply.DO_NOT_REPLY, SessionState.NO_STATE_CHANGE));
    Mockito.verify(mailObject).writeLine(line, 0, line.length);
  }

  @Test
  public void consumeMultipleLine() throws Exception {
    byte[][] lines =
        new byte[][] {
          "foo\r\n".getBytes("UTF-8"), "bar\r\n".getBytes("UTF-8"), "frobozz\r\n".getBytes("UTF-8")
        };

    for (int i = 0; i < 3; i++) {
      it.consume(session, new Line(lines[i]));
    }

    for (int i = 0; i < 3; i++) {
      verify(mailObject).writeLine(lines[i], 0, lines[i].length);
    }
  }

  @Test
  public void endOfData() throws Exception {
    final Reply okReply = new Reply(250, "okay, boss");
    when(session.completeMailObject()).thenReturn(okReply);

    Transition actual = it.consume(session, new Line("."));

    verify(session).removeLineConsumer(Data.DATA_FILTER_KEY);
    assertEquals(0, dataSink.size());
    assertThat(actual, TransitionMatcher.with(okReply, StandardStates.AFTER_EHLO));
  }

  @Test
  public void endOfData_completeThrows() throws Exception {
    when(session.completeMailObject()).thenThrow(new RuntimeException("zoinks"));

    Transition actual = it.consume(session, new Line("."));

    verify(session).removeLineConsumer(Data.DATA_FILTER_KEY);
    assertEquals(0, dataSink.size());
    assertThat(actual, TransitionMatcher.with(Reply._554_SERVER_ERROR, StandardStates.AFTER_EHLO));
  }

  @Test
  public void dotStuffing() throws Exception {
    byte[] line = "..foo\r\n".getBytes("UTF-8");
    Transition actual = it.consume(session, new Line(line));

    assertThat(actual, TransitionMatcher.with(Reply.DO_NOT_REPLY, SessionState.NO_STATE_CHANGE));
    verify(mailObject).writeLine(line, 1, line.length - 1);
    verify(session, never()).removeLineConsumer(Data.DATA_FILTER_KEY);
  }

  @Test
  public void errorWritingData() throws Exception {
    byte[] line = "foo\r\n".getBytes("UTF-8");
    byte[] line2 = "bar\r\n".getBytes("UTF-8");

    Mockito.doThrow(IOException.class).when(mailObject).writeLine(line, 0, line.length);

    Transition actual = it.consume(session, new Line(line));

    // Even though there was an error, we should continue accepting data
    assertThat(actual, TransitionMatcher.with(Reply.DO_NOT_REPLY, SessionState.NO_STATE_CHANGE));

    it.consume(session, new Line(line2));

    // This line was accepted, but we should not have tried to write it again. So
    // getOutputStream() should only have been called on the first consume invocation.
    verify(mailObject, never()).writeLine(line2, 0, line2.length);

    // Send the end-of-data
    actual = it.consume(session, new Line("."));

    verify(session).removeLineConsumer(Data.DATA_FILTER_KEY);
    assertThat(actual, TransitionMatcher.with(Reply._554_SERVER_ERROR, StandardStates.AFTER_EHLO));
  }
}
