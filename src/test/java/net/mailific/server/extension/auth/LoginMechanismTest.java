package net.mailific.server.extension.auth;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class LoginMechanismTest {

  @Mock AuthCheck authCheck;

  LoginMechanism it;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    it = new LoginMechanism(authCheck);
  }

  @Test
  public void name() {
    assertEquals("LOGIN", it.getName());
  }
}
