// Copyright (C) 2009 - 2012 Philip Aston
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

package net.grinder.scriptengine.jython.instrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import net.grinder.common.StubTest;
import net.grinder.common.UncheckedGrinderException;
import net.grinder.common.UncheckedInterruptedException;
import net.grinder.script.NonInstrumentableTypeException;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.script.Test.InstrumentationFilter;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.Recorder;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.RandomStubFactory;

import org.junit.Test;
import org.python.core.PyClass;
import org.python.core.PyException;
import org.python.core.PyInstance;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyProxy;
import org.python.core.PySystemState;
import org.python.core.PyTuple;
import org.python.util.PythonInterpreter;


/**
 * Instrumentation unit tests.
 *
 * @author Philip Aston
 */
public abstract class AbstractJythonInstrumenterTestCase {

  {
    PySystemState.initialize();
  }

  protected final Instrumenter m_instrumenter;

  protected final PythonInterpreter m_interpreter =
    new PythonInterpreter(null, new PySystemState());

  protected final PyObject m_zero = new PyInteger(0);
  protected final PyObject m_one = new PyInteger(1);
  protected final PyObject m_two = new PyInteger(2);
  protected final PyObject m_three = new PyInteger(3);
  protected final PyObject m_six = new PyInteger(6);
  protected final net.grinder.common.Test m_test = new StubTest(1, "test");

  protected final RandomStubFactory<Recorder> m_recorderStubFactory =
      RandomStubFactory.create(Recorder.class);
  protected final Recorder m_recorder = m_recorderStubFactory.getStub();

  public AbstractJythonInstrumenterTestCase(final Instrumenter instrumenter) {
      super();
      m_instrumenter = instrumenter;
      m_recorderStubFactory.setIgnoreObjectMethods();
  }

  protected abstract void assertTestReference(PyObject proxy,
                                              net.grinder.common.Test test);

  protected abstract void assertTargetReference(PyObject proxy,
                                                Object original,
                                                boolean unwrapTarget);

  protected void assertTargetReference(final PyObject proxy, final Object original) {
    assertTargetReference(proxy, original, false);
  }

  public static Integer[] getJythonVersion() throws Exception {
    final PyTuple pyTuple =
      (PyTuple) PySystemState.class.getField("version_info").get(null);

    return new Integer[] { getVersionPart(pyTuple, 0),
                           getVersionPart(pyTuple, 1),
                           getVersionPart(pyTuple, 2), };
  }

  private static Integer getVersionPart(final PyTuple versionTuple, final int part) {
    // PyTuple.get()/toArray() do not exist in Jython 2.1.
    return (Integer)versionTuple.__finditem__(part).__tojava__(Integer.class);
  }

  public static void assertVersion(final String expected) throws Exception {
    AssertUtilities.assertContainsPattern(
      PySystemState.class.getField("version").get(null).toString(),
      expected);
  }

  protected final PythonInterpreter getInterpretter() {
    return m_interpreter;
  }

  protected final void assertNotWrappable(final Object o) throws Exception {
    try {
      m_instrumenter.createInstrumentedProxy(null, null, o);
      fail("Expected NotWrappableTypeException");
    }
    catch (final NotWrappableTypeException e) {
    }
  }

  protected final void assertNotWrappableByThisInstrumenter(final Object o)
    throws Exception {
    assertNull(m_instrumenter.createInstrumentedProxy(null, null, o));
  }

  protected final Object createInstrumentedProxy(final net.grinder.common.Test test,
                                                 final Recorder recorder,
                                                 final PyObject pyTarget)
    throws NotWrappableTypeException {

    // In the real world, the Java conversion happens implicitly because
    // wrap() and record() are implemented in Java.
    final Object javaTarget = pyTarget.__tojava__(Object.class);

    return m_instrumenter.createInstrumentedProxy(test, recorder, javaTarget);
  }

