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
package net.mailific.mailbox;

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

import java.text.ParseException;

/**
 * Extracts a mailbox (in the RFC 5321 sense) from a String.
 *
 * <p>This is implementation assumes the happy path: the String is expected to contain mailbox
 * within angle brackets, without any cute tricks like escaped brackets in the address.
 *
 * <p>Spec compliance and self defense are on my todo list.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class MailboxParser {

  private final int start;
  private final int end;
  private final String mailbox;

  /**
   * @param input String containing a mailbox.
   * @param offset Position to start looking for a mailbox.
   * @throws ParseException If a mailbox can't be found in input after offset.
   */
  public MailboxParser(String input, int offset) throws ParseException {
    // TODO: Make this spec compliant and efficient.
    start = input.indexOf('<', offset) + 1;
    if (start == 0) {
      throw new ParseException("Expected <", offset + 1);
    }
    end = input.indexOf('>', start);
    if (end == -1) {
      throw new ParseException("Expected >", input.length());
    }
    mailbox = input.substring(start, end);
  }

  /**
   * @return the offset into input where the mailbox starts
   */
  public int getStart() {
    return start;
  }

  /**
   * @return the offset into input where the mailbox ends. That is, the position of the last char in
   *     the mailbox + 1;
   */
  public int getEnd() {
    return end;
  }

  /**
   * @return the offset into input where the path ends. That is, the index after the mailbox and any
   *     closing bracket (>).
   */
  public int getPathEnd() {
    return end + 1;
  }

  /**
   * @return The first mailbox found in input (after offset)
   */
  public String getMailbox() {
    return mailbox;
  }
}
