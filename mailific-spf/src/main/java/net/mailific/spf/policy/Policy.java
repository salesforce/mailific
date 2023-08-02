/*-
 * Mailific SMTP Server Library
 *
 * Copyright (C) 2023 Joe Humphreys
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

package net.mailific.spf.policy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import net.mailific.spf.parser.ParseException;
import net.mailific.spf.parser.SpfPolicy;

public class Policy {
  private final String version;
  private final List<Directive> directives;
  private final Redirect redirect;
  private final Explanation explanation;
  private final List<Modifier> modifiers;

  public static Policy parse(String spfRecord, String explainPrefix)
      throws ParseException, PolicySyntaxException {
    SpfPolicy parser =
        new SpfPolicy(
            new ByteArrayInputStream(spfRecord.getBytes(StandardCharsets.US_ASCII)), "US-ASCII");
    parser.setExplainPrefix(explainPrefix);
    return parser.policy();
  }

  public Policy(String version, List<Directive> directives, List<Modifier> modifiers)
      throws PolicySyntaxException {
    this.version = version;
    this.directives = directives == null ? Collections.emptyList() : directives;
    this.modifiers = modifiers == null ? Collections.emptyList() : modifiers;
    Redirect redirect = null;
    Explanation explanation = null;
    for (Modifier m : this.modifiers) {
      if (m instanceof Redirect) {
        if (redirect == null) {
          redirect = (Redirect) m;
        } else {
          throw new PolicySyntaxException("More than one redirect found.");
        }
      } else if (m instanceof Explanation) {
        if (explanation == null) {
          explanation = (Explanation) m;
        } else {
          throw new PolicySyntaxException("More than one explanation found.");
        }
      }
    }
    this.redirect = redirect;
    this.explanation = explanation;
  }

  public String getVersion() {
    return version;
  }

  public List<Directive> getDirectives() {
    return directives;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getVersion());
    directives.forEach(d -> sb.append(' ').append(d));
    modifiers.forEach(m -> sb.append(' ').append(m));
    return sb.toString();
  }

  public Redirect getRedirect() {
    return redirect;
  }

  public Explanation getExplanation() {
    return explanation;
  }
}
