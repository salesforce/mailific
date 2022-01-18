package net.mailific.server.extension.starttls;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import net.mailific.server.Line;
import net.mailific.server.extension.auth.TransitionMatcher;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class StartTlsCommandHandlerTest {

  @Mock SmtpSession session;

  StartTlsCommandHandler it;

  final InetSocketAddress remoteAddress = new InetSocketAddress(25);

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    it = new StartTlsCommandHandler();
  }

  @Test
  public void handleCommand_clearMailObject() {
    it.handleCommand(session, new Line("STARTTLS"));
    verify(session).clearMailObject();
  }

  @Test
  public void handleCommand_callsPrepare() {
    AtomicReference<Boolean> called = new AtomicReference<>(false);
    it =
        new StartTlsCommandHandler() {
          @Override
          protected void prepareForHandshake(SmtpSession session) {
            called.set(true);
          }
        };

    it.handleCommand(session, new Line("STARTTLS"));
    assertTrue(called.get());
  }

  @Test
  public void handleCommand_returnValue() {
    Transition actual = it.handleCommand(session, new Line("STARTTLS"));
    assertThat(actual, TransitionMatcher.with(StartTls._220_READY, StandardStates.CONNECTED));
  }

  @Test
  public void handleCommand_tlsAlreadyStarted() {
    when(session.isTlsStarted()).thenReturn(true);
    Transition actual = it.handleCommand(session, new Line("STARTTLS"));
    assertThat(
        actual, TransitionMatcher.with(Reply._503_BAD_SEQUENCE, StandardStates.NO_STATE_CHANGE));
  }

  @Test
  public void command() {
    assertEquals(StartTls.NAME, it.verb());
  }
}
