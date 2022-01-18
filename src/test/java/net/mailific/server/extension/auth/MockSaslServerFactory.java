package net.mailific.server.extension.auth;

import java.util.Map;
import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;
import org.mockito.Mockito;

public class MockSaslServerFactory implements SaslServerFactory {

  SaslServer mockSaslServer = Mockito.mock(SaslServer.class);

  @Override
  public SaslServer createSaslServer(
      String mechanism,
      String protocol,
      String serverName,
      Map<String, ?> props,
      CallbackHandler cbh)
      throws SaslException {
    return mockSaslServer;
  }

  @Override
  public String[] getMechanismNames(Map<String, ?> props) {
    return null;
  }
}
