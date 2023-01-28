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

package net.mailific.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Template-based command-line argument parser.
 *
 * <p>I know -- there are a million libraries that do this better. I didn't want to add a
 * dependency, especially as this is really just for testing.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class SimpleArgParser {

  private Set<String> supportedFlags = new HashSet<>();
  private Set<String> supportedOptions = new HashSet<>();

  private Map<String, String> parsedArgs;
  private Set<String> parsedFlags = new HashSet<>();
  private List<String> remainder;

  /**
   * Creates a parser based on a template you pass in. For example, if you want to support a flag -x
   * and a value-taking arg --foo, pass either "-x --foo value" or "--foo anyval -x". Do not include
   * any positional parameters.
   *
   * <p>After construction, call parseArgs(String[]), and then getFlac, getString, and getInt to
   * access the options given. getRemainder() returns the non-option arguments.
   *
   * @param argDefString a template for your arguments.
   */
  public SimpleArgParser(String argDefString) {
    Objects.requireNonNull(argDefString, "Argument definition string must not be null.");
    String[] argdefs = argDefString.trim().split("\\s+");
    for (int i = 0; i < argdefs.length; i++) {
      final String thisArg = argdefs[i];
      if (thisArg.charAt(0) == '-') {
        int next = i + 1;
        if (next >= argdefs.length) {
          // This is the last argument
          supportedFlags.add(thisArg);
          break;
        }
        if (argdefs[next].charAt(0) == '-') {
          // No option value
          supportedFlags.add(thisArg);
        } else {
          supportedOptions.add(thisArg);
          ++i; // Consume the value for this option
        }
      } else {
        throw new IllegalArgumentException(
            String.format("Argument value '%s' appears in template without arg: ", thisArg));
      }
    }
  }

  /** Parse the arguments based on the template given. */
  public void parseArgs(String[] args) {
    Map<String, String> parsedArgsInProgress = new HashMap<>();
    Set<String> parsedFlagsInProgress = new HashSet<>();
    List<String> remainderInProgress = new ArrayList<>();

    // Will hold any arg that is waiting for its value
    String lastArg = null;

    for (int i = 0; i < args.length; i++) {
      String thisArg = args[i];

      if (lastArg != null) {
        // We last had an arg that takes a value, so use this arg as that value
        if (parsedArgsInProgress.put(lastArg, thisArg) != null) {
          throw new IllegalArgumentException(lastArg + " appeared more than once");
        }
        lastArg = null;
      } else {
        if (thisArg.charAt(0) == '-') {
          if (!remainderInProgress.isEmpty()) {
            // We've already had a value with no option -- we should be in positional
            // arguments, not getting more flags
            // TODO: allow a "-" arg, and after receiving it, treat this case a
            // positional arg that starts with -
            throw new IllegalArgumentException(
                String.format(
                    "Received option %s after positional args %s",
                    thisArg, remainderInProgress.get(0)));
          }
          if (supportedFlags.contains(thisArg)) {
            if (!parsedFlagsInProgress.add(thisArg)) {
              throw new IllegalArgumentException(thisArg + " appeared more than once");
            }
          } else if (supportedOptions.contains(thisArg)) {
            lastArg = thisArg;
          } else {
            throw new IllegalArgumentException("Unexpected option " + thisArg);
          }
        } else {
          // Value does not start with -
          remainderInProgress.add(thisArg);
        }
      }
    }
    // We've seen all the args. If we left an option waiting for a value, complain
    if (lastArg != null) {
      throw new IllegalArgumentException(lastArg += " requires a value");
    }

    this.parsedArgs = parsedArgsInProgress;
    this.parsedFlags = parsedFlagsInProgress;
    this.remainder = remainderInProgress;
  }

  /**
   * @return any positional arguments remaining after the options are consumed.
   */
  public List<String> getRemainder() {
    return Collections.unmodifiableList(remainder);
  }

  /**
   * Look for flags. Note that unlike {@link #getString(String...)} and {@link #getInt(int,
   * String...)}, this method does not throw if more than one of the specified options is set.
   *
   * @param flags A set of flags.
   * @return true if any of the given flags was supplied with the arguments. Else false.
   */
  public boolean getFlag(String... flags) {
    for (String flag : flags) {
      if (parsedFlags.contains(flag)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param options A set of synonymous options.
   * @return the argument passed for one of these options, or null if none of the synonyms were
   *     passed.
   * @throws IllegalArgumentException if more than one of the options was specified.
   */
  public String getString(String... options) {
    String value = null;
    for (String opt : options) {
      if (parsedArgs.containsKey(opt)) {
        if (value == null) {
          value = parsedArgs.get(opt);
        } else {
          throw new IllegalArgumentException("Passed more than one synonym for " + opt);
        }
      }
    }
    return value;
  }

  /**
   * @param defaultValue value to return if the option was not set
   * @param options A set of synonymous options.
   * @return The value of {@link #getString(String...)}, converted to an int.
   * @throws IllegalArgumentException if more than one of the given options is present, or if the
   *     value supplied for the option is not a number.
   */
  public int getInt(int defaultValue, String... options) {
    String stringVal = getString(options);
    if (stringVal == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(stringVal);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Can't parse " + stringVal + " as an int.");
    }
  }
}
