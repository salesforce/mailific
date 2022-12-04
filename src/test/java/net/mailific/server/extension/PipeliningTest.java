package net.mailific.server.extension;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PipeliningTest {

  Pipelining it = new Pipelining();

  @Test
  public void name() {
    assertEquals(Pipelining.NAME, it.getName());
  }

  @Test
  public void ehloKeyword() {
    assertEquals(Pipelining.EHLO_KEYWORD, it.getEhloKeyword());
  }

  @Test
  public void verbs() {
    assertThat(it.commandHandlers(), empty());
  }
}
