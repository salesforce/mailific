package net.mailific.server;

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

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.mailific.server.commands.CommandHandler;
import net.mailific.server.extension.Extension;
import net.mailific.server.extension.starttls.StartTls;
import net.mailific.server.session.SmtpSessionFactory;
import net.mailific.server.session.SmtpSessionFactoryImp;

/**
 * Collects all the data needed to build an SmtpServer.
 *
 * <p>In general, you will want to specify all fields except the SessionFactory. A {@link
 * SmtpSessionFactory} will then be created for you, using the other fields. However, on the off
 * chance you want to create your own SessionFactory, you can specify it in the Builder.
 *
 * <p>If you supply the TLS cert information, a StartTls extension will automatically be added to
 * the extensions. However, if you have your own extension implementation , just add it to the
 * extensions yourself, and it will be used instead.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class ServerConfig {

  private final String listenHost;
  private final int listenPort;
  private final File tlsCert;
  private final File tlsCertKey;
  private final String tlsCertPassword;
  private final SmtpSessionFactory sessionFactory;

  private ServerConfig(Builder builder) {
    this.listenHost = builder.listenHost;
    this.listenPort = builder.listenPort;
    this.tlsCert = builder.tlsCert;
    this.tlsCertKey = builder.tlsCertKey;
    this.tlsCertPassword = builder.tlsCertPassword;
    if (builder.sessionFactory != null) {
      this.sessionFactory = builder.sessionFactory;
    } else {
      if (this.tlsCert != null) {
        builder.extensions.putIfAbsent(StartTls.NAME, new StartTls());
      }
      this.sessionFactory =
          new SmtpSessionFactoryImp(
              new SmtpCommandMap(builder.commandHandlers, builder.connectHandler),
              builder.extensions.values());
    }
  }

  public String getListenHost() {
    return listenHost;
  }

  public int getListenPort() {
    return listenPort;
  }

  public File getTlsCert() {
    return tlsCert;
  }

  public File getTlsCertKey() {
    return tlsCertKey;
  }

  public String getTlsCertPassword() {
    return tlsCertPassword;
  }

  public SmtpSessionFactory getSessionFactory() {
    return sessionFactory;
  }

  /**
   * Creates builder to build {@link ServerConfig}.
   *
   * <p>In general, you will want to specify all fields except the SessionFactory. A {@link
   * SmtpSessionFactory} will then be created for you, using the other fields. However, on the off
   * chance you want to create your own SessionFactory, you can specify it.
   *
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder to build {@link ServerConfig}. */
  public static final class Builder {
    private String listenHost;
    private int listenPort;
    private File tlsCert;
    private File tlsCertKey;
    private String tlsCertPassword;
    private SmtpSessionFactory sessionFactory;
    private Map<String, Extension> extensions = new HashMap<>();
    private Collection<CommandHandler> commandHandlers;
    private CommandHandler connectHandler;

    private Builder() {}

    public Builder withListenHost(String listenHost) {
      this.listenHost = listenHost;
      return this;
    }

    public Builder withListenPort(int listenPort) {
      this.listenPort = listenPort;
      return this;
    }

    public Builder withTlsCert(File tlsCert) {
      this.tlsCert = tlsCert;
      return this;
    }

    public Builder withTlsCertKey(File tlsCertKey) {
      this.tlsCertKey = tlsCertKey;
      return this;
    }

    public Builder withTlsCertPassword(String tlsCertPassword) {
      this.tlsCertPassword = tlsCertPassword;
      return this;
    }

    /**
     * In general, you will want to specify all fields except the SessionFactory. A {@link
     * SmtpSessionFactory} will then be created for you, using the other fields. However, on the off
     * chance you want to create your own SessionFactory, you can specify it with this method.
     *
     * @param sessionFactory to use for building sessions.
     */
    public Builder withSessionFactory(SmtpSessionFactory sessionFactory) {
      this.sessionFactory = sessionFactory;
      return this;
    }

    public Builder withExtensions(Collection<Extension> extensions) {
      extensions.forEach(
          e -> {
            this.extensions.put(e.getEhloKeyword().toUpperCase(), e);
          });
      return this;
    }

    public Builder withAdditionalExtension(Extension extension) {
      this.extensions.put(extension.getEhloKeyword().toUpperCase(), extension);
      return this;
    }

    public Builder withCommandHandlers(Collection<CommandHandler> commandHandlers) {
      this.commandHandlers = commandHandlers;
      return this;
    }

    public Builder withConnectHandler(CommandHandler connectHandler) {
      this.connectHandler = connectHandler;
      return this;
    }

    public ServerConfig build() {
      return new ServerConfig(this);
    }
  }
}
