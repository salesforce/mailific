package net.mailific.server.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.List;
import net.mailific.server.Line;
import net.mailific.server.ServerConfig;
import net.mailific.server.extension.Extension;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SmtpSessionFactoryImpTest {

  Line line1 = new Line("line1");
  Line line2 = new Line("line2");

  Transition transition1 = new Transition(new Reply(8, ""), StandardStates.AFTER_RCPT);

  @Mock Extension extension;

  @Mock SmtpEventLogger logger;

  MockHandler commandHandler;

  MockHandler connectHandler;

  SmtpSessionFactoryImp it;

  final InetSocketAddress remoteAddress = new InetSocketAddress(25);

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    commandHandler = new MockHandler(line1.getVerb());
    connectHandler = new MockHandler(null);

    when(extension.getEhloKeyword()).thenReturn("EXT1");

    ServerConfig config =
        ServerConfig.builder()
            .withCommandHandlers(List.of(commandHandler))
            .withConnectHandler(connectHandler)
            .withExtensions(List.of(extension))
            .withLogger(logger)
            .build();

    it = (SmtpSessionFactoryImp) config.getSessionFactory();
  }

  @Test
  public void newSmtpSession() {
    // Need this to verify the extension was passed in
    MockHandler handler = new MockHandler(line2.getVerb());
    when(extension.getLineConsumer()).thenReturn(new SingleCommandLineConsumer(handler));

    // Create a session
    SmtpSession session = it.newSmtpSession(remoteAddress);

    // Verify the session was passed the args we expect
    assertEquals(remoteAddress, session.getRemoteAddress());
    assertEquals(logger, session.getEventLogger());

    // Verify connect handler
    session.connect();
    assertTrue(connectHandler.called());

    // Verify commandHandler
    session.consumeLine(line1.getLine());
    assertTrue(commandHandler.called());
    assertEquals(line1.getStripped(), commandHandler.getConsumed().get(0).getStripped());

    // Verify extensions arg
    session.consumeLine(line2.getLine());
    assertTrue(handler.called());
  }
}
