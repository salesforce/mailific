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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import java.nio.charset.StandardCharsets;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class LoginSaslServerTest {

  private static final byte[] USERNAME_CHALLENGE = "Username:".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] PASSWORD_CHALLENGE = "Password:".getBytes(StandardCharsets.US_ASCII);

  String A_USERNAME = "AzureDiamond";
  final byte[] A_USERNAME_AS_BYTES = A_USERNAME.getBytes(StandardCharsets.UTF_8);
  byte[] A_PASSWORD = "hunter2".getBytes(StandardCharsets.UTF_8);

  AuthorizeCallback authCallback;

  LoginSaslServer it;

  @Mock AuthCheck authCheck;

  private AutoCloseable closeable;

  @Before
  public void setUp() throws Exception {
    closeable = MockitoAnnotations.openMocks(this);

    authCallback = new AuthorizeCallback(A_USERNAME, A_USERNAME);
    authCallback.setAuthorized(true);

    Mockito.when(authCheck.authorize(null, A_USERNAME, A_PASSWORD)).thenReturn(authCallback);

    it = new LoginSaslServer(authCheck);
  }

  @After
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  @Test
  public void getMechanismName() {
    assertEquals("LOGIN", it.getMechanismName());
  }

  @Test
  public void evaluateResponse_initialResponseNotAllowed() {
    Assert.assertThrows(
        SaslException.class, () -> it.evaluateResponse("Something".getBytes("UTF-8")));
  }

  @Test
  public void evaluateResponse_initialNull() throws Exception {
    byte[] challenge = it.evaluateResponse(null);
    assertArrayEquals(USERNAME_CHALLENGE, challenge);
  }

  @Test
  public void evaluateResponse_initialEmpty() throws Exception {
    byte[] challenge = it.evaluateResponse(new byte[0]);
    assertArrayEquals(USERNAME_CHALLENGE, challenge);
  }

  @Test
  public void evaluateResponse_pwChallenge() throws Exception {
    it.evaluateResponse(null);
    byte[] challenge = it.evaluateResponse(A_USERNAME_AS_BYTES);
    assertArrayEquals(PASSWORD_CHALLENGE, challenge);
  }

  @Test
  public void evaluateResponse_nullChallengeOnComplete() throws Exception {
    it.evaluateResponse(null);
    it.evaluateResponse(A_USERNAME_AS_BYTES);
    byte[] challenge = it.evaluateResponse(A_PASSWORD);
    assertNull(challenge);
  }

  @Test
  public void evaluateResponse_credsPassedToAuthCheck() throws Exception {
    it.evaluateResponse(null);
    it.evaluateResponse(A_USERNAME_AS_BYTES);
    it.evaluateResponse(A_PASSWORD);

    Mockito.verify(authCheck).authorize(isNull(), eq(A_USERNAME), eq(A_PASSWORD));
  }

  @Test
  public void evaluateResponse_nullUN_PW_passedToAuthCheck() throws Exception {
    it.evaluateResponse(null);
    it.evaluateResponse(null);
    it.evaluateResponse(null);

    Mockito.verify(authCheck).authorize(isNull(), isNull(), isNull());
  }

  @Test
  public void evaluateResponse_dataAfterComplete() throws Exception {
    it.evaluateResponse(null);
    it.evaluateResponse(A_USERNAME_AS_BYTES);
    it.evaluateResponse(A_PASSWORD);

    assertThrows(SaslException.class, () -> it.evaluateResponse("hi".getBytes()));
  }

  @Test
  public void incompleteUntilCompleted() throws SaslException {
    assertFalse(it.isComplete());
    it.evaluateResponse(null);
    assertFalse(it.isComplete());
    it.evaluateResponse(A_USERNAME_AS_BYTES);
    assertFalse(it.isComplete());
    it.evaluateResponse(A_PASSWORD);
    assertTrue(it.isComplete());
  }

  @Test
  public void getAuthorizationID_beforeComplete() {
    assertThrows(IllegalStateException.class, () -> it.getAuthorizationID());
  }

  @Test
  public void getAuthorizationID() throws Exception {
    // Complete successfully first
    it.evaluateResponse(null);
    it.evaluateResponse(A_USERNAME_AS_BYTES);
    it.evaluateResponse(A_PASSWORD);

    assertEquals(A_USERNAME, it.getAuthorizationID());
  }

  @Test
  public void getNegotiatedProperty_beforeComplete() {
    assertThrows(
        IllegalStateException.class, () -> it.getNegotiatedProperty(Auth.AUTH_RESULTS_PROPERTY));
  }

  @Test
  public void getNegotiatedProperty() throws SaslException {
    // Complete successfully first
    it.evaluateResponse(null);
    it.evaluateResponse(A_USERNAME_AS_BYTES);
    it.evaluateResponse(A_PASSWORD);

    Object actual = it.getNegotiatedProperty(Auth.AUTH_RESULTS_PROPERTY);

    assertEquals(authCallback, actual);
  }

  @Test
  public void getNegotiatedProperty_unknownProperty() throws SaslException {
    // Complete successfully first
    it.evaluateResponse(null);
    it.evaluateResponse(A_USERNAME_AS_BYTES);
    it.evaluateResponse(A_PASSWORD);

    Object actual = it.getNegotiatedProperty("some.madeup.property");

    assertNull(actual);
  }

  @Test
  public void unsupported() {
    assertThrows(IllegalStateException.class, () -> it.wrap(null, 0, 0));
    assertThrows(IllegalStateException.class, () -> it.unwrap(null, 0, 0));
  }

  @Test
  public void dispose() throws SaslException {
    // does nothing
    it.dispose();
  }
}
