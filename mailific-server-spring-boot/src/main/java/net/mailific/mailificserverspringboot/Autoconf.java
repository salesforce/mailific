/*-
 * Mailific SMTP Server Library
 *
 * Copyright (C) 2022 Joe Humphreys
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

package net.mailific.mailificserverspringboot;

import java.io.File;
import java.util.Collection;
import java.util.List;
import net.mailific.main.Main;
import net.mailific.server.LineConsumer;
import net.mailific.server.MailObject;
import net.mailific.server.MailObjectFactory;
import net.mailific.server.ServerConfig;
import net.mailific.server.SmtpCommandMap;
import net.mailific.server.SmtpServer;
import net.mailific.server.commands.CommandHandler;
import net.mailific.server.commands.Connect;
import net.mailific.server.extension.Extension;
import net.mailific.server.extension.starttls.StartTls;
import net.mailific.server.netty.NettySmtpServer;
import net.mailific.server.reference.BaseMailObjectFactory;
import net.mailific.server.session.SmtpSessionFactory;
import net.mailific.server.session.SmtpSessionFactoryImp;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Provides Spring autoconfiguration for the Mailific SMTP listener library.
 *
 * @author jhumphreys
 */
@AutoConfiguration
public class Autoconf {

  /**
   * Provides a Netty-based SmtpServer.
   *
   * @param config See {@link #serverConfig(String, int, String, String, String,
   *     SmtpSessionFactory)}
   */
  @Bean
  @ConditionalOnMissingBean
  public SmtpServer smtpServer(ServerConfig config) {
    return new NettySmtpServer(config);
  }

  /**
   * Provides the ServerConfig used by {@link #smtpServer(ServerConfig)}
   *
   * @param listenHost Hostname to listen to. Taken from the property mailific.server.listenHost.
   *     The property defaults to "localhost".
   * @param listenPort Port to listen on. Taken from the property mailific.server.listenPort. The
   *     property defaults to 2525.
   * @param certPath File path to an SSL cert-chain file, in PEM format. If set to empty or null,
   *     the server won't offer STARTTLS. Taken from the property mailific.server.certPath. The
   *     property defaults to empty.
   * @param certKeyPath File path to the key (in PKCS8 format) for the SSL cert. Taken from the
   *     property mailific.server.certKeyPath. The poperty defaults to empty.
   * @param certPass Password for the key specified by certKeyPath. Leave null if the key file is
   *     not encrypted. Taken from the property mailific.server.certPassword. Defaults to null.
   * @param sessionFactory see {@link #smtpSesssionFactory(LineConsumer, ExtensionProvider)}
   */
  @Bean
  @ConditionalOnMissingBean
  public ServerConfig serverConfig(
      @Value("${mailific.server.listenHost:localhost}") String listenHost,
      @Value("${mailific.server.listenPort:2525}") int listenPort,
      @Value("${mailific.server.certPath:}") String certPath,
      @Value("${mailific.server.certKeyPath:}") String certKeyPath,
      @Value("${mailific.server.certPassword:#{NULL}}") String certPass,
      SmtpSessionFactory sessionFactory) {

    ServerConfig.Builder builder =
        ServerConfig.builder()
            .withListenHost(listenHost)
            .withListenPort(listenPort)
            .withSessionFactory(sessionFactory);

    if (certPath != null && !certPath.isBlank()) {
      if (certKeyPath == null || certKeyPath.isBlank()) {
        throw new IllegalArgumentException("If you provide a cert, you must provide the key.");
      }
      File certFile = new File(certPath);
      File certKeyFile = new File(certKeyPath);
      if (!certFile.canRead()) {
        throw new IllegalArgumentException("Cannot read cert file: " + certPath);
      }
      if (!certKeyFile.canRead()) {
        throw new IllegalArgumentException("Cannot read cert key: " + certKeyPath);
      }
      builder.withTlsCert(certFile).withTlsCertKey(certKeyFile).withTlsCertPassword(certPass);
    }
    return builder.build();
  }

