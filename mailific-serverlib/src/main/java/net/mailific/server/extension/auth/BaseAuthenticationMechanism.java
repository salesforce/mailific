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

import java.util.HashMap;
import java.util.Map;
import javax.security.sasl.SaslServerFactory;
import net.mailific.server.session.SmtpSession;

/**
 * Provides some default behavior for implementations of {@link Mechanism}
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public abstract class BaseAuthenticationMechanism implements Mechanism {

  protected final AuthCheck authCheck;

  BaseAuthenticationMechanism(AuthCheck authCheck) {
    this.authCheck = authCheck;
  }

  @Override
  public Class<? extends SaslServerFactory> getSaslServerFactoryClass() {
    return AuthSaslServerFactory.class;
  }

  /**
   * @return true if TLS has been established, false otherwise.
   */
  @Override
  public boolean available(SmtpSession session) {
    return session.isTlsStarted();
  }

  @Override
  public AuthCheck getAuthCheck() {
    return authCheck;
  }

  @Override
  public Map<String, Object> getFactoryProps(SmtpSession session) {
    Map<String, Object> props = new HashMap<>();
    props.put(Auth.AUTH_CHECK_PROPERTY, authCheck);
    return props;
  }
}
