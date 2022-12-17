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

import java.util.Objects;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;

/**
 * Handles the HELO command. See https://tools.ietf.org/html/rfc5321#section-4.1.1.1
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class Helo extends BaseHandler {

  public static final String HELO = "HELO";

  private String domain;

  /**
   * @param domain Domain by which the server identifies itself in the HELO reply
   */
  public Helo(String domain) {
    Objects.requireNonNull(domain, "Domain is required.");
    this.domain = domain;
  }

  @Override
  public Transition handleValidCommand(SmtpSession session, String commandLine) {
    session.clearMailObject();
    return new Transition(new Reply(250, domain), StandardStates.AFTER_EHLO);
  }

  @Override
  public boolean validForState(SessionState state) {
    return state == StandardStates.CONNECTED;
  }

  @Override
  public String verb() {
    return HELO;
  }
}
