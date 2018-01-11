// Copyright (C) 2009 - 2012 Philip Aston
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

package net.grinder.scriptengine.jython.instrumentation.dcr;

import java.util.ArrayList;
import java.util.List;

import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.dcr.DCRContextImplementation;
import net.grinder.scriptengine.CompositeInstrumenter;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.java.JavaScriptEngineService;
import net.grinder.testutility.Jython22Runner;
import net.grinder.util.weave.WeavingException;

import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Unit tests for {@link Jython22Instrumenter}.
 *
 * @author Philip Aston
 */
@RunWith(Jython22Runner.class)
public class TestJython22Instrumenter
  extends AbstractJythonDCRInstrumenterTestCase {

  private static Instrumenter createInstrumenter()
    throws EngineException, WeavingException {

    final List<Instrumenter> instrumenters = new ArrayList<Instrumenter>();

    instrumenters.add(
      new Jython22Instrumenter(DCRContextImplementation.create(null)));

    instrumenters.addAll(
      new JavaScriptEngineService(DCRContextImplementation.create(null))
      .createInstrumenters());

    return new CompositeInstrumenter(instrumenters);
  }

  public TestJython22Instrumenter() throws Exception {
    super(createInstrumenter());
  }

  @Test public void testVersion() throws Exception {
    assertVersion("(2.1|2.2)");
  }
}
