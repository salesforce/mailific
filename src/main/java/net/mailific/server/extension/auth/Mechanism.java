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

import java.util.Map;
import javax.security.sasl.SaslServerFactory;
import net.mailific.server.session.SmtpSession;

/**
 * Provides the data needed to support a SASL Mechanism for the Auth command.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public interface Mechanism {

  /**
   * @return the name to be advertised in the EHLO response. Also used to identify this mechanism
   *     internally.
   */
  String getName();

  /**
   * Answers whether the Mechanism should be supported at this point in the given session. The main
   * use is to require TLS for unencrypted mechanisms. If this method answers false, the EHLO
   * response won't include this mechanism.
   */
  boolean available(SmtpSession session);

  /**
   * @return (no surprise here) a factory for SaslServers that can handle this mechanism.
   */
  Class<? extends SaslServerFactory> getSaslServerFactoryClass();

  /**
   * @return an AuthCheck implementation that will do the actual authentication.
   */
  AuthCheck getAuthCheck();

  /**
   * @return Return the properties to be passed into {@link
   *     SaslServerFactory#createSaslServer(String, String, String, Map,
   *     javax.security.auth.callback.CallbackHandler)}.
   */
  public Map<String, Object> getFactoryProps(SmtpSession session);
}
