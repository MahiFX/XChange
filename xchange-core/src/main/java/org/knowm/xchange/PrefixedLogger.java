package org.knowm.xchange;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.CheckReturnValue;
import org.slf4j.spi.LoggingEventBuilder;

public class PrefixedLogger implements Logger {
  private final String prefix;
  private final Logger delegate;

  public PrefixedLogger(String prefix, Logger logger) {
    this.prefix = prefix;
    delegate = logger;
  }

  private String prefix(String msg) {
    return prefix + ": " + msg;
  }

  @Override
  public String getName() {
    return delegate.getName();
  }


  @Override
  public LoggingEventBuilder makeLoggingEventBuilder(Level level) {
    return delegate.makeLoggingEventBuilder(level);
  }

  @Override
  @CheckReturnValue
  public LoggingEventBuilder atLevel(Level level) {
    return delegate.atLevel(level);
  }

  @Override
  public boolean isEnabledForLevel(Level level) {
    return delegate.isEnabledForLevel(level);
  }

  @Override
  public boolean isTraceEnabled() {
    return delegate.isTraceEnabled();
  }

  @Override
  public void trace(String msg) {
    delegate.trace(prefix(msg));
  }

  @Override
  public void trace(String format, Object arg) {
    delegate.trace(prefix(format), arg);
  }

  @Override
  public void trace(String format, Object arg1, Object arg2) {
    delegate.trace(prefix(format), arg1, arg2);
  }

  @Override
  public void trace(String format, Object... arguments) {
    delegate.trace(prefix(format), arguments);
  }

  @Override
  public void trace(String msg, Throwable t) {
    delegate.trace(prefix(msg), t);
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return delegate.isTraceEnabled(marker);
  }

  @Override
  @CheckReturnValue
  public LoggingEventBuilder atTrace() {
    return delegate.atTrace();
  }

  @Override
  public void trace(Marker marker, String msg) {
    delegate.trace(marker, prefix(msg));
  }

  @Override
  public void trace(Marker marker, String format, Object arg) {
    delegate.trace(marker, prefix(format), arg);
  }

  @Override
  public void trace(Marker marker, String format, Object arg1, Object arg2) {
    delegate.trace(marker, prefix(format), arg1, arg2);
  }

  @Override
  public void trace(Marker marker, String format, Object... argArray) {
    delegate.trace(marker, prefix(format), argArray);
  }

  @Override
  public void trace(Marker marker, String msg, Throwable t) {
    delegate.trace(marker, prefix(msg), t);
  }

  @Override
  public boolean isDebugEnabled() {
    return delegate.isDebugEnabled();
  }

  @Override
  public void debug(String msg) {
    delegate.debug(prefix(msg));
  }

  @Override
  public void debug(String format, Object arg) {
    delegate.debug(prefix(format), arg);
  }

  @Override
  public void debug(String format, Object arg1, Object arg2) {
    delegate.debug(prefix(format), arg1, arg2);
  }

  @Override
  public void debug(String format, Object... arguments) {
    delegate.debug(prefix(format), arguments);
  }

  @Override
  public void debug(String msg, Throwable t) {
    delegate.debug(prefix(msg), t);
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return delegate.isDebugEnabled(marker);
  }

  @Override
  public void debug(Marker marker, String msg) {
    delegate.debug(marker, prefix(msg));
  }

  @Override
  public void debug(Marker marker, String format, Object arg) {
    delegate.debug(marker, prefix(format), arg);
  }

  @Override
  public void debug(Marker marker, String format, Object arg1, Object arg2) {
    delegate.debug(marker, prefix(format), arg1, arg2);
  }

  @Override
  public void debug(Marker marker, String format, Object... arguments) {
    delegate.debug(marker, prefix(format), arguments);
  }

  @Override
  public void debug(Marker marker, String msg, Throwable t) {
    delegate.debug(marker, prefix(msg), t);
  }

  @Override
  @CheckReturnValue
  public LoggingEventBuilder atDebug() {
    return delegate.atDebug();
  }

  @Override
  public boolean isInfoEnabled() {
    return delegate.isInfoEnabled();
  }

  @Override
  public void info(String msg) {
    delegate.info(prefix(msg));
  }