  protected final Class<?> getClassForInstance(final PyInstance target)
    throws IllegalArgumentException, IllegalAccessException {

    Field f;

    try {
      // Jython 2.1
      f = PyObject.class.getField("__class__");
    }
    catch (final NoSuchFieldException e) {
      // Jython 2.2a1+
      try {
        f = PyInstance.class.getField("instclass");
      }
      catch (final NoSuchFieldException e2) {
        throw new AssertionError("Incompatible Jython release in classpath");
      }
    }

    final PyClass pyClass = (PyClass)f.get(target);

    return (Class<?>) pyClass.__tojava__(Class.class);
  }

  @Test public void testCreateProxyWithPyFunction() throws Exception {
    m_interpreter.exec("def return1(): return 1");
    final PyObject pyFunction = m_interpreter.get("return1");
    final PyObject pyFunctionProxy = (PyObject)
      createInstrumentedProxy(m_test, m_recorder, pyFunction);

    final PyObject result = pyFunctionProxy.invoke("__call__");
    assertEquals(m_one, result);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
    assertTestReference(pyFunctionProxy, m_test);
    assertTargetReference(pyFunctionProxy, pyFunction);

    m_interpreter.exec("def multiply(x, y): return x * y");
    final PyObject pyFunction2 = m_interpreter.get("multiply");
    final PyObject pyFunctionProxy2 = (PyObject)
      createInstrumentedProxy(m_test, m_recorder, pyFunction2);
    final PyObject result2 =
      pyFunctionProxy2.invoke("__call__", m_two, m_three);
    assertEquals(m_six, result2);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
    assertTargetReference(pyFunctionProxy2, pyFunction2);

    final PyObject result3 =
      pyFunctionProxy2.invoke("__call__", new PyObject[] { m_two, m_three});
    assertEquals(m_six, result3);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_interpreter.exec("def square(x): return x * x");
    final PyObject pyFunction11 = m_interpreter.get("square");
    final PyObject pyFunctionProxy11 = (PyObject)
      createInstrumentedProxy(m_test, m_recorder, pyFunction11);
    final PyObject result11 = pyFunctionProxy11.invoke("__call__", m_two);
    assertEquals(new PyInteger(4), result11);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
    assertTargetReference(pyFunctionProxy11, pyFunction11);

    // From Jython.
    m_interpreter.set("proxy", pyFunctionProxy);
    m_interpreter.set("proxy2", pyFunctionProxy2);

    m_interpreter.exec("result5 = proxy()");
    final PyObject result5 = m_interpreter.get("result5");
    assertEquals(m_one, result5);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  @Test public void testCreateProxyWithPyInstance() throws Exception {
    // PyInstance.
    m_interpreter.exec(
      "class Foo:\n" +
      " def two(self): return 2\n" +
      " def identity(self, x): return x\n" +
      " def sum(self, x, y): return x + y\n" +
      " def sum3(self, x, y, z): return x + y + z\n" +
      "x=Foo()");

    final PyObject pyInstance = m_interpreter.get("x");
    final PyObject pyInstanceProxy = (PyObject)
        createInstrumentedProxy(m_test, m_recorder, pyInstance);
    final PyObject result1 = pyInstanceProxy.invoke("two");
    assertEquals(m_two, result1);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
    assertTestReference(pyInstanceProxy, m_test);
    assertNull(pyInstanceProxy.__findattr__("__blah__"));
    assertTargetReference(pyInstanceProxy, pyInstance);

    final PyObject result2 = pyInstanceProxy.invoke("identity", m_one);
    assertSame(m_one, result2);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    final PyObject result3 = pyInstanceProxy.invoke("sum", m_one, m_two);
    assertEquals(m_three, result3);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    final PyObject result4 = pyInstanceProxy.invoke("sum3", new PyObject[] {
        m_one, m_two, m_three });
    assertEquals(m_six, result4);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    final PyObject result5 = pyInstanceProxy.invoke("sum", new PyObject[] {
        m_one, m_two }, new String[] { "x", "y" });
    assertEquals(m_three, result5);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.set("proxy", pyInstanceProxy);

    m_interpreter.exec("result6 = proxy.sum(2, 4)");
    final PyObject result6 = m_interpreter.get("result6");
    assertEquals(m_six, result6);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  @Test public void testCreateProxyWithUnboundPyMethod() throws Exception {
    m_interpreter.exec(
      "class Foo:\n" +
      " def two(self): return 2\n" +
      " def identity(self, x): return x\n" +
      " def sum(self, x, y): return x + y\n" +
      " def sum3(self, x, y, z): return x + y + z\n" +
      "x=Foo()");
    final PyObject pyInstance = m_interpreter.get("x");
    m_interpreter.exec("y=Foo.two");
    final PyObject pyMethod = m_interpreter.get("y");
    final PyObject pyMethodProxy = (PyObject)
      createInstrumentedProxy(m_test, m_recorder, pyMethod);
    final PyObject result = pyMethodProxy.invoke("__call__", pyInstance);
    assertEquals(m_two, result);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
    assertTestReference(pyMethodProxy, m_test);
    assertNull(pyMethodProxy.__findattr__("__blah__"));
    assertTargetReference(pyMethodProxy, pyMethod);

    m_interpreter.exec("y=Foo.identity");
    final PyObject pyMethod2 = m_interpreter.get("y");
    final PyObject pyMethodProxy2 = (PyObject)
      createInstrumentedProxy(m_test, m_recorder, pyMethod2);
    final PyObject result2 =
      pyMethodProxy2.invoke("__call__", pyInstance, m_one);
    assertEquals(m_one, result2);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_interpreter.exec("y=Foo.sum");
    final PyObject pyMethod3 = m_interpreter.get("y");
    final PyObject pyMethodProxy3 = (PyObject)
      createInstrumentedProxy(m_test, m_recorder, pyMethod3);
    final PyObject result3 =
      pyMethodProxy3.invoke(
        "__call__", new PyObject[] { pyInstance, m_one, m_two });
    assertEquals(m_three, result3);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.set("proxy", pyMethodProxy);

    m_interpreter.exec("result5 = proxy(x)");
    final PyObject result5 = m_interpreter.get("result5");
    assertEquals(m_two, result5);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  @Test public void testCreateProxyWithBoundPyMethod() throws Exception {
    m_interpreter.exec(
      "class Foo:\n" +
      " def two(self): return 2\n" +
      "x=Foo()\n" +
      "y=Foo()\n");

    m_interpreter.exec("z=x.two");
    final PyObject pyMethod = m_interpreter.get("z");
    final PyObject pyMethodProxy = (PyObject)
      createInstrumentedProxy(m_test, m_recorder, pyMethod);
    final PyObject result = pyMethodProxy.invoke("__call__");
    assertEquals(m_two, result);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);

    // Other instance is not instrumented.
    m_interpreter.exec("z=y.two");
    final PyObject pyMethod2 = m_interpreter.get("z");
    final PyObject result2 = pyMethod2.invoke("__call__");
    assertEquals(m_two, result2);

    m_recorderStubFactory.assertNoMoreCalls();
  }

  @Test public void testCreateProxyWithPyReflectedFunction() throws Exception {
    m_interpreter.exec("from grinder.test import MyClass\nx=MyClass(6, 5, 4)");
    final PyObject pyJava = m_interpreter.get("x");
    m_interpreter.exec("y=MyClass.getA");
    final PyObject pyJavaMethod = m_interpreter.get("y");
    final PyObject pyJavaMethodProxy = (PyObject)
        createInstrumentedProxy(m_test, m_recorder, pyJavaMethod);
    final PyObject result = pyJavaMethodProxy.__call__(pyJava);
    assertEquals(m_six, result);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
    assertTestReference(pyJavaMethodProxy, m_test);
    assertNull(pyJavaMethodProxy.__findattr__("__blah__"));
    assertTargetReference(pyJavaMethodProxy, pyJavaMethod);

    // From Jython.
    m_interpreter.set("proxy", pyJavaMethodProxy);

    m_interpreter.exec("result2 = proxy(x)");
    final PyObject result2 = m_interpreter.get("result2");
    assertEquals(m_six, result2);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  private PyObject getPyInstance(final PyProxy pyProxy) throws Exception {
    // Dynamic invocation because return type has changed in 2.5.
    return (PyObject) PyProxy.class.getMethod("_getPyInstance").invoke(pyProxy);
  }

  @Test public void testCreateProxyWithPyProxy() throws Exception {
    m_interpreter.exec("from java.util import Random");
    m_interpreter.exec(
      "class PyRandom(Random):\n" +
      " def one(self): return 1\n" +
      "x=PyRandom()");
    final PyObject pyInstance = m_interpreter.get("x");

    // PyProxy's come paired with PyInstances - need to call
    // __tojava__ to get the PyProxy.
    final PyProxy pyProxy = (PyProxy) pyInstance.__tojava__(PyProxy.class);
    final Object pyProxyProxy =
      m_instrumenter.createInstrumentedProxy(m_test,
                                             m_recorder,
                                             pyProxy);

    final PyObject pyProxyInstance =
      (pyProxyProxy instanceof PyProxy) ?
          getPyInstance((PyProxy) pyProxyProxy) : (PyObject)pyProxyProxy;

    final PyObject result = pyProxyInstance.invoke("one");
    assertEquals(m_one, result);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
    assertTestReference(pyProxyInstance, m_test);
    assertTargetReference(pyProxyInstance, pyInstance);

    // From Jython.
    m_interpreter.set("proxy", pyProxyProxy);

    m_interpreter.exec("result2 = proxy.one()");
    final PyObject result2 = m_interpreter.get("result2");
    assertEquals(m_one, result2);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_interpreter.exec("result3 = proxy.nextInt()");
    final PyObject result3 = m_interpreter.get("result3");
    assertNotNull(result3);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  @Test public void testCreateProxyWithRecursiveCode() throws Exception {
    m_interpreter.exec(
      "class Recurse:\n" +
      "  def __init__(self):\n" +
      "    self.i = 3\n" +
      "  def foo(self):\n" +
      "    self.i = self.i - 1\n" +
      "    if self.i == 0: return 0\n" +
      "    return self.i + self.foo()\n" +
      "r = Recurse()");

    final PyObject proxy = (PyObject)
      createInstrumentedProxy(m_test, m_recorder, m_interpreter.get("r"));

    final PyObject result = proxy.invoke("foo");

    assertEquals(new PyInteger(3), result);
    // The dispatcher will be called multiple times. The real dispatcher
    // only records the outer invocation.
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  @Test public void testPyDispatcherErrorHandling() throws Exception {
    m_interpreter.exec("def blah(): raise Exception('a problem')");
    final PyObject pyFunction = m_interpreter.get("blah");
    final PyObject pyFunctionProxy = (PyObject)
      createInstrumentedProxy(m_test, m_recorder, pyFunction);
    try {
      pyFunctionProxy.invoke("__call__");
      fail("Expected PyException");
    }
    catch (final PyException e) {
      AssertUtilities.assertContains(e.toString(), "a problem");
    }

    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", false);
    m_recorderStubFactory.assertNoMoreCalls();

    final UncheckedGrinderException e = new UncheckedInterruptedException(null);
    m_recorderStubFactory.setThrows("start", e);

    try {
      pyFunctionProxy.invoke("__call__");
      fail("Expected UncheckedGrinderException");
    }
    catch (final UncheckedGrinderException e2) {
      assertSame(e, e2);
    }
    catch (final PyException e3) {
      assertSame(e, e3.value.__tojava__(Exception.class));
    }
  }

  @Test public void testSelectiveInstrumentUnsupported() throws Exception {
    m_interpreter.exec(
      "class Foo:\n" +
      " def two(self): return 2\n" +
      " def identity(self, x): return x\n" +
      " def sum(self, x, y): return x + y\n" +
      " def sum3(self, x, y, z): return x + y + z\n" +
      "x=Foo()");

    final PyObject pyInstance = m_interpreter.get("x");

    final InstrumentationFilter filter = new InstrumentationFilter() {
        @Override
        public boolean matches(final Object item) {
          return true;
        }
      };

    try {
      m_instrumenter.instrument(m_test, m_recorder, pyInstance, filter);
      fail("Expected NonInstrumentableTypeException");
    }
    catch (final NonInstrumentableTypeException e) {
    }
  }
}
