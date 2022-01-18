package net.mailific.server.commands;

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

import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;

/**
 * Handles the QUIT command. RFC5321.4.1.1.10
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class Quit extends BaseHandler {

  public static final String QUIT = "QUIT";

  /*
   * Returns a 221, which causes the server to close the connection.
   */
  @Override
  public Transition handleValidCommand(SmtpSession connection, String commandLine) {
    return new Transition(Reply._221_OK, StandardStates.ENDING_SESSION);
  }

  @Override
  public boolean validForState(SessionState state) {
    return true;
  }

  @Override
  public String verb() {
    return QUIT;
  }
}
