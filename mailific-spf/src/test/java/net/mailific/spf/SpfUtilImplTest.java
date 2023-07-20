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

package net.mailific.spf;

import java.net.InetAddress;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class SpfUtilImplTest {

  private SpfUtilImp it;

  private AutoCloseable mocks;

  @Before
  public void setup() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    it = new SpfUtilImp();
  }

  @After
  public void releaseMocks() throws Exception {
    mocks.close();
  }

  @Test
  public void cidrMatch() throws Exception {
    assertCiderMatch("1.23.45.67", "1.23.255.255", 16, true);
    assertCiderMatch("1.23.45.67", "1.23.255.255", 17, false);
    assertCiderMatch("1.23.45.67", "1.23.45.67", -1, true);
    assertCiderMatch("1.23.45.67", "1.23.255.255", -1, false);
  }

  void assertCiderMatch(String ip1, String ip2, int bits, boolean match) throws Exception {
    InetAddress address1 = InetAddress.getByName(ip1);
    InetAddress address2 = InetAddress.getByName(ip2);
    Assert.assertEquals(match, it.cidrMatch(address1, address2, bits));
  }
}
