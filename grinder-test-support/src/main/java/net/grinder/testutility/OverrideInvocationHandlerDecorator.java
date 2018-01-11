// Copyright (C) 2004 - 2009 Philip Aston
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;


/**
 * <code>InvocationHandler</code> decorator that allows a supplied
 * object to handle some of the methods.
 *
 * @author    Philip Aston
 */
public final class OverrideInvocationHandlerDecorator
  implements InvocationHandler {

  private final InvocationHandler m_delegate;
  private final Object m_overrider;
  private final String m_overridePrefix;

  public OverrideInvocationHandlerDecorator(InvocationHandler delegate,
                                            Object overrider) {
    this(delegate, overrider, "override_");
  }

  public OverrideInvocationHandlerDecorator(InvocationHandler delegate,
                                            Object overrider,
                                            String overridePrefix) {
    m_delegate = delegate;
    m_overrider = overrider;
    m_overridePrefix = overridePrefix;
  }

  public Object invoke(Object proxy, Method method, Object[] parameters)
    throws Throwable {

    final Method overriddenMethod = getOverriddenMethod(method);

    if (overriddenMethod != null) {
      final Object[] proxyAndParameters;

      if (parameters != null) {
        proxyAndParameters = new Object[parameters.length + 1];
        proxyAndParameters[0] = proxy;
        System.arraycopy(parameters, 0, proxyAndParameters, 1,
                         parameters.length);
      }
      else {
        proxyAndParameters = new Object[] { proxy };
      }

      return overriddenMethod.invoke(m_overrider, proxyAndParameters);
    }
    else {
      return m_delegate.invoke(proxy, method, parameters);
    }
  }

  private Method getOverriddenMethod(Method method) {
    try {
      final Class<?>[] methodParameterTypes = method.getParameterTypes();

      final Class<?>[] parameterTypes =
        new Class<?>[methodParameterTypes.length + 1];
      parameterTypes[0] = Object.class;
      System.arraycopy(methodParameterTypes, 0, parameterTypes, 1,
                       methodParameterTypes.length);

      final Method candidate =
        m_overrider.getClass().getMethod(m_overridePrefix + method.getName(),
                                         parameterTypes);

      if (candidate.getReturnType().equals(method.getReturnType())) {
        return candidate;
      }
      else {
        return null;
      }
    }
    catch (NoSuchMethodException e) {
      return null;
    }
  }
}
