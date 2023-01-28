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

package net.mailific.server.extension.auth;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AuthSaslServerFactoryTest {

  @Mock AuthCheck authCheck;

  Map<String, Object> props = new HashMap<>();

  AuthSaslServerFactory it = new AuthSaslServerFactory();

  private AutoCloseable closeable;

  @Before
  public void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
    props.put(Auth.AUTH_CHECK_PROPERTY, authCheck);
  }

  @After
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  @Test
  public void getMechanismNames() {
    Assert.assertArrayEquals(new String[] {"PLAIN", "LOGIN"}, it.getMechanismNames(null));
  }

  @Test
  public void createSaslServer_plain() throws SaslException {
    SaslServer actual = it.createSaslServer("PLAIN", "smtp", "foo", props, null);
    assertThat(actual, instanceOf(PlainSaslServer.class));
  }

  @Test
  public void createSaslServer_login() throws SaslException {
    SaslServer actual = it.createSaslServer("LOGIN", "smtp", "foo", props, null);
    assertThat(actual, instanceOf(LoginSaslServer.class));
  }

  @Test
  public void createSaslServer_unsupported() throws SaslException {
    SaslServer actual = it.createSaslServer("FOO", "smtp", "foo", props, null);
    assertNull(actual);
  }
}
