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
import net.mailific.server.extension.SmtpUtf8;
import net.mailific.server.extension.starttls.StartTls;
import net.mailific.server.reference.BaseMailObjectFactory;
import net.mailific.server.session.SmtpSessionFactory;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ServerConfigTest {

  @Mock SmtpSessionFactory sessionFactory;

  @Mock Extension extension;

  ServerConfig.Builder builder = ServerConfig.builder();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(extension.getEhloKeyword()).thenReturn(StartTls.NAME);

    builder
        .withExtensions(Main.harmlessExtensions())
        .withCommandHandlers(
            Main.baseCommandHandlers("foo", null, new BaseMailObjectFactory()).values())
        .withConnectHandler(new Connect("foo"));
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

  @SuppressWarnings("unchecked")
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
            instanceOf(SmtpUtf8.class), instanceOf(EightBitMime.class), is(extension)));
  }
}
