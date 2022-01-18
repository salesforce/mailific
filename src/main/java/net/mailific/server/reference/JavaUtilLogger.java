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
package net.mailific.server.reference;

import java.util.logging.Level;
import java.util.logging.Logger;
import net.mailific.server.session.SmtpEventLogger;

/**
 * Simple bridge to java.util.logging for SmtpEventLogger.
 *
 * @author jhumphreys
 * @since 1.0.0
 */
public class JavaUtilLogger implements SmtpEventLogger {

  public static final String LOGGER_NAME = "SmtpServer";

  private Logger logger = Logger.getLogger(LOGGER_NAME);

  @Override
  public void info(String event, Throwable t, String format, Object... data) {
    log(Level.INFO, event, t, format, data);
  }

  private void log(Level level, String event, Throwable t, String format, Object... data) {
    if (logger.isLoggable(level)) {
      format = format == null ? event : event + " " + format;
      logger.log(level, String.format(format, data), t);
    }
  }

  @Override
  public void trace(String event, Throwable t, String format, Object... data) {
    log(Level.FINE, event, t, format, data);
  }

  @Override
  public void error(String event, Throwable t, String format, Object... data) {
    log(Level.SEVERE, event, t, format, data);
  }
}
