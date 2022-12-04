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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import net.mailific.server.commands.ParsedCommandLine;
import net.mailific.server.session.Reply;

/**
 * Represents a Mail Object as defined in RFC 5321 section 2.3.1.
 *
 * <p>Provides the methods needed to incrementally build up a message as data is read from the
 * client.
 *
 * <p>SmtpSessions must obey this contract with regard to the following list of methods:
 *
 * <ol>
 *   <li>{@link #mailFrom(ParsedCommandLine)}
 *   <li>{@link #rcptTo(ParsedCommandLine)}
 *   <li>{@link #writeLine(byte[], int, int)}
 *   <li>{@link #complete()}
 *   <li>{@link #dispose()}
 * </ol>
 *
 * <ul>
 *   <li>{@link #dispose()} may be called at any time, after which none of the other methods will be
 *       called.
 *   <li>Otherwise, the methods will only be called in the order given above.
 *   <li>{@link #mailFrom(ParsedCommandLine)} and {@link #complete()} will be called only once.
 *   <li>{@link #rcptTo(ParsedCommandLine)}, {@link #writeLine(byte[], int, int)}, and {@link
 *       #dispose()} may be called any number of times.
 * </ul>
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public interface MailObject {

  /**
   * Accept a MAIL FROM command.
   *
   * @param parsedCommandLine MAIL FROM line from the client
   * @return Reply indicating whether the reverse-path and parameters in the command are accepted.
   *     In order to handle pipelining properly, return a Reply with immediate set to false.
   */
  Reply mailFrom(ParsedCommandLine parsedCommandLine);

  /**
   * Makes the MAIL FROM command available in case it should be needed later in the session (e.g.,
   * to check that the sender is allowed to send to particular recipients).
   *
   * @return the Mail From command line, if it has already occurred. Else null.
   */
  ParsedCommandLine getMailFromLine();

  /**
   * Accept an RCPT TO command. If this method returns a 250 reply, then this recipient will be
   * included in subsequent calls to {@link #getAcceptedRcptToLines()}. If a non-250 reply is
   * returned, then the recipient will be omitted from calls to {@link #getAcceptedRcptToLines()}.
   *
   * @param parsedCommandLine RCPT TO line from the client
   * @return Reply indicating whether the forward-path and parameters in the command are accepted.
   *     This Reply should have immediate set to false to support Pipelining.
   */
  Reply rcptTo(ParsedCommandLine parseCommandLine);

  /**
   * @return all the RCPT TO lines that were sent for this mail object and resulted in a 250 reply.
   *     Returns an empty list if no recipients have yet been accepted.
   */
  List<ParsedCommandLine> getAcceptedRcptToLines();

  /** Handle a line of data. Should end in \r\n. */
  void writeLine(byte[] line, int offset, int length) throws IOException;

  // TODO: document some guarantees about what will or won't be done by the framework after
  // complete()
  // is called.
  /**
   * Do whatever needs to be done with a completed mail object. This method will be called at most
   * once, when all data for the message has been accepted by {@link #writeLine(byte[], int, int)}.
   *
   * @return A 250 reply if the mail object was processed without error. This reply should have
   *     immediate set to false to support Pipelining. Return a 5xx or 4xx reply if unsuccessful.
   */
  Reply complete();

  /**
   * Clean up any resources that were allocated to handle the mail object. The framework will try to
   * call this at least once for each mail object, but may call it more than once, so be sure it is
   * idempotent.
   */
  void dispose();

  /**
   * SMTP Extensions may store data pertinent to the Mail Object here.
   *
   * @param key A key for retrieving the material.
   * @param material The material to store.
   * @return Material previously stored with this key, or null if no such material exists.
   */
  Object putExtensionMaterial(String key, Object material);

  /**
   * Retrieve material stored in this Mail Object by an SMTP Extension.
   *
   * @param key used to store the material.
   * @return Material stored under the given key, or null if no such exists.
   */
  Object getExtensionMaterial(String key);

  /**
   * Returns the distinct accepted recipients. Pass in a function that takes a mailbox and
   * transforms it to a canonical state such that {@link String#equals(Object)} will return true for
   * any similarly canonicalized address that should be considered as naming the same mailbox. A
   * good start might be to pass {@link String#toLowerCase()}.
   *
   * @param canonicalizer Function to canonicalize the email addresses.
   * @return All of the distinct email addresses of the accepted recipient.s
   */
  Collection<String> getDistinctForwardPathMailboxes(Function<String, String> canonicalizer);

  /**
   * @return All of the accepted recipients' email addresses (may include duplicates).
   */
  List<String> getForwardPathMailBoxes();

  /**
   * @return The mailbox (email address) part of the return path.
   */
  String getReversePathMailbox();

  /**
   * Called when the DATA command has been received. Implementations should acquire any resources
   * they need to process the message data.
   */
  void prepareForData();
}
