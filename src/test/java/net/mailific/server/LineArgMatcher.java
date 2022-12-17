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

import java.util.Objects;
import org.mockito.ArgumentMatcher;

public class LineArgMatcher implements ArgumentMatcher<Line> {

  private final String stripped;

  public LineArgMatcher(String stripped) {
    this.stripped = stripped;
  }

  public LineArgMatcher(Line line) {
    stripped = line.getStripped();
  }

  @Override
  public boolean matches(Line argument) {
    return Objects.equals(stripped, argument.getStripped());
  }
}
