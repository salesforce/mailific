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

import java.nio.ByteBuffer;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

class MockSSLEngine extends SSLEngine {

  private SSLSession session;

  public MockSSLEngine(SSLSession sslSession) {
    this.session = sslSession;
  }

  @Override
  public void beginHandshake() throws SSLException {
    // TODO Auto-generated method stub

  }

  @Override
  public void closeInbound() throws SSLException {
    // TODO Auto-generated method stub

  }

  @Override
  public void closeOutbound() {
    // TODO Auto-generated method stub

  }

  @Override
  public Runnable getDelegatedTask() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean getEnableSessionCreation() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String[] getEnabledCipherSuites() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String[] getEnabledProtocols() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public HandshakeStatus getHandshakeStatus() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean getNeedClientAuth() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public SSLSession getSession() {
    return session;
  }

  @Override
  public String[] getSupportedCipherSuites() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String[] getSupportedProtocols() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean getUseClientMode() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean getWantClientAuth() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isInboundDone() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isOutboundDone() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void setEnableSessionCreation(boolean arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setEnabledCipherSuites(String[] arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setEnabledProtocols(String[] arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setNeedClientAuth(boolean arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setUseClientMode(boolean arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setWantClientAuth(boolean arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public SSLEngineResult unwrap(ByteBuffer arg0, ByteBuffer[] arg1, int arg2, int arg3)
      throws SSLException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SSLEngineResult wrap(ByteBuffer[] arg0, int arg1, int arg2, ByteBuffer arg3)
      throws SSLException {
    // TODO Auto-generated method stub
    return null;
  }
}
