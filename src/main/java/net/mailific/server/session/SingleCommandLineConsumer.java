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

package net.mailific.server.session;

import net.mailific.server.Line;
import net.mailific.server.LineConsumer;
import net.mailific.server.commands.CommandHandler;

/**
 * That's "Single-Command Line Consumer" not "Single Command-Line Consumer". Limitations of camel
 * case.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class SingleCommandLineConsumer implements LineConsumer {

  private final CommandHandler handler;

  public SingleCommandLineConsumer(CommandHandler handler) {
    this.handler = handler;
  }

  /**
   * If the line starts with {@link #handler}'s verb, then consume it. Otherwise return {@link
   * Transition#UNHANDLED}.
   */
  @Override
  public Transition consume(SmtpSession session, Line line) {
    if (handler.verb().equalsIgnoreCase(line.getVerb())) {
      return handler.handleCommand(session, line);
    }
    return Transition.UNHANDLED;
  }
}
