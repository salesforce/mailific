package net.mailific.server.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.ParseException;
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
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RcptTest {

  @Mock SmtpSession session;

  @Mock MailObject mailObject;

  Rcpt it = new Rcpt();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(session.getMailObject()).thenReturn(mailObject);
  }

  @Test
  public void incompleteCommand() {
    Transition t = it.handleValidCommand(session, "RCPT");
    assertThat(t, TransitionMatcher.with(Reply._500_UNRECOGNIZED, SessionState.NO_STATE_CHANGE));
  }

  @Test
  public void syntaxError() {
    Transition t = it.handleValidCommand(session, "RCPT OF:<joe@example.com>");
    assertThat(t, TransitionMatcher.with(Reply._500_UNRECOGNIZED, SessionState.NO_STATE_CHANGE));
  }

  @Test
  public void happyPath() {
    when(mailObject.rcptTo(ArgumentMatchers.any())).thenReturn(Reply._250_OK);

    Transition t = it.handleValidCommand(session, "RCPT TO:<joe@example.com>");

    verify(mailObject).rcptTo(any());
    assertThat(t, TransitionMatcher.with(Reply._250_OK, StandardStates.AFTER_RCPT));
  }

  @Test
  public void mailObjectObjects() {
    Reply reply = new Reply(450, "Do not want");
    when(mailObject.rcptTo(ArgumentMatchers.any())).thenReturn(reply);

    Transition t = it.handleValidCommand(session, "RCPT TO:<joe@example.com>");

    verify(mailObject).rcptTo(any());
    assertThat(t, TransitionMatcher.with(reply, SessionState.NO_STATE_CHANGE));
  }

  @Test
  public void unparseable() {
    Transition t = it.handleValidCommand(session, "RCPT TO:!notanaddress!!");

    assertThat(t, TransitionMatcher.with(Reply._501_BAD_ARGS, SessionState.NO_STATE_CHANGE));
  }

  @Test
  public void validForState() {
    for (StandardStates state : EnumSet.allOf(StandardStates.class)) {
      switch (state) {
        case AFTER_MAIL:
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
    assertEquals("RCPT", it.verb());
  }

  @Test
  public void parseCommandLine_validNoParams() throws Exception {
    final String line = "RCPT TO:<joe@example.com>";

    ParsedCommandLine actual = it.parseCommandLine(line);

    assertEquals("RCPT", actual.getCommand());
    assertEquals(line, actual.getLine());
    assertEquals("joe@example.com", actual.getPath());
    assertThat(actual.getParameters().getParameterNames(), empty());
  }

  @Test
  public void parseCommandLine_validWithParams() throws Exception {
    final String line = "RCPT TO:<joe@example.com> foo=bar";

    ParsedCommandLine actual = it.parseCommandLine(line);

    assertEquals("RCPT", actual.getCommand());
    assertEquals(line, actual.getLine());
    assertEquals("joe@example.com", actual.getPath());
    assertEquals("bar", actual.getParameters().get("Foo"));
  }

  @Test
  public void parseCommandLine_invalid() {
    assertThrows(
        ParseException.class,
        () -> {
          it.parseCommandLine("RCPT TO:not a valid address");
        });
  }
}
