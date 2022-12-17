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

import java.text.ParseException;
import net.mailific.mailbox.MailboxParser;
import net.mailific.server.Parameters;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;

/**
 * Handles the RCPT command. RFC5321.4.1.1.3
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class Rcpt extends BaseHandler {

  public static final String RCPT = "RCPT";

  /*
   * Parse out the forward path, and offer it to the MailObject. The work of
   * deciding whether it's a good recipient and what reply to return is
   * delegated to the MailObject.
   */
  @Override
  public Transition handleValidCommand(SmtpSession connection, String commandLine) {
    if (!(commandLine.length() > 8)) {
      return new Transition(Reply._500_UNRECOGNIZED_BUFFERED, SessionState.NO_STATE_CHANGE);
    }
    if (!("RCPT TO:".equalsIgnoreCase(commandLine.substring(0, 8)))) {
      return new Transition(Reply._500_UNRECOGNIZED_BUFFERED, SessionState.NO_STATE_CHANGE);
    }
    try {
      Reply reply = connection.getMailObject().rcptTo(parseCommandLine(commandLine));
      if (reply.getCode() == 250) {
        return new Transition(reply, StandardStates.AFTER_RCPT);
      } else {
        return new Transition(reply, SessionState.NO_STATE_CHANGE);
      }
    } catch (ParseException e) {
      return new Transition(Reply._501_BAD_ARGS_BUFFERED, SessionState.NO_STATE_CHANGE);
    }
  }

  @Override
  public boolean validForState(SessionState state) {
    return state == StandardStates.AFTER_MAIL || state == StandardStates.AFTER_RCPT;
  }

  @Override
  public String verb() {
    return RCPT;
  }

  /**
   * @param line The command line, with no \r\n
   */
  ParsedCommandLine parseCommandLine(String line) throws ParseException {
    final MailboxParser mailboxParser = new MailboxParser(line, 8);
    String mailbox = mailboxParser.getMailbox();
    return new ParsedCommandLine(
        line, verb(), mailbox, new Parameters(line, mailboxParser.getPathEnd()));
  }
}
