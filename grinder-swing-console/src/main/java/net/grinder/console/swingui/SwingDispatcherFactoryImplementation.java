// Copyright (C) 2005 - 2011 Philip Aston
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

package net.grinder.console.swingui;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.swing.SwingUtilities;

import net.grinder.console.common.ErrorHandler;


/**
 * Factory for dynamic proxies that dispatch work in the Swing thread.
 *
 * @author Philip Aston
 */
final class SwingDispatcherFactoryImplementation
  implements SwingDispatcherFactory {

  private final ErrorHandler m_errorHandler;

  public SwingDispatcherFactoryImplementation(ErrorHandler errorHandler) {
    m_errorHandler = errorHandler;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> T create(Class<T> clazz, final T delegate) {

    final InvocationHandler invocationHandler =
      new InvocationHandler() {
        public Object invoke(Object proxy,
                             final Method method,
                             final Object[] args) {

          SwingUtilities.invokeLater(
            new Runnable() {
              public void run() {
                try {
                  method.invoke(delegate, args);
                }
                catch (InvocationTargetException e) {
                  m_errorHandler.handleException(e.getCause());
                }
                catch (IllegalArgumentException e) {
                  throw new AssertionError(e);
                }
                catch (IllegalAccessException e) {
                  throw new AssertionError(e);
                }
              }
            });

          return null;
        }
      };

    final Class<?> delegateClass = delegate.getClass();

    return (T)Proxy.newProxyInstance(delegateClass.getClassLoader(),
                                     new Class<?>[] { clazz },
                                     invocationHandler);
  }
}
