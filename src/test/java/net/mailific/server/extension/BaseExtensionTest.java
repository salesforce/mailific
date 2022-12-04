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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.mailific.server.Line;
import net.mailific.server.LineConsumer;
import net.mailific.server.commands.CommandHandler;
import net.mailific.server.session.MockHandler;
import net.mailific.server.session.SmtpSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BaseExtensionTest {

  @Mock SmtpSession session;

  Collection<CommandHandler> handlers = null;

  Extension it;

  private AutoCloseable closeable;

  @Before
  public void setUp() throws Exception {
    closeable = MockitoAnnotations.openMocks(this);

    it =
        new BaseExtension() {

          @Override
          public String getName() {
            return "Foo";
          }

          @Override
          public String getEhloKeyword() {
            return "FOO";
          }

          @Override
          public Collection<CommandHandler> commandHandlers() {
            return handlers;
          }
        };
  }

  @After
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  @Test
  public void defaultEhloAdvertisement() {
    assertEquals("FOO", it.getEhloAdvertisment(session));
  }

  @Test
  public void defaultAvailable() {
    assertTrue(it.available(session));
  }

  @Test
  public void getLineConsume_nullHandlers() {
    it =
        new BaseExtension() {

          @Override
          public String getName() {
            return "Foo";
          }

          @Override
          public String getEhloKeyword() {
            return "FOO";
          }
        };
    assertNull(it.getLineConsumer());
  }

  @Test
  public void getLineConsume_emptyHandlers() {
    it =
        new BaseExtension() {

          @Override
          public String getName() {
            return "Foo";
          }

          @Override
          public String getEhloKeyword() {
            return "FOO";
          }

          @Override
          public Collection<CommandHandler> commandHandlers() {
            return Collections.emptyList();
          }
        };
    assertNull(it.getLineConsumer());
  }

  @Test
  public void getLineConsumer_oneHandler() throws Exception {
    MockHandler handler = new MockHandler("foo");
    handlers = List.of(handler);

    LineConsumer consumer = it.getLineConsumer();
    consumer.consume(session, new Line("foo"));

    assertTrue(handler.called());
  }

  @Test
  public void getLineConsumer_multipleHandlers() throws Exception {
    MockHandler handler1 = new MockHandler("foo");
    MockHandler handler2 = new MockHandler("bar");
    handlers = List.of(handler1, handler2);

    LineConsumer consumer = it.getLineConsumer();
    consumer.consume(session, new Line("foo baz"));
    assertTrue(handler1.called());
    assertFalse(handler2.called());

    consumer.consume(session, new Line("bar baz"));
    assertTrue(handler2.called());
  }
}
