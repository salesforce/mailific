package net.mailific.server.extension.auth;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PlainMechanismTest {

  @Mock AuthCheck authCheck;

  PlainMechanism it;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    it = new PlainMechanism(authCheck);
  }

  @Test
  public void name() {
    assertEquals("PLAIN", it.getName());
  }
}
