package net.mailific.server.session;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class TransitionTest {

  Transition it;

  Reply reply = new Reply(234, "OK dude");

  SessionState state = StandardStates.AFTER_EHLO;

  @Before
  public void setup() {
    it = new Transition(reply, state);
  }

  @Test
  public void nullArgs() {
    assertThrows(
        NullPointerException.class,
        () -> {
          new Transition(null, state);
        });
    assertThrows(
        NullPointerException.class,
        () -> {
          new Transition(reply, null);
        });
  }

  @Test
  public void getters() {
    assertEquals(reply, it.getReply());
    assertEquals(state, it.getNextState());
  }

  @Test
  public void testToString() {
    assertThat(it.toString(), CoreMatchers.containsString("reply=Reply [234, OK dude]"));
    assertThat(it.toString(), CoreMatchers.containsString("nextState=AFTER_EHLO"));
  }
}
