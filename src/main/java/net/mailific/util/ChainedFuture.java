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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Simple class for waiting on multiple Futures. This was built for a particular need, not to be a
 * general-purpose solution, so be warned that its assumptions may not match your needs. I'll expand
 * it if needed.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class ChainedFuture implements Future<Void> {

  final List<Future<?>> chain = new ArrayList<>();

  /**
   * Add a future to the chain.
   *
   * @return itself so you can chain calls
   */
  public ChainedFuture chain(Future<?> f) {
    Objects.requireNonNull(f, "You may not chain null Futures.");
    chain.add(f);
    return this;
  }

  /**
   * Calls {@link Future#cancel(boolean)} on all chained futures.
   *
   * @return true if chain is empty or all chained Futures returned true.
   */
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    boolean success = true;
    for (Future<?> f : chain) {
      success = f.cancel(mayInterruptIfRunning) && success;
    }
    return success;
  }

  /**
   * @return true if {@link Future#isCancelled()} returns true on any of the chained futures.
   */
  @Override
  public boolean isCancelled() {
    return chain.stream().anyMatch(Future::isCancelled);
  }

  /**
   * @return true if chain is empty or {@link Future#isDone()} returns true for all chained futures.
   */
  @Override
  public boolean isDone() {
    return chain.stream().allMatch(Future::isDone);
  }

  /** Calls {@link Future#get()} on each future in the chain. */
  @Override
  public Void get() throws InterruptedException, ExecutionException {
    for (Future<?> f : chain) {
      f.get();
    }
    return null;
  }

  /**
   * Calls {@link Future#get(long, TimeUnit)} on each future in the chain, aborting if the total
   * time exceeds the timeout.
   */
  @Override
  public Void get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    long end = nanoTime() + unit.toNanos(timeout);
    for (Future<?> f : chain) {
      if (nanoTime() > end) {
        throw new TimeoutException();
      }
      f.get(end - nanoTime(), TimeUnit.NANOSECONDS);
    }
    return null;
  }

  long nanoTime() {
    return System.nanoTime();
  }
}
