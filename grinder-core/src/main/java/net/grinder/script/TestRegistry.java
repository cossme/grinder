// Copyright (C) 2008 - 2011 Philip Aston
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

package net.grinder.script;

import net.grinder.common.Test;
import net.grinder.script.Test.InstrumentationFilter;


/**
 * Registry of Tests.
 *
 * @author Philip Aston
 */
public interface TestRegistry {

  /**
   * Register a new test.
   *
   * @param test
   *          The test.
   * @return A ProxyFactory that can be used to create proxies instrumented for
   *         the test.
   */
  RegisteredTest register(Test test);

  /**
   * Interface for test handles.
   */
  public interface RegisteredTest {
    /**
     * Create a proxy object that wraps an target object for this test.
     *
     * @param o Object to wrap.
     * @return The proxy.
     * @throws NotWrappableTypeException If the target could not be wrapped.
     */
    Object createProxy(Object o) throws NotWrappableTypeException;

    /**
     * Instrument the given object.
     *
     * @param target
     *          The object to instrument.
     * @throws NonInstrumentableTypeException
     *           If the target could not be instrumented.
     */
    void instrument(Object target) throws NonInstrumentableTypeException;

    /**
     * Selectively instrument the given object.
     *
     * @param target
     *          The object to instrument.
     * @param filter
     *          Selects the parts of {@code target} to instrument.
     * @throws NonInstrumentableTypeException
     *           If the target could not be instrumented.
     */
    void instrument(Object target, InstrumentationFilter filter)
      throws NonInstrumentableTypeException;
  }
}
