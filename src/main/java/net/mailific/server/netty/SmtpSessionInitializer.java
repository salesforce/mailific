package net.mailific.server.netty;

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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.mailific.server.session.SmtpSessionFactory;

/**
 * For each incoming connection, creates and associates a new SmtpSession.Sets up the channel
 * pipeline with a 5-minute timeout, CRLF-based frame decoder, and SmtpServerHandler.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class SmtpSessionInitializer extends ChannelInitializer<SocketChannel> {

  private static final Logger logger = Logger.getLogger(SmtpSessionInitializer.class.getName());

  private static final StringEncoder STRING_ENCODER = new StringEncoder(StandardCharsets.UTF_8);
  private static final ByteBuf CRLF = Unpooled.wrappedBuffer(new byte[] {'\r', '\n'});
  private SslContext sslContext;
  private SmtpSessionFactory sessionFactory;

  public SmtpSessionInitializer(SmtpSessionFactory sessionFactory, SslContext sslContext) {
    this.sessionFactory = sessionFactory;
    this.sslContext = sslContext;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {

    logger.log(Level.FINE, "INIT_CHANNEL");
    ch.attr(SmtpServerHandler.SESSION_KEY).set(sessionFactory.newSmtpSession(ch.remoteAddress()));

    ChannelPipeline pipeline = ch.pipeline();

    // TODO: allow to enforce shorter line limits.
    // TODO: allow variable timeouts. For now, 300s is recommended by RFC5321.4.5.3.2.7
    pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(300));
    pipeline.addLast("frame", new DelimiterBasedFrameDecoder(2048, false, true, CRLF));
    pipeline.addLast(STRING_ENCODER);
    pipeline.addLast("smtp", new SmtpServerHandler(sslContext));
  }
}
