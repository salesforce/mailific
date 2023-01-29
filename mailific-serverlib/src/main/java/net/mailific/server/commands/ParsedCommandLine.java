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

import net.mailific.server.Parameters;

/**
 * A data object to hold a command line and its constituent parts.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class ParsedCommandLine {

  private final String line;
  private final String command;
  private final String path;
  private final Parameters params;

  /**
   * @param line The complete command line, without the CRLF
   * @param command The command verb parsed out of the line
   * @param path The path, if any, parsed out of the line
   * @param params The parameters, if any, parsed out of the line
   */
  public ParsedCommandLine(String line, String command, String path, Parameters params) {
    this.line = line;
    this.command = command;
    this.path = path;
    this.params = params;
  }

  /**
   * @return The command verb parsed out of the line
   */
  public String getCommand() {
    return command;
  }

  /**
   * @return The complete command line, without the CRLF
   */
  public String getLine() {
    return line;
  }

  /**
   * @return The parameters, if any, parsed out of the line. May return null.
   */
  public Parameters getParameters() {
    return params;
  }

  /**
   * @return The path, if any, parsed out of the line. May return null.
   */
  public String getPath() {
    return path;
  }
}
