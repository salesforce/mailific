package net.mailific.server.extension;

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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BaseExtensionTest {

  @Mock SmtpSession session;

  Collection<CommandHandler> handlers = null;

  Extension it;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

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
