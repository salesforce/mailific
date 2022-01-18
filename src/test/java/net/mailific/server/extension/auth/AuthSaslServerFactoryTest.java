package net.mailific.server.extension.auth;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AuthSaslServerFactoryTest {

  @Mock AuthCheck authCheck;

  Map<String, Object> props = new HashMap<>();

  AuthSaslServerFactory it = new AuthSaslServerFactory();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    props.put(Auth.AUTH_CHECK_PROPERTY, authCheck);
  }

  @Test
  public void getMechanismNames() {
    Assert.assertArrayEquals(new String[] {"PLAIN", "LOGIN"}, it.getMechanismNames(null));
  }

  @Test
  public void createSaslServer_plain() throws SaslException {
    SaslServer actual = it.createSaslServer("PLAIN", "smtp", "foo", props, null);
    assertThat(actual, instanceOf(PlainSaslServer.class));
  }

  @Test
  public void createSaslServer_login() throws SaslException {
    SaslServer actual = it.createSaslServer("LOGIN", "smtp", "foo", props, null);
    assertThat(actual, instanceOf(LoginSaslServer.class));
  }

  @Test
  public void createSaslServer_unsupported() throws SaslException {
    SaslServer actual = it.createSaslServer("FOO", "smtp", "foo", props, null);
    assertNull(actual);
  }
}
