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

package net.mailific.server.session;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.net.ssl.SSLSession;
import net.mailific.server.Line;
import net.mailific.server.LineConsumer;
import net.mailific.server.MailObject;
import net.mailific.server.commands.ParsedCommandLine;
import net.mailific.server.extension.Extension;

/**
 * Default implementation of SmtpSession.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class SmtpSessionImp implements SmtpSession {

  private static final Logger logger = Logger.getLogger(SmtpSessionImp.class.getName());

  // just something to uniquify keys
  private static final String COMMAND_MAP_FILTER_KEY =
      SmtpSessionImp.class.getName() + ".commandMapFilterKey";

  private final InetSocketAddress remoteAddress;
  private SessionState state = StandardStates.BEFORE_CONNECT;
  private ParsedCommandLine ehlo;
  private MailObject currentMailObject;
  private Collection<Extension> supportedExtensions;
  private SSLSession tlsSession;
  private final Map<String, Object> properties = new HashMap<>();
  private LineConsumerChain consumerChain = new LineConsumerChain();

  public SmtpSessionImp(
      InetSocketAddress remoteAddress,
      LineConsumer commandMap,
      Collection<Extension> supportedExtensions) {
    this.remoteAddress = remoteAddress;
    this.supportedExtensions =
        supportedExtensions == null
            ? Collections.emptyList()
            : new ArrayList<>(supportedExtensions);
    addLineConsumer(COMMAND_MAP_FILTER_KEY, commandMap);
    if (supportedExtensions != null) {
      supportedExtensions.forEach(this::addExtensionLineConsumers);
    }
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return remoteAddress;
  }

  @Override
  public SessionState getConnectionState() {
    return state;
  }

  @Override
  public void setConnectionState(SessionState newState) {
    state = newState;
  }

  @Override
  public ParsedCommandLine getEhloCommandLine() {
    return ehlo;
  }

  @Override
  public MailObject getMailObject() {
    return currentMailObject;
  }

  @Override
  public void newMailObject(MailObject newMailObject) {
    if (currentMailObject != null) {
      currentMailObject.dispose();
    }
    currentMailObject = newMailObject;
  }

  @Override
  public Reply completeMailObject() {
    if (currentMailObject == null) {
      // Should never occur
      logger.severe("COMPLETE_MISSING_MAIL: MailObject is null when completed.");
      return Reply._554_SERVER_ERROR;
    }
    try {
      return currentMailObject.complete(this);
    } finally {
      clearMailObject();
    }
  }

  @Override
  public void clearMailObject() {
    if (currentMailObject != null) {
      currentMailObject.dispose();
      currentMailObject = null;
    }
  }

  @Override
  public void setEhloCommandLine(ParsedCommandLine ehlo) {
    this.ehlo = ehlo;
  }

  // TODO: should not be exposing this
  @Override
  public Collection<Extension> getSupportedExtensions() {
    return supportedExtensions;
  }

  @Override
  public boolean isTlsStarted() {
    return tlsSession != null;
  }

  @Override
  public void setSslSession(SSLSession tlsSession) {
    this.tlsSession = tlsSession;
  }

  @Override
  public SSLSession getSslSession() {
    return this.tlsSession;
  }

  @Override
  public Object getProperty(String key) {
    return properties.get(key);
  }

  @Override
  public Object setProperty(String key, Object property) {
    return properties.put(key, property);
  }

  @Override
  public Object clearProperty(String key) {
    return properties.remove(key);
  }

  @Override
  public Reply connect() {
    Transition transition = consumerChain.connect(this);
    if (transition.getNextState() != SessionState.NO_STATE_CHANGE) {
      setConnectionState(transition.getNextState());
    }
    if (transition == Transition.UNHANDLED) {
      return Reply._554_SERVER_ERROR;
    }
    return transition.getReply();
  }

  @Override
  public Reply consumeLine(byte[] line) {
    Transition transition = consumerChain.consume(this, new Line(line));
    if (transition.getNextState() != SessionState.NO_STATE_CHANGE) {
      setConnectionState(transition.getNextState());
    }
    return transition.getReply();
  }

  @Override
  public void addLineConsumer(String selector, LineConsumer consumer) {
    consumerChain.addLineConsumer(selector, consumer);
  }

  @Override
  public void removeLineConsumer(String selector) {
    consumerChain.removeLineConsumer(selector);
  }

  private void addExtensionLineConsumers(Extension extension) {
    LineConsumer consumer = extension.getLineConsumer();
    if (consumer != null) {
      // TODO: need a selector that the extension can figure out if it wants to remove/replace
      addLineConsumer(getClass().getName() + ".extension." + extension.getName(), consumer);
    }
  }
}
