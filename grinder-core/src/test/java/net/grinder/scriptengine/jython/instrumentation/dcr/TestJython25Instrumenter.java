// Copyright (C) 2005 - 2013 Philip Aston
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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import net.grinder.engine.process.dcr.DCRContextImplementation;
import net.grinder.scriptengine.CompositeInstrumenter;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.java.JavaScriptEngineService;
import net.grinder.testutility.Jython25_27Runner;
import net.grinder.testutility.RandomStubFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.python.core.PyObject;
import org.python.core.PyProxy;


/**
 * Unit tests for {@link Jython25Instrumenter}.
 *
 * @author Philip Aston
 */
@RunWith(Jython25_27Runner.class)
public class TestJython25Instrumenter
  extends AbstractJythonDCRInstrumenterTestCase {

  private static Instrumenter createInstrumenter() throws Exception {
    final List<Instrumenter> instrumenters = new ArrayList<Instrumenter>();

    instrumenters.add(
      new Jython25Instrumenter(DCRContextImplementation.create(null)));

    instrumenters.addAll(
      new JavaScriptEngineService(DCRContextImplementation.create(null))
      .createInstrumenters());

    return new CompositeInstrumenter(instrumenters);
  }

  public TestJython25Instrumenter() throws Exception {
    super(createInstrumenter());
  }

  @Test public void testVersion() throws Exception {
    assertVersion("2.5");
  }

  @Test public void testBrokenPyProxy() throws Exception {
    final RandomStubFactory<PyProxy> pyProxyStubFactory =
      RandomStubFactory.create(PyProxy.class);
    final PyProxy pyProxy = pyProxyStubFactory.getStub();

    assertNotWrappable(pyProxy);
  }

  @Test public void testCreateProxyWithJavaClassAnd__call__() throws Exception {
    m_interpreter.exec("from grinder.test import MyClass");
    final PyObject pyJavaType = m_interpreter.get("MyClass");
    createInstrumentedProxy(m_test, m_recorder, pyJavaType);

    m_interpreter.exec("result2 = MyClass.__call__(1, 2, 3)");
    final PyObject result2 = m_interpreter.get("result2");
    assertEquals(m_one, result2.invoke("getA"));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }
}
