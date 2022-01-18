package net.mailific.server.extension.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;
import net.mailific.server.session.SmtpSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BaseAuthenticationMechanismTest {

  @Mock AuthCheck authCheck;

  @Mock SmtpSession session;

  BaseAuthenticationMechanism it;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    it =
        new BaseAuthenticationMechanism(authCheck) {
          @Override
          public String getName() {
            return null;
          }
        };
  }

  @Test
  public void getSaslServerFactoryClass() {
    assertEquals(it.getSaslServerFactoryClass(), AuthSaslServerFactory.class);
  }

  @Test
  public void available_noTLS() {
    when(session.isTlsStarted()).thenReturn(false);
    assertFalse(it.available(session));
  }

  @Test
  public void available_TLS() {
    when(session.isTlsStarted()).thenReturn(true);
    assertTrue(it.available(session));
  }

  @Test
  public void authCheck() {
    assertEquals(authCheck, it.getAuthCheck());
  }

  @Test
  public void getFactoryProps_containsAuthCheck() {
    Map<String, Object> props = it.getFactoryProps(session);
    Object actual = props.get(Auth.AUTH_CHECK_PROPERTY);
    assertEquals(actual, authCheck);
  }
}
