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

import java.util.Collection;
import java.util.Collections;
import net.mailific.server.commands.CommandHandler;

/**
 * Adds support for SMTPUTF8 (RFC1426).
 *
 * <p>The base server already handles 8-bit content, so this extension really just advertises that
 * SMTPUTF8 is acceptable.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class SmtpUtf8 extends BaseExtension {

  public static final String NAME = "Internationalized Email";
  public static final String EHLO_KEYWORD = "SMTPUTF8";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getEhloKeyword() {
    return EHLO_KEYWORD;
  }

  @Override
  public Collection<CommandHandler> commandHandlers() {
    return Collections.emptyList();
  }
}
