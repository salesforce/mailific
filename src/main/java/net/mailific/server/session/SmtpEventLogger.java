/*
 * Copyright 2021 Joe Humphreys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package net.mailific.server.session;

/**
 * Rather than adding a dependency on one of the million different "last logging libraries you'll
 * ever need", I just made an interface that all the logging goest through.
 *
 * <p>The event param should be a shortish unique string for searching on.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public interface SmtpEventLogger {

  /** @param event a shortish unique string for searching on. */
  void info(String event, Throwable t, String format, Object... data);

  /** @param event a shortish unique string for searching on. */
  void trace(String event, Throwable t, String format, Object... data);

  /** @param event a shortish unique string for searching on. */
  void error(String event, Throwable t, String format, Object... data);
}
