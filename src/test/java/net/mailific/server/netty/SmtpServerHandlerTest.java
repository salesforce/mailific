package net.mailific.server.netty;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.Attribute;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import net.mailific.server.extension.starttls.StartTls;
import net.mailific.server.netty.SmtpServerHandler.TlsStartListener;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SmtpEventLogger;
import net.mailific.server.session.SmtpSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SmtpServerHandlerTest {

  @Mock ChannelHandlerContext ctx;

  @Mock SocketChannel socketChannel;

  @Mock EventExecutor eventExecutor;

  @Mock ChannelFuture channelFuture;

  @Mock SmtpEventLogger logger;

  @Mock SmtpSession session;

  @Mock Attribute<SmtpSession> sessionAttr;

  @Mock ChannelPipeline pipeline;

  @Mock Future<Channel> sslHandlerFuture;

  MockSslContext sslContext;

  InetSocketAddress remoteHost = new InetSocketAddress("remote.host", 1234);

  SmtpServerHandler it;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(ctx.executor()).thenReturn(eventExecutor);
    when(ctx.channel()).thenReturn(socketChannel);
    when(ctx.write(any())).thenReturn(channelFuture);
    when(ctx.pipeline()).thenReturn(pipeline);

    when(sessionAttr.get()).thenReturn(session);

    when(socketChannel.attr(SmtpServerHandler.SESSION_KEY)).thenReturn(sessionAttr);
    when(socketChannel.remoteAddress()).thenReturn(remoteHost);

    when(session.connect()).thenReturn(new Reply(220, "example.com"));

    sslContext = new MockSslContext();

    it = new SmtpServerHandler(sslContext, logger);
  }

  @Test
  public void channelActive_isShuttingDown() throws Exception {
    when(eventExecutor.isShuttingDown()).thenReturn(true);
    it.channelActive(ctx);
    verifyShuttingDown();
  }

  private void verifyShuttingDown() {
    verify(ctx).write("421 Server shutting down\r\n");
    verify(ctx).flush();
    verify(channelFuture).addListener(ChannelFutureListener.CLOSE);
  }

  @Test
  public void channelActive_isShutDown() throws Exception {
    when(eventExecutor.isShutdown()).thenReturn(true);
    it.channelActive(ctx);
    verifyShuttingDown();
  }

  @Test
  public void channelActive() throws Exception {
    it.channelActive(ctx);

    verify(ctx).write("220 example.com\r\n");
    verify(ctx).flush();
  }

  @Test
  public void channelRead_shuttingDown() throws Exception {
    when(eventExecutor.isShuttingDown()).thenReturn(true);
    ByteBuf buf = new EmptyByteBuf(ByteBufAllocator.DEFAULT);

    it.channelRead(ctx, buf);

    verifyShuttingDown();
  }

  @Test
  public void channelRead_shutDown() throws Exception {
    when(eventExecutor.isShutdown()).thenReturn(true);
    ByteBuf buf = new EmptyByteBuf(ByteBufAllocator.DEFAULT);

    it.channelRead(ctx, buf);

    verifyShuttingDown();
  }

  @Test
  public void channelRead_noReply() throws Exception {
    final String line = "foo\r\n";
    MockByteBuf buf = new MockByteBuf(line);
    ArgumentCaptor<byte[]> lineCaptor = ArgumentCaptor.forClass(byte[].class);
    when(session.consumeLine(lineCaptor.capture())).thenReturn(Reply.DO_NOT_REPLY);

    it.channelRead(ctx, buf);

    assertArrayEquals(line.getBytes("UTF-8"), lineCaptor.getValue());
    verify(ctx, never()).write(any());
    verify(ctx, never()).flush();
    assertTrue(buf.released);
  }

  @Test
  public void channelRead_withReply() throws Exception {
    final String line = "foo\r\n";
    MockByteBuf buf = new MockByteBuf(line);
    ArgumentCaptor<byte[]> lineCaptor = ArgumentCaptor.forClass(byte[].class);
    when(session.consumeLine(lineCaptor.capture())).thenReturn(Reply._250_OK);

    it.channelRead(ctx, buf);

    assertArrayEquals(line.getBytes("UTF-8"), lineCaptor.getValue());
    verify(ctx).write(Reply._250_OK.replyString());
    verify(ctx).flush();
    verify(channelFuture, never()).addListener(ChannelFutureListener.CLOSE);
    assertTrue(buf.released);
  }

  @Test
  public void channelRead_withReply_ArrayBackedByteBuf() throws Exception {
    final String line = "foo\r\n";
    MockByteBuf buf = new MockByteBuf(line);
    buf.hasArray = true;
    ArgumentCaptor<byte[]> lineCaptor = ArgumentCaptor.forClass(byte[].class);
    when(session.consumeLine(lineCaptor.capture())).thenReturn(Reply._250_OK);

    it.channelRead(ctx, buf);

    assertArrayEquals(line.getBytes("UTF-8"), lineCaptor.getValue());
    verify(ctx).write(Reply._250_OK.replyString());
    verify(ctx).flush();
    verify(channelFuture, never()).addListener(ChannelFutureListener.CLOSE);
    assertTrue(buf.released);
  }

  @Test
  public void channelRead_with221Reply() throws Exception {
    final String line = "foo\r\n";
    MockByteBuf buf = new MockByteBuf(line);
    ArgumentCaptor<byte[]> lineCaptor = ArgumentCaptor.forClass(byte[].class);
    when(session.consumeLine(lineCaptor.capture())).thenReturn(Reply._221_OK);

    it.channelRead(ctx, buf);

    assertArrayEquals(line.getBytes("UTF-8"), lineCaptor.getValue());
    verify(ctx).write(Reply._221_OK.replyString());
    verify(ctx).flush();
    verify(channelFuture).addListener(ChannelFutureListener.CLOSE);
    assertTrue(buf.released);
  }

  @Test
  public void exceptionCaught() throws Exception {
    RuntimeException cause = new RuntimeException();

    it.exceptionCaught(ctx, cause);

    verify(logger).error("CHANNEL_ERROR", cause, "Unhandled exception");
    verify(ctx).write("451 Requested action aborted: local error in processing\r\n");
    verify(ctx).flush();
  }

  @Test
  public void exceptionCaught_timeout() throws Exception {
    ReadTimeoutException e = ReadTimeoutException.INSTANCE;

    it.exceptionCaught(ctx, e);

    verify(logger)
        .info(
            "CHANNEL_TIMEOUT",
            null,
            "Timeout waiting for data from client. Closed connection from %s",
            remoteHost);
    verify(ctx).close();
  }

  @Test
  public void channelRead_startTLS() throws Exception {
    final String line = "STARTTLS\r\n";
    MockByteBuf buf = new MockByteBuf(line);
    when(session.consumeLine(any())).thenReturn(StartTls._220_READY);

    it.channelRead(ctx, buf);

    verify(pipeline).addFirst(any(SslHandler.class));

    MockSslContext.MockSslHandler sslHandler = sslContext.getSslHandler();
    verify(sslHandler.handshakeFuture).addListener(any(SmtpServerHandler.TlsStartListener.class));

    verify(ctx).write(StartTls._220_READY.replyString());
    verify(ctx).flush();
    assertTrue(buf.released);
  }

  @Test
  public void tlsStartListener_success() throws Exception {
    when(sslHandlerFuture.isSuccess()).thenReturn(true);
    TlsStartListener it = new TlsStartListener(session);
    it.operationComplete(sslHandlerFuture);
    verify(session).setTlsStarted(true);
  }

  @Test
  public void tlsStartListener_fail() throws Exception {
    when(sslHandlerFuture.isSuccess()).thenReturn(false);
    TlsStartListener it = new TlsStartListener(session);
    it.operationComplete(sslHandlerFuture);
    verify(session, never()).setTlsStarted(true);
  }

  @Test
  public void channelInactive() throws Exception {
    it.channelInactive(ctx);
    verify(session).clearMailObject();
  }
}
