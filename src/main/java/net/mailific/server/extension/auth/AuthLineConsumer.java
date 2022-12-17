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

package net.mailific.server.extension.auth;

import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import net.mailific.server.Line;
import net.mailific.server.LineConsumer;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;

/**
 * LineConsumer used by the Auth extension to process SASL responses.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class AuthLineConsumer implements LineConsumer {

  private static final Logger logger = Logger.getLogger(AuthLineConsumer.class.getName());

  private final Mechanism mech;
  private final IAuth authExtension;

  AuthLineConsumer(Mechanism mech, IAuth auth) {
    this.mech = mech;
    this.authExtension = auth;
  }

  @Override
  public Transition consume(SmtpSession session, Line line) {

    // Strip trailing crlf
    byte[] encodedResponse = new byte[line.getLine().length - 2];
    System.arraycopy(line.getLine(), 0, encodedResponse, 0, encodedResponse.length);

    // A line consisting of just * means the client wants to cancel auth
    if (encodedResponse.length == 1 && encodedResponse[0] == '*') {
      try {
        SaslServer saslServer = (SaslServer) session.getProperty(Auth.SASL_SERVER_PROPERTY);
        if (saslServer == null) {
          // Should not occur. TODO find other places we read this property and similarly log.
          throw new SaslException("SaslServer missing. Log a bug.");
        }
        saslServer.dispose();
      } catch (SaslException e) {
        logger.log(Level.SEVERE, "SASL_CANCEL_ERROR", e);
      }
      session.clearProperty(Auth.SASL_SERVER_PROPERTY);
      session.removeLineConsumer(Auth.AUTH_LINE_CONSUMER_SELECTOR);
      return new Transition(Auth._501_CANCELED, StandardStates.AFTER_EHLO);
    }

    byte[] response;
    try {
      response = Base64.getDecoder().decode(encodedResponse);
    } catch (IllegalArgumentException e) {
      // Since it might contain secrets, best not to log the bad response. Sorry.
      logger.info("SASL_DECODE_ERROR: Response was not valid base64");
      return new Transition(Reply._501_BAD_ARGS, SessionState.NO_STATE_CHANGE);
    }

    SaslServer saslServer = (SaslServer) session.getProperty(Auth.SASL_SERVER_PROPERTY);
    try {
      byte[] challenge = saslServer.evaluateResponse(response);
      if (saslServer.isComplete()) {
        session.removeLineConsumer(Auth.AUTH_LINE_CONSUMER_SELECTOR);
        return authExtension.saslCompleted(session, mech, saslServer);
      } else {
        return new Transition(
            authExtension.challengeToReply(challenge), SessionState.NO_STATE_CHANGE);
      }

    } catch (SaslException e) {
      logger.log(Level.INFO, "SASL_EVAL_ERROR", e);
      return new Transition(Reply._554_SERVER_ERROR, SessionState.NO_STATE_CHANGE);
    }
  }
}
