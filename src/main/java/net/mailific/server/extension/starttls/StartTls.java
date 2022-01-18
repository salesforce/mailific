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
package net.mailific.server.extension.starttls;

import java.util.Arrays;
import java.util.Collection;
import net.mailific.server.commands.CommandHandler;
import net.mailific.server.extension.BaseExtension;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SmtpSession;

/**
 * Implementation of the STARTTLS extension (RFC2487)
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class StartTls extends BaseExtension {

  // TODO: client cert check?

  public static final String NAME = "STARTTLS";
  public static final Reply _220_READY = new Reply(220, "Ready to start TLS");

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getEhloKeyword() {
    return NAME;
  }

  @Override
  public Collection<CommandHandler> commandHandlers() {
    return Arrays.asList(new StartTlsCommandHandler());
  }

  @Override
  public boolean available(SmtpSession session) {
    return !session.isTlsStarted();
  }
}
