// Copyright (C) 2011 - 2013 Philip Aston
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

import static java.lang.System.arraycopy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.grinder.testutility.RandomObjectFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.listeners.InvocationListener;
import org.mockito.listeners.MethodInvocationReport;
import org.slf4j.Logger;
import org.slf4j.Marker;


/**
 * Unit tests for {@code ExternalLogger}.
 *
 * @author Philip Aston
 */
public class TestExternalLogger {

  @Mock private Logger m_delegate;
  @Mock private ThreadContextLocator m_threadContextLocator;
  @Mock private ThreadContext m_threadContext;
  @Mock private Marker m_marker;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test public void testGetName() {
    final Logger logger =
      new ExternalLogger(m_delegate, m_threadContextLocator);

    when(m_delegate.getName()).thenReturn("foo");

    assertEquals("foo", logger.getName());
  }

  @Test public void testDelegateMethodsTakingNoMarker() throws Exception {

    when(m_threadContextLocator.get()).thenReturn(m_threadContext);
    when(m_threadContext.getLogMarker()).thenReturn(m_marker);

    final List<InvocationOnMock> invocations =
      new ArrayList<InvocationOnMock>();

    final InvocationListener listeners = new InvocationListener() {
      @Override
      public void reportInvocation(final MethodInvocationReport report) {
        invocations.add((InvocationOnMock) report.getInvocation());
      }
    };

    final Logger delegate =
      mock(Logger.class, withSettings().invocationListeners(listeners));

    final ExternalLogger logger =
      new ExternalLogger(delegate, m_threadContextLocator);

    final Method[] allMethods = Logger.class.getDeclaredMethods();

    for (final Method m : allMethods) {
      final Class<?>[] parameterTypes = m.getParameterTypes();

      if (parameterTypes.length > 0 && parameterTypes[0].equals(Marker.class)) {
        continue;
      }

      final Class<?>[] delegateTypes = new Class<?>[parameterTypes.length + 1];

      delegateTypes[0] = Marker.class;
      arraycopy(parameterTypes, 0, delegateTypes, 1, parameterTypes.length);

      final Method delegateMethod;
      try {
        delegateMethod = Logger.class.getMethod(m.getName(), delegateTypes);
      }
      catch (final NoSuchMethodException e) {
        continue;
      }

      final RandomObjectFactory randomObjectFactory = new RandomObjectFactory();

      final List<Object> parameters = new ArrayList<Object>();

      for (final Class<?> type : parameterTypes) {
        parameters.add(randomObjectFactory.generateParameter(type));
      }

      m.invoke(logger, parameters.toArray());

      final InvocationOnMock invocation = invocations.remove(0);

      assertEquals(delegateMethod, invocation.getMethod());

      int i = 0;
      final Object[] invokedArguments = invocation.getArguments();

      assertSame(m_marker, invokedArguments[0]);

      if (delegateMethod.isVarArgs()) {
        // Mockito expands varargs.
        final Object[] vs = (Object[]) parameters.remove(parameters.size() - 1);
        for (final Object v : vs) {
          parameters.add(v);
        }
      }

      for (final Object p : parameters) {
        assertSame(p, invokedArguments[++i]);
      }
    }

    assertEquals(0, invocations.size());
  }

  @Test public void testGetMarkerNullContext() {
    final Logger logger =
      new ExternalLogger(m_delegate, m_threadContextLocator);

    logger.isTraceEnabled();

    verify(m_delegate).isTraceEnabled(null);
  }
}
