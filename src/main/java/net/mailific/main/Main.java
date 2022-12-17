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

import java.io.File;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.net.ssl.SSLException;
import javax.security.sasl.AuthorizeCallback;
import net.mailific.server.MailObjectFactory;
import net.mailific.server.ServerConfig;
import net.mailific.server.ServerConfig.Builder;
import net.mailific.server.SmtpServer;
import net.mailific.server.commands.CommandHandler;
import net.mailific.server.commands.Connect;
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
import net.mailific.server.extension.auth.Auth;
import net.mailific.server.extension.auth.AuthCheck;
import net.mailific.server.extension.auth.LoginMechanism;
import net.mailific.server.extension.auth.PlainMechanism;
import net.mailific.server.netty.NettySmtpServer;
import net.mailific.server.reference.BaseMailObjectFactory;

/**
 * Starts up a demo server. Includes some static utility methods that may be helpful in constructing
 * real servers.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class Main {

  static final String ARG_TEMPLATE =
      "--listen <listen_address> "
          + "--port <port> "
          + "--cert </path/to/sslCert> "
          + "--certkey </path/to/certkey> "
          + "--certpass <passphrase> "
          + "--auth "
          + "--help -h";

  private static SmtpServer server;

  /** Start up a server configured according to the arguments given. */
  public static void main(String[] args) {
    server = null;
    try {
      server = run(args);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * @return The last server, if any, that was started by calling {@link #main(String[])}.
   */
  public static SmtpServer getServer() {
    return server;
  }

  static SmtpServer run(String[] args) throws SSLException, HelpException, InterruptedException {
    ServerConfig config = parseArgs(args, new BaseMailObjectFactory());
    SmtpServer server = new NettySmtpServer(config);
    server.start();
    return server;
  }

  static ServerConfig parseArgs(String[] args, MailObjectFactory mailObjectFactory)
      throws SSLException, HelpException {
    SimpleArgParser argParser = new SimpleArgParser(ARG_TEMPLATE);
    argParser.parseArgs(args);

    if (argParser.getFlag("-h", "--help")) {
      throw new HelpException();
    }

    ServerConfig.Builder builder = defaultServerConfigBuilder(mailObjectFactory);

    String listenAddress = argParser.getString("--listen");
    if (listenAddress != null) {
      builder.withListenHost(listenAddress);
    }

    int port = argParser.getInt(-1, "--port");
    if (port > 0) {
      builder.withListenPort(port);
    }

    parseTlsArgs(argParser, builder);

    if (argParser.getFlag("--auth")) {
      builder.withAdditionalExtension(getTestAuthExtension(listenAddress));
    }
    return builder.build();
  }

  /**
   * Returns a ServerConfig.Builder with some reasonable defaults.
   *
   * @return a ServerConfig.Builder populated with default values:
   *     <ul>
   *       <li>listenHost: the value returned by {@link #defaultListenHost()}
   *       <li>listenPort: 2525
   *       <li>tlsCert/tlsCertKey/tlsCertPass: all null
   *       <li>logger: a new {@link JavaUtilLogger}
   *       <li>extensions: the value returned by {@link #harmlessExtensions()}
   *       <li>commandHandlers: the values from the Map returned by {@link
   *           #baseCommandHandlers(String, String, MailObjectFactory)}
   *       <li>connectHandler: a new {@link Connect} constructed with a guess at the domain
   *     </ul>
   */
  public static ServerConfig.Builder defaultServerConfigBuilder(
      MailObjectFactory mailObjectFactory) {
    String listenHost = defaultListenHost();
    String domain = domainFromHost(listenHost);
    return ServerConfig.builder()
        .withListenHost(listenHost)
        .withListenPort(2525)
        .withCommandHandlers(baseCommandHandlers(domain, null, mailObjectFactory).values())
        .withExtensions(harmlessExtensions())
        .withConnectHandler(new Connect(domain));
  }

  /**
   * @return a guess at the server's domain, based on its hostname. If the hostname has more than
   *     one dot, strip off the leftmost label. Otherwise just return the hostname given.
   */
  static String domainFromHost(String host) {
    if (host == null) {
      return null;
    }
    int first = host.indexOf('.');
    if (first < 0) {
      return host;
    }
    if (host.indexOf('.', first + 1) < 0) {
      return host;
    }
    return host.substring(first + 1);
  }

  /**
   * @return an {@link Auth} extension good only for testing. It authorizes any userid if it's given
   *     the password "french fries".
   */
  private static Extension getTestAuthExtension(String serverName) {
    final AuthCheck authCheck = new ThePasswordIsFrenchFries();
    return new Auth(
        List.of(new PlainMechanism(authCheck), new LoginMechanism(authCheck)), serverName);
  }

  /**
   * @return The hostname, if we can figure it out. Otherwise "localhost".
   */
  public static String defaultListenHost() {
    String listenAddress;
    try {
      listenAddress = InetAddress.getLocalHost().getCanonicalHostName();
    } catch (Exception e) {
      listenAddress = "localhost";
    }
    return listenAddress;
  }

  /**
   * @param argParser representing already parsed arguments
   * @param configBuilder to add information to
   * @return true if tls information has been supplied
   */
  static boolean parseTlsArgs(SimpleArgParser argParser, Builder configBuilder)
      throws SSLException {
    String certPath = argParser.getString("--cert");
    if (certPath != null) {
      String certKeyPath = argParser.getString("--certkey");
      if (certKeyPath == null) {
        throw new IllegalArgumentException("If you provide a cert, you must provide the key.");
      }
      String certPass = argParser.getString("--certpass");
      File certFile = new File(certPath);
      File certKeyFile = new File(certKeyPath);
      if (!certFile.canRead()) {
        throw new IllegalArgumentException("Cannot read cert file: " + certPath);
      }
      if (!certKeyFile.canRead()) {
        throw new IllegalArgumentException("Cannot read cert key: " + certKeyPath);
      }
      configBuilder.withTlsCert(certFile);
      configBuilder.withTlsCertKey(certKeyFile);
      configBuilder.withTlsCertPassword(certPass);
      return true;
    }
    return false;
  }

  /**
   * Provides a Map containing CommandHandlers for the commands required by rfc 5321. The key is the
   * verb, in uppercase. You can replace these with customized handlers, add any optional handlers
   * you want, and use {@link Map#values()} to get the Collection needed by {@link ServerConfig}.
   *
   * @param domain The domain to be used in EHLO/HELO responses
   * @param greeting Extra greeting for the EHLO response (may be null)
   * @param mailObjectFactory passed to the MAIL command handler
   */
  public static Map<String, CommandHandler> baseCommandHandlers(
      String domain, String greeting, MailObjectFactory mailObjectFactory) {
    Map<String, CommandHandler> rv = new HashMap<>();
    Stream.of(
            new Ehlo(domain, greeting),
            new Helo(domain),
            new Mail(mailObjectFactory),
            new Rcpt(),
            new Data(),
            new Quit(),
            new Rset(),
            new Noop())
        .forEach(c -> rv.put(c.verb().toUpperCase(), c));
    return rv;
  }

  /**
   * @return a list of extensions that "just work"
   */
  public static List<Extension> harmlessExtensions() {
    List<Extension> extensions = new ArrayList<>();
    extensions.add(new EightBitMime());
    extensions.add(new SmtpUtf8());
    extensions.add(new Pipelining());
    return extensions;
  }

  // Shh. Don't tell anyone.
  static class ThePasswordIsFrenchFries implements AuthCheck {
    @Override
    public AuthorizeCallback authorize(String authzid, String authcid, byte[] credential) {
      AuthorizeCallback result = new AuthorizeCallback(authcid, authzid);
      result.setAuthorized(new String(credential, StandardCharsets.UTF_8).equals("french fries"));
      return result;
    }
  }

  /** Thrown to indicate we should just print the help. */
  public static class HelpException extends Exception {
    @Override
    public String getMessage() {
      return ARG_TEMPLATE;
    }
  }
}
