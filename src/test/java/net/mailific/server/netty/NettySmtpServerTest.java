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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.concurrent.Future;
import net.mailific.server.ServerConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NettySmtpServerTest {

  NettySmtpServer it;
  ServerConfig.Builder builder;
  int port = 12525;

  @Before
  public void setup() {
    builder = ServerConfig.builder().withListenHost("localhost").withListenPort(port);
    it = new NettySmtpServer(builder.build());
  }

  @After
  public void teardown() throws Exception {
    it.shutdown();
  }

  @Test
  public void start() throws Exception {
    it.start();
  }

  @Test
  public void startTwice() throws Exception {
    Future<Void> future1 = it.start();
    Future<Void> future2 = it.start();
    assertEquals(future1, future2);
  }

  @Test
  public void withTls() throws Exception {
    builder
        .withTlsCert(new File("src/test/resources/certs/cert.pem"))
        .withTlsCertKey(new File("src/test/resources/certs/pk8.pem"));
    it = new NettySmtpServer(builder.build());
    it.start();
  }
}
