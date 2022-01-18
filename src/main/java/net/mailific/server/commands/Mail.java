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

import java.text.ParseException;
import net.mailific.mailbox.MailboxParser;
import net.mailific.server.MailObject;
import net.mailific.server.MailObjectFactory;
import net.mailific.server.Parameters;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;

/**
 * Handles the MAIL command (RFC5321.4.1.1.2)
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class Mail extends BaseHandler {

  public static final String MAIL = "MAIL";

  private MailObjectFactory mailObjectFactory;

  public Mail(MailObjectFactory mailObjectFactory) {
    this.mailObjectFactory = mailObjectFactory;
  }

  /*
   * The MAIL command initiates a new mail object and specifies the return path
   * for that message.
   */
  @Override
  public Transition handleValidCommand(SmtpSession session, String commandLine) {
    if (!(commandLine.length() > 10)) {
      return new Transition(Reply._500_UNRECOGNIZED, SessionState.NO_STATE_CHANGE);
    }
    if (!("MAIL FROM:".equalsIgnoreCase(commandLine.substring(0, 10)))) {
      return new Transition(Reply._500_UNRECOGNIZED, SessionState.NO_STATE_CHANGE);
    }
    MailObject mailObject = mailObjectFactory.newMailObject(session);
    try {
      Reply reply = mailObject.mailFrom(parseCommandLine(commandLine));
      if (reply.getCode() == 250) {
        session.newMailObject(mailObject);
        return new Transition(reply, StandardStates.AFTER_MAIL);
      } else {
        mailObject.dispose();
        return new Transition(reply, SessionState.NO_STATE_CHANGE);
      }
    } catch (ParseException e) {
      mailObject.dispose();
      // TODO Reply could describe the error better
      return new Transition(Reply._501_BAD_ARGS, SessionState.NO_STATE_CHANGE);
    } catch (RuntimeException e) {
      mailObject.dispose();
      throw e;
    }
  }

  @Override
  public boolean validForState(SessionState state) {
    return state == StandardStates.AFTER_EHLO;
  }

  @Override
  public String verb() {
    return MAIL;
  }

  // TODO: Per rfc 5321#4.1.1.11, we ought to return a 555 if there are parameters
  // not recognized by some extension.
  /** @param line The complete command line, without the CRLF */
  ParsedCommandLine parseCommandLine(String line) throws ParseException {
    final MailboxParser mailboxParser = new MailboxParser(line, 10);
    String mailbox = mailboxParser.getMailbox();
    Parameters params = new Parameters(line, mailboxParser.getPathEnd());
    return new ParsedCommandLine(line, verb(), mailbox, params);
  }
}
