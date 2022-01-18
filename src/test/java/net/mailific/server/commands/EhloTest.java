package net.mailific.server.commands;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.EnumSet;
import net.mailific.server.extension.Extension;
import net.mailific.server.extension.auth.TransitionMatcher;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.StandardStates;
import net.mailific.server.session.Transition;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class EhloTest {

  @Mock SmtpSession session;

  @Mock Extension extension1;
  @Mock Extension extension2;
  @Mock Extension extension3;

  Ehlo it;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(extension1.getEhloAdvertisment(session)).thenReturn("ext1");
    when(extension1.available(session)).thenReturn(true);

    when(extension2.getEhloAdvertisment(session)).thenReturn("ext2");
    when(extension2.available(session)).thenReturn(false);

    when(extension3.getEhloAdvertisment(session)).thenReturn("ext3");
    when(extension3.available(session)).thenReturn(true);

    it = new Ehlo("foo");
  }

  @Test
  public void happyPath() {
    Transition t = it.handleValidCommand(session, "EHLO example.com");

    verify(session).clearMailObject();
    MatcherAssert.assertThat(t, TransitionMatcher.with(250, is("foo"), StandardStates.AFTER_EHLO));
  }

  @Test
  public void withExtensions() {
    when(session.getSupportedExtensions())
        .thenReturn(Arrays.asList(extension1, extension2, extension3));

    Transition t = it.handleValidCommand(session, "EHLO example.com");

    verify(session).clearMailObject();
    assertThat(
        t,
        TransitionMatcher.with(
            is("250-foo\r\n250-ext1\r\n250 ext3\r\n"), StandardStates.AFTER_EHLO));
  }

  @Test
  public void validForState() {
    for (StandardStates state : EnumSet.allOf(StandardStates.class)) {
      switch (state) {
        case CONNECTED:
          assertTrue(it.validForState(state));
          break;
        default:
          assertFalse(it.validForState(state));
      }
    }
  }

  @Test
  public void command() {
    assertEquals("EHLO", it.verb());
  }

  @Test
  public void greeting() {
    it = new Ehlo("domain", "greeting");

    Transition t = it.handleValidCommand(session, "EHLO example.com");

    assertEquals("250 domain greeting\r\n", t.getReply().replyString());
  }
}
