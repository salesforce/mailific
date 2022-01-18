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
package net.mailific.server.commands;

import java.io.IOException;
import net.mailific.server.Line;
import net.mailific.server.LineConsumer;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SessionState;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;

/**
 * LineConsumer that handles message data. Installed by the {@link Data} command handler when
 * needed.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class DataLineConsumer implements LineConsumer {

  /* Once the Data transfer has started, the server can't signal errors until the client
   * sends the end of data marker. So we cache any errors here. If an error has happened,
   * we will go into a mode where we ignore any further data.
   */
  private Reply pendingErrorReply;

  /*
   * As each line is read, send it to the MailObject, until we hit the end-of-data
   * marker or an error occurs.
   */
  @Override
  public Transition consume(SmtpSession session, Line line) {
    // We'll use this to skip any leading dot
    int startIndex = 0;
    byte[] data = line.getLine();
    if (data[0] == '.') {
      if (data.length == 3) {
        // There will always be a CRLF, so this must be the end-of-data command
        session.removeLineConsumer(Data.DATA_FILTER_KEY);
        if (errorPending()) {
          session.clearMailObject();
          return new Transition(pendingErrorReply, StandardStates.AFTER_EHLO);
        } else {
          Reply reply = Reply._554_SERVER_ERROR;
          try {
            reply = session.completeMailObject();
          } catch (Exception e) {
            session.getEventLogger().error("MAIL_COMPLETE_ERROR", e, e.getMessage());
          }
          return new Transition(reply, StandardStates.AFTER_EHLO);
        }
      } else {
        // If a dot is followed by more data, skip the leading dot. See RFC5321.4.5.2
        startIndex = 1;
      }
    }

    try {
      if (!errorPending()) {
        session.getMailObject().writeLine(data, startIndex, data.length - startIndex);
      }
    } catch (IOException e) {
      session.getEventLogger().error("DATA_PHASE_ERROR", e, e.getMessage());
      pendingErrorReply = Reply._554_SERVER_ERROR;
    }
    return new Transition(Reply.DO_NOT_REPLY, SessionState.NO_STATE_CHANGE);
  }

  private boolean errorPending() {
    return pendingErrorReply != null;
  }
}
