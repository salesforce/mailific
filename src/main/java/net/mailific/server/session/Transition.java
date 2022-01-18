/*
 * Copyright 2021 Joe Humphreys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package net.mailific.server.session;

import java.util.Objects;

/**
 * Represents the result of processing a line, which may include a reply back to the client and/or a
 * change in the session state.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class Transition {

  /** For use to indicate a decision not to handle input. */
  public static final Transition UNHANDLED =
      new Transition(Reply._500_UNRECOGNIZED, StandardStates.NO_STATE_CHANGE);

  private final Reply reply;
  private final SessionState nextState;

  public Transition(Reply reply, SessionState nextState) {
    Objects.requireNonNull(reply, "Reply must not be null.");
    Objects.requireNonNull(nextState, "NextState must not be null.");
    this.reply = reply;
    this.nextState = nextState;
  }

  /** @return A Reply to be returned to the client (unless it's {@link Reply#DO_NOT_REPLY} */
  public Reply getReply() {
    return reply;
  }

  /**
   * @return The SessionState that the session should move to (or {@link
   *     SessionState#NO_STATE_CHANGE} to indicate the state should not change.
   */
  public SessionState getNextState() {
    return nextState;
  }

  @Override
  public String toString() {
    return "Transition [reply=" + reply + ", nextState=" + nextState.name() + "]";
  }
}
