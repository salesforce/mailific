/*-
 * Mailific SMTP Server Library
 *
 * Copyright (C) 2021-2022 Joe Humphreys
 *
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
 */
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
