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

package net.mailific.server;

import static org.junit.Assert.assertEquals;

import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.Transition;
import org.junit.Test;

public class LineConsumerTest {

  @Test
  public void defaultConnect() {
    LineConsumer it =
        new LineConsumer() {

          @Override
          public Transition consume(SmtpSession session, Line line) {
            return null;
          }
        };

    assertEquals(Transition.UNHANDLED, it.connect(null));
  }
}
