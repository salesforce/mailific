package net.mailific.server.reference;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.Before;
import org.junit.Test;

public class JavaUtilLoggerTest {

  MockHandler handler;

  JavaUtilLogger it = new JavaUtilLogger();

  @Before
  public void setup() {
    handler = new MockHandler();
    handler.setLevel(Level.FINEST);
    final Logger logger = Logger.getLogger(JavaUtilLogger.LOGGER_NAME);
    logger.addHandler(handler);
    logger.setLevel(Level.FINEST);
  }

  @Test
  public void info() {
    NullPointerException thrown = new NullPointerException();
    it.info("foo", thrown, ">%s<", "bar");

    assertEquals(1, handler.logged.size());
    LogRecord rec = handler.logged.get(0);
    assertEquals(Level.INFO, rec.getLevel());
    assertEquals("foo >bar<", rec.getMessage());
    assertEquals(thrown, rec.getThrown());
  }

  @Test
  public void trace() {
    NullPointerException thrown = new NullPointerException();
    it.trace("foo", thrown, ">%s<", "bar");

    assertEquals(1, handler.logged.size());
    LogRecord rec = handler.logged.get(0);
    assertEquals(Level.FINE, rec.getLevel());
    assertEquals("foo >bar<", rec.getMessage());
    assertEquals(thrown, rec.getThrown());
  }

  @Test
  public void error() {
    NullPointerException thrown = new NullPointerException();
    it.error("foo", thrown, ">%s<", "bar");

    assertEquals(1, handler.logged.size());
    LogRecord rec = handler.logged.get(0);
    assertEquals(Level.SEVERE, rec.getLevel());
    assertEquals("foo >bar<", rec.getMessage());
    assertEquals(thrown, rec.getThrown());
  }

  @Test
  public void tooFine() {
    Logger.getLogger(JavaUtilLogger.LOGGER_NAME).setLevel(Level.SEVERE);
    it.trace("foo", null, ">%s<", "bar");
    assertEquals(0, handler.logged.size());
  }

  private static class MockHandler extends Handler {
    public List<LogRecord> logged = new ArrayList<>();

    @Override
    public void publish(LogRecord record) {
      logged.add(record);
    }

    @Override
    public void flush() {}

    @Override
    public void close() throws SecurityException {}
  }
}
