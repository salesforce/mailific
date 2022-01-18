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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class PlainSaslServerTest {

  String AUTHZID = "SomeRole";
  String USERNAME = "AzureDiamond";
  String PASSWORD = "hunter2";

  AuthorizeCallback authWithAuthz;
  AuthorizeCallback authNoAuthz;

  PlainSaslServer it;

  @Mock AuthCheck authCheck;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    authWithAuthz = new AuthorizeCallback(USERNAME, AUTHZID);
    authWithAuthz.setAuthorized(true);

    authNoAuthz = new AuthorizeCallback(USERNAME, USERNAME);
    authNoAuthz.setAuthorized(true);

    Mockito.when(authCheck.authorize(AUTHZID, USERNAME, PASSWORD.getBytes("UTF-8")))
        .thenReturn(authWithAuthz);

    Mockito.when(authCheck.authorize("", USERNAME, PASSWORD.getBytes("UTF-8")))
        .thenReturn(authNoAuthz);

    it = new PlainSaslServer(authCheck);
  }

  @Test
  public void getMechanismName() {
    assertEquals("PLAIN", it.getMechanismName());
  }

  @Test
  public void evaluateResponse_null_uninitialized() throws Exception {
    byte[] actual = it.evaluateResponse(null);
    assertEquals(0, actual.length);
  }

  @Test
  public void evaluateResponse_empty_uninitialized() throws Exception {
    byte[] actual = it.evaluateResponse(new byte[0]);
    assertEquals(0, actual.length);
  }

  @Test
  public void evaluateResponse_null_initialized() throws Exception {
    // This is just to make it initialized
    it.evaluateResponse(null);

    assertThrows(
        SaslException.class,
        // action under test
        () -> it.evaluateResponse(null));
  }

  @Test
  public void evaluateResponse_empty_initialized() throws Exception {
    // This is just to make it initialized
    it.evaluateResponse(null);

    Assert.assertThrows(
        SaslException.class,
        // action under test
        () -> it.evaluateResponse(new byte[0]));
  }

  @Test
  public void evaluateResponse_noNulls() throws Exception {
    byte[] message = new String("abcdefg").getBytes("UTF-8");

    assertThrows(
        SaslException.class,
        // action under test
        () -> it.evaluateResponse(message));
  }

  @Test
  public void evaluateResponse_oneNull() throws Exception {
    byte[] message = new String("abc\u0000defg").getBytes("UTF-8");

    assertThrows(
        SaslException.class,
        // action under test
        () -> it.evaluateResponse(message));
  }

  @Test
  public void evaluateResponse_withAuthz() throws Exception {
    byte[] message = (AUTHZID + "\u0000" + USERNAME + "\u0000" + PASSWORD).getBytes("UTF-8");

    it.evaluateResponse(message);

    assertEquals(authWithAuthz, it.getNegotiatedProperty(Auth.AUTH_RESULTS_PROPERTY));
  }

  @Test
  public void evaluateResponse_NoAuthz() throws Exception {
    byte[] message = ("\u0000" + USERNAME + "\u0000" + PASSWORD).getBytes("UTF-8");

    it.evaluateResponse(message);

    assertEquals(authNoAuthz, it.getNegotiatedProperty(Auth.AUTH_RESULTS_PROPERTY));
  }

  @Test
  public void evaluateResponse_after_initial_null() throws Exception {
    byte[] message = (AUTHZID + "\u0000" + USERNAME + "\u0000" + PASSWORD).getBytes("UTF-8");

    it.evaluateResponse(null);
    it.evaluateResponse(message);

    assertEquals(authWithAuthz, it.getNegotiatedProperty(Auth.AUTH_RESULTS_PROPERTY));
  }

  @Test
  public void getAuthorizationID_not_complete() {
    assertThrows(IllegalStateException.class, () -> it.getAuthorizationID());
  }

  @Test
  public void getAuthorizationID_withAuthz() throws Exception {
    byte[] message = (AUTHZID + "\u0000" + USERNAME + "\u0000" + PASSWORD).getBytes("UTF-8");
    it.evaluateResponse(message);

    assertEquals(AUTHZID, it.getAuthorizationID());
  }

  @Test
  public void getAuthorization_noAuthz() throws Exception {
    byte[] message = ("\u0000" + USERNAME + "\u0000" + PASSWORD).getBytes("UTF-8");
    it.evaluateResponse(message);

    assertEquals(USERNAME, it.getAuthorizationID());
  }

  @Test
  public void getNegotiatedProperty_random_property() throws Exception {
    byte[] message = (AUTHZID + "\u0000" + USERNAME + "\u0000" + PASSWORD).getBytes("UTF-8");
    it.evaluateResponse(message);

    assertNull(it.getNegotiatedProperty("foo"));
  }

  @Test
  public void getNegotiatedProperty_not_complete() {
    assertThrows(
        IllegalStateException.class, () -> it.getNegotiatedProperty(Auth.AUTH_RESULTS_PROPERTY));
  }

  @Test
  public void unsupportedMethods() {
    assertThrows(IllegalStateException.class, () -> it.wrap(new byte[1], 0, 1));
    assertThrows(IllegalStateException.class, () -> it.unwrap(new byte[1], 0, 1));
  }

  @Test
  public void dispose() throws SaslException {
    // Does nothing, so nothing to verify
    it.dispose();
  }

  @Test
  public void isComplete() throws Exception {
    byte[] message = (AUTHZID + "\u0000" + USERNAME + "\u0000" + PASSWORD).getBytes("UTF-8");

    assertFalse(it.isComplete());
    it.evaluateResponse(message);
    assertTrue(it.isComplete());
  }
}
