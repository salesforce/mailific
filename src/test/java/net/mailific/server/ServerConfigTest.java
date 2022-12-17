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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Collection;
import net.mailific.main.Main;
import net.mailific.server.commands.Connect;
import net.mailific.server.extension.EightBitMime;
import net.mailific.server.extension.Extension;
import net.mailific.server.extension.Pipelining;
import net.mailific.server.extension.SmtpUtf8;
import net.mailific.server.extension.starttls.StartTls;
import net.mailific.server.reference.BaseMailObjectFactory;
import net.mailific.server.session.SmtpSessionFactory;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ServerConfigTest {

  @Mock SmtpSessionFactory sessionFactory;

  @Mock Extension extension;

  ServerConfig.Builder builder = ServerConfig.builder();

  private AutoCloseable closeable;

  @Before
  public void setUp() throws Exception {
    closeable = MockitoAnnotations.openMocks(this);

    when(extension.getEhloKeyword()).thenReturn(StartTls.NAME);

    builder
        .withExtensions(Main.harmlessExtensions())
        .withCommandHandlers(
            Main.baseCommandHandlers("foo", null, new BaseMailObjectFactory()).values())
        .withConnectHandler(new Connect("foo"));
  }

  @After
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  /**
   * Populate the builder with all the stuff to make a SessionFactory, but also with a
   * SessionFactory. Verify that the explicitly passed SessionFactory is used.
   */
  @Test
  public void sessionFactorySpecified() {
    builder
        .withSessionFactory(sessionFactory)
        .withExtensions(Main.harmlessExtensions())
        .withCommandHandlers(
            Main.baseCommandHandlers("foo", null, new BaseMailObjectFactory()).values())
        .withConnectHandler(new Connect("foo"));

    ServerConfig config = builder.build();

    assertEquals(sessionFactory, config.getSessionFactory());
  }

  @Test
  public void sessionFactoryCreated() {
    ServerConfig config = builder.build();

    assertNotNull(config.getSessionFactory());
  }

  @Test
  public void additionalExtensions() {
    SmtpUtf8 utf8Extension = new SmtpUtf8();
    builder.withAdditionalExtension(extension).withAdditionalExtension(utf8Extension);

    ServerConfig config = builder.build();
    Collection<Extension> sessionExtensions =
        config
            .getSessionFactory()
            .newSmtpSession(new InetSocketAddress(1234))
            .getSupportedExtensions();

    assertThat(sessionExtensions, hasItem(extension));
    // verify the added SmtpUtf8 replaces the one that was already in there.
    assertThat(sessionExtensions, hasItem(utf8Extension));
  }

  @Test
  public void startTlsAdded() {
    builder.withTlsCert(new File("foo"));

    ServerConfig config = builder.build();
    Collection<Extension> sessionExtensions =
        config
            .getSessionFactory()
            .newSmtpSession(new InetSocketAddress(1234))
            .getSupportedExtensions();

    assertThat(sessionExtensions, hasItem(instanceOf(StartTls.class)));
  }

  @Test
  public void startTlsNotReplaced() {
    builder.withTlsCert(new File("foo")).withAdditionalExtension(extension);

    ServerConfig config = builder.build();
    Collection<Extension> sessionExtensions =
        config
            .getSessionFactory()
            .newSmtpSession(new InetSocketAddress(1234))
            .getSupportedExtensions();

    assertThat(
        sessionExtensions,
        IsIterableContainingInAnyOrder.containsInAnyOrder(
            instanceOf(SmtpUtf8.class),
            instanceOf(EightBitMime.class),
            instanceOf(Pipelining.class),
            is(extension)));
  }
}
