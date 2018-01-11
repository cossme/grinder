// Copyright (C) 2009 - 2011 Philip Aston
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
import net.grinder.util.weave.WeavingException;


/**
 * Convenient base class for DCR instrumenters.
 *
 * @author Philip Aston
 */
public abstract class AbstractDCRInstrumenter implements Instrumenter {

  private final DCRContext m_context;

  /**
   * Constructor for AbstractDCRInstrumenter.
   *
   * @param context The DCR context.
   */
  protected AbstractDCRInstrumenter(DCRContext context) {
    m_context = context;
  }

  /**
   * Provide subclasses convenient access to the DCR context.
   *
   * @return The DCR context.
   */
  protected final DCRContext getContext() {
    return m_context;
  }

  /**
   * {@inheritDoc}
   */
  @Override public final Object createInstrumentedProxy(Test test,
                                                        Recorder recorder,
                                                        Object target)
    throws NotWrappableTypeException {

    try {
      return instrument(test, recorder, target)? target : null;
    }
    catch (NonInstrumentableTypeException e) {
      throw new NotWrappableTypeException(e.getMessage(), e);
    }
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
  @Override public final boolean instrument(Test test,
                                            Recorder recorder,
                                            Object target,
                                            InstrumentationFilter filter)
    throws NonInstrumentableTypeException {

    final boolean changed = instrument(target, recorder, filter);

    if (changed) {
      try {
        m_context.applyChanges();
      }
      catch (WeavingException e) {
        throw new NonInstrumentableTypeException(e.getMessage());
      }
    }

    return changed;
  }

  /**
   * Hook for sub-class to implement instrumentation.
   *
   * @param target
   *          Target object.
   * @param recorder
   *          Recorder.
   * @param filter
   *          Selects the parts of {@code target} to instrument.
   * @return {@code true} If this instrumenter successfully processed {@code
   *         target}, otherwise {@code false}.
   * @throws NonInstrumentableTypeException
   *           If the target object is not of an instrumentable type.
   */
  protected abstract boolean instrument(Object target,
                                        Recorder recorder,
                                        InstrumentationFilter filter)
    throws NonInstrumentableTypeException;
}
