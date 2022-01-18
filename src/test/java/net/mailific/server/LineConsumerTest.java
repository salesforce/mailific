package net.mailific.server;

import static org.junit.Assert.assertEquals;

import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.Transition;
import org.junit.Test;

public class LineConsumerTest {

  @Test
  public void defaultConnect() {
    LineConsumer it =
        new LineConsumer() {

          @Override
          public Transition consume(SmtpSession session, Line line) {
            return null;
          }
        };

    assertEquals(Transition.UNHANDLED, it.connect(null));
  }
}
