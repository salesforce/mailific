package net.mailific.server.netty;

/*-
 * #%L
 * Mailific SMTP Server Library
 * %%
 * Copyright (C) 2021 - 2022 Joe Humphreys
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.Attribute;
import java.net.InetSocketAddress;
import net.mailific.server.session.SmtpSession;
import net.mailific.server.session.SmtpSessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SmtpSessionInitializerTest {

  @Mock SmtpSessionFactory sessionFactory;

  @Mock SmtpSession session;

  @Mock SocketChannel channel;

  @Mock ChannelPipeline pipeline;

  @Mock Attribute<SmtpSession> attribute;

  SmtpSessionInitializer it;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    InetSocketAddress address = InetSocketAddress.createUnresolved("foo.bar", 2525);

    when(channel.pipeline()).thenReturn(pipeline);
    when(channel.remoteAddress()).thenReturn(address);
    when(channel.attr(SmtpServerHandler.SESSION_KEY)).thenReturn(attribute);

    when(sessionFactory.newSmtpSession(address)).thenReturn(session);

    it = new SmtpSessionInitializer(sessionFactory, new MockSslContext());
  }

  @Test
  public void init() throws Exception {
    it.initChannel(channel);

    verify(attribute).set(session);
    verify(pipeline).addLast(anyString(), any(DelimiterBasedFrameDecoder.class));
    verify(pipeline).addLast(any(StringEncoder.class));
    verify(pipeline).addLast(anyString(), any(SmtpServerHandler.class));
  }
}
