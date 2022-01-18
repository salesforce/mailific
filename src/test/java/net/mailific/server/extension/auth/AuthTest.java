package net.mailific.server.extension.auth;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Security;
import java.util.Arrays;
import java.util.Collection;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import net.mailific.server.commands.CommandHandler;
import net.mailific.server.reference.JavaUtilLogger;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class AuthTest {

  @Mock SmtpSession session;

  @Mock Mechanism fooMechanism;

  @Mock Mechanism barMechanism;

  @Mock AuthorizeCallback authCallback;

  @Mock SaslServer saslServer;

  MockSaslServerFactory saslServerFactory;

  Auth it;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    saslServerFactory = new MockSaslServerFactory();

    when(fooMechanism.getName()).thenReturn("FOO");
    Mockito.doReturn(MockSaslServerFactory.class).when(fooMechanism).getSaslServerFactoryClass();
    when(fooMechanism.available(session)).thenReturn(true);

    when(barMechanism.getName()).thenReturn("BAR");
    Mockito.doReturn(MockSaslServerFactory.class).when(barMechanism).getSaslServerFactoryClass();
    when(barMechanism.available(session)).thenReturn(true);

    // TODO: verify logging
    when(session.getEventLogger()).thenReturn(new JavaUtilLogger());

    it = new Auth(Arrays.asList(fooMechanism, barMechanism), "someServer");
  }

  @After
  public void tearDown() {
    // Undo the change to Security that was done in Auth constructor.
    Security.removeProvider("SmtpAuthSaslProvider");
  }

  @Test
  public void getEhloAdvertisement_allAvailable() {
    String actual = it.getEhloAdvertisment(session);
    assertThat(actual, anyOf(is("AUTH FOO BAR"), is("AUTH BAR FOO")));
  }

  @Test
  public void getEhloAdvertisement_someAvailable() {
    when(barMechanism.available(session)).thenReturn(false);

    String actual = it.getEhloAdvertisment(session);

    assertEquals("AUTH FOO", actual);
  }

  @Test
  public void available_someAvailable() {
    assertTrue(it.available(session));
  }

  @Test
  public void available_noneAvailable() {
    when(fooMechanism.available(session)).thenReturn(false);
    when(barMechanism.available(session)).thenReturn(false);

    assertFalse(it.available(session));
  }

  @Test
  public void getName() {
    assertEquals("Authentication", it.getName());
  }

  @Test
  public void getEhloKeyword() {
    assertEquals("AUTH", it.getEhloKeyword());
  }

  @Test
  public void verbs() {
    Collection<CommandHandler> actual = it.commandHandlers();

    assertEquals(1, actual.size());
    assertThat(actual.iterator().next(), instanceOf(AuthCommandHandler.class));
  }

  @Test
  public void getServerName() {
    assertEquals("someServer", it.getServerName());
  }

  @Test
  public void getMechanism() {
    assertEquals(fooMechanism, it.getMechanism("FOO"));
    assertEquals(barMechanism, it.getMechanism("BAR"));
    assertNull(it.getMechanism("BAZ"));
  }

  @Test
  public void challengeToReply() {
    Reply actual = it.challengeToReply("some challenge".getBytes());
    assertEquals(334, actual.getCode());
    assertEquals("c29tZSBjaGFsbGVuZ2U=", actual.getDetail());
  }

  @Test
  public void challengeToReply_null() {
    Reply actual = it.challengeToReply(null);
    assertEquals(334, actual.getCode());
    assertEquals("", actual.getDetail());
  }

  @Test
  public void challengeToReply_empty() {
    Reply actual = it.challengeToReply(new byte[0]);
    assertEquals(334, actual.getCode());
    assertEquals("", actual.getDetail());
  }

  @Test
  public void saslCompleted_authorized() {
    when(authCallback.isAuthorized()).thenReturn(true);
    when(saslServer.getNegotiatedProperty(Auth.AUTH_RESULTS_PROPERTY)).thenReturn(authCallback);

    Transition actual = it.saslCompleted(session, fooMechanism, saslServer);

    verify(session).setProperty(Auth.AUTH_RESULTS_PROPERTY, authCallback);
    verify(session).clearProperty(Auth.SASL_SERVER_PROPERTY);
    assertThat(actual, TransitionMatcher.with(Auth._235_AUTH_SUCCESS, StandardStates.AFTER_EHLO));
  }

  @Test
  public void saslCompleted_unauthorized() {
    when(authCallback.isAuthorized()).thenReturn(false);
    when(saslServer.getNegotiatedProperty(Auth.AUTH_RESULTS_PROPERTY)).thenReturn(authCallback);

    Transition actual = it.saslCompleted(session, fooMechanism, saslServer);

    verify(session).clearProperty(Auth.SASL_SERVER_PROPERTY);

    assertThat(actual, TransitionMatcher.with(Auth._535_AUTH_FAILURE, StandardStates.AFTER_EHLO));
  }

  @Test
  public void saslCompleted_disposeThrows() throws SaslException {
    Mockito.doThrow(SaslException.class).when(saslServer).dispose();
    when(authCallback.isAuthorized()).thenReturn(false);
    when(saslServer.getNegotiatedProperty(Auth.AUTH_RESULTS_PROPERTY)).thenReturn(authCallback);

    Transition actual = it.saslCompleted(session, fooMechanism, saslServer);

    assertThat(actual, TransitionMatcher.with(Auth._535_AUTH_FAILURE, StandardStates.AFTER_EHLO));
  }
}