  @Override
  public void info(String format, Object arg) {
    delegate.info(prefix(format), arg);
  }

  @Override
  public void info(String format, Object arg1, Object arg2) {
    delegate.info(prefix(format), arg1, arg2);
  }

  @Override
  public void info(String format, Object... arguments) {
    delegate.info(prefix(format), arguments);
  }

  @Override
  public void info(String msg, Throwable t) {
    delegate.info(prefix(msg), t);
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return delegate.isInfoEnabled(marker);
  }

  @Override
  public void info(Marker marker, String msg) {
    delegate.info(marker, prefix(msg));
  }

  @Override
  public void info(Marker marker, String format, Object arg) {
    delegate.info(marker, prefix(format), arg);
  }

  @Override
  public void info(Marker marker, String format, Object arg1, Object arg2) {
    delegate.info(marker, prefix(format), arg1, arg2);
  }

  @Override
  public void info(Marker marker, String format, Object... arguments) {
    delegate.info(marker, prefix(format), arguments);
  }

  @Override
  public void info(Marker marker, String msg, Throwable t) {
    delegate.info(marker, prefix(msg), t);
  }

  @Override
  @CheckReturnValue
  public LoggingEventBuilder atInfo() {
    return delegate.atInfo();
  }

  @Override
  public boolean isWarnEnabled() {
    return delegate.isWarnEnabled();
  }

  @Override
  public void warn(String msg) {
    delegate.warn(prefix(msg));
  }

  @Override
  public void warn(String format, Object arg) {
    delegate.warn(prefix(format), arg);
  }

  @Override
  public void warn(String format, Object... arguments) {
    delegate.warn(prefix(format), arguments);
  }

  @Override
  public void warn(String format, Object arg1, Object arg2) {
    delegate.warn(prefix(format), arg1, arg2);
  }

  @Override
  public void warn(String msg, Throwable t) {
    delegate.warn(prefix(msg), t);
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return delegate.isWarnEnabled(marker);
  }

  @Override
  public void warn(Marker marker, String msg) {
    delegate.warn(marker, prefix(msg));
  }

  @Override
  public void warn(Marker marker, String format, Object arg) {
    delegate.warn(marker, prefix(format), arg);
  }

  @Override
  public void warn(Marker marker, String format, Object arg1, Object arg2) {
    delegate.warn(marker, prefix(format), arg1, arg2);
  }

  @Override
  public void warn(Marker marker, String format, Object... arguments) {
    delegate.warn(marker, prefix(format), arguments);
  }

  @Override
  public void warn(Marker marker, String msg, Throwable t) {
    delegate.warn(marker, prefix(msg), t);
  }

  @Override
  @CheckReturnValue
  public LoggingEventBuilder atWarn() {
    return delegate.atWarn();
  }

  @Override
  public boolean isErrorEnabled() {
    return delegate.isErrorEnabled();
  }

  @Override
  public void error(String msg) {
    delegate.error(prefix(msg));
  }

  @Override
  public void error(String format, Object arg) {
    delegate.error(prefix(format), arg);
  }

  @Override
  public void error(String format, Object arg1, Object arg2) {
    delegate.error(prefix(format), arg1, arg2);
  }

  @Override
  public void error(String format, Object... arguments) {
    delegate.error(prefix(format), arguments);
  }

  @Override
  public void error(String msg, Throwable t) {
    delegate.error(prefix(msg), t);
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return delegate.isErrorEnabled(marker);
  }

  @Override
  public void error(Marker marker, String msg) {
    delegate.error(marker, prefix(msg));
  }

  @Override
  public void error(Marker marker, String format, Object arg) {
    delegate.error(marker, prefix(format), arg);
  }

  @Override
  public void error(Marker marker, String format, Object arg1, Object arg2) {
    delegate.error(marker, prefix(format), arg1, arg2);
  }

  @Override
  public void error(Marker marker, String format, Object... arguments) {
    delegate.error(marker, prefix(format), arguments);
  }

  @Override
  public void error(Marker marker, String msg, Throwable t) {
    delegate.error(marker, prefix(msg), t);
  }

  @Override
  @CheckReturnValue
  public LoggingEventBuilder atError() {
    return delegate.atError();
  }
}
