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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.util.concurrent.Future;
import javax.net.ssl.SSLException;
import net.mailific.server.ServerConfig;
import net.mailific.server.SmtpServer;
import net.mailific.util.ChainedFuture;

/**
 * Netty-based implementation of SmtpServer
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class NettySmtpServer implements SmtpServer {

  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;

  private boolean started = false;
  private ChannelFuture startFuture;

  private ServerConfig config;

  public NettySmtpServer(ServerConfig config) {
    this.config = config;
  }

  @Override
  public synchronized ChannelFuture start() throws InterruptedException, SSLException {
    if (started) {
      return startFuture;
    }
    started = true;

    SslContext sslContext = buildSslContext();

    bossGroup = new NioEventLoopGroup();
    workerGroup = new NioEventLoopGroup();
    ServerBootstrap b = new ServerBootstrap();
    b.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new SmtpSessionInitializer(config.getSessionFactory(), sslContext));
    startFuture =
        b.bind(config.getListenHost(), config.getListenPort()).sync().channel().closeFuture();
    return startFuture;
  }

  private SslContext buildSslContext() throws SSLException {
    SslContext sslContext = null;
    if (config.getTlsCert() != null) {
      sslContext =
          SslContextBuilder.forServer(
                  config.getTlsCert(), config.getTlsCertKey(), config.getTlsCertPassword())
              .startTls(true)
              .build();
    }
    return sslContext;
  }

  /** Just shuts down the socket. */
  @Override
  public Future<?> shutdown() {
    ChainedFuture f = new ChainedFuture();
    if (bossGroup != null) {
      f.chain(bossGroup.shutdownGracefully());
    }
    if (workerGroup != null) {
      // TODO: This results in the socket just closing :(
      workerGroup.terminationFuture();
      f.chain(workerGroup.shutdownGracefully());
    }
    return f;
  }
}
