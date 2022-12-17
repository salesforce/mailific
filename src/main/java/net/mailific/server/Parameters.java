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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// RFC5321 says that extensions may specify case-sensitive elements, but I do not know
// of any that specify case-sensitive parameter names. It seems more robust to assume
// case insensitivity. If necessary, we can add case-sensitive methods later.

/**
 * Represents a set of parameters included in an SMTP command line.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class Parameters {

  private static final Pattern splitter = Pattern.compile("([^ =]+)(?:=([^ =]+))?");

  private final Map<String, String> params = new HashMap<>();

  /**
   * @param line The command line
   * @param offset The offset into line where the parameters start.
   */
  public Parameters(String line, int offset) {
    // TODO optimize and validate
    Matcher m = splitter.matcher(line);
    if (m.find(offset)) {
      do {
        String key = m.group(1).toUpperCase();
        String value = m.group(2);
        if (value == null) {
          value = "";
        }
        params.put(key, value);
      } while (m.find());
    }
  }

  /**
   * @param param case-insensitive keyword
   * @return true if the parameter was specified
   */
  public boolean exists(String param) {
    return params.containsKey(param.toUpperCase());
  }

  /**
   * @param param case-insensitive keyword
   * @return the value associated with this param, or "" if the param was given with no value, or
   *     null if the param was not given.
   */
  public String get(String param) {
    return params.get(param.toUpperCase());
  }

  /**
   * @return A Set containing the names of all supplied parameters. Names will be in all uppercase,
   *     regardless of how they were supplied.
   */
  public Set<String> getParameterNames() {
    return Collections.unmodifiableSet(params.keySet());
  }
}
