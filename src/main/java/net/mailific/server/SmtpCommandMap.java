/*-
 * Mailific SMTP Server Library
 *
 * Copyright (C) 2021-2022 Joe Humphreys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.mailific.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.mailific.server.commands.CommandHandler;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.Transition;

/**
 * A LineConsumer that handles a number of SMTP commands.
 *
 * <p>When given a line to consume, SmtpCommandMap parses out the first word, and gives the line to
 * the CommandHandler (if any) whose {@link CommandHandler#verb()} method returns that word.
 *
 * <p>If constructed with a connectHandler, then any calls to {@link #connect(SmtpSession)} will be
 * passed to that CommandHandler.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class SmtpCommandMap implements LineConsumer {

  private final Map<String, CommandHandler> map = new HashMap<>();
  private final CommandHandler connectHandler;

  /**
   * @param handlers A set of CommandHandlers for SMTP verbs. May be null.
   * @param connectHandler CommandHandler that will respond to connection events.
   */
  public SmtpCommandMap(Collection<CommandHandler> handlers, CommandHandler connectHandler) {
    if (handlers != null) {
      handlers.forEach(this::putCommandHandler);
    }
    this.connectHandler = connectHandler;
  }

  private void putCommandHandler(CommandHandler handler) {
    map.put(handler.verb().toUpperCase(), handler);
  }

  private CommandHandler getCommandHandlerForLine(Line line) {
    String verb = line.getVerb();
    return map.get(verb.toUpperCase());
  }

  /**
   * Extracts the verb (first word) from the line and looks for a CommandHandler that matches that
   * verb.
   *
   * @param line is assumed to end in CRLF -- no checking is done here.
   * @return The result of {@link CommandHandler#handleCommand(SmtpSession, String)}, if this
   *     SmtpCommandMap has a CommandHandler for this type of command. Otherwise, {@link
   *     Transition#UNHANDLED}.
   * @throws NullPointerException or IndexOutOfBoundsException if line is null or less than 2 chars.
   */
  @Override
  public Transition consume(SmtpSession session, Line line) {
    CommandHandler handler = getCommandHandlerForLine(line);
    if (handler == null) {
      return Transition.UNHANDLED;
    } else {
      return handler.handleCommand(session, line);
    }
  }

  /**
   * @return The result of {@link CommandHandler#handleCommand(SmtpSession, String)}, if this
   *     SmtpCommandMap was constructed with a connectHandler, otherwise {@link
   *     Transition#UNHANDLED}.
   */
  @Override
  public Transition connect(SmtpSession session) {
    if (connectHandler == null) {
      return Transition.UNHANDLED;
    } else {
      return connectHandler.handleCommand(session, null);
    }
  }
}
