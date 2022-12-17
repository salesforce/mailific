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

import java.security.Provider;
import java.util.List;

/**
 * Security Provider that can supply SaslServerFactories for a list of Mechanisms.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class SaslMechProvider extends Provider {
  public SaslMechProvider(List<Mechanism> mechanisms) {
    super(
        "SmtpAuthSaslProvider",
        "1.0",
        "Provides SASL server mechanisms for the AUTH smtp extension.");
    for (Mechanism mech : mechanisms) {
      put("SaslServerFactory." + mech.getName(), mech.getSaslServerFactoryClass().getName());
    }
  }
}
