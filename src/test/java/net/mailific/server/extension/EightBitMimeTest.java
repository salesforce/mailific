package net.mailific.server.extension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EightBitMimeTest {

  EightBitMime it = new EightBitMime();

  @Test
  public void name() {
    assertEquals(EightBitMime.NAME, it.getName());
  }

  @Test
  public void ehloKeyword() {
    assertEquals(EightBitMime.EHLO_KEYWORD, it.getEhloKeyword());
  }

  @Test
  public void verbs() {
    assertThat(it.commandHandlers(), empty());
  }
}
