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

package net.mailific.server;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Represents a line sent from the client. The main purpose is to allow LineConsumers to modify the
 * line before it's passed to the next consumer in the chain. It also caches the string version of
 * the line and the verb.
 *
 * <p>Note that this implementation is <b>not thread safe</b>. It is also not very defensive. Don't
 * pass it unexpected stuff (like bytes with no \r\n at the end).
 *
 * <p>TODO: defend against unexpected stuff
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class Line {
  private final byte[] originalLine;
  private byte[] line;
  private String stripped;
  private String verb;

  /**
   * @param line Must end in CRLF
   */
  public Line(byte[] line) {
    this.originalLine = line;
  }

  /**
   * @param s String representation of a line. CRLF ending optional.
   */
  public Line(String s) {
    if (!s.endsWith("\r\n")) {
      s = s + "\r\n";
    }
    this.originalLine = s.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * @return the current version of the line
   */
  public byte[] getLine() {
    return line == null ? originalLine : line;
  }

  /**
   * @return the original version of the line
   */
  public byte[] getOriginalLine() {
    return originalLine;
  }

  /**
   * @param line update the line
   */
  public void setLine(byte[] line) {
    Objects.requireNonNull(line);
    this.line = line;
    stripped = null;
    verb = null;
  }

  /**
   * @return a String version of the line, with the trailing CRLF removed.
   */
  public String getStripped() {
    if (stripped == null) {
      stripped = new String(getLine(), 0, getLine().length - 2, StandardCharsets.UTF_8);
    }
    return stripped;
  }

  /**
   * @return The first word of the line
   */
  public String getVerb() {
    if (verb == null) {
      verb = getStripped().split(" ", 2)[0];
    }
    return verb;
  }
}
