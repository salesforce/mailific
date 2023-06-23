/*-
 * Mailific SMTP Server Library
 *
 * Copyright (C) 2023 Joe Humphreys
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

package net.mailific.server.netty;

import java.security.Principal;
import java.security.cert.Certificate;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;

public class MockSslSession implements SSLSession {

  @Override
  public byte[] getId() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SSLSessionContext getSessionContext() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long getCreationTime() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public long getLastAccessedTime() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void invalidate() {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean isValid() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void putValue(String name, Object value) {
    // TODO Auto-generated method stub

  }

  @Override
  public Object getValue(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void removeValue(String name) {
    // TODO Auto-generated method stub

  }

  @Override
  public String[] getValueNames() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Certificate[] getLocalCertificates() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Principal getLocalPrincipal() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getCipherSuite() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getProtocol() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getPeerHost() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getPeerPort() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getPacketBufferSize() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getApplicationBufferSize() {
    // TODO Auto-generated method stub
    return 0;
  }
}
