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

import javax.security.sasl.SaslServer;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.Transition;

/**
 * Interfacification of {@link Auth} purely for unit test mockery. Because apparently we still can't
 * figure out that testing tools ought to be built into the language.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public interface IAuth {

  /** @return The FQ hostname that should be passed to SaslMechanisms */
  String getServerName();

  /**
   * @param name Presumably from
   *     https://www.iana.org/assignments/sasl-mechanisms/sasl-mechanisms.xhtml
   * @return a {@link Mechanism} that implements the named mechanism
   */
  Mechanism getMechanism(String name);

  Reply challengeToReply(byte[] challenge);

  Transition saslCompleted(SmtpSession session, Mechanism mech, SaslServer saslServer);
}
