// Copyright (C) 2001 - 2013 Philip Aston
// Copyright (C) 2005 Martin Wagner
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

import java.io.File;
import java.lang.reflect.Method;

import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.ScriptEngineService;
import net.grinder.scriptengine.ScriptEngineService.ScriptEngine;
import net.grinder.scriptengine.ScriptEngineService.WorkerRunnable;
import net.grinder.scriptengine.ScriptExecutionException;

import org.python.core.PyClass;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;


/**
 * Wrap up the context information necessary to invoke a Jython script.
 *
 * Package scope.
 *
 * @author Philip Aston
 */
final class JythonScriptEngine implements ScriptEngine {

  private static final String PYTHON_HOME = "python.home";
  private static final String PYTHON_CACHEDIR = "python.cachedir";
  private static final String CACHEDIR_DEFAULT_NAME = "cachedir";

  private static final String TEST_RUNNER_CALLABLE_NAME = "TestRunner";

  private final PySystemState m_systemState;
  private final PythonInterpreter m_interpreter;
  private final PyClass m_dieQuietly;  // The softly spoken Welshman.
  private final String m_version;

  private final PyObject m_testRunnerFactory;

  /**
   * Constructor for JythonScriptEngine.
   * @param pySystemState Python system state.
   *
   * @throws EngineException If the script engine could not be created.
   */
  public JythonScriptEngine(final ScriptLocation script)
      throws EngineException {

    // Work around Jython issue 1894900.
    // If the python.cachedir has not been specified, and Jython is loaded
    // via the manifest classpath or the jar in the lib directory is
    // explicitly mentioned in the CLASSPATH, then set the cache directory to
    // be alongside jython.jar.
    if (System.getProperty(PYTHON_HOME) == null &&
        System.getProperty(PYTHON_CACHEDIR) == null) {
      final String classpath = System.getProperty("java.class.path");

      final File grinderJar = findFileInPath(classpath, "grinder.jar");
      final File grinderJarDirectory =
        grinderJar != null ? grinderJar.getParentFile() : new File(".");

      final File jythonJar = findFileInPath(classpath, "jython.jar");
      final File jythonHome =
        jythonJar != null ? jythonJar.getParentFile() : grinderJarDirectory;

      if (grinderJarDirectory == null && jythonJar == null ||
          grinderJarDirectory != null &&
          grinderJarDirectory.equals(jythonHome)) {
        final File cacheDir = new File(jythonHome, CACHEDIR_DEFAULT_NAME);
        System.setProperty("python.cachedir", cacheDir.getAbsolutePath());
      }
    }

    m_systemState = new PySystemState();

    try {
      m_interpreter = new PythonInterpreter(null, m_systemState);

      m_interpreter.exec("class ___DieQuietly___: pass");
      m_dieQuietly = (PyClass) m_interpreter.get("___DieQuietly___");

      String version;

      try {
        version = PySystemState.class.getField("version").get(null).toString();
      }
      catch (final Exception e) {
        version = "Unknown";
      }

      m_version = version;

      // Prepend the script directory to the Python path. This matches the
      // behaviour of the Jython interpreter.
      m_systemState.path.insert(0,
        new PyString(script.getFile().getParent()));

      // Additionally, add the working directory to the Python path. I think
      // this will always be the same as the worker's CWD. Users expect to be
      // able to import from the directory the agent is running in or (when the
      // script has been distributed), the distribution directory.
      m_systemState.path.insert(1,
        new PyString(script.getDirectory().getFile().getPath()));

      try {
        // Run the test script, script does global set up here.
        m_interpreter.execfile(script.getFile().getPath());
      }
      catch (final PyException e) {
        throw new JythonScriptExecutionException("initialising test script", e);
      }

      // Find the callable that acts as a factory for test runner instances.
      m_testRunnerFactory = m_interpreter.get(TEST_RUNNER_CALLABLE_NAME);

      if (m_testRunnerFactory == null || !m_testRunnerFactory.isCallable()) {
        throw new JythonScriptExecutionException(
          "There is no callable (class or function) named '" +
          TEST_RUNNER_CALLABLE_NAME + "' in " + script);
      }
    }
    catch (final EngineException e) {
      shutdown();
      throw e;
    }
  }

