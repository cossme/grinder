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

import static net.grinder.testutility.AssertUtilities.assertContains;
import static net.grinder.testutility.FileUtilities.createFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.StringReader;

import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.ScriptEngineService.ScriptEngine;
import net.grinder.scriptengine.ScriptEngineService.WorkerRunnable;
import net.grinder.scriptengine.ScriptExecutionException;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.util.Directory;

import org.junit.Test;

import clojure.lang.Compiler;


/**
 * Unit tests for {@link ClojureScriptEngine}.
 *
 * @author Philip Aston
 */
public class TestClojureScriptEngine extends AbstractJUnit4FileTestCase {

  private static int s_called;

  @Test public void testBadScript() throws Exception {

    final ScriptLocation script =
      new ScriptLocation(new Directory(getDirectory()), new File("my.clj"));

    createFile(script.getFile(),
               "(");

    try {
      new ClojureScriptEngineService().createScriptEngine(script);
      fail("Expected ScriptExecutionException");
    }
    catch (final ScriptExecutionException e) {
      assertTrue(e.getCause() instanceof RuntimeException);
    }
  }

  @Test public void testNilRunnerFactory() throws Exception {

    final ScriptLocation script =
      new ScriptLocation(new Directory(getDirectory()), new File("my.clj"));

    createFile(script.getFile(),
               "()");

    try {
      new ClojureScriptEngineService().createScriptEngine(script);
      fail("Expected ScriptExecutionException");
    }
    catch (final ScriptExecutionException e) {
      assertContains(e.getMessage(), "should return a function");
    }
  }

  @Test public void testNoneRunnerFactory() throws Exception {

    final ScriptLocation script =
      new ScriptLocation(new Directory(getDirectory()), new File("my.clj"));

    createFile(script.getFile(),
               "");

    try {
      new ClojureScriptEngineService().createScriptEngine(script);
      fail("Expected ScriptExecutionException");
    }
    catch (final ScriptExecutionException e) {
      assertContains(e.getMessage(), "should return a function");
    }
  }

  @Test public void testNoWorkerRunnable() throws Exception {

    final ScriptLocation script =
      new ScriptLocation(new Directory(getDirectory()), new File("my.clj"));

    createFile(script.getFile(),
               "(fn [] ())");

    final ScriptEngine scriptEngine =
      new ClojureScriptEngineService().createScriptEngine(script);

    try {
      scriptEngine.createWorkerRunnable();
      fail("Expected ScriptExecutionException");
    }
    catch (final ScriptExecutionException e) {
      assertContains(e.getMessage(), "should return a function");
    }
  }

  @Test public void testNilWorkerRunnable() throws Exception {

    final ScriptLocation script =
      new ScriptLocation(new Directory(getDirectory()), new File("my.clj"));

    createFile(script.getFile(),
               "(fn [] nil)");

    final ScriptEngine scriptEngine =
      new ClojureScriptEngineService().createScriptEngine(script);

    try {
      scriptEngine.createWorkerRunnable();
      fail("Expected ScriptExecutionException");
    }
    catch (final ScriptExecutionException e) {
      assertContains(e.getMessage(), "should return a function");
    }
  }

  @Test public void testBadRunnerFactory() throws Exception {

    final ScriptLocation script =
      new ScriptLocation(new Directory(getDirectory()), new File("my.clj"));

    createFile(script.getFile(),
               "(fn [] (throw))");

    final ScriptEngine scriptEngine =
      new ClojureScriptEngineService().createScriptEngine(script);

    try {
      scriptEngine.createWorkerRunnable();
      fail("Expected ScriptExecutionException");
    }
    catch (final ScriptExecutionException e) {
      assertTrue(e.getCause() instanceof NullPointerException);
    }
  }

  @Test public void testGoodWorkerRunnable() throws Exception {

    final ScriptLocation script =
      new ScriptLocation(new Directory(getDirectory()), new File("my.clj"));

    createFile(script.getFile(),
               "(fn [] (fn [] ()))");

    final ScriptEngine scriptEngine =
      new ClojureScriptEngineService().createScriptEngine(script);

    assertContains(scriptEngine.getDescription(), "Clojure");

    final WorkerRunnable workerRunnable = scriptEngine.createWorkerRunnable();

    workerRunnable.run();

    workerRunnable.shutdown();

    scriptEngine.shutdown();
  }

  @Test public void testBadWorkerRunnable() throws Exception {

    final ScriptLocation script =
      new ScriptLocation(new Directory(getDirectory()), new File("my.clj"));

    createFile(script.getFile(),
               "(fn [] (fn [] (throw)))");

    final ScriptEngine scriptEngine =
      new ClojureScriptEngineService().createScriptEngine(script);

    final WorkerRunnable workerRunnable = scriptEngine.createWorkerRunnable();

    try {
      workerRunnable.run();
      fail("Expected ScriptExecutionException");
    }
    catch (final ScriptExecutionException e) {
      assertTrue(e.getCause() instanceof NullPointerException);
    }

    workerRunnable.shutdown();
  }

  @Test public void testCreateWorkerRunnableWithBadRunner() throws Exception {

    final ScriptLocation script =
      new ScriptLocation(new Directory(getDirectory()), new File("my.clj"));

    createFile(script.getFile(), "(fn [] ())");

    final ScriptEngine scriptEngine =
      new ClojureScriptEngineService().createScriptEngine(script);

    try {
      scriptEngine.createWorkerRunnable(this);
      fail("Expected ScriptExecutionException");
    }
    catch (final ScriptExecutionException e) {
      assertContains(e.getShortMessage(), "testRunner is not a function");
    }
  }

  @Test public void testCreateWorkerRunnableWithGoodRunner() throws Exception {

    final ScriptLocation script =
      new ScriptLocation(new Directory(getDirectory()), new File("my.clj"));

    createFile(script.getFile(), CALL_ME);

    s_called = 0;

    final ScriptEngine scriptEngine =
      new ClojureScriptEngineService().createScriptEngine(script);

    final Object runner = Compiler.load(new StringReader(CALL_ME));

    final WorkerRunnable workerRunnable =
      scriptEngine.createWorkerRunnable(runner);

    assertEquals(0, s_called);

    s_called = 0;

    workerRunnable.run();

    assertEquals(1, s_called);

    workerRunnable.shutdown();
  }

  private static final String CALL_ME =
    "(fn [] (" + TestClojureScriptEngine.class.getName() + "/called))";

  public static void called() {
    ++s_called;
  }
}
