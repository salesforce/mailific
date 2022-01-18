package net.mailific.server.reference;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.mailific.server.MailObject;
import net.mailific.server.commands.ParsedCommandLine;
import net.mailific.server.session.Reply;
import net.mailific.util.Distinguisher;

/**
 * Simple implementation of MailObject. This implementation just discards the message data.
 * Implementers will likely want to override {@link #getOutputStream()} so that it stores the
 * message somewhere.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class BaseMailObject implements MailObject {

  private ParsedCommandLine mailFrom;
  private List<ParsedCommandLine> acceptedRcpts = new ArrayList<>();
  private Map<String, Object> extensionMaterial = new HashMap<>();

  @Override
  public Reply mailFrom(ParsedCommandLine mailFrom) {
    Objects.requireNonNull(mailFrom, "MAIL FROM line may not be null");
    this.mailFrom = mailFrom;
    return Reply._250_OK;
  }

  @Override
  public final ParsedCommandLine getMailFromLine() {
    return mailFrom;
  }

  /**
   * @return The reverse-path for the Mail Object, as specified in the Mail From command. Left open
   *     to override in case subclasses wish to alter the reverse path in some way.
   */
  @Override
  public String getReversePathMailbox() {
    return mailFrom == null ? null : mailFrom.getPath();
  }

  @Override
  public final Reply rcptTo(ParsedCommandLine rcpt) {
    Objects.requireNonNull(rcpt, "RCPT TO line may not be null");
    Reply reply = offerRecipient(rcpt);
    if (reply.getCode() == 250) {
      acceptedRcpts.add(rcpt);
    }
    return reply;
  }

  /**
   * Extension point. Decide whether the recipient should be accepted, and return an appropriate
   * reply.
   *
   * @return A Reply with code 250 if the recipient should be accepted, or with any other code if it
   *     should be rejected.
   */
  public Reply offerRecipient(ParsedCommandLine rcpt) {
    return Reply._250_OK;
  }

  @Override
  public final List<ParsedCommandLine> getAcceptedRcptToLines() {
    return Collections.unmodifiableList(acceptedRcpts);
  }

  /**
   * Extension point.
   *
   * <p>Default implementation just discards the data.
   */
  @Override
  public void writeLine(byte[] line, int offset, int length) throws IOException {}

  @Override
  public Reply complete() {
    return Reply._250_OK;
  }

  @Override
  public void dispose() {}

  /** @return All of the accepted recipients' email addresses. */
  @Override
  public List<String> getForwardPathMailBoxes() {
    return acceptedRcpts.stream().map(p -> p.getPath()).collect(Collectors.toList());
  }

  @Override
  public Collection<String> getDistinctForwardPathMailboxes(
      Function<String, String> canonicalizer) {
    if (acceptedRcpts.isEmpty()) {
      return Collections.emptyList();
    }
    return new Distinguisher<String>(canonicalizer)
        .distinct(acceptedRcpts.stream().map(p -> p.getPath()));
  }
  ;

  @Override
  public Object putExtensionMaterial(String key, Object material) {
    return extensionMaterial.put(key, material);
  }

  @Override
  public Object getExtensionMaterial(String key) {
    return extensionMaterial.get(key);
  }

  @Override
  public void prepareForData() {}
}
