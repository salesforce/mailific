package net.mailific.server.session;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReplyTest {

  @Test
  public void success() {
    assertFalse(new Reply(199, "foo").success());
    assertTrue(new Reply(200, "foo").success());
    assertTrue(new Reply(220, "foo").success());
    assertTrue(new Reply(250, "foo").success());
    assertTrue(new Reply(299, "foo").success());
    assertFalse(new Reply(300, "foo").success());
    assertFalse(new Reply(421, "foo").success());
    assertFalse(new Reply(500, "foo").success());
  }
}
