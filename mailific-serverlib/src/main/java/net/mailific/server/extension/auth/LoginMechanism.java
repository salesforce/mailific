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

/**
 * Implementation of the Login SASL Mechanism.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class LoginMechanism extends BaseAuthenticationMechanism {

  public LoginMechanism(AuthCheck authCheck) {
    super(authCheck);
  }

  @Override
  public String getName() {
    return LoginSaslServer.NAME;
  }
}
