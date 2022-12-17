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
package net.mailific.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ChainedFutureTest {

  @Mock Future<Boolean> f1;

  @Mock Future<Void> f2;

  ChainedFuture it;

  private AutoCloseable closeable;

  @Before
  public void setup() {
    closeable = MockitoAnnotations.openMocks(this);
    it = new ChainedFuture();
  }

  @After
  public void releaseMocks() throws Exception {
    closeable.close();
  }

  @Test
  public void chain_nullArg() {
    assertThrows(
        NullPointerException.class,
        () -> {
          it.chain(null);
        });
  }

  @Test
  public void cancel_returnValue() {
    // Nothing chained
    assertTrue(it.cancel(true));

    // Single chained returns true
    when(f1.cancel(true)).thenReturn(true);
    it.chain(f1);
    assertTrue(it.cancel(true));

    // Chained return true and false
    when(f2.cancel(true)).thenReturn(false);
    it.chain(f2);
    assertFalse(it.cancel(true));

    // All chained return false
    when(f1.cancel(true)).thenReturn(false);
    it.chain(f1);
    assertFalse(it.cancel(true));

    // Chained return false and true
    when(f2.cancel(true)).thenReturn(true);
    it.chain(f2);
    assertFalse(it.cancel(true));
  }

  @Test
  public void cancel_cancelsChained() {
    it.chain(f1).chain(f2);

    it.cancel(true);

    verify(f1).cancel(true);
    verify(f2).cancel(true);
  }

  @Test
  public void cancel_passArgument() {
    it.chain(f1);

    it.cancel(true);
    verify(f1).cancel(true);

    it.cancel(false);
    verify(f1).cancel(false);
  }

  @Test
  public void isCanceled() {
    when(f1.isCancelled()).thenReturn(false);
    when(f2.isCancelled()).thenReturn(true);

    // Empty chain
    assertFalse(it.isCancelled());

    // None canceled
    it.chain(f1);
    assertFalse(it.isCancelled());

    // Some canceled
    it.chain(f2);
    assertTrue(it.isCancelled());
  }

  @Test
  public void isDone() {
    when(f1.isDone()).thenReturn(true);
    when(f2.isDone()).thenReturn(false);

    // Empty Chain
    assertTrue(it.isDone());

    // All Done
    it.chain(f1);
    assertTrue(it.isDone());

    // Some not Done
    it.chain(f2);
    assertFalse(it.isDone());
  }

  @Test
  public void get() throws Exception {
    it.chain(f1).chain(f2);

    it.get();

    verify(f1).get();
    verify(f2).get();
  }

  @Test
  public void get_withTimeOut() throws Exception {
    it.chain(f1).chain(f2);

    it.get(100l, TimeUnit.DAYS);
  }

  @Test
  public void get_enoughTime() throws Exception {
    it =
        new ChainedFuture() {
          long[] times = {10l, 20l, 30l, 40l, 50l, 60l};
          int i = 0;

          @Override
          long nanoTime() {
            return times[i++];
          }
          ;
        };

    it.chain(f1).chain(f2);

    it.get(100l, TimeUnit.NANOSECONDS);

    verify(f1).get(80l, TimeUnit.NANOSECONDS);
    verify(f2).get(60l, TimeUnit.NANOSECONDS);
  }

  @Test
  public void get_notEnoughTime() throws Exception {
    it =
        new ChainedFuture() {
          long[] times = {10l, 20l, 30l, 40l, 50l, 60l};
          int i = 0;

          @Override
          long nanoTime() {
            return times[i++];
          }
          ;
        };

    it.chain(f1).chain(f2);

    assertThrows(
        TimeoutException.class,
        () -> {
          it.get(25l, TimeUnit.NANOSECONDS);
        });

    verify(f1).get(5l, TimeUnit.NANOSECONDS);
    verify(f2, never()).get(anyLong(), any());
  }
}
