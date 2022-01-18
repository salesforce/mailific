package net.mailific.server.commands;

/*-
 * #%L
 * Mailific SMTP Server Library
 * %%
 * Copyright (C) 2021 - 2022 Joe Humphreys
 * %%
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
 * #L%
 */

import java.util.ArrayList;
import java.util.List;
import net.mailific.server.session.Reply;

/**
 * A Reply with multiple lines of response text.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class ExtendedReply extends Reply {

  private final List<String> details;

  /**
   * @param code The return code
   * @param detailLines A list of lines to return. Do not include the return code prefix.
   */
  private ExtendedReply(int code, List<String> detailLines) {
    super(code, null);
    this.details = detailLines;
  }

  @Override
  public String replyString() {
    int size = details.size();
    int i = 0;
    StringBuilder sb = new StringBuilder();
    while (i < size - 1) {
      sb.append(String.format("%d-%s\r\n", getCode(), details.get(i++)));
    }
    sb.append(String.format("%d %s\r\n", getCode(), details.get(i)));
    return sb.toString();
  }

  @Override
  public String toString() {
    return "ExtendedReply [" + replyString().replaceAll("\\s+", " ") + "]";
  }

  public static class Builder {
    int code;
    List<String> details = new ArrayList<>();

    public Builder(int code) {
      this.code = code;
    }

    public Builder withDetail(String detail) {
      details.add(detail);
      return this;
    }

    Reply build() {
      if (details.isEmpty()) {
        throw new RuntimeException("Reply must have details");
      }
      if (details.size() == 1) {
        return new Reply(code, details.get(0));
      }
      return new ExtendedReply(code, details);
    }
  }
}
