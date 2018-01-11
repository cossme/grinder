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

import static net.grinder.testutility.AssertUtilities.assertContains;
import static net.grinder.testutility.FileUtilities.createFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.ScriptEngineService.ScriptEngine;
import net.grinder.scriptengine.ScriptEngineService.WorkerRunnable;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.util.Directory;

import org.junit.Before;
import org.junit.Test;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;


/**
 * Unit tests for {@link JythonScriptEngine}.
 *
 * @author Philip Aston
 */
public class TestJythonScriptEngine extends AbstractJUnit4FileTestCase {

  private static Object s_lastCallbackObject;

  {
    PySystemState.initialize();
  }

  private final PythonInterpreter m_interpreter =
    new PythonInterpreter(null, new PySystemState());

  private ScriptLocation m_script;

  @Before public void initialise() throws Exception {
    final File scriptFile = new File(getDirectory(), "script");

    m_script = new ScriptLocation(new Directory(getDirectory()), scriptFile);
  }

  @Test public void testInitialiseNoFile() throws Exception {
    try {
      new JythonScriptEngine(m_script);
      fail("Expected JythonScriptExecutionException");
    }
    catch (final JythonScriptExecutionException e) {
      assertContains(e.getShortMessage(), "IOError");
    }
  }

  @Test public void testInitialiseNoCallable() throws Exception {
    createFile(m_script.getFile());

    try {
      new JythonScriptEngine(m_script);
      fail("Expected EngineException");
    }
    catch (final EngineException e) {
      assertContains(e.getMessage(), "no callable");
    }
  }

  @Test public void testInitialiseNoCallable2() throws Exception {
    createFile(m_script.getFile(),
               "TestRunner = 1");

    try {
      new JythonScriptEngine(m_script);
      fail("Expected EngineException");
    }
    catch (final EngineException e) {
      assertContains(e.getMessage(), "no callable");
    }
  }

  @Test public void testInitialise() throws Exception {
    createFile(m_script.getFile(),
               "class TestRunner:pass");

    final ScriptEngine scriptEngine = new JythonScriptEngine(m_script);
    assertContains(scriptEngine.getDescription(), "Jython");
    scriptEngine.shutdown();
  }

  @Test public void testInitialiseJythonException() throws Exception {
    final File directory = new File(getDirectory(), "bah/foo");
    assertTrue(directory.mkdirs());

    // Bad root directory, causes import to fail.
    final ScriptLocation script =
      new ScriptLocation(new Directory(new File("bah")),
                         new File(getDirectory(), "script"));

    createFile(script.getFile(),
               "import foo",
               "class TestRunner:pass");

    try {
      new JythonScriptEngine(script);
      fail("Expected JythonScriptExecutionException");
    }
    catch (final JythonScriptExecutionException e) {
      assertContains(e.getShortMessage(), "ImportError");
    }
  }

  @Test public void testInitialisePathScriptWorkingDirectory()
      throws Exception {
    final File directory = new File(getDirectory(), "bah/foo");
    assertTrue(directory.mkdirs());

    // Import works from ScriptLocation directory.
    final ScriptLocation script =
        new ScriptLocation(new Directory(new File(getDirectory(), "bah")),
                           new File(getDirectory(), "script"));

    createFile(script.getFile(),
               "import foo",
               "class TestRunner:pass");

    final ScriptEngine scriptEngine = new JythonScriptEngine(script);
    scriptEngine.shutdown();
  }

  @Test public void testInitialisePathScriptDirectory() throws Exception {
    final File directory = new File(getDirectory(), "bah/foo");
    assertTrue(directory.mkdirs());

    // Import works from script directory.
    final ScriptLocation script =
        new ScriptLocation(new Directory(new File("bah")),
                           new File(getDirectory(), "script"));

    createFile(script.getFile(),
               "import bah.foo",
               "class TestRunner:pass");

    final ScriptEngine scriptEngine = new JythonScriptEngine(script);
    scriptEngine.shutdown();
  }


  @Test public void testShutdownExitHook() throws Exception {

    callback(null);

    createFile(
      m_script.getFile(),
      "from net.grinder.scriptengine.jython import TestJythonScriptEngine",
      "import sys",
      "def f():",
      " TestJythonScriptEngine.callback(TestJythonScriptEngine)",
      "sys.exitfunc = f",
      "class TestRunner:pass");

    final JythonScriptEngine scriptEngine = new JythonScriptEngine(m_script);
    scriptEngine.shutdown();

    assertSame(TestJythonScriptEngine.class, s_lastCallbackObject);
  }

