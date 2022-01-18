package net.mailific.server;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import net.mailific.server.commands.CommandHandler;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpEventLogger;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class SmtpCommandMapTest {

  private static final Transition BAR_TRANSITION =
      new Transition(Reply._503_BAD_SEQUENCE, SessionState.NO_STATE_CHANGE);
  private static final Transition FOO_TRANSITION =
      new Transition(Reply._250_OK, SessionState.NO_STATE_CHANGE);

  @Mock SmtpSession session;

  @Mock SmtpEventLogger logger;

  SmtpCommandMap commandMap;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    Mockito.when(session.getEventLogger()).thenReturn(logger);

    commandMap =
        new SmtpCommandMap(
            Arrays.asList(
                new CommandHandler() {
                  @Override
                  public Transition handleCommand(SmtpSession session, Line commandLine) {
                    return FOO_TRANSITION;
                  }

                  @Override
                  public String verb() {
                    return "fOo";
                  }
                },
                new CommandHandler() {
                  @Override
                  public Transition handleCommand(SmtpSession session, Line commandLine) {
                    return BAR_TRANSITION;
                  }

                  @Override
                  public String verb() {
                    return "BAR";
                  }
                }),
            null);
  }

  @Test
  public void constructorAddsAll() {
    assertEquals(FOO_TRANSITION, commandMap.consume(session, new Line("FOO")));
    assertEquals(BAR_TRANSITION, commandMap.consume(session, new Line("BAR")));
    assertUnhandled(commandMap.consume(session, new Line("BAZ")));
  }

  @Test
  public void caseInsensitive() {
    assertEquals(FOO_TRANSITION, commandMap.consume(session, new Line("FoO")));
  }

  @Test
  public void commandWithSpace() {
    assertEquals(FOO_TRANSITION, commandMap.consume(session, new Line("FOO BAZ")));
  }

  @Test
  public void commandWithOutSpaceNotRecognized() {
    assertUnhandled(commandMap.consume(session, new Line("FOOBAZ")));
  }

  @Test
  public void commandIsEntireLine() {
    assertEquals(FOO_TRANSITION, commandMap.consume(session, new Line("FOO")));
  }

  /* Test just documents the behavior when a missing CRLF means the command is not recognized. */
  @Test
  public void noLineEnding() {
    assertUnhandled(commandMap.consume(session, new Line("FOO".getBytes(StandardCharsets.UTF_8))));
  }

  /* Test just documents the behavior when a missing CRLF means the command is not recognized. */
  @Test
  public void bareNewline() {
    assertUnhandled(
        commandMap.consume(session, new Line("FOO\n".getBytes(StandardCharsets.UTF_8))));
  }

  /* Test just documents the behavior when a missing CRLF means the command is not recognized. */
  @Test
  public void bareCarriageReturn() {
    assertUnhandled(
        commandMap.consume(session, new Line("FOO\r".getBytes(StandardCharsets.UTF_8))));
  }

  @Test
  public void shortLine() {
    Assert.assertThrows(
        IndexOutOfBoundsException.class,
        () -> commandMap.consume(session, new Line("x".getBytes(StandardCharsets.UTF_8))));
  }

  @Test
  public void nullLine() {
    Assert.assertThrows(NullPointerException.class, () -> commandMap.consume(session, null));
  }

  @Test
  public void noHandlerForCommand() {
    assertUnhandled(commandMap.consume(session, new Line("BAZ")));
  }

  @Test
  public void noHandlerForConnect() {
    assertUnhandled(commandMap.connect(session));
  }

  @Test
  public void connect() {
    Transition expected = new Transition(new Reply(220, "Hi"), StandardStates.CONNECTED);

    commandMap =
        new SmtpCommandMap(
            null,
            new CommandHandler() {
              @Override
              public Transition handleCommand(SmtpSession session, Line commandLine) {
                return expected;
              }

              @Override
              public String verb() {
                return null;
              }
            });

    assertEquals(expected, commandMap.connect(session));
  }

  private void assertUnhandled(Transition actual) {
    assertEquals(Transition.UNHANDLED, actual);
  }
}
