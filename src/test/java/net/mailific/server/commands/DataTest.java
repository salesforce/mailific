package net.mailific.server.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import net.mailific.server.MailObject;
import net.mailific.server.extension.auth.TransitionMatcher;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DataTest {

  @Mock SmtpSession session;
  @Mock MailObject mailObject;

  Data it = new Data();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(session.getMailObject()).thenReturn(mailObject);
  }

  @Test
  public void handleValidCommand_extraArgs() {
    Transition t = it.handleValidCommand(session, "DATA some args");

    assertThat(t, TransitionMatcher.with(Reply._501_BAD_ARGS, SessionState.NO_STATE_CHANGE));
  }

  @Test
  public void handleValidCommand() {
    Transition t = it.handleValidCommand(session, "DATA");

    verify(session).addLineConsumer(eq(Data.DATA_FILTER_KEY), any(DataLineConsumer.class));
    verify(mailObject).prepareForData();
    assertThat(t, TransitionMatcher.with(Reply._354_CONTINUE, StandardStates.READING_DATA));
  }

  @Test
  public void validForState() {
    for (StandardStates state : EnumSet.allOf(StandardStates.class)) {
      switch (state) {
        case AFTER_RCPT:
          assertTrue(it.validForState(state));
          break;
        default:
          assertFalse(it.validForState(state));
      }
    }
  }

  @Test
  public void command() {
    assertEquals("DATA", it.verb());
  }
}
