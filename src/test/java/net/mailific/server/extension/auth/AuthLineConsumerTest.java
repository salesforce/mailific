package net.mailific.server.extension.auth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import net.mailific.server.Line;
import net.mailific.server.reference.JavaUtilLogger;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class AuthLineConsumerTest {

  @Mock IAuth authExtension;

  AuthLineConsumer it;

  @Mock Mechanism fooMechanism;

  @Mock SmtpSession session;

  @Mock SaslServer saslServer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    // We can assume this property has been added at the same time the
    // AuthLineConsumer was put in place
    when(session.getProperty(Auth.SASL_SERVER_PROPERTY)).thenReturn(saslServer);

    // TODO verify logging
    when(session.getEventLogger()).thenReturn(new JavaUtilLogger());

    it = new AuthLineConsumer(fooMechanism, authExtension);
  }

  @Test
  public void consume_star() throws Exception {
    Transition t = it.consume(session, new Line("*"));

    verify(saslServer).dispose();
    verify(session).clearProperty(Auth.SASL_SERVER_PROPERTY);
    verify(session).removeLineConsumer(Auth.AUTH_LINE_CONSUMER_SELECTOR);
    assertThat(t, TransitionMatcher.with(Auth._501_CANCELED, StandardStates.AFTER_EHLO));
  }

  @Test
  public void consume_star_property_missing() throws Exception {
    when(session.getProperty(Auth.SASL_SERVER_PROPERTY)).thenReturn(null);

    Transition t = it.consume(session, new Line("*"));

    verify(session).removeLineConsumer(Auth.AUTH_LINE_CONSUMER_SELECTOR);
    assertThat(t, TransitionMatcher.with(Auth._501_CANCELED, StandardStates.AFTER_EHLO));
  }

  @Test
  public void consume_disposeThrows_Ignored() throws Exception {
    Mockito.doThrow(SaslException.class).when(saslServer).dispose();

    Transition t = it.consume(session, new Line("*"));

    verify(saslServer).dispose();
    verify(session).clearProperty(Auth.SASL_SERVER_PROPERTY);
    verify(session).removeLineConsumer(Auth.AUTH_LINE_CONSUMER_SELECTOR);
    assertThat(t, TransitionMatcher.with(Auth._501_CANCELED, StandardStates.AFTER_EHLO));
  }

  @Test
  public void consume_badBase64() throws Exception {
    Transition t = it.consume(session, new Line("x"));
    assertThat(t, TransitionMatcher.with(Reply._501_BAD_ARGS, StandardStates.NO_STATE_CHANGE));
  }

  @Test
  public void consume_completingResponse() throws Exception {
    Transition expected = new Transition(new Reply(8, "foo"), SessionState.NO_STATE_CHANGE);
    when(saslServer.isComplete()).thenReturn(true);
    when(authExtension.saslCompleted(session, fooMechanism, saslServer)).thenReturn(expected);

    Transition actual =
        it.consume(session, new Line(Base64.getEncoder().encode("hi\r\n".getBytes("UTF-8"))));

    assertEquals(expected, actual);
    verify(session).removeLineConsumer(Auth.AUTH_LINE_CONSUMER_SELECTOR);
  }

  @Test
  public void consume_nonCompletingResponse() throws Exception {
    String challengeString = "some challenge";
    byte[] challengeBytes = challengeString.getBytes("UTF-8");
    Reply reply = new Reply(386, challengeString);

    when(saslServer.evaluateResponse(ArgumentMatchers.any())).thenReturn(challengeBytes);
    when(authExtension.challengeToReply(challengeBytes)).thenReturn(reply);

    Transition actual =
        it.consume(session, new Line(Base64.getEncoder().encode("hi\r\n".getBytes("UTF-8"))));

    assertThat(actual, TransitionMatcher.with(reply, SessionState.NO_STATE_CHANGE));
  }

  @Test
  public void consume_saslServerException() throws Exception {
    when(saslServer.evaluateResponse(ArgumentMatchers.any())).thenThrow(SaslException.class);

    Transition actual =
        it.consume(session, new Line(Base64.getEncoder().encode("hi\r\n".getBytes("UTF-8"))));

    assertThat(
        actual, TransitionMatcher.with(Reply._554_SERVER_ERROR, SessionState.NO_STATE_CHANGE));
  }
}
