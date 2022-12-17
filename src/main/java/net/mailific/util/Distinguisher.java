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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility for collapsing a collection to its distinct members, where "distinct" is defined as
 * "producing a non-equals() output when passed to the supplied canonicalizing function."
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class Distinguisher<T> {

  Function<T, ?> canonicalizer;

  public Distinguisher(Function<T, ?> canonicalizer) {
    this.canonicalizer = canonicalizer;
  }

  /**
   * @return the distinct members of inputs, where "distinct" is defined as "producing a
   *     non-equals() output when passed to {@link #canonicalizer}. If inputs is an ordered
   *     collection, that order will be preserved.
   */
  public List<T> distinct(Stream<T> inputs) {
    Set<Object> seen = new HashSet<>();
    return inputs.filter(i -> seen.add(canonicalizer.apply(i))).collect(Collectors.toList());
  }
}
