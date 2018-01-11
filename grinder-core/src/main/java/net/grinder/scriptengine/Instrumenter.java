// Copyright (C) 2007 - 2011 Philip Aston
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

package net.grinder.scriptengine;

import net.grinder.common.Test;
import net.grinder.script.NonInstrumentableTypeException;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.script.Test.InstrumentationFilter;


/**
 * A factory for instrumented proxies.
 *
 * @author Philip Aston
 */
public interface Instrumenter {

  /**
   * Create a proxy object that wraps an target object for a test.
   *
   * @param test
   *          The test.
   * @param recorder
   *          The proxy should use this to instrument the work.
   * @param target
   *          Object to wrap.
   * @return The instrumented proxy.
   * @throws NotWrappableTypeException
   *           If the target object cannot be wrapped.
   */
  Object createInstrumentedProxy(Test test, Recorder recorder, Object target)
    throws NotWrappableTypeException;

  /**
   * Instrument a target object with a test.
   *
   * @param test
   *          The test.
   * @param recorder
   *          Wire the instrumentation to this {@link Recorder}.
   * @param target
   *          The object to instrument.
   * @return {@code true} if instrumentation was added.
   * @throws NonInstrumentableTypeException
   *           If the target object cannot be instrumented.
   */
  boolean instrument(Test test,
                     Recorder recorder,
                     Object target)
    throws NonInstrumentableTypeException;


  /**
   * Selectively instrument a target object with a test.
   *
   * @param test
   *          The test.
   * @param recorder
   *          Wire the instrumentation to this {@link Recorder}.
   * @param target
   *          The object to instrument.
   * @param filter
   *          Selects the parts of {@code target} to instrument.
   * @return {@code true} if instrumentation was added.
   * @throws NonInstrumentableTypeException
   *           If the target object cannot be instrumented.
   */
  boolean instrument(Test test,
                     Recorder recorder,
                     Object target,
                     InstrumentationFilter filter)
    throws NonInstrumentableTypeException;

  /**
   * Public description of the {@code Instrumenter}.
   *
   * @return The description; {@code null} for internal {@code Instrumenters}.
   */
  String getDescription();

  /**
   * Instrumentation filter that matches everything.
   */
  InstrumentationFilter ALL_INSTRUMENTATION = new InstrumentationFilter() {
      public boolean matches(Object item) {
        return true;
      }
    };
}
