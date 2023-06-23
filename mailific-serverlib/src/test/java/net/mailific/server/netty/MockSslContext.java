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

package net.mailific.server.netty;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.ApplicationProtocolNegotiator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import java.util.List;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;

@SuppressWarnings("deprecation")
public class MockSslContext extends SslContext {

  MockSslHandler mockSslHandler;

  public MockSslHandler getSslHandler() {
    return mockSslHandler;
  }

  @Override
  public boolean isClient() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public List<String> cipherSuites() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long sessionCacheSize() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public long sessionTimeout() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public ApplicationProtocolNegotiator applicationProtocolNegotiator() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SSLEngine newEngine(ByteBufAllocator alloc) {
    return new MockSSLEngine(new MockSslSession());
  }

  @Override
  public SSLEngine newEngine(ByteBufAllocator alloc, String peerHost, int peerPort) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SSLSessionContext sessionContext() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected SslHandler newHandler(ByteBufAllocator alloc, boolean startTls) {
    mockSslHandler = new MockSslHandler(newEngine(alloc));
    return mockSslHandler;
  }
}
