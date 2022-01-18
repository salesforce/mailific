package net.mailific.server.session;

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

/**
 * Represents an SMTP reply.
 *
 * <p>Includes constants for the most common replies, and the special value DO_NOT_REPLY, which
 * indicates that the server should not reply and should continue listening for more data.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class Reply {

  public static final Reply _221_OK = new Reply(221, "OK");
  public static final Reply _250_OK = new Reply(250, "OK");

  public static final Reply _354_CONTINUE = new Reply(354, "Your dime");

  public static final Reply _421_SHUTTING_DOWN = new Reply(421, "Server shutting down");

  public static final Reply _451_LOCAL_ERROR =
      new Reply(451, "Requested action aborted: local error in processing");

  public static final Reply _500_UNRECOGNIZED = new Reply(500, "unrecognized command");
  public static final Reply _501_BAD_ARGS =
      new Reply(501, "Syntax error in parameters or arguments");
  public static final Reply _503_BAD_SEQUENCE = new Reply(503, "bad sequence of commands");
  public static final Reply _504_BAD_PARAM = new Reply(504, "Command parameter not implemented");
  public static final Reply _554_SERVER_ERROR = new Reply(554, "Server error");

  /** Special value indicating that no reply should be sent. */
  public static final Reply DO_NOT_REPLY = new Reply(-1, "DO NOT REPLY");

  private final int code;
  private final String detail;

  public Reply(int code, String detail) {
    this.code = code;
    this.detail = detail;
  }

  public int getCode() {
    return code;
  }

  public String getDetail() {
    return detail;
  }

  public boolean success() {
    return code >= 200 && code < 300;
  }

  /** @return The reply formatted per the SMTP specification. */
  public String replyString() {
    return String.format("%d %s\r\n", code, detail);
  }

  /** A representation for logging and debugging -- NOT in SMTP format. */
  @Override
  public String toString() {
    return "Reply [" + code + ", " + detail + "]";
  }
}
