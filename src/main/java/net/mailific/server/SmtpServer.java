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

import java.util.concurrent.Future;
import javax.net.ssl.SSLException;

/**
 * An SMTP listener on a single host/port.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public interface SmtpServer {

  /**
   * Begin listening for incoming SMTP connections.
   *
   * @return Future<Void>
   * @throws InterruptedException
   */
  public abstract Future<Void> start() throws InterruptedException, SSLException;

  /**
   * Stop listening for new connections. TODO: this should allow at least an option for in-flight
   * sessions to complete. As of now, there are no guarantees except that the server will stop
   * listening.
   *
   * @return a Future that completes when the server is no longer listening, and all in-flight
   *     connections have finished.
   */
  public abstract Future<?> shutdown();
}
