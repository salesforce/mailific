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
package net.mailific.server.extension.auth;

import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.Transition;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class TransitionMatcher extends TypeSafeMatcher<Transition> {

  private Reply specificReply;
  private Integer replyCode;
  private Matcher<String> replyDetailMatcher;
  private Matcher<String> replyStringMatcher;
  private SessionState state;

  /**
   * @param reply The exact reply object (compared with ==)
   */
  public static Matcher<Transition> with(Reply reply, SessionState state) {
    return new Builder().withSpecificReply(reply).withState(state).build();
  }

  public static Matcher<Transition> with(
      int code, Matcher<String> detailMatcher, SessionState state) {
    return new Builder()
        .withReplyCode(code)
        .withReplyDetailMatcher(detailMatcher)
        .withState(state)
        .build();
  }

  public static Matcher<Transition> with(Matcher<String> replyStringMatcher, SessionState state) {
    return new Builder().withReplyStringMatcher(replyStringMatcher).withState(state).build();
  }

  private TransitionMatcher(Builder builder) {
    this.specificReply = builder.specificReply;
    this.replyCode = builder.replyCode;
    this.replyDetailMatcher = builder.replyDetailMatcher;
    this.replyStringMatcher = builder.replyStringMatcher;
    this.state = builder.state;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("A transition ");
    if (specificReply != null) {
      description.appendText("with the Reply <" + specificReply + "> ");
    } else {
      if (replyCode != null) {
        description.appendText("with a Reply code of " + replyCode);
      }
      if (replyDetailMatcher != null) {
        description.appendText(" with a detail ");
        replyDetailMatcher.describeTo(description);
      }
      if (replyStringMatcher != null) {
        description.appendText(" with a replyString ");
        replyStringMatcher.describeTo(description);
      }
    }
    if (state != null) {
      description.appendText(" and a state change to " + state.name());
    }
  }

  @Override
  protected boolean matchesSafely(Transition transition) {
    if (specificReply != null) {
      if (specificReply != transition.getReply()) {
        return false;
      }
    }
    if (replyCode != null && transition.getReply().getCode() != replyCode) {
      return false;
    }
    if (replyDetailMatcher != null
        && !replyDetailMatcher.matches(transition.getReply().getDetail())) {
      return false;
    }
    if (replyStringMatcher != null
        && !replyStringMatcher.matches(transition.getReply().replyString())) {
      return false;
    }
    if (state != null && !state.name().equals(transition.getNextState().name())) {
      return false;
    }
    return true;
  }

  /**
   * Creates builder to build {@link TransitionMatcher}.
   *
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder to build {@link TransitionMatcher}. */
  public static final class Builder {
    private Reply specificReply;
    private Integer replyCode;
    private Matcher<String> replyDetailMatcher;
    private Matcher<String> replyStringMatcher;
    private SessionState state;

    private Builder() {}

    public Builder withSpecificReply(Reply specificReply) {
      this.specificReply = specificReply;
      return this;
    }

    public Builder withReplyCode(Integer replyCode) {
      this.replyCode = replyCode;
      return this;
    }

    public Builder withReplyDetailMatcher(Matcher<String> replyDetailMatcher) {
      this.replyDetailMatcher = replyDetailMatcher;
      return this;
    }

    public Builder withReplyStringMatcher(Matcher<String> replyStringMatcher) {
      this.replyStringMatcher = replyStringMatcher;
      return this;
    }

    public Builder withState(SessionState state) {
      this.state = state;
      return this;
    }

    public TransitionMatcher build() {
      return new TransitionMatcher(this);
    }
  }
}
