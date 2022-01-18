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

import java.util.Map;
import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;

/**
 * Implementation of SaslServerFactory that can return SaslServers for the PLAIN and LOGIN
 * mechanisms, for use by the {@link Auth} extension.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class AuthSaslServerFactory implements SaslServerFactory {

  @Override
  public SaslServer createSaslServer(
      String mechanism,
      String protocol,
      String serverName,
      Map<String, ?> props,
      CallbackHandler cbh)
      throws SaslException {
    switch (mechanism) { // because I'm totally going to make some more
      case PlainSaslServer.NAME:
        return new PlainSaslServer((AuthCheck) props.get(Auth.AUTH_CHECK_PROPERTY));
      case LoginSaslServer.NAME:
        return new LoginSaslServer((AuthCheck) props.get(Auth.AUTH_CHECK_PROPERTY));
      default:
        return null;
    }
  }

  @Override
  public String[] getMechanismNames(Map<String, ?> props) {
    return new String[] {PlainSaslServer.NAME, LoginSaslServer.NAME};
  }
}
