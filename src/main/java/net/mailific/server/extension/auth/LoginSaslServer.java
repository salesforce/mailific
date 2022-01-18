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

import java.nio.charset.StandardCharsets;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

/**
 * SaslServer that handles the Login Mechanism.
 *
 * <p>https://datatracker.ietf.org/doc/html/draft-murchison-sasl-login-00
 *
 * <p>This is an obsolete Mechanism (according to the IANA registry at
 * https://www.iana.org/assignments/sasl-mechanisms/sasl-mechanisms.xhtml ) but it happens to be the
 * one Microsoft supports.
 *
 * <p>There are some ambiguities in the spec and I had intended to resolve them in favor of interop
 * with Outlook, but I don't remember whether I got around to doing that.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class LoginSaslServer implements SaslServer {

  public static final String NAME = "LOGIN";

  // The example in the obsolete draft spec shows null characters at the end of the
  // challenge, but the text doesn't mention that, and nothing in the SASL or SMTP AUTH
  // spec supports it. I dunno. I think the values here are what Exchange uses.

  private static final byte[] USERNAME_CHALLENGE = "Username:".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] PASSWORD_CHALLENGE = "Password:".getBytes(StandardCharsets.US_ASCII);

  private static enum PHASE {
    INITIAL,
    USERNAME_CHALLENGE_SENT,
    PASSWORD_CHALLENGE_SENT,
    COMPLETE
  }

  private final AuthCheck authcheck;
  private PHASE phase = PHASE.INITIAL;
  private AuthorizeCallback result = null;
  private String username;

  public LoginSaslServer(AuthCheck authcheck) {
    this.authcheck = authcheck;
  }

  @Override
  public String getMechanismName() {
    return NAME;
  }

  @Override
  public byte[] evaluateResponse(byte[] message) throws SaslException {
    switch (phase) {
      case INITIAL:
        if (message != null && message.length > 0) {
          throw new SaslException("LOGIN mechanism does not allow initial response.");
        }
        phase = PHASE.USERNAME_CHALLENGE_SENT;
        return USERNAME_CHALLENGE;
      case USERNAME_CHALLENGE_SENT:
        username = message == null ? null : new String(message, StandardCharsets.UTF_8);
        phase = PHASE.PASSWORD_CHALLENGE_SENT;
        return PASSWORD_CHALLENGE;
      case PASSWORD_CHALLENGE_SENT:
        // TODO: scan for banned chars
        result = authcheck.authorize(null, username, message);
        phase = PHASE.COMPLETE;
        return null;
      default:
        throw new SaslException("Data after sasl exchange complete.");
    }
  }

  @Override
  public boolean isComplete() {
    return phase == PHASE.COMPLETE;
  }

  @Override
  public String getAuthorizationID() {
    if (!isComplete()) {
      throw new IllegalStateException(
          "getAuthorizationID() cannot be called until session is complete.");
    }
    return result.getAuthorizationID();
  }

  @Override
  public byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException {
    throw new IllegalStateException("No protection negotiated.");
  }

  @Override
  public byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException {
    throw new IllegalStateException("No protection negotiated.");
  }

  @Override
  public Object getNegotiatedProperty(String propName) {
    if (!isComplete()) {
      throw new IllegalStateException("Sasl negotiotion is incomplete.");
    }
    if (propName.equals(Auth.AUTH_RESULTS_PROPERTY)) {
      return result;
    }
    return null;
  }

  @Override
  public void dispose() throws SaslException {
    // Nothing to dispose
  }
}
