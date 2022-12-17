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

import java.util.ArrayList;
import java.util.List;
import net.mailific.server.Line;
import net.mailific.server.commands.CommandHandler;

public class MockHandler implements CommandHandler {

  String command;
  Transition transition;
  List<Line> consumed = new ArrayList<>();

  public MockHandler(String command) {
    this.command = command;
    this.transition = new Transition(new Reply(123, "Nice " + command), StandardStates.AFTER_EHLO);
  }

  @Override
  public Transition handleCommand(SmtpSession session, Line commandLine) {
    consumed.add(commandLine);
    return transition;
  }

  @Override
  public String verb() {
    return command;
  }

  public boolean called() {
    return !consumed.isEmpty();
  }

  public List<Line> getConsumed() {
    return consumed;
  }

  public int timesCalled() {
    return consumed.size();
  }
}
