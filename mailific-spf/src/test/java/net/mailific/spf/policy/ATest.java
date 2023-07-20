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

package net.mailific.spf.policy;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.List;
import net.mailific.spf.SpfUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ATest {

  A it;

  @Mock SpfUtil spf;

  InetAddress ip;
  String domain = "foo.bar";
  String sender = "joe@foo.bar";
  String ehloParam = domain;

  private AutoCloseable mocks;

  @Before
  public void setup() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    ip = InetAddress.getByName("1.2.3.4");
  }

  @After
  public void releaseMocks() throws Exception {
    mocks.close();
  }

  @Test
  public void happyPath() throws Exception {
    when(spf.getIpsByHostname(domain, true)).thenReturn(List.of(ip));
    when(spf.cidrMatch(ip, ip, -1)).thenReturn(true);
    it = new A(null, -1);
    assertTrue(it.matches(spf, ip, domain, sender, ehloParam));
  }
}
