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

package net.mailific.main;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.security.sasl.AuthorizeCallback;
import net.mailific.main.Main.HelpException;
import net.mailific.server.ServerConfig;
import net.mailific.server.ServerConfig.Builder;
import net.mailific.server.SmtpServer;
import net.mailific.server.commands.CommandHandler;
import net.mailific.server.commands.Data;
import net.mailific.server.commands.Ehlo;
import net.mailific.server.commands.Helo;
import net.mailific.server.commands.Mail;
import net.mailific.server.commands.Noop;
import net.mailific.server.commands.Quit;
import net.mailific.server.commands.Rcpt;
import net.mailific.server.commands.Rset;
import net.mailific.server.extension.EightBitMime;
import net.mailific.server.extension.Extension;
import net.mailific.server.extension.Pipelining;
import net.mailific.server.extension.SmtpUtf8;
import net.mailific.server.extension.auth.AuthCheck;
import net.mailific.server.reference.BaseMailObjectFactory;
import org.junit.Test;

public class MainTest {

  SimpleArgParser argParser = new SimpleArgParser(Main.ARG_TEMPLATE);
  Builder builder = ServerConfig.builder();

  @Test
  public void defaults() throws Exception {
    Main.main(new String[0]);
    assertNotNull(Main.getServer());
    Main.getServer().shutdown().get();

    // Stupid, but just for coverage
    new Main();
  }

  @Test
  public void main_badArgs() {
    Main.main(new String[] {"--no-such-arg"});
    assertNull(Main.getServer());
  }

