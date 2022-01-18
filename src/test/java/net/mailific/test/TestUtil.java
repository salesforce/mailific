package net.mailific.test;

/*-
 * #%L
 * Mailific SMTP Server Library
 * %%
 * Copyright (C) 2021 - 2022 Joe Humphreys
 * %%
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
 * #L%
 */

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
