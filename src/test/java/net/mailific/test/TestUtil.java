package net.mailific.test;

import net.mailific.server.Line;
import net.mailific.server.commands.CommandHandler;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.Transition;
import org.mockito.Mockito;

public class TestUtil {
  public static CommandHandler mockCommandHandler(
      SmtpSession session, Line line, Transition reply) {
    CommandHandler handler = Mockito.mock(CommandHandler.class);
    Mockito.when(handler.verb()).thenReturn(line.getVerb());
    Mockito.when(handler.handleCommand(session, line)).thenReturn(reply);
    return handler;
  }
}
