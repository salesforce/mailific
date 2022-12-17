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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.mailific.server.extension.starttls.StartTls;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SmtpSession;

/**
 * Passes each incoming line to an SmtpSession retrieved from the connection context. Returns the
 * result, looking out for special return values that require special actions.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class SmtpServerHandler extends ChannelInboundHandlerAdapter {

  private static final Logger logger = Logger.getLogger(SmtpServerHandler.class.getName());

  private final SslContext sslContext;

  SmtpServerHandler(SslContext sslContext) {
    this.sslContext = sslContext;
  }

  static final AttributeKey<SmtpSession> SESSION_KEY =
      AttributeKey.valueOf(SmtpServerHandler.class, "SMTP_SESSION");

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    SocketChannel channel = (SocketChannel) ctx.channel();
    if (ctx.executor().isShuttingDown() || ctx.executor().isShutdown()) {
      shutdown(ctx);
    } else {
      SmtpSession session = channel.attr(SESSION_KEY).get();
      Reply result = session.connect();
      ctx.write(result.replyString());
      ctx.flush();
    }
  }

  private void shutdown(ChannelHandlerContext ctx) {
    ChannelFuture future = ctx.write(Reply._421_SHUTTING_DOWN.replyString());
    future.addListener(ChannelFutureListener.CLOSE);
    ctx.flush();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    final ByteBuf buf = (ByteBuf) msg;
    try {
      if (ctx.executor().isShuttingDown() || ctx.executor().isShutdown()) {
        shutdown(ctx);
        return;
      }
      Reply reply = Reply._451_LOCAL_ERROR;

      Channel channel = ctx.channel();
      final SmtpSession session = channel.attr(SESSION_KEY).get();
      byte[] line = byteBufToArray(buf);
      reply = session.consumeLine(line);

      if (reply == StartTls._220_READY) {
        SslHandler sslHandler = sslContext.newHandler(channel.alloc());
        ctx.pipeline().addFirst(sslHandler);
        sslHandler.handshakeFuture().addListener(new TlsStartListener(session));
      }
      if (reply != Reply.DO_NOT_REPLY) {
        ChannelFuture future = ctx.write(reply.replyString());
        // In most cases, the response is turned immediately. But to support buffered responses when
        // clients
        // use Pipelining, certain replies do not cause an immediate flush. Those will be flushed
        // when the
        // input buffer is empty, which results in a call to #channelReadComplete. That also takes
        // care of
        // the case where a non-Pipelining client has issued a command with a non-immediate
        // response. If we
        // trusted clients to only pipeline the commands they should, we could dispense with this
        // flush entirely.
        if (reply.isImmediate()) {
          ctx.flush();
        }
        if (reply.getCode() == 221) {
          // TODO: instead of being triggered by a code, there should probably
          // be a shouldShutDown method in session
          future.addListener(ChannelFutureListener.CLOSE);
        }
      }
    } finally {
      buf.release();
    }
  }

  /**
   * If there are buffered responses (because Pipelining is being used by the client) then they
   * should be flushed whenever the input buffer is empty.
   */
  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }

  private byte[] byteBufToArray(ByteBuf buf) throws IOException {
    byte[] line;
    if (buf.hasArray()) {
      line = new byte[buf.readableBytes()];
      buf.readBytes(line);
    } else {
      ByteArrayOutputStream copyStream = new ByteArrayOutputStream(buf.readableBytes());
      buf.readBytes(copyStream, buf.readableBytes());
      line = copyStream.toByteArray();
    }
    return line;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (cause instanceof ReadTimeoutException) {
      logger.log(
          Level.INFO,
          "CHANNEL_TIMEOUT: Timeout waiting for data from client. Closed connection from %s",
          ctx.channel().remoteAddress());
      ctx.close();
      return;
    }
    logger.log(Level.SEVERE, "CHANNEL_ERROR: UnhandledException", cause);
    ctx.write(Reply._451_LOCAL_ERROR.replyString());
    ctx.flush();
  }

  static class TlsStartListener implements GenericFutureListener<Future<? super Channel>> {
    private final SmtpSession session;

    TlsStartListener(SmtpSession session) {
      this.session = session;
    }

    @Override
    public void operationComplete(Future<? super Channel> future) throws Exception {
      if (future.isSuccess()) {
        session.setTlsStarted(true);
      }
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    SocketChannel channel = (SocketChannel) ctx.channel();
    SmtpSession session = channel.attr(SESSION_KEY).get();
    session.clearMailObject();
    super.channelInactive(ctx);
  }
}
