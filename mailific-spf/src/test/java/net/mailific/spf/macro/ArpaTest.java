/*-
 * Mailific SMTP Server Library
 *
 * Copyright (C) 2023 Joe Humphreys
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

package net.mailific.spf.macro;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ArpaTest {

  @Test
  public void string() {
    assertEquals("%{v}", new Arpa(0, false, null, false).toString());
    assertEquals("%{V}", new Arpa(0, false, null, true).toString());
    assertEquals("%{v1r+-}", new Arpa(1, true, "+-", false).toString());
  }
}