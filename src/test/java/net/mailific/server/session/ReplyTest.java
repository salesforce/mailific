package net.mailific.server.session;

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
