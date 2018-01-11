// Copyright (C) 2004 - 2013 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.engine.process;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MarkerIgnoringBase;


/**
 * {@link Logger} implementation for external consumption. Delegates to the
 * another {@code Logger}, providing the appropriate context specific markers.
 *
 * @author Philip Aston
 */
final class ExternalLogger extends MarkerIgnoringBase {

  private final Logger m_delegate;
  private final ThreadContextLocator m_threadContextLocator;

  public ExternalLogger(final Logger processLogger,
                        final ThreadContextLocator threadContextLocator) {

    m_delegate = processLogger;
    m_threadContextLocator = threadContextLocator;
  }

  private Marker getMarker() {
    final ThreadContext threadContext = m_threadContextLocator.get();

    if (threadContext != null) {
      return threadContext.getLogMarker();
    }

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override public String getName() {
    return m_delegate.getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override public boolean isTraceEnabled() {
    return m_delegate.isTraceEnabled(getMarker());
  }

  /**
   * {@inheritDoc}
   */
  @Override public void trace(final String msg) {
    m_delegate.trace(getMarker(), msg);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void trace(final String format, final Object arg) {
    m_delegate.trace(getMarker(), format, arg);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void trace(final String format,
                              final Object arg1,
                              final Object arg2) {
    m_delegate.trace(getMarker(), format, arg1, arg2);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void trace(final String format, final Object... argArray) {
    m_delegate.trace(getMarker(), format, argArray);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void trace(final String msg, final Throwable t) {
    m_delegate.trace(getMarker(), msg, t);
  }

  /**
   * {@inheritDoc}
   */
  @Override public boolean isDebugEnabled() {
    return m_delegate.isDebugEnabled(getMarker());
  }

  /**
   * {@inheritDoc}
   */
  @Override public void debug(final String msg) {
    m_delegate.debug(getMarker(), msg);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void debug(final String format, final Object arg) {
    m_delegate.debug(getMarker(), format, arg);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void debug(final String format,
                              final Object arg1,
                              final Object arg2) {
    m_delegate.debug(getMarker(), format, arg1, arg2);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void debug(final String format, final Object... argArray) {
    m_delegate.debug(getMarker(), format, argArray);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void debug(final String msg, final Throwable t) {
    m_delegate.debug(getMarker(), msg, t);
  }

  /**
   * {@inheritDoc}
   */
  @Override public boolean isInfoEnabled() {
    return m_delegate.isInfoEnabled(getMarker());
  }

  /**
   * {@inheritDoc}
   */
  @Override public void info(final String msg) {
    m_delegate.info(getMarker(), msg);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void info(final String format, final Object arg) {
    m_delegate.info(getMarker(), format, arg);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void info(final String format,
                             final Object arg1,
                             final Object arg2) {
    m_delegate.info(getMarker(), format, arg1, arg2);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void info(final String format, final Object... argArray) {
    m_delegate.info(getMarker(), format, argArray);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void info(final String msg, final Throwable t) {
    m_delegate.info(getMarker(), msg, t);
  }

  /**
   * {@inheritDoc}
   */
  @Override public boolean isWarnEnabled() {
    return m_delegate.isWarnEnabled(getMarker());
  }

  /**
   * {@inheritDoc}
   */
  @Override public void warn(final String msg) {
    m_delegate.warn(getMarker(), msg);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void warn(final String format, final Object arg) {
    m_delegate.warn(getMarker(), format, arg);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void warn(final String format,
                             final Object arg1,
                             final Object arg2) {
    m_delegate.warn(getMarker(), format, arg1, arg2);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void warn(final String format, final Object... argArray) {
    m_delegate.warn(getMarker(), format, argArray);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void warn(final String msg, final Throwable t) {
    m_delegate.warn(getMarker(), msg, t);
  }

  /**
   * {@inheritDoc}
   */
  @Override public boolean isErrorEnabled() {
    return m_delegate.isErrorEnabled(getMarker());
  }

  /**
   * {@inheritDoc}
   */
  @Override public void error(final String msg) {
    m_delegate.error(getMarker(), msg);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void error(final String format, final Object arg) {
    m_delegate.error(getMarker(), format, arg);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void error(final String format,
                              final Object arg1,
                              final Object arg2) {
    m_delegate.error(getMarker(), format, arg1, arg2);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void error(final String format, final Object... argArray) {
    m_delegate.error(getMarker(), format, argArray);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void error(final String msg, final Throwable t) {
    m_delegate.error(getMarker(), msg, t);
  }
}
