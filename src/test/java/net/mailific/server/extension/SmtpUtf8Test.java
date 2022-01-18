package net.mailific.server.extension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SmtpUtf8Test {

  SmtpUtf8 it = new SmtpUtf8();

  @Test
  public void name() {
    assertEquals(SmtpUtf8.NAME, it.getName());
  }

  @Test
  public void ehloKeyword() {
    assertEquals(SmtpUtf8.EHLO_KEYWORD, it.getEhloKeyword());
  }

  @Test
  public void verbs() {
    assertThat(it.commandHandlers(), empty());
  }
}
