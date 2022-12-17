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
package net.mailific.server.session;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import net.mailific.server.LineConsumer;
import net.mailific.server.extension.Extension;

/**
 * SmtpSessionFactory that creates the default implementation of SmtpSession
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class SmtpSessionFactoryImp implements SmtpSessionFactory {

  private LineConsumer lineConsumer;
  private Collection<Extension> extensions;

  public SmtpSessionFactoryImp(LineConsumer lineConsumer, Collection<Extension> extensions) {
    this.lineConsumer = lineConsumer;
    this.extensions = extensions;
  }

  @Override
  public SmtpSession newSmtpSession(InetSocketAddress remoteAddress) {
    return new SmtpSessionImp(
        remoteAddress, this.lineConsumer, Collections.unmodifiableCollection(this.extensions));
  }
}
