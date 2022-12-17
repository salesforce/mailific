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

import javax.security.sasl.AuthorizeCallback;

/**
 * This interface does the actual checking of credentials.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public interface AuthCheck {

  /**
   * @param authzid Identity the entity wishes to act as, if different from authcid. Passing null is
   *     the same as passing authcid.
   * @param authcid Identity of the entity authenticating.
   * @param credential Authentication credentials
   * @return Result of the authorization.
   */
  public AuthorizeCallback authorize(String authzid, String authcid, byte[] credential);
}
