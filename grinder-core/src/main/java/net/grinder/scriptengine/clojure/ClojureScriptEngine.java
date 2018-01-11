// Copyright (C) 2011 - 2012 Philip Aston
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

package net.grinder.scriptengine.clojure;

import java.io.StringReader;
import java.util.concurrent.Callable;

import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.ScriptEngineService.ScriptEngine;
import net.grinder.scriptengine.ScriptEngineService.WorkerRunnable;
import net.grinder.scriptengine.ScriptExecutionException;
import clojure.lang.Compiler;
import clojure.lang.RT;


/**
 * Clojure script engine.
 *
 * @author Philip Aston
 */
class ClojureScriptEngine implements ScriptEngine {

  private final Callable<?> m_runnerFactory;

  private static String describeResult(final Object r) {
    return r == null ? "nil" : r.getClass().getName();
  }

  public ClojureScriptEngine(final ScriptLocation script)
      throws EngineException {

    final Object result;

    try {
      RT.load("clojure/core"); // CLJ-1172
      result = Compiler.loadFile(script.getFile().getPath());
    }
    catch (final Exception e) {
      throw new ClojureScriptExecutionException("Failed to load " + script, e);
    }

    if (!(result instanceof Callable<?>)) {
      throw new ClojureScriptExecutionException(
        "The script should return a function that creates a test runner " +
        "function " +
        "[It returned " + describeResult(result) + "]");
    }

    m_runnerFactory = (Callable<?>)result;
  }

  @Override
  public WorkerRunnable createWorkerRunnable() throws EngineException {

    final Object result;

    try {
      result = m_runnerFactory.call();
    }
    catch (final Exception e) {
      throw new ClojureScriptExecutionException(
        "Failed to create test runner function", e);
    }

    if (!(result instanceof Callable<?>)) {
      throw new ClojureScriptExecutionException(
        "The script should return a function that creates a test runner " +
        "function " +
        "[It returned " + describeResult(result) + "]");
    }

    return new ClojureWorkerRunnable((Callable<?>) result);
  }

  @Override
  public WorkerRunnable createWorkerRunnable(final Object testRunner)
    throws EngineException {

    if (testRunner instanceof Callable<?>) {
      return new ClojureWorkerRunnable((Callable<?>) testRunner);
    }

    throw new ClojureScriptExecutionException(
      "supplied testRunner is not a function");
  }

  @Override
  public String getDescription() {
    final String versionString =
      "(let [v *clojure-version*] " +
      "(format \"Clojure %s.%s.%s\" (v :major) (v :minor) (v :incremental)))";

    return Compiler.load(new StringReader(versionString)).toString();
  }

  @Override
  public void shutdown() throws EngineException {
    // No-op, until we discover whether Clojure defines an exit hook mechanism.
  }

  private static final class ClojureWorkerRunnable implements WorkerRunnable {
    private final Callable<?> m_workerFn;

    private ClojureWorkerRunnable(final Callable<?> result) {
      m_workerFn = result;
    }

    @Override
    public void run() throws ScriptExecutionException {
      try {
        m_workerFn.call();
      }
      catch (final Exception e) {
        throw new ClojureScriptExecutionException(
          "Worker thread raised exception", e);
      }
    }

    @Override
    public void shutdown() throws ScriptExecutionException {
      // No-op.
    }
  }

  private static final class ClojureScriptExecutionException
    extends ScriptExecutionException {

    public ClojureScriptExecutionException(final String s) {
      super(s);
    }

    public ClojureScriptExecutionException(final String s, final Throwable t) {
      super(s, t);
    }
  }
}
