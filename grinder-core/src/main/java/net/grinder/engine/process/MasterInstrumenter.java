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

package net.grinder.engine.process;

import java.util.List;

import net.grinder.common.Test;
import net.grinder.script.NonInstrumentableTypeException;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.script.Test.InstrumentationFilter;
import net.grinder.scriptengine.CompositeInstrumenter;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.Recorder;


/**
 * Composite instrumenter that knows all other instrumenters.
 *
 * @author Philip Aston
 */
final class MasterInstrumenter extends CompositeInstrumenter {

  /**
   * Constructor for MasterInstrumenter.
   *
   * @param instrumenters Ordered list of instrumenters.
   */
  public MasterInstrumenter(List<Instrumenter> instrumenters) {
    super(instrumenters);
  }

  /**
   * {@inheritDoc}
   */
  @Override public Object createInstrumentedProxy(Test test,
                                                  Recorder recorder,
                                                  Object target)
    throws NotWrappableTypeException {

    if (target == null) {
      throw new NotWrappableTypeException("Can't wrap null/None");
    }

    final Object result = super.createInstrumentedProxy(test, recorder, target);

    if (result != null) {
      return result;
    }

    throw new NotWrappableTypeException("Failed to wrap " + target);
  }

  /**
   * {@inheritDoc}
   */
  @Override public boolean instrument(Test test,
                                      Recorder recorder,
                                      Object target,
                                      InstrumentationFilter filter)
    throws NonInstrumentableTypeException {

    if (target == null) {
      throw new NonInstrumentableTypeException("Can't instrument null/None");
    }

    final boolean result = super.instrument(test, recorder, target, filter);

    if (result) {
      return true;
    }

    throw new NonInstrumentableTypeException("Failed to wrap " + target);
  }

  /**
   * {@inheritDoc}
   */
  @Override public String getDescription() {
    final String result = super.getDescription();

    if (result.length() == 0) {
      return "NO INSTRUMENTER COULD BE LOADED";
    }

    return result.toString();
  }
}
