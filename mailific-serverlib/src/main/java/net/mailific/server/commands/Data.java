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

import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;

/**
 * CommandHandler for the DATA command.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class Data extends BaseHandler {

  public static final String DATA = "DATA";

  static final String DATA_FILTER_KEY = Data.class.getName() + ".filterKey";

  /*
   * When we receive the DATA command, we transition to a mode where we're accepting
   * message data and building up the mail object. We do this by adding a new
   * filter to the session that will keep consuming lines from the client and
   * treating them as message data until it gets the end-of-message marker (line
   * with just a dot).
   */
  @Override
  public Transition handleValidCommand(SmtpSession session, String commandLine) {
    if (!(commandLine.equalsIgnoreCase(DATA))) {
      // I'm assuming if we got here it at least starts with DATA, so the problem
      // must be that there are unexpected arguments
      return new Transition(Reply._501_BAD_ARGS, SessionState.NO_STATE_CHANGE);
    }
    session.addLineConsumer(DATA_FILTER_KEY, new DataLineConsumer());
    session.getMailObject().prepareForData(session);
    return new Transition(Reply._354_CONTINUE, StandardStates.READING_DATA);
  }

  @Override
  public boolean validForState(SessionState state) {
    return state == StandardStates.AFTER_RCPT;
  }

  @Override
  public String verb() {
    return DATA;
  }
}
