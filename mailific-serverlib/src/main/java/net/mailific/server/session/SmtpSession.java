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
import java.util.Collection;
import javax.net.ssl.SSLSession;
import net.mailific.server.LineConsumer;
import net.mailific.server.MailObject;
import net.mailific.server.commands.ParsedCommandLine;
import net.mailific.server.extension.Extension;

/**
 * An SMTP session, as described in RFC 5321.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public interface SmtpSession {

  /**
   * @return the address of the client.
   */
  InetSocketAddress getRemoteAddress();

  /**
   * @return Where the server believes it is in the SMTP dialog. This will determine what commands
   *     it is willing to accept next.
   */
  SessionState getConnectionState();

  /**
   * @param newState Dialog state the server should be in. This will determine what commands it is
   *     willing to accept next.
   */
  void setConnectionState(SessionState newState);

  /** Process a connection event and return the SMTP banner line. */
  Reply connect();

  /**
   * Consume one line of input from the client.
   *
   * <p>Lines are expected to end in \r\n.
   *
   * @return the Reply that should go back to the client, or {@link Reply#DO_NOT_REPLY} if nothing
   *     should be sent back to the client.
   */
  Reply consumeLine(byte[] line);

  /**
   * Adds a LineConsumer to the front of the chain of existing line consumers.
   *
   * <p>Each line will be offered first to the most recently added consumer, and so on.
   *
   * <p>If a consumer returns a Transition containing {@link Reply#DO_NOT_REPLY}, then no further
   * processing happens to that line. No response is sent to the client, and the server reads the
   * next line.
   *
   * <p>If a consumer returns {@link Transition#UNHANDLED}, then the next consumer is called. If
   * there are no more consumers, "500 unrecognized command" is returned to the client.
   *
   * <p>If a consumer returns any other Transition, then its reply is returned to the client and no
   * further processing of the line takes place.
   *
   * @param key An identifier that can be passed to {@link #removeLineConsumer(String)} to remove
   *     this filter.
   */
  void addLineConsumer(String key, LineConsumer consumer);

  /**
   * Safe to call during a consumeLine operation, but will not have an effect until the next line is
   * consumed.
   *
   * @param key The key that was supplied to addLineConsumer when the target consumer was added.
   */
  void removeLineConsumer(String key);

  /**
   * @return a collection of the Extensions that the server supports, independent of whether they're
   *     available in the current state.
   */
  Collection<Extension> getSupportedExtensions();

  /**
   * @return The EHLO command line (if any) that was supplied by the client
   */
  ParsedCommandLine getEhloCommandLine();

  /**
   * @param ehlo EHLO command line sent by client.
   */
  void setEhloCommandLine(ParsedCommandLine ehlo);

  /**
   * @return the Mail Object currently being communicated, if any.
   */
  MailObject getMailObject();

  // TODO: why is this exposed?
  /** Discard any existing MailObject and begin working on the one supplied. */
  void newMailObject(MailObject mailObject);

  /**
   * Call when the data for a mail object has been completely communicated.
   *
   * @return The response that should be given to the DATA command just completed. Note that if
   *     successful, this Reply should have immediate set to false to support Pipelining.
   */
  Reply completeMailObject();

  /** Discard any existing mail object being worked on. */
  void clearMailObject();

  /**
   * @return true if TLS has been started using the STARTTLS extension
   */
  boolean isTlsStarted();

  /**
   * {@link #setProperty(String, Object)} and {@link #getProperty(String)} provide a way to track
   * state in a session. Since many of the objects (like CommandHandlers and Extensions) used in the
   * library are stateless and have no references to each other, properties are a way they can
   * communicate.
   */
  Object getProperty(String key);

  /**
   * {@link #setProperty(String, Object)} and {@link #getProperty(String)} provide a way to track
   * state in a session. Since many of the objects (like CommandHandlers and Extensions) used in the
   * library are stateless and have no references to each other, properties are a way they can
   * communicate.
   */
  Object setProperty(String key, Object property);

  /**
   * Remove a property that may have been set by {@link #setProperty(String, Object)}.
   *
   * <p>{@link #setProperty(String, Object)} and {@link #getProperty(String)} provide a way to track
   * state in a session. Since many of the objects (like CommandHandlers and Extensions) used in the
   * library are stateless and have no references to each other, properties are a way they can
   * communicate.
   */
  Object clearProperty(String key);

  /**
   * Note to implementers: SSLSessions are mutable objects that people can probably find a way to do
   * mischief with. You might consider keeping only an immutable copy that doesn't hand out
   * references to more internals.
   *
   * @param session SSLSession in use, or null if no SSLSession is in use.
   */
  void setSslSession(SSLSession session);

  /**
   * Note to implementers: SSLSessions are mutable objects that people can probably find a way to do
   * mischief with. You might consider returning an immutable copy that doesn't hand out references
   * to more internals.
   *
   * @return the SSLSession in use for this SmtpSession, or null if TLS has not been established.
   */
  SSLSession getSslSession();
}
