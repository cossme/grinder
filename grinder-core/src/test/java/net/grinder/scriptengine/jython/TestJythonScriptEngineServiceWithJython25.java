// Copyright (C) 2011 - 2013 Philip Aston
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

import static net.grinder.scriptengine.jython.instrumentation.AbstractJythonInstrumenterTestCase.assertVersion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.util.List;

import net.grinder.common.GrinderProperties;
import net.grinder.engine.process.dcr.DCRContextImplementation;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.scriptengine.DCRContext;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.ScriptEngineService.ScriptEngine;
import net.grinder.testutility.Jython25_27Runner;
import net.grinder.testutility.JythonVersionRunner.UseSeparateInterpreter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.python.core.PyInstance;
import org.python.core.PyObject;

/**
 * Unit tests for {@link JythonScriptEngineService}.
 *
 * @author Philip Aston
 */
@RunWith(Jython25_27Runner.class)
public class TestJythonScriptEngineServiceWithJython25
  extends AbstractJythonScriptEngineServiceTests {

  @Test
  public void testVersion() throws Exception {
    assertVersion("2.5");
  }

  @Test
  @UseSeparateInterpreter
  public void testInitialisationWithCacheDir() throws Exception {

    System.clearProperty("python.cachedir");
    final String originalClasspath = System.getProperty("java.class.path");

    final File anotherDirectory = new File(getDirectory(), "foo");
    anotherDirectory.mkdirs();
    final File grinderJar = new File(anotherDirectory, "grinder.jar");
    grinderJar.createNewFile();
    final File jythonJar = new File(getDirectory(), "jython.jar");
    jythonJar.createNewFile();

    System.setProperty("java.class.path",
      grinderJar.getAbsolutePath() + File.pathSeparator +
          jythonJar.getAbsolutePath());

    try {
      final ScriptEngine se =
          m_jythonScriptEngineService.createScriptEngine(m_pyScript);

      // Version specific!
      assertNotNull(System.getProperty("python.cachedir"));
      se.shutdown();
    }
    finally {
      System.setProperty("java.class.path", originalClasspath);
      System.clearProperty("python.cachedir");
    }
  }

  @Test
  @UseSeparateInterpreter
  public void testInitialisationWithEmptyClasspath() throws Exception {
    System.clearProperty("python.cachedir");
    final String originalClasspath = System.getProperty("java.class.path");

    System.setProperty("java.class.path", "");

    try {
      final ScriptEngine se =
          m_jythonScriptEngineService.createScriptEngine(m_pyScript);

      assertNotNull(System.getProperty("python.cachedir"));

      se.shutdown();
    }
    finally {
      System.setProperty("java.class.path", originalClasspath);
      System.clearProperty("python.cachedir");
    }
  }

  @Test
  @UseSeparateInterpreter
  public void testInitialisationWithCollocatedGrinderAndJythonJars()
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
      final ScriptEngine se =
          m_jythonScriptEngineService.createScriptEngine(m_pyScript);

      assertNotNull(System.getProperty("python.cachedir"));
      System.clearProperty("python.cachedir");

      se.shutdown();
    }
    finally {
      System.setProperty("java.class.path", originalClasspath);
    }
  }

  @Test
  public void testCreateInstrumentedProxy() throws Exception {
    final GrinderProperties properties = new GrinderProperties();
    final DCRContext context = DCRContextImplementation.create(null);

    final List<Instrumenter> instrumenters =
        new JythonScriptEngineService(properties, context, m_pyScript)
            .createInstrumenters();

    assertEquals(1, instrumenters.size());

    final Instrumenter instrumenter = instrumenters.get(0);

    assertEquals("byte code transforming instrumenter for Jython 2.5",
      instrumenter.getDescription());

    final Object original = new PyInstance();

    final Object proxy =
        instrumenter.createInstrumentedProxy(m_test, m_recorder, original);

    assertSame(original, proxy);
  }

  @Test(expected=NotWrappableTypeException.class)
  public void testCreateInstrumentedProxyNotWrappable() throws Exception {
    final GrinderProperties properties = new GrinderProperties();
    final DCRContext context = DCRContextImplementation.create(null);

    final List<Instrumenter> instrumenters =
        new JythonScriptEngineService(properties, context, m_pyScript)
            .createInstrumenters();

    assertEquals(1, instrumenters.size());

    final Instrumenter instrumenter = instrumenters.get(0);

    assertEquals("byte code transforming instrumenter for Jython 2.5",
      instrumenter.getDescription());

    instrumenter.createInstrumentedProxy(m_test,
      m_recorder,
      new PyObject());
  }

  @Test
  public void testInstrument() throws Exception {
    final GrinderProperties properties = new GrinderProperties();
    final DCRContextImplementation context = DCRContextImplementation
        .create(null);

    final List<Instrumenter> instrumenters =
        new JythonScriptEngineService(properties, context, m_pyScript)
            .createInstrumenters();

    instrumenters.get(0).instrument(m_test, m_recorder, new PyInstance());
  }
}
