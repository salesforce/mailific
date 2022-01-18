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