  /**
   * Find a file, given a search path.
   *
   * @param path The path to search.
   * @param fileName Name of the jar file to find.
   */
  private static File findFileInPath(final String path, final String fileName) {

    for (final String pathEntry : path.split(File.pathSeparator)) {
      final File file = new File(pathEntry);

     if (file.exists() && file.getName().equals(fileName)) {
        return file;
      }
    }

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override public WorkerRunnable createWorkerRunnable()
    throws EngineException {

    final PyObject pyTestRunner;

    try {
      // Script does per-thread initialisation here and
      // returns a callable object.
      pyTestRunner = m_testRunnerFactory.__call__();
    }
    catch (final PyException e) {
      throw new JythonScriptExecutionException(
        "creating per-thread TestRunner object", e);
    }

    if (!pyTestRunner.isCallable()) {
      throw new JythonScriptExecutionException(
        "The result of '" + TEST_RUNNER_CALLABLE_NAME +
        "()' is not callable");
    }

    return new JythonWorkerRunnable(pyTestRunner);
  }

  /**
   * {@inheritDoc}
   */
  @Override public WorkerRunnable createWorkerRunnable(final Object testRunner)
    throws EngineException {

    if (testRunner instanceof PyObject) {
      final PyObject pyTestRunner = (PyObject) testRunner;

      if (pyTestRunner.isCallable()) {
        return new JythonWorkerRunnable(pyTestRunner);
      }
    }

    throw new JythonScriptExecutionException(
      "testRunner object is not callable");
  }

  /**
   * Shut down the engine.
   *
   * <p>
   * We don't use m_interpreter.cleanup(), which delegates to
   * PySystemState.callExitFunc, as callExitFunc logs problems to stderr.
   * Instead we duplicate the callExitFunc behaviour raise our own exceptions.
   * </p>
   *
   * @throws EngineException
   *           If the engine could not be shut down.
   */
  @Override
  public void shutdown() throws EngineException {

    try {
      final PyObject exitfunc = m_systemState.__findattr__("exitfunc");

      if (exitfunc != null) {
        try {
          exitfunc.__call__();
        }
        catch (final PyException e) {
          throw new JythonScriptExecutionException(
            "calling script exit function", e);
        }
      }
    }
    finally {
      // Later versions PythonInterpreter.cleanup() call
      // PySystemState.cleanup().
      try {
        final Method method = m_systemState.getClass().getMethod("cleanup");
        method.invoke(m_systemState);
      }
      catch (final NoSuchMethodException e) {
        // Ignore
      }
      catch (final Exception e) {
        throw new EngineException(e.getMessage(), e);
      }
    }
  }

  /**
   * Returns a description of the script engine for the log.
   *
   * @return The description.
   */
  @Override
  public String getDescription() {
    return "Jython " + m_version;
  }

  /**
   * Wrapper for script's TestRunner.
   */
  private final class JythonWorkerRunnable
    implements ScriptEngineService.WorkerRunnable {

    private final PyObject m_testRunner;

    public JythonWorkerRunnable(final PyObject testRunner) {
      m_testRunner = testRunner;
    }

    @Override
    public void run() throws ScriptExecutionException {

      try {
        m_testRunner.__call__();
      }
      catch (final PyException e) {
        throw new JythonScriptExecutionException("calling TestRunner", e);
      }
    }

    /**
     * <p>
     * Ensure that if the test runner has a {@code __del__} attribute, it is
     * called when the thread is shutdown. Normally Jython defers this to the
     * Java garbage collector, so we might have done something like
     *
     * <blockquote>
     *
     * <pre>
     * m_testRunner = null;
     * Runtime.getRuntime().gc();
     *</pre>
     *
     * </blockquote>
     *
     * instead. However this would have a number of problems:
     *
     * <ol>
     * <li>Some JVM's may chose not to finalise the test runner in response to
     * {@code gc()}.</li>
     * <li>{@code __del__} would be called by a GC thread.</li>
     * <li>The standard Jython finalizer wrapping around {@code __del__} logs
     * to {@code stderr}.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Instead, we call any {@code __del__} ourselves. After calling this
     * method, the {@code PyObject} that underlies this class is made invalid.
     * </p>
     */
    @Override
    public void shutdown() throws ScriptExecutionException {

      final PyObject del = m_testRunner.__findattr__("__del__");

      if (del != null) {
        try {
          del.__call__();
        }
        catch (final PyException e) {
          throw new JythonScriptExecutionException(
            "deleting TestRunner instance", e);
        }
        finally {
          // To avoid the (pretty small) chance of the test runner being
          // finalised and __del__ being run twice, we disable it.

          // Unfortunately, Jython caches the __del__ attribute and makes
          // it impossible to turn it off at a class level. Instead we do
          // this:
          m_testRunner.__setattr__("__class__", m_dieQuietly);
        }
      }
    }
  }
}
