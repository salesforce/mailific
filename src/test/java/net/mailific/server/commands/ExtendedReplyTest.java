package net.mailific.server.commands;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

public class ExtendedReplyTest {

  @Test
  public void oneDetail() {
    ExtendedReply.Builder builder = new ExtendedReply.Builder(250).withDetail("foo");

    assertEquals("250 foo\r\n", builder.build().replyString());
  }

  @Test
  public void multiDetails() {
    ExtendedReply.Builder builder =
        new ExtendedReply.Builder(275).withDetail("foo").withDetail("bar").withDetail("baz");

    assertEquals("275-foo\r\n275-bar\r\n275 baz\r\n", builder.build().replyString());
  }

  @Test
  public void noDetails() {
    ExtendedReply.Builder builder = new ExtendedReply.Builder(275);

    Assert.assertThrows(
        RuntimeException.class,
        () -> {
          builder.build();
        });
  }

  @Test
  public void testToString() {
    ExtendedReply.Builder builder =
        new ExtendedReply.Builder(250).withDetail("foo").withDetail("bar");

    assertEquals("ExtendedReply [250-foo 250 bar ]", builder.build().toString());
  }
}