  @Test
  public void badArgs() {
    IllegalArgumentException result =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              Main.run(new String[] {"--no-such-arg"});
            });
    assertThat(result.getMessage(), containsString("Unexpected option"));
  }

  @Test
  public void help() {
    HelpException result =
        assertThrows(
            Main.HelpException.class,
            () -> {
              Main.run(new String[] {"-h"});
            });
    assertThat(result.getMessage(), containsString("--port"));
  }

  @Test
  public void withTls() throws Exception {
    SmtpServer server =
        Main.run(
            new String[] {
              "--cert",
              "src/test/resources/certs/cert.pem",
              "--certkey",
              "src/test/resources/certs/pk8.pem"
            });
    assertNotNull(server);
    // TODO check STARTTLS offered
    server.shutdown().get();
  }

  @Test
  public void withTls_error() {
    IllegalArgumentException result =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              Main.run(
                  new String[] {
                    "--cert",
                    "src/test/resources/certs/cert.pem",
                    "--certkey",
                    "src/test/resources/certs/pk8.pem",
                    "--certpass",
                    "not really the password"
                  });
            });
    assertThat(result.getMessage(), containsString("private key"));
  }

  @Test
  public void withAuth() throws Exception {
    SmtpServer server = Main.run(new String[] {"--auth"});
    assertNotNull(server);
    // TODO check AUTH offered
    server.shutdown().get();
  }

  @Test
  public void harmlessExtensions() {
    List<Extension> actual = Main.harmlessExtensions();
    assertThat(
        actual,
        containsInAnyOrder(
            instanceOf(EightBitMime.class),
            instanceOf(SmtpUtf8.class),
            instanceOf(Pipelining.class)));
  }

  @Test
  public void baseCommandHandlers() {
    Map<String, CommandHandler> actual =
        Main.baseCommandHandlers("foo.com", "bar", new BaseMailObjectFactory());
    assertEquals(8, actual.size());
    assertThat(actual.get("EHLO"), instanceOf(Ehlo.class));
    assertThat(actual.get("HELO"), instanceOf(Helo.class));
    assertThat(actual.get("MAIL"), instanceOf(Mail.class));
    assertThat(actual.get("RCPT"), instanceOf(Rcpt.class));
    assertThat(actual.get("DATA"), instanceOf(Data.class));
    assertThat(actual.get("QUIT"), instanceOf(Quit.class));
    assertThat(actual.get("RSET"), instanceOf(Rset.class));
    assertThat(actual.get("NOOP"), instanceOf(Noop.class));
    // TODO: some further verification that the arguments were threaded through to the
    // commandhandlers.
  }

  @Test
  public void determineTlsCert_noCert() throws Exception {
    argParser.parseArgs(new String[0]);

    boolean actual = Main.parseTlsArgs(argParser, builder);

    ServerConfig config = builder.build();
    assertFalse(actual);
    assertNull(config.getTlsCert());
  }

  @Test
  public void determineTlsCert_noCertKey() throws Exception {
    argParser.parseArgs(new String[] {"--cert", "/foo/bar"});

    Exception e =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              Main.parseTlsArgs(argParser, builder);
            });

    assertThat(e.getMessage(), containsString("must provide the key"));
  }

  @Test
  public void determineTlsCert_badCertPath() throws Exception {
    argParser.parseArgs(
        new String[] {
          "--cert",
          "src/test/resources/certs/nosuchcert.pem",
          "--certkey",
          "src/test/resources/certs/pk8.pem"
        });

    Exception e =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              Main.parseTlsArgs(argParser, builder);
            });

    assertThat(e.getMessage(), containsString("Cannot read cert file"));
  }

  @Test
  public void determineTlsCert_badCertKeyPath() throws Exception {
    argParser.parseArgs(
        new String[] {
          "--cert",
          "src/test/resources/certs/cert.pem",
          "--certkey",
          "src/test/resources/certs/nosuchkey.pem"
        });

    Exception e =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              Main.parseTlsArgs(argParser, builder);
            });

    assertThat(e.getMessage(), containsString("Cannot read cert key"));
  }

  /*
    @Test
    public void determineListenAddress_default() throws Exception {
      argParser.parseArgs(new String[0]);

      Main.determineListenAddress(argParser, builder);

      ServerConfig config = builder.build();
      assertEquals(Main.defaultListenHost(), config.getListenHost());
    }

    @Test
    public void determineListenAddress_Specified() throws Exception {
      argParser.parseArgs(new String[] {"-l", "1.2.3.4"});

      Main.determineListenAddress(argParser, builder);

      ServerConfig config = builder.build();
      assertEquals("1.2.3.4", config.getListenHost());
    }
  */
  @Test
  public void defaultListenHost_noError() throws Exception {
    assertEquals(InetAddress.getLocalHost().getCanonicalHostName(), Main.defaultListenHost());
  }

  @Test
  public void defaultListenHost_withCallable() throws Exception {
    assertEquals("foo", Main.defaultListenHost(() -> "foo"));
  }

  @Test
  public void defaultListenHost_withCallable_Null() throws Exception {
    assertEquals("localhost", Main.defaultListenHost(() -> null));
  }

  @Test
  public void defaultListenHost_Exception() throws Exception {
    assertEquals(
        "localhost",
        Main.defaultListenHost(
            () -> {
              throw new UnknownHostException();
            }));
  }

  @Test
  public void thePasswordIsFrenchFries() {
    AuthCheck check = new Main.ThePasswordIsFrenchFries();

    AuthorizeCallback result =
        check.authorize("foo", "bar", "french fries".getBytes(StandardCharsets.UTF_8));
    assertTrue(result.isAuthorized());

    result = check.authorize("foo", "bar", "onion rings".getBytes(StandardCharsets.UTF_8));
    assertFalse(result.isAuthorized());
  }

  @Test
  public void defaultServerConfigBuilder() {
    ServerConfig.Builder builder = Main.defaultServerConfigBuilder(new BaseMailObjectFactory());
    ServerConfig actual = builder.build();
    assertEquals(Main.defaultListenHost(), actual.getListenHost());
    assertEquals(2525, actual.getListenPort());
  }

  @Test
  public void parseArgs_listenAddressAndPort() throws Exception {
    ServerConfig config =
        Main.parseArgs(
            new String[] {"--listen", "1.2.3.4", "--port", "4321"}, new BaseMailObjectFactory());
    assertEquals("1.2.3.4", config.getListenHost());
    assertEquals(4321, config.getListenPort());
  }

  @Test
  public void domainFromHost_null() {
    assertNull(Main.domainFromHost(null));
  }

  @Test
  public void domainFromHost_noDots() {
    assertEquals("foo", Main.domainFromHost("foo"));
  }

  @Test
  public void domainFromHost_oneDot() {
    assertEquals("foo.com", Main.domainFromHost("foo.com"));
  }

  @Test
  public void domainFromHost_moreThanOneDot() {
    assertEquals("bar.com", Main.domainFromHost("foo.bar.com"));
  }
}
