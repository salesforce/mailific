package net.mailific.server.commands;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.EnumSet;
import net.mailific.server.extension.auth.TransitionMatcher;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class HeloTest {

  @Mock SmtpSession session;

  Helo it;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    it = new Helo("foo");
  }

  @Test
  public void happyPath() {
    Transition t = it.handleValidCommand(session, "HELO example.com");

    verify(session).clearMailObject();
    assertThat(t, TransitionMatcher.with(250, is("foo"), StandardStates.AFTER_EHLO));
  }

  @Test
  public void validForState() {
    for (StandardStates state : EnumSet.allOf(StandardStates.class)) {
      switch (state) {
        case CONNECTED:
          assertTrue(it.validForState(state));
          break;
        default:
          assertFalse(it.validForState(state));
      }
    }
  }

  @Test
  public void command() {
    assertEquals("HELO", it.verb());
  }
}
