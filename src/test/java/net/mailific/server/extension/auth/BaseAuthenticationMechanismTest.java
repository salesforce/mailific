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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;
import net.mailific.server.session.SmtpSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BaseAuthenticationMechanismTest {

  @Mock AuthCheck authCheck;

  @Mock SmtpSession session;

  BaseAuthenticationMechanism it;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    it =
        new BaseAuthenticationMechanism(authCheck) {
          @Override
          public String getName() {
            return null;
          }
        };
  }

  @Test
  public void getSaslServerFactoryClass() {
    assertEquals(it.getSaslServerFactoryClass(), AuthSaslServerFactory.class);
  }

  @Test
  public void available_noTLS() {
    when(session.isTlsStarted()).thenReturn(false);
    assertFalse(it.available(session));
  }

  @Test
  public void available_TLS() {
    when(session.isTlsStarted()).thenReturn(true);
    assertTrue(it.available(session));
  }

  @Test
  public void authCheck() {
    assertEquals(authCheck, it.getAuthCheck());
  }

  @Test
  public void getFactoryProps_containsAuthCheck() {
    Map<String, Object> props = it.getFactoryProps(session);
    Object actual = props.get(Auth.AUTH_CHECK_PROPERTY);
    assertEquals(actual, authCheck);
  }
}
