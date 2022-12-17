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

import java.security.Security;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import net.mailific.server.commands.CommandHandler;
import net.mailific.server.extension.BaseExtension;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;

// I dithered extensively over whether to use the javax.security.sasl package.
// It provides absolutely nothing of value for the two mechanisms (PLAIN and LOGIN)
// that I intend to implement now, and it imposes a load of nonsense like loading
// factories via reflection, passing arguments around as property maps,
// and the utterly inscrutable Callback framework.

// Ultimately I decided to use it for a few reasons:
//
// 1. If we ever want to support some of the more complicated SASL mechanisms,
//    it will be much easier to use the ones included with the JDK.
//
// 2. I found myself implementing methods that were pretty close to
//    what is already spec'd in the JDK interfaces.
//
// 3. I've never done this before, so I thought maybe as I went along I would
//    have an a-ha moment and realize the java sasl package is actually awesome.
//    But ... that never happened. And I still don't understand
//    what servers are supposed to do with a CallbackHandler.

/**
 * Provides support for the SMTP AUTH extension (see RFC4954).
 *
 * <p>To use this extension, follow these steps:
 *
 * <ol>
 *   <li>Implement the {@link AuthCheck} interface to do the actual authentication.
 *   <li>Use your Authcheck to construct a {@link PlainMechanism} and/or {@link LoginMechanism}.
 *   <li>Use your Mechanisms to construct an Auth extension (this class).
 *   <li>Include the Auth extension in the {@link net.mailific.server.ServerConfig}.
 * </ol>
 *
 * There's an example of all this in {@link net.mailific.main.Main} (see the code for the
 * getTestAuthExtension method).
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class Auth extends BaseExtension implements IAuth {

  private static final Logger logger = Logger.getLogger(Auth.class.getName());

  public static final String AUTH = "AUTH";

  // Also used as a sasl negotiated property name to pull actual auth results as returned by the
  // auth check
  /** Used to store and retrieve auth results in the SmtpSession's properties. */
  public static final String AUTH_RESULTS_PROPERTY = Auth.class.getName() + ".AuthResults";

  // Used as a property name to pass the AuthCheck implementation into the server factory
  static final String AUTH_CHECK_PROPERTY = "AuthCheck";

  // Used to store the SaslServer in the SmtpSession properties
  static final String SASL_SERVER_PROPERTY = Auth.class.getName() + ".SaslServer";

  // Used as the selector for adding a line filter
  static final String AUTH_LINE_CONSUMER_SELECTOR = Auth.class.getName() + ".Consumer";

  public static Reply _235_AUTH_SUCCESS = new Reply(235, "Authentication successful");
  public static Reply _535_AUTH_FAILURE = new Reply(535, "Authentication failed");

  private final Map<String, Mechanism> mechanisms = new HashMap<>();
  private final String serverName;

  public static Reply _501_CANCELED = new Reply(501, "Authentication canceled");

  /**
   * @param serverName The FQ hostname that should be passed to SaslMechanisms
   */
  public Auth(List<Mechanism> mechanisms, String serverName) {
    this.serverName = serverName;
    mechanisms.forEach(m -> this.mechanisms.put(m.getName().toUpperCase(), m));
    Security.insertProviderAt(new SaslMechProvider(mechanisms), 0);
  }

  @Override
  public String getName() {
    return "Authentication";
  }

  @Override
  public String getEhloKeyword() {
    return AUTH;
  }

  @Override
  public String getEhloAdvertisment(SmtpSession session) {
    // Note that we expect at least one available mechanism,
    // otherwise we would have returned false when asked if this
    // extension is available, and this method should never
    // have been called.
    String available =
        mechanisms.values().stream()
            .filter(m -> m.available(session))
            .map(Mechanism::getName)
            .collect(Collectors.joining(" "));
    return AUTH + " " + available;
  }

  @Override
  public boolean available(SmtpSession session) {
    return mechanisms.values().stream().anyMatch(m -> m.available(session));
  }

  @Override
  public Collection<CommandHandler> commandHandlers() {
    return List.of(new AuthCommandHandler(this));
  }

  @Override
  public String getServerName() {
    return serverName;
  }

  @Override
  public Mechanism getMechanism(String name) {
    return mechanisms.get(name);
  }

  @Override
  public Reply challengeToReply(byte[] challenge) {
    String challengeString = "";
    if (challenge != null && challenge.length > 0) {
      challengeString = Base64.getEncoder().encodeToString(challenge);
    }
    // Note that in case of an empty challenge, this will result in an SMTP
    // reply of "334 " (with a trailing space). That is correct per rfc 4954.
    return new Reply(334, challengeString);
  }

  @Override
  public Transition saslCompleted(SmtpSession session, Mechanism mech, SaslServer saslServer) {
    final AuthorizeCallback authResult =
        (AuthorizeCallback) saslServer.getNegotiatedProperty(AUTH_RESULTS_PROPERTY);
    try {
      saslServer.dispose();
    } catch (SaslException e) {
      logger.log(Level.SEVERE, "SASL_DISPOSE_ERROR", e);
    }
    session.clearProperty(SASL_SERVER_PROPERTY);
    if (authResult.isAuthorized()) {
      session.setProperty(AUTH_RESULTS_PROPERTY, authResult);
      return new Transition(_235_AUTH_SUCCESS, StandardStates.AFTER_EHLO);
    } else {
      // TODO: spec allows for a more specific error
      return new Transition(_535_AUTH_FAILURE, StandardStates.AFTER_EHLO);
    }
  }
}
