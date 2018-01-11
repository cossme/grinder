// Copyright (C) 2005 - 2012 Philip Aston
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

package net.grinder.scriptengine.jython;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import net.grinder.common.GrinderProperties;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.engine.process.dcr.DCRContextImplementation;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.scriptengine.DCRContext;
import net.grinder.scriptengine.Instrumenter;

import org.junit.Test;
import org.python.core.PyObject;
import org.python.core.PySystemState;


/**
 * Unit tests for {@link JythonScriptEngine}.
 *
 * @author Philip Aston
 */
public class TestJythonScriptEngineService
  extends AbstractJythonScriptEngineServiceTests {

  @Test public void testInitialisationWithCollocatedGrinderAndJythonJars()
    throws Exception {

    System.clearProperty("python.cachedir");
    final String originalClasspath = System.getProperty("java.class.path");

    final File grinderJar = new File(getDirectory(), "grinder.jar");
    grinderJar.createNewFile();
    final File jythonJar = new File(getDirectory(), "jython.jar");
    jythonJar.createNewFile();

    System.setProperty("java.class.path",
                       grinderJar.getAbsolutePath() + File.pathSeparator +
                       jythonJar.getAbsolutePath());

    try {
      m_jythonScriptEngineService.createScriptEngine(m_pyScript);

      assertNotNull(System.getProperty("python.cachedir"));
      System.clearProperty("python.cachedir");
    }
    finally {
      System.setProperty("java.class.path", originalClasspath);
    }
  }

  @Test public void testCreateInstrumentedProxy() throws Exception {
    final GrinderProperties properties = new GrinderProperties();
    final DCRContext context = DCRContextImplementation.create(null);

    final List<Instrumenter> instrumenters =
      new JythonScriptEngineService(properties, context, m_pyScript)
      .createInstrumenters();

    assertEquals(1, instrumenters.size());

    final Instrumenter instrumenter = instrumenters.get(0);

    assertEquals("byte code transforming instrumenter for Jython 2.5",
                 instrumenter.getDescription());

    final Object foo = new Object();

    PySystemState.initialize();

    instrumenter.createInstrumentedProxy(m_test, m_recorder, foo);

    try {
      instrumenter.createInstrumentedProxy(m_test,
                                           m_recorder,
                                           new PyObject());
      fail("Expected NotWrappableTypeException");
    }
    catch (NotWrappableTypeException e) {
    }
  }

  @Test public void testInstrument() throws Exception {
    final GrinderProperties properties = new GrinderProperties();
    final DCRContextImplementation context = DCRContextImplementation.create(null);

    final List<Instrumenter> instrumenters =
      new JythonScriptEngineService(properties, context, m_pyScript)
      .createInstrumenters();

    assertEquals(1, instrumenters.size());

    final Instrumenter instrumenter = instrumenters.get(0);

    final Object foo = new Object();

    instrumenter.instrument(m_test, m_recorder, foo, null);
  }

  @Test public void testWithNoInstrumenters() throws Exception {
    final List<Instrumenter> instrumenters =
      new JythonScriptEngineService(new GrinderProperties(),
                                    null,
                                    new ScriptLocation(new File("notpy.blah")))
      .createInstrumenters();

    assertEquals(0, instrumenters.size());
  }
}
