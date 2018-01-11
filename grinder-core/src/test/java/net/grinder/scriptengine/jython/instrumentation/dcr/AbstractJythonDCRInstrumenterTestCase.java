// Copyright (C) 2009 - 2013 Philip Aston
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

package net.grinder.scriptengine.jython.instrumentation.dcr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import net.grinder.engine.process.dcr.RecorderLocatorAccess;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.jython.instrumentation.AbstractJythonInstrumenterTestCase;

import org.junit.After;
import org.junit.Test;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;


/**
 * Common stuff for Jython DCR instrumenters.
 *
 * @author Philip Aston
 */
public abstract class AbstractJythonDCRInstrumenterTestCase
  extends AbstractJythonInstrumenterTestCase {

  public AbstractJythonDCRInstrumenterTestCase(
    final Instrumenter instrumenter) {
    super(instrumenter);
  }

  @After public void tearDown() throws Exception {
    RecorderLocatorAccess.clearRecorders();
  }

  @Override
  protected void assertTestReference(final PyObject pyObject,
                                     final net.grinder.common.Test test) {
    // No-op, AbstractDCRInstrumenter doesn't support __test__.
  }

  @Override
  protected void assertTargetReference(final PyObject proxy,
                                       final Object original,
                                       final boolean unwrapTarget) {
    // AbstractDCRInstrumenter doesn't support __target__.
  }

  @Test public void testInstrumentationWithNonWrappableParameters()
    throws Exception {

    // The types that can be wrapped depend on the Instrumenter.

    final PythonInterpreter interpreter = getInterpretter();

    // Can't wrap PyInteger.
    interpreter.exec("x=1");
    assertNotWrappable(interpreter.get("x"));

    assertNotWrappableByThisInstrumenter(null);

    // assertNotWrappableByThisInstrumenter(MyClass.class);
  }

  @Test public void testInstrumentationWithPyClass() throws Exception {
    m_interpreter.exec("class Foo:\n" +
                       " def __init__(self, a, b, c):\n" +
                       "  self.a = a\n" +
                       "  self.b = b\n" +
                       "  self.c = c\n" +
                       " def six(self): return 6\n");

    final PyObject pyType = m_interpreter.get("Foo");
    createInstrumentedProxy(m_test, m_recorder, pyType);
    final PyObject result = pyType.__call__(m_two, m_three, m_one);
    assertEquals(m_two, result.__getattr__("a"));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.set("proxy", pyType);

    m_interpreter.exec("result2 = Foo(1, 2, 3)");
    final PyObject result2 = m_interpreter.get("result2");
    assertEquals(m_two, result2.__getattr__("b"));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_interpreter.exec("result3 = proxy(0, 0, 0)");
    final PyObject result3 = m_interpreter.get("result3");
    assertEquals(m_zero, result3.__getattr__("b"));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // Instrumenting a class doesn't instrument methods.
    m_interpreter.exec("result4 = result3.six()");
    assertEquals(m_six, m_interpreter.get("result4"));
    m_recorderStubFactory.assertNoMoreCalls();
  }

  @Test public void testInstrumentationWithPyDerivedClass() throws Exception {
    m_interpreter.exec("from grinder.test import MyClass\n" +
                       "class Foo(MyClass):\n" +
                       " def six(self): return 6\n" +
                       "x=Foo()");

    final PyObject pyType = m_interpreter.get("Foo");
    createInstrumentedProxy(m_test, m_recorder, pyType);
    final PyObject result = pyType.__call__();
    assertEquals(m_zero, result.invoke("getA"));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.set("proxy", pyType);

    m_interpreter.exec("result2 = Foo()");
    final PyObject result2 = m_interpreter.get("result2");
    assertEquals(m_zero, result2.invoke("getB"));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_interpreter.exec("result3 = proxy(0, 0, 0)");
    final PyObject result3 = m_interpreter.get("result3");
    assertEquals(m_zero, result3.invoke("getB"));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // Instrumenting a class doesn't instrument methods.
    m_interpreter.exec("result4 = result3.six()");
    assertEquals(m_six, m_interpreter.get("result4"));
    m_recorderStubFactory.assertNoMoreCalls();
  }

  @Test public void testInstrumentationWithStaticMethod() throws Exception {
    m_interpreter.exec("from grinder.test import MyClass\n" +
                       "x=MyClass.staticSix");

    final PyObject pyType = m_interpreter.get("x");
    createInstrumentedProxy(m_test, m_recorder, pyType);
    final PyObject result = pyType.__call__();
    assertEquals(m_six, result);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.set("proxy", pyType);

    // From Jython 2.5.2, static method references are bound to distinct Jython
    // instances.
    m_interpreter.exec("result2 = x() # MyClass.staticSix()");
    final PyObject result2 = m_interpreter.get("result2");
    assertEquals(m_six, result2);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  @Test public void testInstrumentationWithReflectedConstructor()
      throws Exception {
    m_interpreter.exec("from grinder.test import MyClass\n" +
                       "x=MyClass.__init__");

    final PyObject myClass = m_interpreter.get("MyClass");
    final PyObject py = m_interpreter.get("x");
    createInstrumentedProxy(m_test, m_recorder, py);
    myClass.__call__();
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.exec("MyClass()");
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  @Test public void testInstrumentationWithPyLambda() throws Exception {
    m_interpreter.exec("f=lambda x:x+1");

    final PyObject pyType = m_interpreter.get("f");
    createInstrumentedProxy(m_test, m_recorder, pyType);
    final PyObject result = pyType.__call__(m_two);
    assertEquals(m_three, result);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.set("proxy", pyType);

    m_interpreter.exec("result2 = f(0)");
    final PyObject result2 = m_interpreter.get("result2");
    assertEquals(m_one, result2);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  // This doesn't work for DCR.
  @Test public void testCreateProxyWithPyJavaInstance() throws Exception {
    m_interpreter.exec("from grinder.test import MyClass\nx=MyClass()");
    final PyObject pyJava = m_interpreter.get("x");
    createInstrumentedProxy(m_test, m_recorder, pyJava);

    final PyObject result = pyJava.invoke("getA");
    assertEquals(m_zero, result);

    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.exec("result = x.getB()");
    final PyObject result2 = m_interpreter.get("result");
    assertEquals(m_zero, result2);

    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  @Test public void testCreateProxyWithJavaClass() throws Exception {
    m_interpreter.exec("from grinder.test import MyClass");
    final PyObject pyJavaType = m_interpreter.get("MyClass");
    createInstrumentedProxy(m_test, m_recorder, pyJavaType);

    m_interpreter.exec("result = MyClass(2, 3, 1)");
    final PyObject result = m_interpreter.get("result");

    assertEquals(m_two, result.invoke("getA"));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython.

    m_interpreter.exec("result2 = MyClass(1, 2, 3)");
    final PyObject result2 = m_interpreter.get("result2");
    assertEquals(m_two, result2.invoke("getB"));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_interpreter.exec("result3 = MyClass.staticSix()");
    assertEquals(m_six, m_interpreter.get("result3"));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  /**
   * See bug 2992248.
   */
  @Test public void testJavaBoundMethod() throws Exception {
    m_interpreter.exec("from grinder.test import MyClass\nx=MyClass(1, 2, 3)");

    m_interpreter.exec("y=x.getA");
    final PyObject pyJavaMethod = m_interpreter.get("y");
    createInstrumentedProxy(m_test, m_recorder, pyJavaMethod);

    final PyObject result = pyJavaMethod.__call__();
    assertEquals(m_one, result);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // Test instrumentation works through separate references.
    m_interpreter.exec("z=x.getA");
    final PyObject pyJavaMethod2 = m_interpreter.get("z");

    final PyObject result2 = pyJavaMethod2.__call__();
    assertNotSame(result, result2);

    assertEquals(m_one, result);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython
    m_interpreter.exec("z()");
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // Other instances are not instrumented.
    m_interpreter.exec("a=MyClass(1, 2, 3)");
    m_interpreter.exec("a.getA()");
    m_recorderStubFactory.assertNoMoreCalls();
  }

  /**
   * See bug 2992248.
   */
  @Test public void testJavaStaticMethod() throws Exception {
    m_interpreter.exec("from grinder.test import MyClass");

    m_interpreter.exec(
      "y=MyClass.staticSix\nz=MyClass.staticSix");
    final PyObject pyJavaMethod = m_interpreter.get("y");
    final PyObject pyJavaMethod2 = m_interpreter.get("z");
    createInstrumentedProxy(m_test, m_recorder, pyJavaMethod);

    final PyObject result = pyJavaMethod.__call__();
    assertEquals(m_six, result);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    final Integer[] version = getJythonVersion();

    if (version[0] < 2 ||
        version[0] == 2 && version[1] < 5 ||
        version[0] == 2 && version[1] == 5 && version[2] < 2) {
      // Up to Jython 2.5.1, static bindings are resolved once...
      assertSame(pyJavaMethod, pyJavaMethod2);
    }
    else {
      assertNotSame(pyJavaMethod, pyJavaMethod2);
    }

    // ... either way, z() is instrumented.

    final PyObject result2 = pyJavaMethod2.__call__();
    assertEquals(m_six, result2);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  @Test public void testJavaUnboundMethod() throws Exception {
    m_interpreter.exec("from grinder.test import MyClass\n" +
                       "x=MyClass(1, 2, 3)\n" +
                       "y=MyClass(3, 2, 1)\n" +
                       "m=MyClass.getA");

    final PyObject instance = m_interpreter.get("x");
    final PyObject unboundMethod = m_interpreter.get("m");
    createInstrumentedProxy(m_test, m_recorder, unboundMethod);

    final PyObject result = unboundMethod.__call__(instance);
    assertEquals(m_one, result);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // Test instrumentation works through a separate binding.
    final PyObject instance2 = m_interpreter.get("y");
    final PyObject result2 = unboundMethod.__call__(instance2);
    assertEquals(m_three, result2);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  @Test public void testJavaBoundMethodSuperClassImplementation()
    throws Exception {

    m_interpreter.exec(
     "from grinder.test import MyExtendedClass\nx=MyExtendedClass()");

    m_interpreter.exec("y=x.getA");
    final PyObject pyJavaMethod = m_interpreter.get("y");
    createInstrumentedProxy(m_test, m_recorder, pyJavaMethod);

    final PyObject result = pyJavaMethod.__call__();
    assertEquals(m_zero, result);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // Test instrumentation works through separate references.
    m_interpreter.exec("z=x.getA");
    final PyObject pyJavaMethod2 = m_interpreter.get("z");

    final PyObject result2 = pyJavaMethod2.__call__();
    assertNotSame(result, result2);

    assertEquals(m_zero, result);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython
    m_interpreter.exec("z()");
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // Other instances are not instrumented.
    m_interpreter.exec("a=MyExtendedClass()");
    m_interpreter.exec("a.getA()");
    m_recorderStubFactory.assertNoMoreCalls();
  }

  @Test public void testJavaBoundMethodThroughInterface() throws Exception {
    m_interpreter.exec("from grinder.test import MyExtendedClass\n"+
                       "x=MyExtendedClass.create()");

    m_interpreter.exec("y=x.addOne");
    final PyObject pyJavaMethod = m_interpreter.get("y");
    createInstrumentedProxy(m_test, m_recorder, pyJavaMethod);

    final PyObject result = pyJavaMethod.__call__(m_one);
    assertEquals(m_three, result);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython
    m_interpreter.exec("x.addOne(123)");
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // Other instances are not instrumented.
    m_interpreter.exec("a=MyExtendedClass.create()");
    m_interpreter.exec("a.addOne(1)");
    m_recorderStubFactory.assertNoMoreCalls();
  }

  @Test public void testJythonUnboundMethods() throws Exception {
    m_interpreter.exec("class C:\n" +
        " def f(self): pass\n" +
        " def g(self): pass\n" +
        "c=C()\n" +
        "m=C.f");

    final PyObject m = m_interpreter.get("m");
    createInstrumentedProxy(m_test, m_recorder, m);

    m_interpreter.exec("c.f()");
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_interpreter.exec("c.g()");
    m_recorderStubFactory.assertNoMoreCalls();
  }

  // Bug #228
  @Test public void testJythonBoundMethods() throws Exception {
    m_interpreter.exec("class C:\n" +
        " def f(self): pass\n" +
        " def g(self): pass\n" +
        "c=C()\n"+
        "m=c.f");

    final PyObject m = m_interpreter.get("m");

    createInstrumentedProxy(m_test, m_recorder, m);

    m_interpreter.exec("c.f()");
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_interpreter.exec("c.g()");
    m_recorderStubFactory.assertNoMoreCalls();
  }
}
