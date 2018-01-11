// Copyright (C) 2011 Philip Aston
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

import java.util.Arrays;
import java.util.List;

import net.grinder.common.Test;
import net.grinder.script.NonInstrumentableTypeException;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.script.Test.InstrumentationFilter;


/**
 * Composite instrumenter.
 *
 * @author Philip Aston
 */
public class CompositeInstrumenter implements Instrumenter {

  private final List<Instrumenter> m_instrumenters;

  /**
   * Constructor.
   *
   * @param instrumenters Ordered list of instrumenters.
   */
  public CompositeInstrumenter(Instrumenter... instrumenters) {
    this(Arrays.asList(instrumenters));
  }

  /**
   * Constructor.
   *
   * @param instrumenters Ordered list of instrumenters.
   */
  public CompositeInstrumenter(List<Instrumenter> instrumenters) {
    m_instrumenters = instrumenters;
  }

  /**
   * {@inheritDoc}
   */
  @Override public Object createInstrumentedProxy(Test test,
                                                  Recorder recorder,
                                                  Object target)
    throws NotWrappableTypeException {

    for (Instrumenter instrumenter : m_instrumenters) {
      final Object result = instrumenter.createInstrumentedProxy(test,
                                                                 recorder,
                                                                 target);

      if (result != null) {
        return result;
      }
    }

    // Don't throw, unlike MasterInstrumenter, we don't claim to know the
    // entire chain.
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override public boolean instrument(Test test,
                                      Recorder recorder,
                                      Object target)
    throws NonInstrumentableTypeException {
    return instrument(test, recorder, target, ALL_INSTRUMENTATION);
  }

  /**
   * {@inheritDoc}
   */
  @Override public boolean instrument(Test test,
                                      Recorder recorder,
                                      Object target,
                                      InstrumentationFilter filter)
    throws NonInstrumentableTypeException {

    for (Instrumenter instrumenter : m_instrumenters) {
      if (instrumenter.instrument(test, recorder, target, filter)) {
        return true;
      }
    }

    // Don't throw, unlike MasterInstrumenter, we don't claim to know the
    // entire chain.
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override public String getDescription() {
    final StringBuilder result = new StringBuilder();

    for (Instrumenter instrumenter : m_instrumenters) {
      final String description = instrumenter.getDescription();

      if (description != null) {
        if (result.length() > 0) {
          result.append("; ");
        }

        result.append(description);
      }
    }

    return result.toString();
  }
}
