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

package net.mailific.server.extension.starttls;

import net.mailific.server.commands.BaseHandler;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;

/**
 * Handles the STARTTLS command. Installed by the StartTls Extension. @Author jhumphreys
 *
 * @since 1.0.0
 */
public class StartTlsCommandHandler extends BaseHandler {

  @Override
  protected Transition handleValidCommand(SmtpSession session, String commandLine) {
    session.clearMailObject();
    prepareForHandshake(session);
    return new Transition(StartTls._220_READY, StandardStates.CONNECTED);
  }

  /**
   * Extension point. Do anything special you need to do to get ready for a TLS handshake. Base
   * implementation does nothing and assumes something is watching for the 220 reply.
   */
  protected void prepareForHandshake(SmtpSession session) {
    //
  }

  @Override
  public String verb() {
    return StartTls.NAME;
  }

  @Override
  protected boolean validForSession(SmtpSession session) {
    return !session.isTlsStarted();
  }
}
