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
