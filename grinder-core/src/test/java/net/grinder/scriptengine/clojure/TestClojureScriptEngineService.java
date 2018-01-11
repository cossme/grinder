// Copyright (C) 2011 Philip Aston
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

import static java.util.Collections.singleton;
import static net.grinder.testutility.AssertUtilities.assertContains;
import static net.grinder.testutility.FileUtilities.createFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Collections;
import java.util.List;

import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.ScriptEngineService;
import net.grinder.scriptengine.ScriptEngineService.ScriptEngine;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.util.BlockingClassLoader;
import net.grinder.util.Directory;

import org.junit.Test;


/**
 * Unit tests for {@link ClojureScriptEngineService}.
 *
 * @author Philip Aston
 */
public class TestClojureScriptEngineService extends AbstractJUnit4FileTestCase {

  @Test public void testCreateInstrumenters() throws Exception {
    final List<? extends Instrumenter> instrumenters =
      new ClojureScriptEngineService().createInstrumenters();

    assertEquals(0, instrumenters.size());
  }

  @Test public void testCreateScriptEngineWrongType() throws Exception {

    final ScriptLocation someScript =
      new ScriptLocation(new File("some.thing"));

    final ScriptEngine result =
      new ClojureScriptEngineService().createScriptEngine(someScript);

    assertNull(result);
  }

  @Test public void testCreateScriptEngine() throws Exception {

    final ScriptLocation script =
      new ScriptLocation(new Directory(getDirectory()), new File("my.clj"));

    createFile(script.getFile(),
               "(fn [] (fn [] ()))");

    final ScriptEngine result =
      new ClojureScriptEngineService().createScriptEngine(script);

    assertContains(result.getDescription(), "Clojure");
  }

  @Test public void testCreateScriptEngineNoClojure() throws Exception {

    final ScriptLocation script =
      new ScriptLocation(new Directory(getDirectory()), new File("my.clj"));

    final ClassLoader blockingLoader =
      new BlockingClassLoader(singleton("clojure.*"),
                              singleton("net.grinder.scriptengine.clojure.*"),
                              Collections.<String>emptySet(),
                              false);

    final ScriptEngineService service =
      (ScriptEngineService) blockingLoader.loadClass(
         ClojureScriptEngineService.class.getName()).newInstance();

    try {
      service.createScriptEngine(script);
      fail("Expected EngineException");
    }
    catch (EngineException e) {
      assertContains(e.getMessage(), "classpath");
    }
  }
}
