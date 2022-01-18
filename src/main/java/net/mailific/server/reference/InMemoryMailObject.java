package net.mailific.server.reference;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.mailific.server.session.Reply;

/**
 * A MailObject that caches message data in memory.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public abstract class InMemoryMailObject extends BaseMailObject {

  private ByteArrayOutputStream data;
  private final int initialSize;

  public InMemoryMailObject(int initialSize) {
    this.initialSize = initialSize;
  }

  @Override
  public Reply complete() {
    Reply reply = processFinished(data.toByteArray());
    dispose();
    return reply;
  }

  /**
   * Do something with the message data.
   *
   * @return {@link Reply#_250_OK} unless something went wrong.
   */
  protected abstract Reply processFinished(byte[] messageBytes);

  @Override
  public void dispose() {
    this.data = null;
    super.dispose();
  }

  @Override
  public void prepareForData() {
    this.data = new ByteArrayOutputStream(initialSize);
  }

  @Override
  public void writeLine(byte[] line, int offset, int length) throws IOException {
    data.write(line, offset, length);
  }
}
