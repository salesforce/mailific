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
package net.mailific.server.session;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import net.mailific.server.Line;
import net.mailific.server.LineConsumer;

/**
 * LineConsumer that tries a list of other LineConsumers in order.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class LineConsumerChain implements LineConsumer {

  private List<Filter> filters = new LinkedList<>();

  @Override
  public Transition connect(SmtpSession session) {
    for (Filter f : filters) {
      Transition transition = f.consumer.connect(session);
      if (transition != Transition.UNHANDLED) {
        return transition;
      }
    }
    return Transition.UNHANDLED;
  }

  @Override
  public Transition consume(SmtpSession session, Line line) {
    for (Filter f : filters) {
      Transition transition = f.consumer.consume(session, line);
      if (transition != Transition.UNHANDLED) {
        return transition;
      }
    }
    return Transition.UNHANDLED;
  }

  /**
   * @param selector A key that can be used to remove the consumer later. The new consumer will
   *     replace any existing consumer with the same key.
   */
  public void addLineConsumer(String selector, LineConsumer consumer) {
    removeLineConsumer(selector);
    filters.add(0, new Filter(selector, consumer));
  }

  /**
   * Removes a consumer from the chain. This is safe to do during a consume / connect operation, but
   * won't take effect for that operation.
   *
   * @param selector Selector that was used to add the consumer you want to remove.
   */
  public void removeLineConsumer(String selector) {
    filters =
        filters.stream().filter(f -> !f.selector.equals(selector)).collect(Collectors.toList());
  }

  private static class Filter {
    String selector;
    LineConsumer consumer;

    Filter(String selector, LineConsumer consumer) {
      this.selector = selector;
      this.consumer = consumer;
    }
  }
}
