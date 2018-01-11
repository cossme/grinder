// Copyright (C) 2005 - 2010 Philip Aston
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import net.grinder.scriptengine.jython.JythonScriptExecutionException;
import net.grinder.testutility.AbstractFileTestCase;
import static net.grinder.testutility.AssertUtilities.assertContains;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyString;
import org.python.core.PySystemState;


public class TestJythonScriptExecutionException extends AbstractFileTestCase {

  protected void setUp() throws Exception {
    super.setUp();
    final Properties properties = new Properties();
    properties.put("python.cachedir", getDirectory());
    PySystemState.initialize(properties, null, null);
  }

  public void testWithSimpleJythonException() throws Exception {
    final PyException pe = new PyException();
    final JythonScriptExecutionException e =
      new JythonScriptExecutionException("Hello", pe);

    assertNull(e.getCause());
    assertContains(e.getShortMessage(), "Jython exception");
  }

  public void testWithJythonStringException() throws Exception {
    final PyException pe = new PyException(new PyString("lah"));
    final JythonScriptExecutionException e =
      new JythonScriptExecutionException("Hello", pe);

    assertNull(e.getCause());
    assertContains(e.getShortMessage(), "Jython exception");
  }

  public void testWithJythonClassException() throws Exception {
    final PyException pe = new PyException(Py.RuntimeError, "Its all wrong");
    final JythonScriptExecutionException e =
      new JythonScriptExecutionException("Hello", pe);

    assertNull(e.getCause());
    assertContains(e.getShortMessage(), "Jython exception");
  }

  public void testWithWrappedJavaException() throws Exception {
    final Throwable wrapped = new Throwable();
    final PyException pe = Py.JavaError(wrapped);
    final JythonScriptExecutionException e =
      new JythonScriptExecutionException("Hello", pe);

    assertNotSame(pe, e.getCause());
    assertSame(wrapped, e.getCause());

    final StringWriter writer = new StringWriter();
    e.printStackTrace(new PrintWriter(writer));
    final String stack = writer.toString();

    assertContains(stack, "Hello");
    assertContains(stack, "java.lang.Throwable");
    assertSame(wrapped, e.getCause());
    assertContains(e.getShortMessage(), "Java exception");
  }

  // Bug 2988755
  public void testWithNullTraceBack() {
    final PyException pe = new PyException();
    pe.traceback = null;

    final JythonScriptExecutionException e =
      new JythonScriptExecutionException("Hello", pe);

    assertContains(e.getShortMessage(), "Jython");

    final StringWriter writer = new StringWriter();
    e.printStackTrace(new PrintWriter(writer));
    final String stack = writer.toString();

    assertContains(stack, "None");
  }
}
