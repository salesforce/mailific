/*
 * Copyright 2021 Joe Humphreys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package net.mailific.server.extension.auth;

import java.util.Base64;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import net.mailific.server.commands.BaseHandler;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;

/**
 * Handler for the Auth command (see RFC4954).
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class AuthCommandHandler extends BaseHandler {

  private final IAuth authExtension;
  private final SaslServerCreator serverCreator;

  AuthCommandHandler(IAuth auth) {
    this.authExtension = auth;
    this.serverCreator = new SaslServerCreator();
  }

  AuthCommandHandler(IAuth auth, SaslServerCreator serverCreator) {
    this.authExtension = auth;
    this.serverCreator = serverCreator;
  }

  @Override
  public String verb() {
    return Auth.AUTH;
  }

  @Override
  protected boolean validForSession(SmtpSession session) {
    SessionState connectionState = session.getConnectionState();
    // TODO: the actual requirements is that it can't be during
    // a mail transaction.
    if (!(connectionState.equals(StandardStates.AFTER_EHLO)
        || connectionState.equals(StandardStates.CONNECTED))) {
      return false;
    }

    // Only one successful AUTH per session is allowed
    if (session.getProperty(Auth.AUTH_RESULTS_PROPERTY) != null) {
      return false;
    }

    return true;
  }

  @Override
  protected Transition handleValidCommand(SmtpSession session, String commandLine) {
    // The command should look like: AUTH <SPACE> <mechanism> [ <SPACE> base64-encoded initial
    // data or =]
    String[] parts = commandLine.split(" ");
    if (parts.length < 2 || parts.length > 3) {
      return new Transition(
          new Reply(504, "Expected AUTH <mechanism>[ initial]"), SessionState.NO_STATE_CHANGE);
    }

    Mechanism mech = authExtension.getMechanism(parts[1].toUpperCase());
    if (mech == null || !mech.available(session)) {
      return new Transition(Reply._504_BAD_PARAM, SessionState.NO_STATE_CHANGE);
    }

    byte[] initialResponse = null;
    if (parts.length == 3) {
      if (parts[2].equals("=")) { // rfc 4954.4 -- "=" means zero-length initial response
        initialResponse = new byte[0];
      } else {
        try {
          initialResponse = Base64.getDecoder().decode(parts[2].trim());
        } catch (IllegalArgumentException e) {
          session.getEventLogger().info("SASL_INIT_ERROR", null, "Response was not valid base64");
          return new Transition(Reply._501_BAD_ARGS, SessionState.NO_STATE_CHANGE);
        }
      }
    }

    SaslServer saslServer;
    // Not sure if there's a constant somewhere in the Java Security morass for SMTP protocol
    try {
      saslServer = serverCreator.createServer(mech, session, authExtension.getServerName());
      if (saslServer == null) {
        throw new SaslException("Can't create server for mechanism " + mech.getName());
      }
      byte[] challenge = saslServer.evaluateResponse(initialResponse);
      if (saslServer.isComplete()) {
        return authExtension.saslCompleted(session, mech, saslServer);
      } else {
        // Sasl exchange is not complete. We'll need to store the server in a session property,
        // in case it has state we want to reuse.
        session.setProperty(Auth.SASL_SERVER_PROPERTY, saslServer);
        session.addLineConsumer(
            Auth.AUTH_LINE_CONSUMER_SELECTOR, new AuthLineConsumer(mech, authExtension));
        return new Transition(
            authExtension.challengeToReply(challenge), SessionState.NO_STATE_CHANGE);
      }
    } catch (SaslException e) {
      // TODO Log this somewhere better
      return new Transition(Reply._501_BAD_ARGS, SessionState.NO_STATE_CHANGE);
    }
  }

  // No hope of unit testing without breaking this out.
  static class SaslServerCreator {
    public SaslServer createServer(Mechanism mech, SmtpSession session, String serverName)
        throws SaslException {
      return Sasl.createSaslServer(
          mech.getName(), "smtp", serverName, mech.getFactoryProps(session), null);
    }
  }
}
