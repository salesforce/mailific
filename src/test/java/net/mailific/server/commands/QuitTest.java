package net.mailific.server.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import net.mailific.server.extension.auth.TransitionMatcher;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class QuitTest {

  @Mock SmtpSession session;

  Quit it;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    it = new Quit();
  }

  @Test
  public void happyPath() {
    // TODO: verify no params
    Transition t = it.handleValidCommand(session, "QUIT");
    assertThat(t, TransitionMatcher.with(Reply._221_OK, StandardStates.ENDING_SESSION));
  }

  @Test
  public void validForState() {
    for (StandardStates state : EnumSet.allOf(StandardStates.class)) {
      assertTrue(it.validForState(state));
    }
  }

  @Test
  public void command() {
    assertEquals("QUIT", it.verb());
  }
}
