package net.mailific.server.commands;

import static org.junit.Assert.assertEquals;

import net.mailific.server.session.Reply;
import org.junit.Test;

public class ExtendedResponseTest {

  @Test
  public void testOne() {
    Reply oneLiner = new ExtendedReply.Builder(9).withDetail("foo").build();
    assertEquals("9 foo\r\n", oneLiner.replyString());
  }

  @Test
  public void testMulti() {
    Reply oneLiner = new ExtendedReply.Builder(11).withDetail("bar").withDetail("baz").build();
    assertEquals("11-bar\r\n11 baz\r\n", oneLiner.replyString());
  }
}
