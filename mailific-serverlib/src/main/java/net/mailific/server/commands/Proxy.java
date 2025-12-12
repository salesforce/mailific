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

import net.mailific.server.session.*;

/**
 * Implements the PROXY protocol https://www.haproxy.org/download/1.8/doc/proxy-protocol.txt used by
 * HAProxy and nginx to pass client connection information. nginx supports the PROXY protocol when
 * it proxies a mail server, but to do things like SPF, we need to know the original client IP
 * address.
 *
 * <p>This command collects the client IP address from the PROXY command and stores it in the
 * SESSION_CLIENTIP_PROPERTY property of the session. (Perhaps at some point we should add the
 * ability to change the remote address of the SmtpSession.)
 */
public class Proxy extends BaseHandler {
  public static final String SESSION_CLIENTIP_PROPERTY = "proxied-client.ip";

  @Override
  protected Transition handleValidCommand(SmtpSession session, String commandLine) {
    var parts = commandLine.split(" ");
    // line is of the form (TCP6 is also supported):
    // PROXY TCP4 src_ip dst_ip src_port dst_port
    if (parts.length == 6) {
      var clientIp = parts[2];
      session.setProperty(SESSION_CLIENTIP_PROPERTY, clientIp);
    }
    return new Transition(Reply.DO_NOT_REPLY, SessionState.NO_STATE_CHANGE);
  }

  @Override
  protected boolean validForState(SessionState state) {
    return state == StandardStates.CONNECTED;
  }

  @Override
  public String verb() {
    return "PROXY";
  }
}
