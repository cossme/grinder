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
 *  {@link AbstractStubFactory} that takes a
 *  <code>Class</code> and generates stubs that support all the
 *  interface of the given class and have methods that return random
 *  primitive results and null objects for complex results.
 *
 * @author    Philip Aston
 */
public class RandomStubFactory<T> extends AbstractStubFactory<T> {

  public static <T> RandomStubFactory<T> create(Class<T> stubbedInterface) {
    return new RandomStubFactory<T>(stubbedInterface);
  }

  protected RandomStubFactory(Class<T> stubbedInterface) {
    super(stubbedInterface,
          new OverrideInvocationHandlerDecorator(
            new RandomResultInvocationHandler(),
            new SimpleEqualityDecoration(
              "a stub " + stubbedInterface.getName())));
  }

  private static final class RandomResultInvocationHandler
    implements InvocationHandler {

    private final RandomObjectFactory m_randomObjectFactory =
      new RandomObjectFactory();

    public Object invoke(Object proxy, Method method, Object[] parameters)
      throws Throwable {

      return m_randomObjectFactory.generateParameter(method.getReturnType());
    }
  }
}