  @Test public void testShutdownBadHook() throws Exception {

    createFile(
      m_script.getFile(),
      "import sys",
      "def f():",
      " raise Exception('a problem')",
      "sys.exitfunc = f",
      "class TestRunner:pass");

    final JythonScriptEngine scriptEngine = new JythonScriptEngine(m_script);

    try {
      scriptEngine.shutdown();
      fail("Expected JythonScriptExecutionException");
    }
    catch (final JythonScriptExecutionException e) {
      assertContains(e.getShortMessage(), "a problem");
    }
  }

  @Test public void testWorkerRunnableNoCallable() throws Exception {

    createFile(
      m_script.getFile(),
      "class TestRunner:pass");

    final JythonScriptEngine scriptEngine = new JythonScriptEngine(m_script);

    try {
      scriptEngine.createWorkerRunnable();
      fail("Expected EngineException");
    }
    catch (final EngineException e) {
      assertContains(e.getMessage(), "is not callable");
    }
  }

  @Test public void testWorkerRunnableBadRunner() throws Exception {

    createFile(
      m_script.getFile(),
      "class TestRunner:",
      " def __init__(self): raise Exception('a problem')"
      );

    final JythonScriptEngine scriptEngine = new JythonScriptEngine(m_script);

    try {
      scriptEngine.createWorkerRunnable();
      fail("Expected JythonScriptExecutionException");
    }
    catch (final JythonScriptExecutionException e) {
      assertContains(e.getShortMessage(), "a problem");
    }
  }

  @Test public void testWorkerRunnable() throws Exception {

    createFile(
      m_script.getFile(),
      "class TestRunner:",
      " def __call__(self): pass"
      );

    final JythonScriptEngine scriptEngine = new JythonScriptEngine(m_script);
    final WorkerRunnable runnable1 = scriptEngine.createWorkerRunnable();
    final WorkerRunnable runnable2 = scriptEngine.createWorkerRunnable();
    assertNotSame(runnable1, runnable2);
    runnable1.run();
    runnable2.run();

    runnable1.shutdown();
    runnable2.shutdown();

    scriptEngine.shutdown();
  }

  @Test public void testWorkerRunnableBadRunner2() throws Exception {

    createFile(
      m_script.getFile(),
      "class TestRunner:",
      " def __call__(self): raise Exception('a problem')");

    final JythonScriptEngine scriptEngine = new JythonScriptEngine(m_script);
    final WorkerRunnable runnable = scriptEngine.createWorkerRunnable();

    try {
      runnable.run();
      fail("Expected JythonScriptExecutionException");
    }
    catch (final JythonScriptExecutionException e) {
      assertContains(e.getShortMessage(), "a problem");
    }
    finally {
      scriptEngine.shutdown();
    }
  }

  @Test public void testWorkerRunnableBadRunner3() throws Exception {

    createFile(
      m_script.getFile(),
      "class TestRunner:",
      " def __call__(self): pass",
      " def __del__(self): raise Exception('a problem')");

    final JythonScriptEngine scriptEngine = new JythonScriptEngine(m_script);
    final WorkerRunnable runnable = scriptEngine.createWorkerRunnable();

    try {
      runnable.shutdown();
      fail("Expected JythonScriptExecutionException");
    }
    catch (final JythonScriptExecutionException e) {
      assertContains(e.getShortMessage(), "a problem");
    }

    // Try it again, __del__ should now be disabled.
    runnable.shutdown();

    scriptEngine.shutdown();
  }

  @Test public void testNewWorkerRunnableWithTestRunner() throws Exception {
    createFile(
      m_script.getFile(),
      "class TestRunner: pass");

    final JythonScriptEngine scriptEngine = new JythonScriptEngine(m_script);

    try {
      scriptEngine.createWorkerRunnable(null);
      fail("Expected JythonScriptExecutionException");
    }
    catch (final JythonScriptExecutionException e) {
      assertContains(e.getMessage(), "is not callable");
    }

    final Object badRunner = new Object();

    try {
      scriptEngine.createWorkerRunnable(badRunner);
      fail("Expected JythonScriptExecutionException");
    }
    catch (final JythonScriptExecutionException e) {
      assertContains(e.getMessage(), "is not callable");
    }

    m_interpreter.exec("result=1");
    m_interpreter.exec("def myRunner():\n global result\n result=99");
    final PyObject goodRunner = m_interpreter.get("myRunner");

    final WorkerRunnable workerRunnable =
      scriptEngine.createWorkerRunnable(goodRunner);

    assertEquals("1", m_interpreter.get("result").toString());

    workerRunnable.run();
    assertEquals("99", m_interpreter.get("result").toString());

    final PyObject badRunner2 =  m_interpreter.get("result");

    try {
      scriptEngine.createWorkerRunnable(badRunner2);
      fail("Expected JythonScriptExecutionException");
    }
    catch (final JythonScriptExecutionException e) {
      assertContains(e.getMessage(), "is not callable");
    }

    scriptEngine.shutdown();
  }

  public static void callback(final Object o) {
    s_lastCallbackObject = o;
  }
}
