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

package net.grinder.scriptengine.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.List;

import net.grinder.engine.common.ScriptLocation;
import net.grinder.engine.process.dcr.DCRContextImplementation;
import net.grinder.scriptengine.DCRContext;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.java.JavaScriptEngineService;

import org.junit.Test;


/**
 * Unit tests for {@link JavaScriptEngineService}.
 *
 * @author Philip Aston
 */
public class TestJavaScriptEngineService {

  @Test public void testCreateInstrumenter()
    throws Exception {

    final DCRContext context = DCRContextImplementation.create(null);

    final List<? extends Instrumenter> instrumenters =
      new JavaScriptEngineService(context).createInstrumenters();

    assertEquals(1, instrumenters.size());
    assertEquals("byte code transforming instrumenter for Java",
                 instrumenters.get(0).getDescription());
  }

  @Test public void testCreateInstrumenterWithNoInstrumentation()
    throws Exception {

    final List<? extends Instrumenter> instrumenters =
      new JavaScriptEngineService().createInstrumenters();

    assertEquals(0, instrumenters.size());
  }

  @Test public void testGetScriptEngine() throws Exception {
    final ScriptLocation script = new ScriptLocation(new File("foo.java"));

    assertNull(new JavaScriptEngineService(null).createScriptEngine(script));
  }
}
