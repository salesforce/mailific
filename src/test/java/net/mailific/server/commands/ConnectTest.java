package net.mailific.server.commands;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import net.mailific.server.extension.auth.TransitionMatcher;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ConnectTest {

  @Mock SmtpSession session;

  Connect it;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    it = new Connect("foo");
  }

  @Test
  public void happyPath() {
    Transition t = it.handleValidCommand(session, null);
    assertThat(t, TransitionMatcher.with(220, is("foo"), StandardStates.CONNECTED));
  }

  @Test
  public void validForState() {
    for (StandardStates state : EnumSet.allOf(StandardStates.class)) {
      switch (state) {
        case BEFORE_CONNECT:
          assertTrue(it.validForState(state));
          break;
        default:
          assertFalse(it.validForState(state));
      }
    }
  }

  @Test
  public void command() {
    assertNull(it.verb());
  }

  @Test
  public void reject() {
    it =
        new Connect("foo") {
          @Override
          protected boolean shouldAllow(SmtpSession connection) {
            return false;
          }
        };

    Transition t = it.handleValidCommand(session, null);

    assertThat(t, TransitionMatcher.with(554, null, StandardStates.CONNECT_REJECTED));
  }
}
