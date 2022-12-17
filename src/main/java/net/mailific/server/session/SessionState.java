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

/**
 * Marker interface for names of session states. See {@link StandardStates} for a list of the common
 * states in an SMTP session. This interface exists so extensions can define their own states. When
 * testing for states, keep in mind that such extension states may exist.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public interface SessionState {

  // TODO: Move to StandardStates? Move to Transition?
  /** For use in {@link Transition} to indicate a decision not to change state. */
  SessionState NO_STATE_CHANGE =
      new SessionState() {
        @Override
        public String name() {
          return "NO_STATE_CHANGE";
        }
      };

  String name();
}
