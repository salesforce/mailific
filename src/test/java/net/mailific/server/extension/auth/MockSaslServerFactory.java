package net.mailific.server.extension.auth;

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
