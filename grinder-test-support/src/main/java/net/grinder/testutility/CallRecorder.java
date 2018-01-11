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

package net.grinder.testutility;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.AssertionFailedError;

import org.junit.Assert;



/**
 *  Utility class used to record and assert method invocations.
 *
 * @author    Philip Aston
 */
public class CallRecorder extends Assert implements CallAssertions {

  private static ThreadRecording s_threadRecording = new ThreadRecording();

    private final LinkedList<CallData> m_callDataList =
    new LinkedList<CallData>();
  private List<MethodFilter> m_methodFilters = new ArrayList<MethodFilter>();
  private boolean m_ignoreCallOrder = false;

  /**
   *  Reset the call data.
   */
  public final void resetCallHistory() {
    synchronized (m_callDataList) {
      m_callDataList.clear();
      m_callDataList.notifyAll();
    }
  }

  public String getCallHistory() {
    final StringBuffer result = new StringBuffer();

    try {
      s_threadRecording.disable();

      synchronized (m_callDataList) {
        for (CallData callData : m_callDataList) {
          result.append(callData);
          result.append("\n");
        }
      }
    }
    finally {
      s_threadRecording.enable();
    }

    return result.toString();
  }

  /**
   *  Check that no methods have been called.
   */
  public final void assertNoMoreCalls() {
    synchronized (m_callDataList) {
      assertEquals("Call history:\n" + getCallHistory(),
                   0, m_callDataList.size());
    }
  }

  public void setIgnoreCallOrder(boolean b) {
    m_ignoreCallOrder = b;
  }

  public void setIgnoreObjectMethods() {
    m_methodFilters.add(
      new MethodFilter() {
        public boolean matches(Method m) {
          return m.getDeclaringClass() == Object.class;
        }
      });
  }

  public void setIgnoreMethod(final String methodName) {
    m_methodFilters.add(
      new MethodFilter() {
        public boolean matches(Method m) {
          return m.getName().equals(methodName);
        }
      });
  }

  public final void record(CallData callData) {
    if (s_threadRecording.isEnabled()) {
      for (MethodFilter filter : m_methodFilters) {
        if (filter.matches(callData.getMethod())) {
          return;
        }
      }

      synchronized (m_callDataList) {
        m_callDataList.add(callData);
        m_callDataList.notifyAll();
      }
    }
  }

  public final CallData assertSuccess(final String methodName,
                                      final Object... parameters) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertSuccess(methodName, parameters);
      }
    }
    .run();
  }

  public final CallData assertSuccess(final String methodName,
                                      final Class<?>... parameterTypes) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertSuccess(methodName, parameterTypes);
      }
    }
    .run();
  }

  public final CallData assertException(final String methodName,
                                        final Throwable throwable,
                                        final Object... parameters) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertException(methodName, throwable, parameters);
      }
    }
    .run();
  }

  public final CallData assertException(final String methodName,
                                        final Throwable throwable,
                                        final Class<?>... parameterTypes) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertException(methodName, throwable, parameterTypes);
      }
    }
    .run();
  }

  public final CallData assertException(final String methodName,
                                        final Class<?> throwableType,
                                        final Object... parameters) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertException(methodName, throwableType, parameters);
      }
    }
    .run();
  }

  public final CallData assertException(final String methodName,
                                        final Class<?> throwableType,
                                        final Class<?>... parameterTypes) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertException(methodName, throwableType, parameterTypes);
      }
    }
    .run();
  }

  private abstract class AssertMatchingCallTemplate {
    public final CallData run() {
      try {
        s_threadRecording.disable();

        if (m_ignoreCallOrder) {
          synchronized (m_callDataList) {
            // Check the earliest call first.
            final Iterator<CallData> iterator = m_callDataList.iterator();

            while (iterator.hasNext()) {
              try {
                final CallData callData = iterator.next();

                test(callData);
                iterator.remove();
                m_callDataList.notifyAll();

                return callData;
              }
              catch (AssertionFailedError e) {
              }
            }
          }

          fail("No matching call");
          return null; // Not reached.
        }
        else {
          // Check the earliest call.
          synchronized (m_callDataList) {
            try {
              final CallData callData = m_callDataList.removeFirst();
              m_callDataList.notifyAll();
              test(callData);
              return callData;
            }
            catch (NoSuchElementException e) {
              fail("No more calls");
              return null; // Not reached.
            }
          }
        }
      }
      finally {
        s_threadRecording.enable();
      }
    }

    public abstract void test(CallData callData);
  }

  /**
   * Used to disable all CallRecorder recording for the calling thread so we
   * don't record side effects of CallRecorder processing. This also prevents
   * some ConcurrentModificationExceptions.
   */
  private static final class ThreadRecording {

    private final ThreadLocal<ThreadRecording> m_threadLocal =
      new ThreadLocal<ThreadRecording>();

    public void disable() {
      m_threadLocal.set(this);
    }

    public void enable() {
      m_threadLocal.set(null);
    }

    public boolean isEnabled() {
      return m_threadLocal.get() == null;
    }
  }

  public interface MethodFilter {
    boolean matches(Method m);
  }
}
