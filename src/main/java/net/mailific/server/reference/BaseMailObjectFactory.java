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

package net.mailific.server.reference;

import net.mailific.server.MailObject;
import net.mailific.server.MailObjectFactory;
import net.mailific.server.session.SmtpSession;

/**
 * You will likely want to replace this with an factory that returns your own implementation of
 * MailObject.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class BaseMailObjectFactory implements MailObjectFactory {

  @Override
  public MailObject newMailObject(SmtpSession session) {
    return new BaseMailObject();
  }
}
