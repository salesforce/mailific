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

import java.net.InetAddress;
import net.mailific.spf.SpfUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SenderLocalTest {

  private AutoCloseable mocks;
  @Mock SpfUtil spf;

  InetAddress ip;

  SenderLocal it;

  @Before
  public void setup() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    ip = InetAddress.getByAddress(new byte[] {1, 2, 3, 4});
  }

  @After
  public void releaseMocks() throws Exception {
    mocks.close();
  }

  @Test
  public void testExpand() throws Exception {
    it = new SenderLocal(0, false, null);

    Assert.assertEquals("foo", it.expand(spf, ip, "baz.com", "foo@bar.com"));
  }
}
