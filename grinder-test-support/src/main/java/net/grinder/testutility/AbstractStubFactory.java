// Copyright (C) 2004 - 2012 Philip Aston
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 *  Dynamic proxy based test utility class that records details of
 *  invocations and allows a controlling unit test to later enquire
 *  about which invocations occurred and in what order. Because it is
 *  based on java.lang.reflect.Proxy it can only intercept invocations
 *  which are defined by interfaces.
 *
 *  @param <T> Stub type.
 *
 * @author    Philip Aston
 */
public abstract class AbstractStubFactory<T> extends CallRecorder {

  private final T m_stub;
  private final Map<String, Object> m_resultMap = new HashMap<String, Object>();
  private final Map<String, Throwable> m_throwsMap =
    new HashMap<String, Throwable>();

  @SuppressWarnings("unchecked")
  public AbstractStubFactory(Class<T> stubbedInterface,
                             InvocationHandler invocationHandler) {

    final InvocationHandler decoratedInvocationHandler =
      new RecordingInvocationHandler(
        new StubResultInvocationHandler(
          new OverrideInvocationHandlerDecorator(invocationHandler, this)));

    m_stub = (T)Proxy.newProxyInstance(stubbedInterface.getClassLoader(),
                                       getAllInterfaces(stubbedInterface),
                                       decoratedInvocationHandler);
  }

  private final class RecordingInvocationHandler implements InvocationHandler {

    private final InvocationHandler m_delegate;

    public RecordingInvocationHandler(InvocationHandler delegate) {
      m_delegate = delegate;
    }

    public Object invoke(Object proxy, Method method, Object[] parameters)
      throws Throwable {

      try {
        final Object result = m_delegate.invoke(proxy, method, parameters);
        record(new CallData(method, result, parameters));
        return result;
      }
      catch (InvocationTargetException t) {
        final Throwable targetException = t.getTargetException();
        record(new CallData(method, targetException, parameters));
        throw targetException;
      }
      catch (Throwable t) {
        record(new CallData(method, t, parameters));
        throw t;
      }
    }
  }

  private final class StubResultInvocationHandler
    implements InvocationHandler {

    private final InvocationHandler m_delegate;

    public StubResultInvocationHandler(InvocationHandler delegate) {
      m_delegate = delegate;
    }

    public Object invoke(Object proxy, Method method, Object[] parameters)
      throws Throwable {

      final String methodName = method.getName();

      if (m_throwsMap.containsKey(methodName)) {
        final Throwable t = m_throwsMap.get(methodName);
        t.fillInStackTrace();
        throw t;
      }

      if (m_resultMap.containsKey(methodName)) {
        return m_resultMap.get(methodName);
      }

      return m_delegate.invoke(proxy, method, parameters);
    }
  }

  public final T getStub() {
    return m_stub;
  }

  public final void setResult(String methodName, Object result) {
    m_resultMap.put(methodName, result);
  }

  public final void setThrows(String methodName, Throwable result) {
    m_throwsMap.put(methodName, result);
  }

  public static Class<?>[] getAllInterfaces(Class<?> c) {
    final Set<Class<?>> interfaces = new HashSet<Class<?>>();

    if (c.isInterface()) {
      interfaces.add(c);
    }

    do {
      final Class<?>[] moreInterfaces = c.getInterfaces();

      if (moreInterfaces != null) {
        interfaces.addAll(Arrays.asList(moreInterfaces));
      }

      c = c.getSuperclass();
    }
    while (c != null);

    return interfaces.toArray(new Class<?>[0]);
  }

  /**
   * Localise need for unchecked cast. I thought the compiler was meant
   * to regarding {@link Object#getClass()} - seems the Eclipse compiler
   * (at least) is not.
   *
   * @param <K> Parameter type.
   * @param o Parameter.
   * @return Parameter's class.
   */
  @SuppressWarnings("unchecked")
  protected static <K> Class<K> getClass(K o) {
    return (Class<K>)o.getClass();
  }
}
