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
import java.util.Arrays;

import org.junit.Assert;


/**
 *  Method call data.
 *
 * @author    Philip Aston
 */
public final class CallData extends Assert implements CallAssertions {
  private final Method m_method;
  private final Object[] m_parameters;
  private final Object m_result;
  private final Throwable m_throwable;

  public CallData(Method method, Object result, Object... parameters) {
    m_method = method;
    m_parameters = parameters;
    m_result = result;
    m_throwable = null;
  }

  public CallData(Method method, Throwable throwable, Object... parameters) {
    m_method = method;
    m_parameters = parameters;
    m_result = null;
    m_throwable = throwable;
  }

  public Method getMethod() {
    return m_method;
  }

  public String getMethodName() {
    return getMethod().getName();
  }

  public Object[] getParameters() {
    return m_parameters;
  }

  public Class<?>[] getParameterTypes() {
    if (m_parameters == null) {
      return new Class[0];
    }

    final Class<?>[] types = new Class<?>[m_parameters.length];

    for (int i=0; i<types.length; ++i) {
      if (m_parameters[i] == null) {
        types[i] = ANY_TYPE.class;
      }
      else {
        types[i] = m_parameters[i].getClass();
      }
    }

    return types;
  }

  /**
   * Class that represents a parameter that is compatible with any type.
   */
  public static class ANY_TYPE { }

  public Object getResult() {
    assertNull(m_throwable);
    return m_result;
  }

  public Throwable getThrowable() {
    return m_throwable;
  }

  /**
   *  Check the given method was called.
   */
  public final CallData assertSuccess(String methodName, Object... parameters) {
    assertCalled(methodName, parameters);
    assertNull(getThrowable());
    return this;
  }

  public final CallData assertSuccess(String methodName,
                                      Class<?>... parameterTypes) {
    assertCalled(methodName, parameterTypes);
    assertNull(getThrowable());
    return this;
  }

  public final CallData assertSuccess(String methodName) {
    return assertSuccess(methodName, new Class[0]);
  }

  public final CallData assertException(String methodName,
                                        Throwable throwable,
                                        Object... parameters) {
    assertCalled(methodName, parameters);
    assertEquals(throwable, getThrowable());
    return this;
  }

  public final CallData assertException(String methodName,
                                        Throwable throwable,
                                        Class<?>... parameterTypes) {
    assertCalled(methodName, parameterTypes);
    assertEquals(throwable, getThrowable());
    return this;
  }

  public final CallData assertException(String methodName,
                                        Class<?> throwableType,
                                        Object... parameters) {
    assertCalled(methodName, parameters);
    assertTrue(throwableType.isAssignableFrom(getThrowable().getClass()));
    return this;
  }

  public final CallData assertException(String methodName,
                                        Class<?> throwableType,
                                        Class<?>... parameterTypes) {
    assertCalled(methodName, parameterTypes);
    assertNotNull(getThrowable());
    assertTrue(throwableType.isAssignableFrom(getThrowable().getClass()));
    return this;
  }

  private void assertCalled(String methodName, Object... parameters) {
    if (parameters.length == 0) {
      parameters = null;
    }

    // Just check method names match. Don't worry about modifiers
    // etc., or even which class the method belongs to.
    assertEquals(methodName, getMethodName());

    assertArrayEquals(
      "Expected " + parametersToString(parameters) +
      " but was " + parametersToString(getParameters()),
      parameters, getParameters());
  }

  private void assertCalled(String methodName, Class<?>... parameterTypes) {

    // Just check method names match. Don't worry about modifiers
    // etc., or even which class the method belongs to.
    assertEquals(methodName, getMethodName());

    final Class<?>[] actualParameterTypes = getParameterTypes();

    if (parameterTypes != null || actualParameterTypes != null) {
      assertNotNull(parameterTypes);
      assertNotNull(actualParameterTypes);

      // If statement is to shut up eclipse null warning.
      if (parameterTypes != null && actualParameterTypes != null) {
        assertEquals("Called with the correct number of parameters",
                     parameterTypes.length,
                     actualParameterTypes.length);

        for (int i = 0; i < parameterTypes.length; ++i) {
          if (!(actualParameterTypes[i].equals(ANY_TYPE.class))) {
            assertTrue("Parameter  " + i + " is instance of  " +
                       actualParameterTypes[i].getName() +
                       " which supports the interfaces " +
                       Arrays.asList(actualParameterTypes[i].getInterfaces()) +
                       " and is not assignable from " +
                       parameterTypes[i].getName(),
                       parameterTypes[i].isAssignableFrom(
                         actualParameterTypes[i]));
          }
        }
      }
    }
  }

  public String toString() {
    final StringBuffer result = new StringBuffer();

    result.append(getMethodName());
    result.append(parametersToString(getParameters()));

    final Throwable throwable = getThrowable();

    if (throwable != null) {
      result.append(" threw " + throwable);
    }
    else {
      result.append(" returned " + getResult());
    }

    return result.toString();
  }

  private static final String parametersToString(Object[] parameters) {

    final StringBuffer result = new StringBuffer();

    result.append('(');

    if (parameters != null) {
      for (int i = 0; i < parameters.length; ++i) {
        if (i != 0) {
          result.append(", ");
        }

        if (parameters[i] != null &&
            !parameters[i].getClass().isPrimitive() &&
            !parameters[i].getClass().isArray()) {
          result.append("\"");
          result.append(parameters[i]);
          result.append("\"");
        }
        else {
          result.append(parameters[i]);
        }
      }
    }

    result.append(')');

    return result.toString();
  }
}
