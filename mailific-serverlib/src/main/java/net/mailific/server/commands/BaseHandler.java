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
import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.Transition;

/**
 * Base class for SMTP command handlers.
 *
 * <p>Subclasses should override {@link #validForState(SessionState)} to ensure the command is only
 * accepted at the proper time in the session.
 *
 * <p>Alternatively (or in addition), you may override {@link #validForSession(SmtpSession)} if you
 * need to enforce further restrictions (such as an active TLS connection).
 *
 * <p>Override {@link #handleValidCommand(SmtpSession, String)} to do the actual work.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public abstract class BaseHandler implements CommandHandler {

  @Override
  public final Transition handleCommand(SmtpSession session, Line commandLine) {
    if (validForSession(session)) {
      return handleValidCommand(session, commandLine == null ? null : commandLine.getStripped());
    } else {
      return new Transition(Reply._503_BAD_SEQUENCE, SessionState.NO_STATE_CHANGE);
    }
  }

  /**
   * Process the commandLine. Called after the command has already been validated to be callable at
   * this point in the session. This is the main extension point -- override this method to do the
   * actual work of the command.
   *
   * @param session The ongoing SMTP session.
   * @param commandLine The full line passed from the client, without the final CRLF
   * @return A Transition indicating the result of processing the line, or null to indicate that the
   *     line was not consumed;
   */
  protected Transition handleValidCommand(SmtpSession session, String commandLine) {
    return new Transition(Reply._250_OK, SessionState.NO_STATE_CHANGE);
  }

  /**
   * Check whether the command can be called at the current point of the session.
   *
   * <p>Extension point: subclasses can override to add checks. The default implementation just
   * calls {@link #validForState(SessionState)} (and if you override this method, you should
   * consider whether you still want it to call that one).
   *
   * <p>If the method returns false, a Transition with {@link Reply#_503_BAD_SEQUENCE} and no state
   * change will be returned. If you want to send the client a different error reply, return true
   * here and return the desired Transition from {@link #handleValidCommand(SmtpSession, String)}
   *
   * @param session The ongoing SMTP session.
   * @return true if the command can run at this point of the session. False otherwise.
   */
  protected boolean validForSession(SmtpSession session) {
    return validForState(session.getConnectionState());
  }

  /**
   * Check whether the command can be called, given that the session is at the specified
   * SessionState.
   *
   * <p>Extension point: most subclasses should override this to specify which states the command is
   * allowed in. Note that if you override {@link #validForSession(SmtpSession)}, then you must
   * explicitly call this method.
   *
   * <p>If the method returns false, {@link Reply#_503_BAD_SEQUENCE} will be returned. If you want
   * to send the client a different error reply, return true here and return the desired reply from
   * {@link #handleValidCommand(SmtpSession, String) }.
   *
   * @param state State of the ongoing SMTP session.
   * @return true if the command can run at this point of the session. False otherwise.
   */
  protected boolean validForState(SessionState state) {
    return true;
  }
}
