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
package net.mailific.server.commands;

import net.mailific.server.Line;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.Transition;

/**
 * Processor for an SMTP command
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public interface CommandHandler {

  /**
   * Process the command.
   *
   * <p>It is acceptable to alter the Line for later handlers by calling {@link
   * Line#setLine(byte[])}. For example, a CommandHandler might want to process some parameters,
   * remove them from the line, and then return {@link Transition#UNHANDLED} to allow another
   * handler to process the command
   *
   * @param session The ongoing SMTP session.
   * @param commandLine The line passed from the client.
   * @return a {@link Transition} that specifies the reply to send back to the client, and the next
   *     session state.
   */
  Transition handleCommand(SmtpSession session, Line commandLine);

  /**
   * @return The verb that begins the command line for this command, e.g. "MAIL" for the MAIL FROM
   *     command.
   */
  String verb();
}