  /**
   * Provides an SmtpSessionFactory. Used by {@link #serverConfig(String, int, String, String,
   * String, SmtpSessionFactory)}
   *
   * @param commandConsumer see {@link #mailificCommandConsumer(CommandHandlerProvider)}
   * @param extensions see {@link #extensionProvider(String)}
   */
  @Bean
  @ConditionalOnMissingBean
  public SmtpSessionFactory smtpSesssionFactory(
      @Qualifier("mailificCommandConsumer") LineConsumer commandConsumer,
      ExtensionProvider extensions) {
    return new SmtpSessionFactoryImp(commandConsumer, extensions.extensions());
  }

  /**
   * Provides a list of extensions for the server to support. Used by {@link
   * #smtpSesssionFactory(LineConsumer, ExtensionProvider)}.
   *
   * @param certPath If nonnull and nonblank, the return value will include the STARTTLS extension.
   *     Taken from the property mailific.server.certPath. The property defaults to empty.
   */
  @Bean
  @ConditionalOnMissingBean
  public ExtensionProvider extensionProvider(
      @Value("${mailific.server.certPath:}") String certPath) {
    List<Extension> extensions = Main.harmlessExtensions();
    if (!certPath.isBlank()) {
      extensions.add(new StartTls());
    }
    return () -> extensions;
  }

  /**
   * Builds an SmtpCommandMap from a CommandHandlerProvider . This is the default implementation for
   * the LineConsumer used by {@link #smtpSesssionFactory(LineConsumer, ExtensionProvider)}.
   *
   * <p>This Bean is named "mailificCommandConsumer" so that it can be distinguished from other
   * possible LineConsumer Beans.
   *
   * @param commands see {@link #commandHandlerProvider(BannerDomainProvider, EhloGreetingProvider,
   *     MailObjectFactory)}
   */
  @Bean()
  @ConditionalOnMissingBean(name = "mailificCommandConsumer")
  public SmtpCommandMap mailificCommandConsumer(CommandHandlerProvider commands) {
    return new SmtpCommandMap(commands.commandHandlers(), commands.connectHandler());
  }

  /**
   * Provides a connect handler and collection of command handlers. Used by {@link
   * #mailificCommandConsumer(CommandHandlerProvider)}.
   *
   * @param domain Domain to be used in the Banner and EHLO responses. See {@link
   *     #bannerDomainProvider()}
   * @param greeting Greeting text to be used in EHLO response. See {@link #ehloGreetingProvider()}
   * @param mailObjectFactory see {@link #mailObjectFactory()}
   */
  @Bean
  @ConditionalOnMissingBean
  public CommandHandlerProvider commandHandlerProvider(
      BannerDomainProvider domain,
      EhloGreetingProvider greeting,
      MailObjectFactory mailObjectFactory) {
    return new CommandHandlerProvider() {
      public Collection<CommandHandler> commandHandlers() {
        return Main.baseCommandHandlers(
                domain.getBannerDomain(), greeting.getEhloGreeting(), mailObjectFactory)
            .values();
      }

      public CommandHandler connectHandler() {
        return new Connect(domain.getBannerDomain());
      }
    };
  }

  /**
   * Provides a factory for {@link MailObject} objects. You will almost certainly need to provide
   * your own implementation here. Used by {@link #commandHandlerProvider(BannerDomainProvider,
   * EhloGreetingProvider, MailObjectFactory)}
   */
  @Bean
  @ConditionalOnMissingBean
  public MailObjectFactory mailObjectFactory() {
    return new BaseMailObjectFactory();
  }

  /**
   * Provides the domain to be advertised in the Banner (connect response). Also used in the EHLO
   * response. Used by {@link #commandHandlerProvider(BannerDomainProvider, EhloGreetingProvider,
   * MailObjectFactory)}
   */
  @Bean
  @ConditionalOnMissingBean
  public BannerDomainProvider bannerDomainProvider() {
    return () -> Main.domainFromHost(Main.defaultListenHost());
  }

  /**
   * Provides the EHLO greeting text. Maybe return null. Used by {@link
   * #commandHandlerProvider(BannerDomainProvider, EhloGreetingProvider, MailObjectFactory)}
   */
  @Bean
  @ConditionalOnMissingBean
  public EhloGreetingProvider ehloGreetingProvider() {
    return () -> null;
  }
}
