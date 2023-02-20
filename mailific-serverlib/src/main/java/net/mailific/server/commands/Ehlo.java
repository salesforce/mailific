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

package net.mailific.server.commands;

import java.text.ParseException;
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.mailific.server.Parameters;
import net.mailific.server.extension.Extension;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;

/**
 * Handles the EHLO command. See https://tools.ietf.org/html/rfc5321#section-4.1.1.1
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class Ehlo extends BaseHandler {

  public static final String EHLO = "EHLO";

  private final String domain;
  private final String greeting;

  // TODO: validate constructor args to all CommandHandlers

  /**
   * For more info on the parameters, see RFC5321 4.1.1.1. There's not much there about the
   * greeting, though.
   *
   * <p>No validation is currently being done on these params, but stick to ASCII and you'll
   * probably be fine.
   *
   * @param domain Domain by which the server identifies itself
   * @param greeting Optional. A warm welcoming message. Most commercial servers seem to echo your
   *     IP back for some reason I don't find in the spec.
   */
  public Ehlo(String domain, String greeting) {
    Objects.requireNonNull(domain, "Domain is required");
    this.domain = domain;
    this.greeting = greeting;
  }

  /** Calls {@link #Ehlo(String, String)}, passing null for greeting. */
  public Ehlo(String domain) {
    this(domain, null);
  }

  /*
   * Responds to the EHLO with a list of supported extensions.
   */
  @Override
  public Transition handleValidCommand(SmtpSession session, String commandLine) {
    session.clearMailObject();
    ExtendedReply.Builder replyBuilder = new ExtendedReply.Builder(250).withDetail(getBanner());
    for (Extension extension : extensionsToPresent(session)) {
      replyBuilder.withDetail(extension.getEhloAdvertisment(session));
    }
    try {
      session.setEhloCommandLine(parseCommandLine(commandLine));
    } catch (ParseException e) {
      return new Transition(Reply._501_BAD_ARGS, SessionState.NO_STATE_CHANGE);
    }
    return new Transition(replyBuilder.build(), StandardStates.AFTER_EHLO);
  }

  Pattern domainFinder = Pattern.compile("\\S+");

  /**
   * Extension point.
   *
   * <p>This is a very lenient parsing that doesn't enforce the BNF for the domain part, and also
   * allows extra parameters. Subclasses can override if they want to be strict, or if they want to
   * be more lenient and not require a domain.
   */
  ParsedCommandLine parseCommandLine(String commandLine) throws ParseException {
    Matcher matcher = domainFinder.matcher(commandLine);
    boolean matched = matcher.find(4);
    if (!matched) {
      throw new ParseException("No domain in EHLO command", 5);
    }
    String path = matcher.group();
    Parameters params = new Parameters(commandLine, matcher.end());
    return new ParsedCommandLine(commandLine, verb(), path, params);
  }

  /**
   * Extension point
   *
   * @return the text for the first line of the OK response. Per rfc5321.4.1.1.1, this should
   *     consist of the domain, optionally followed by a space and an ASCII greeting
   */
  protected String getBanner() {
    String greeting = getGreeting();
    String domain = getDomain();
    return greeting == null ? domain : domain + " " + greeting;
  }

  public String getDomain() {
    return this.domain;
  }

  public String getGreeting() {
    return this.greeting;
  }

  /**
   * Extension point.
   *
   * @return the extensions whose ehlo keywords should be included in the reply.
   */
  protected Collection<Extension> extensionsToPresent(SmtpSession session) {
    return session.getSupportedExtensions().stream()
        .filter(e -> e.available(session))
        .collect(Collectors.toList());
  }

  @Override
  public boolean validForState(SessionState state) {
    return state == StandardStates.CONNECTED;
  }

  @Override
  public String verb() {
    return EHLO;
  }
}
