package net.mailific.server.session;

import static org.junit.Assert.assertEquals;

import net.mailific.server.Line;
import net.mailific.server.commands.CommandHandler;
import net.mailific.test.TestUtil;
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

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    transition = new Transition(new Reply(223, "OK foo"), StandardStates.AFTER_EHLO);
    line = new Line("foo bar");
    handler = TestUtil.mockCommandHandler(session, line, transition);
    it = new SingleCommandLineConsumer(handler);
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
