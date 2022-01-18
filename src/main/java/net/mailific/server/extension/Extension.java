package net.mailific.server.extension;

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

import net.mailific.server.LineConsumer;
import net.mailific.server.session.SmtpSession;

/**
 * Interface for an SMTP Extension as described in rfc 1425.
 *
 * <p>Extensions do their work by (a) advertising their presence and (b) supplying new or overridden
 * {@link CommandHandler}s. (In the future, it might be nice to have an easy way for them to just
 * add parameter-processors to existing commands.)
 *
 * <p>Note that a single instance of the Extension is used to serve all requests, so it should be
 * stateless, immutable, and thread-safe. The same is true for any CommandHandler it supplies. If
 * you need to keep state, use {@link SmtpSession#setProperty(String, Object)} to store it in the
 * session.
 *
 * <p>The CommandHandlers you supply are installed in the SmtpSessionFactory once and used for all
 * sessions. That means your handler will be used even when your extension is not available. So if
 * necessary, the CommandHandler should start every command invocation by checking whether the
 * extended functionality is allowed/needed and, if not, fall back to default behavior. A future
 * release will make it possible for an Extension's CommandHandler to pass on a command to any
 * handler it overrode.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public interface Extension {

  /** @return The human-readable name of the extension. */
  String getName();

  /**
   * @return The keyword that identifies support for this extension, as specified in RFC
   *     5321#1.1.1.1
   */
  String getEhloKeyword();

  // TODO: allow multiple EHLO lines with different params?
  /**
   * Provide the ehlo-line that should be included in the EHLO response to advertise support for the
   * extension. If the extension is not available for the passed-in session, then the return value
   * will be ignored.
   *
   * @return The text to appear in the ehlo reply to advertise this extension's availability. This
   *     should start with the EHLO keyword, optionally followed by an ehlo param, as specified in
   *     RFC 5321#1.1.1.1 Do not include the "250" prefix.
   */
  String getEhloAdvertisment(SmtpSession session);

  /**
   * @return true if the extension is available for the given session. For example, some extensions
   *     may return false unless TLS has already been established.
   */
  boolean available(SmtpSession session);

  /**
   * Supply a LineConsumer that implements any special behavior for the extension. This will be
   * placed at the head of the queue of line consumers.
   *
   * @return a LineConsumer, or null if the extension does not need to handle incoming lines.
   */
  LineConsumer getLineConsumer();
}
