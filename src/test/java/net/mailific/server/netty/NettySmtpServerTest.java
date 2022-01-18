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
