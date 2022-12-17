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
 * CommandHandler for handling connections.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class Connect extends BaseHandler {

  // TODO: IP blocklist/allowlist.
  // TODO: constructor that takes multiple banner lines
  private static final Reply _554_NO_SERVICE = new Reply(554, "No SMTP service here");

  private String domain;

  /**
   * @param domain The domain the server should use to identify itself
   */
  public Connect(String domain) {
    this.domain = domain;
  }

  /*
   * Reject the connection with a 554 code, or accept it and return the banner.
   */
  @Override
  public Transition handleValidCommand(SmtpSession session, String commandLine) {
    if (!shouldAllow(session)) {
      return new Transition(_554_NO_SERVICE, StandardStates.CONNECT_REJECTED);
    }
    return new Transition(new Reply(220, bannerLine()), StandardStates.CONNECTED);
  }

  /**
   * Extension point. Subclasses may return false if they don't wish to provide SMTP service. One
   * could block certain IP addresses, for instance.
   *
   * @return true if connection should be allowed, or false if it should be rejected.
   */
  protected boolean shouldAllow(SmtpSession connection) {
    return true;
  }

  /**
   * Extension point. See https://tools.ietf.org/html/rfc5321#section-3.1
   *
   * @return The greeting to send along with the 220 code on a successful connection.
   */
  protected String bannerLine() {
    return domain;
  }

  @Override
  public boolean validForState(SessionState state) {
    return state == StandardStates.BEFORE_CONNECT;
  }

  @Override
  public String verb() {
    return null;
  }
}
