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
package net.mailific.server.extension;

import java.util.Collection;
import net.mailific.server.LineConsumer;
import net.mailific.server.SmtpCommandMap;
import net.mailific.server.commands.CommandHandler;
import net.mailific.server.session.SingleCommandLineConsumer;
import net.mailific.server.session.SmtpSession;

/**
 * Provides some default behavior for building extensions.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public abstract class BaseExtension implements Extension {

  @Override
  public String getEhloAdvertisment(SmtpSession session) {
    return getEhloKeyword();
  }

  @Override
  public boolean available(SmtpSession session) {
    return true;
  }

  /**
   * @return a {@link LineConsumer} that invokes the handlers returned by {@link
   *     #commandHandlers()}, or null if that method does not return any CommandHandlers.
   */
  @Override
  public LineConsumer getLineConsumer() {
    Collection<CommandHandler> handlers = commandHandlers();
    if (handlers != null && !handlers.isEmpty()) {
      if (handlers.size() == 1) {
        return new SingleCommandLineConsumer(handlers.iterator().next());
      } else {
        return new SmtpCommandMap(handlers, null);
      }
    }
    return null;
  }

  /**
   * Supply a Collection of {@link CommandHandler}s that either add new SMTP verbs handled by the
   * extension, or override any existing CommandHandler for a verb.
   *
   * <p>Whatever handlers this method returns will be offered each line before it's passed to the
   * standard handlers.
   *
   * @return the CommandHandlers to add, or null if the extension doesn't want to add any
   *     CommandHandlers.
   */
  protected Collection<CommandHandler> commandHandlers() {
    return null;
  }
}
