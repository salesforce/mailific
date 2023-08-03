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

package net.mailific.spf.dns;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class JndiResolverTest {

  String baseDomain = "smtpqa.com";
  JndiResolver it;

  @Before
  public void setup() {
    it = new JndiResolver();
  }

  @Test
  public void singleTxtRecord() throws DnsFail {
    List<String> actual = it.resolveTxtRecords("orange." + baseDomain);
    assertThat(actual, contains("orange"));
  }

  @Test
  public void twoTxtRecords() throws DnsFail {
    List<String> actual = it.resolveTxtRecords("fruit." + baseDomain);
    assertThat(actual, containsInAnyOrder("apples", "oranges"));
  }

  @Test
  public void noTxtRecords() throws DnsFail {
    List<String> actual = it.resolveTxtRecords("other." + baseDomain);
    assertEquals(0, actual.size());
  }

  @Test
  public void nameNotFoundTxt() throws DnsFail {
    assertThrows(
        NameNotFound.class,
        () -> {
          it.resolveTxtRecords("nosuchname." + baseDomain);
        });
  }
}
