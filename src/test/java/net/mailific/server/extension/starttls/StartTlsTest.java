package net.mailific.server.extension.starttls;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import net.mailific.server.commands.CommandHandler;
import net.mailific.server.session.SmtpSession;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class StartTlsTest {

  @Mock SmtpSession session;

  StartTls it = new StartTls();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void name() {
    assertEquals(StartTls.NAME, it.getName());
  }

  @Test
  public void ehloKeyword() {
    assertEquals(StartTls.NAME, it.getEhloKeyword());
  }

  @Test
  public void verbs() {
    Collection<CommandHandler> verbs = it.commandHandlers();
    assertThat(verbs, Matchers.hasSize(1));
    assertThat(verbs.iterator().next(), instanceOf(StartTlsCommandHandler.class));
  }

  @Test
  public void available() {
    for (boolean tlsAlreadyStarted : Arrays.asList(true, false)) {
      when(session.isTlsStarted()).thenReturn(tlsAlreadyStarted);
      assertThat(it.available(session), is(!tlsAlreadyStarted));
    }
  }
}
