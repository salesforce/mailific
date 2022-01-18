package net.mailific.server;

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

import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.Transition;

/**
 * Incoming lines are offered to LineConsumers, which may consume them and return a Transition.
 *
 * <p>As a design note, I've gone back and forth about five times between having connect be a
 * separate method, vs just using consume() and passing a null or other special value in the line
 * param to indicate it's a connect. The latter makes for concise code, but the former is a bit
 * harder to screw up.
 *
 * <p>I went with harder to screw up.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public interface LineConsumer {

  /**
   * Optionally consume a line and return a Transition. It is acceptable to mutate the line,
   * although this only makes sense when returning {@link Transition#UNHANDLED} so that subsequent
   * LineConsumers see the mutated version. (For example, extensions may do this to consume
   * parameters.)
   *
   * @param session The SmtpSession in progress
   * @param line The incoming line, wrapped in a {@link Line}
   * @return A Transition indicating the next step in the session. To indicate that the LineComsumer
   *     did not consume the line, return {@link Transition#UNHANDLED}.
   */
  Transition consume(SmtpSession session, Line line);

  /**
   * Optionally consume the client connection request and return a reply. Since most LineConsumers
   * don't handle initial connections, the default implementation just returns UNHANDLED.
   *
   * @param session The SmtpSession in progress
   * @return A Transition indicating the next step in the session. To indicate that the LineComsumer
   *     did not handle the connection event, return {@link Transition#UNHANDLED}.
   */
  default Transition connect(SmtpSession session) {
    return Transition.UNHANDLED;
  }
}
