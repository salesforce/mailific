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
 * Handles the RSET command. RFC5321.4.1.1.5
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class Rset extends BaseHandler {

  public static final String RSET = "RSET";

  @Override
  public Transition handleValidCommand(SmtpSession connection, String commandLine) {
    // TODO: verify no params
    connection.clearMailObject();
    return new Transition(Reply._250_OK, StandardStates.AFTER_EHLO);
  }

  @Override
  public boolean validForState(SessionState state) {
    return true;
  }

  @Override
  public String verb() {
    return RSET;
  }
}
