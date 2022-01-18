package net.mailific.server.extension.auth;

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

import java.nio.charset.StandardCharsets;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

/**
 * SaslServer for the Plain mechanism (RFC4616)
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class PlainSaslServer implements SaslServer {

  public static final String NAME = "PLAIN";

  private static final byte[] EMTPY_CHALLENGE = new byte[0];

  private final AuthCheck authcheck;
  private boolean fresh = true;
  private boolean complete = false;
  private AuthorizeCallback result = null;

  public PlainSaslServer(AuthCheck authcheck) {
    this.authcheck = authcheck;
  }

  @Override
  public String getMechanismName() {
    return NAME;
  }

  @Override
  public byte[] evaluateResponse(byte[] message) throws SaslException {
    // The initial response may be empty, in which case we just
    // return an empty challenge to request the auth data.
    // But we should not get a second empty response after that.
    if (message == null || message.length == 0) {
      if (fresh) {
        fresh = false;
        return EMTPY_CHALLENGE;
      } else {
        throw new SaslException("No data included with SASL response.");
      }
    }
    byte[][] messageParts = parseMessage(message);
    result =
        authcheck.authorize(
            new String(messageParts[0], StandardCharsets.UTF_8),
            new String(messageParts[1], StandardCharsets.UTF_8),
            messageParts[2]);
    complete = true;
    return null;
  }

  /*
   * Split the byte array on nulls (0s) into 3 byte arrays
   */
  private byte[][] parseMessage(byte[] message) throws SaslException {
    int firstNul = -1;
    int secondNul = -1;
    int i = 0;
    for (; i < message.length; ++i) {
      if (message[i] == 0) {
        firstNul = i;
        break;
      }
    }
    for (++i; i < message.length; ++i) {
      if (message[i] == 0) {
        secondNul = i;
        break;
      }
    }
    if (secondNul == -1) {
      throw new SaslException("Bad data sent to PLAIN AUTH mechanism");
    }
    byte[][] rv = new byte[3][];
    rv[0] = new byte[firstNul];
    rv[1] = new byte[(secondNul - firstNul) - 1];
    rv[2] = new byte[(message.length - secondNul) - 1];

    System.arraycopy(message, 0, rv[0], 0, firstNul);
    System.arraycopy(message, firstNul + 1, rv[1], 0, rv[1].length);
    System.arraycopy(message, secondNul + 1, rv[2], 0, rv[2].length);

    return rv;
  }

  @Override
  public boolean isComplete() {
    return complete;
  }

  @Override
  public String getAuthorizationID() {
    if (!complete) {
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
    if (!complete) {
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
