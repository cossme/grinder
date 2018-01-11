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

package net.grinder.engine.process;

import static java.util.Arrays.asList;
import static net.grinder.testutility.AssertUtilities.assertContains;
import static net.grinder.testutility.FileUtilities.createFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;

import net.grinder.common.GrinderProperties;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.DCRContext;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.ScriptEngineService;
import net.grinder.scriptengine.ScriptEngineService.ScriptEngine;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.util.BlockingClassLoader;
import net.grinder.util.Directory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;


/**
 * Unit tests for {@link ScriptEngineContainer}.
 *
 * @author Philip Aston
 */
public class TestScriptEngineContainer extends AbstractJUnit4FileTestCase {

  @Mock private DCRContext m_dcrContext;
  @Mock private GrinderProperties m_properties;
  @Mock private Logger m_logger;

  private ScriptLocation m_pyScript;

  @Before public void initialise() throws Exception {
    MockitoAnnotations.initMocks(this);
    m_pyScript = new ScriptLocation(new File("foo.py"));
  }

  @Test public void testBadResourceLoading() throws Exception {
    final ClassLoader blockingLoader =
      new BlockingClassLoader(
         Collections.<String>emptySet(),
         new HashSet<String>(
             asList(ScriptEngineContainer.class.getName(),
                    ScriptEngineContainerScopeTunnel.class.getName(),
                    ScriptEngineService.class.getName())),
         Collections.<String>emptySet(),
         false) {
        @Override public Enumeration<URL> getResources(final String name)
          throws IOException {
          // Be evil.
          throw new IOException("");
        }
      };

    try {
      constructInClassLoader(blockingLoader);
      fail("Expected EngineException");
    }
    catch (final InvocationTargetException e) {
      assertTrue(e.getCause() instanceof EngineException);
    }
  }

  private final void constructInClassLoader(final ClassLoader loader)
    throws Exception {

    final Class<?> c =
      loader.loadClass(ScriptEngineContainerScopeTunnel.class.getName());

    final Method method =
      c.getMethod("createScriptEngineContainer",
                  GrinderProperties.class,
                  Logger.class,
                  DCRContext.class,
                  ScriptLocation.class);

    method.invoke(null, m_properties, m_logger, m_dcrContext, m_pyScript);
  }

  @Test public void testUnknownImplementation() throws Exception {

    final File path = new File(getDirectory(), "cp");

    createFile(new File(path, ScriptEngineService.RESOURCE_NAME),
               "bobbins");

    final List<URL> additionalClasspath =
      asList(new File(getDirectory(), "cp").toURI().toURL());

    final ClassLoader blockingLoader =
      new BlockingClassLoader(
         additionalClasspath,
         Collections.<String>emptySet(),
         new HashSet<String>(
             asList(ScriptEngineContainer.class.getName(),
                    ScriptEngineContainerScopeTunnel.class.getName(),
                    ScriptEngineService.class.getName(),
                    ScriptEngineService.RESOURCE_NAME)),
         Collections.<String>emptySet(),
         true);

    try {
      constructInClassLoader(blockingLoader);
      fail("Expected EngineException");
    }
    catch (final InvocationTargetException e) {
      assertTrue(e.getCause() instanceof EngineException);
      assertTrue(e.getCause().getCause() instanceof ClassNotFoundException);
    }
  }

  @Test public void testBadImplementation() throws Exception {

    final File path = new File(getDirectory(), "cp");

    createFile(new File(path, ScriptEngineService.RESOURCE_NAME),
               "java.lang.Object");

    final List<URL> additionalClasspath =
      asList(new File(getDirectory(), "cp").toURI().toURL());

    final ClassLoader blockingLoader =
      new BlockingClassLoader(
         additionalClasspath,
         Collections.<String>emptySet(),
         new HashSet<String>(
           asList(ScriptEngineContainer.class.getName(),
                  ScriptEngineContainerScopeTunnel.class.getName(),
                  ScriptEngineService.class.getName(),
                  ScriptEngineService.RESOURCE_NAME)),
         Collections.<String>emptySet(),
         true);

    try {
      constructInClassLoader(blockingLoader);
      fail("Expected EngineException");
    }
    catch (final InvocationTargetException e) {
      assertTrue(e.getCause() instanceof EngineException);
      assertContains(e.getCause().getMessage(), "does not implement");
    }
  }

  @Test public void testStandardInstrumentationNoDCR() throws Exception {
    final ScriptEngineContainer container =
      new ScriptEngineContainer(m_properties, m_logger, null, m_pyScript);

    final Instrumenter instrumenter = container.createInstrumenter();
    assertEquals("NO INSTRUMENTER COULD BE LOADED",
                 instrumenter.getDescription());
  }

  @Test public void testStandardInstrumentationDCR() throws Exception {
    final ScriptEngineContainer container =
      new ScriptEngineContainer(m_properties,
                                m_logger,
                                m_dcrContext,
                                m_pyScript);

    final Instrumenter instrumenter = container.createInstrumenter();
    assertEquals("byte code transforming instrumenter for Jython 2.5; " +
                 "byte code transforming instrumenter for Java",
                 instrumenter.getDescription());
  }

  @Test public void testUnknownScriptType() throws Exception {
    final ScriptEngineContainer container =
      new ScriptEngineContainer(m_properties,
                                m_logger,
                                m_dcrContext,
                                m_pyScript);

    try {
      container.getScriptEngine(new ScriptLocation(new File("foo.xxx")));
      fail("Expected EngineException");
    }
    catch (final EngineException e) {
      assertContains(e.getMessage(), "No suitable script engine");
    }
  }

  @Test public void testJythonScript() throws Exception {
    final ScriptLocation pyScript =
      new ScriptLocation(new Directory(getDirectory()),
                         new File("my.py"));

    createFile(pyScript.getFile(),
               "class TestRunner: pass");

    final ScriptEngineContainer container =
      new ScriptEngineContainer(m_properties,
                                m_logger,
                                m_dcrContext,
                                m_pyScript);

    final ScriptEngine scriptEngine = container.getScriptEngine(pyScript);
    assertContains(scriptEngine.getDescription(), "Jython");
    scriptEngine.shutdown();
  }
}
