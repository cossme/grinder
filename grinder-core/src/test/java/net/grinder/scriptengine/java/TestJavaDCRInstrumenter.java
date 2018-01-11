// Copyright (C) 2009 - 2014 Philip Aston
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
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import grinder.test.MyClass;
import grinder.test.MyClass.IOOperation;
import grinder.test.MyExtendedClass;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;

import net.grinder.engine.process.dcr.AnotherClass;
import net.grinder.engine.process.dcr.DCRContextImplementation;
import net.grinder.engine.process.dcr.RecorderLocatorAccess;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.script.Test.InstrumentationFilter;
import net.grinder.scriptengine.Recorder;
import net.grinder.util.BlockingClassLoader;
import net.grinder.util.weave.agent.ExposeInstrumentation;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;



/**
 * Unit tests for {@link JavaDCRInstrumenter}.
 *
 * @author Philip Aston
 */
public class TestJavaDCRInstrumenter {

  @Mock private Logger m_logger;
  @Mock private Recorder m_recorder;

  private JavaDCRInstrumenter m_instrumenter;
  private Instrumentation m_originalInstrumentation;

  @BeforeClass public static void beforeClass() {
    RecorderLocatorAccess.clearRecorders();
  }

  @Before public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    m_instrumenter =
      new JavaDCRInstrumenter(DCRContextImplementation.create(m_logger));
    m_originalInstrumentation = ExposeInstrumentation.getInstrumentation();
  }

  @After public void tearDown() throws Exception {
    ExposeInstrumentation.premain("", m_originalInstrumentation);
    RecorderLocatorAccess.clearRecorders();
  }

  private void assertNotWrappable(final Object o) throws Exception {
    try {
      m_instrumenter.createInstrumentedProxy(null, null, o);
      fail("Expected NotWrappableTypeException");
    }
    catch (final NotWrappableTypeException e) {
    }
  }

  @Test public void testGetDescription() throws Exception {
    assertTrue(m_instrumenter.getDescription().length() > 0);
  }

  @Test public void testCreateProxyWithNonWrappableParameters() throws Exception {

    assertNotWrappable(Object.class);
    assertNotWrappable(new Object());
    assertNotWrappable(new String());
    assertNotWrappable(java.util.Random.class);

    // Can't wrap classes in net.grinder.engine.process.*
    assertNotWrappable(new AnotherClass());
  }

  @Test public void testInstrumentClass() throws Exception {

    assertEquals(6, MyClass.staticSix());

    final MyClass c1 = new MyClass();

    assertEquals(0, c1.getA());

    final Object result =
      m_instrumenter.createInstrumentedProxy(null, m_recorder, MyClass.class);
    assertSame(MyClass.class, result);

    MyClass.staticSix();

    verify(m_recorder).start();
    verify(m_recorder).end(true);

    assertEquals(0, c1.getA());

    final MyClass c2 = new MyClass();
    verify(m_recorder, times(3)).start();
    verify(m_recorder, times(3)).end(true);
    assertEquals(0, c1.getA());
    assertEquals(0, c2.getA());

    m_instrumenter.createInstrumentedProxy(null,
                                           m_recorder,
                                           MyExtendedClass.class);

    MyClass.staticSix();

    // Single call - instrumenting extension shouldn't instrument superclass
    // statics.
    verify(m_recorder, times(4)).start();
    verify(m_recorder, times(4)).end(true);
    verifyNoMoreInteractions(m_recorder);
  }

  @Test public void testInstrumentUnmodifiableCLass() throws Exception {

    final Instrumentation instrumentation = mock(Instrumentation.class);
    when(instrumentation.isRetransformClassesSupported()).thenReturn(true);

    ExposeInstrumentation.premain("", instrumentation);

    doThrow(new UnmodifiableClassException())
      .when(instrumentation).retransformClasses((Class<?>[]) any());

    instrumentation.retransformClasses(new Class[0]);

    // Create a new weaver to force the weaving.
    final JavaDCRInstrumenter instrumenter =
      new JavaDCRInstrumenter(DCRContextImplementation.create(m_logger));

    try {
      instrumenter.createInstrumentedProxy(null, m_recorder, MyClass.class);
      fail("Expected NotWrappableTypeException");
    }
    catch (final NotWrappableTypeException e) {
    }
  }

  @Test public void testInstrumentInstance() throws Exception {

    final MyClass c1 = new MyExtendedClass();

    assertEquals(0, c1.getA());

    final Object result =
      m_instrumenter.createInstrumentedProxy(null, m_recorder, c1);
    assertSame(c1, result);

    MyClass.staticSix();

    assertEquals(0, c1.getA());
    verify(m_recorder).start();
    verify(m_recorder).end(true);

    final MyClass c2 = new MyClass();
    assertEquals(0, c2.getA());

    verifyNoMoreInteractions(m_recorder);
  }

  @Test public void testSelectivelyInstrumentInstance() throws Exception {

    final MyClass c1 = new MyExtendedClass();

    assertEquals(0, c1.getA());

    final InstrumentationFilter filter =
      new InstrumentationFilter() {
        @Override
        public boolean matches(final Object item) {
          return ((Method)item).getName().equals("getA");
        }
    };

    m_instrumenter.instrument(null, m_recorder, c1, filter);

    MyClass.staticSix();

    assertEquals(0, c1.getA());
    assertEquals(0, c1.getB());

    verify(m_recorder).start();
    verify(m_recorder).end(true);

    final MyClass c2 = new MyClass();
    assertEquals(0, c2.getA());

    verifyNoMoreInteractions(m_recorder);
  }


  @Test public void testWithNull() throws Exception {
    assertNull(m_instrumenter.createInstrumentedProxy(null, null, null));
  }

  @Test public void testArrays() throws Exception {

    try {
      m_instrumenter.createInstrumentedProxy(null, m_recorder, new MyClass[0]);
      fail("Expected NotWrappableTypeException");
    }
    catch (final NotWrappableTypeException e) {
    }

    try {
      m_instrumenter.createInstrumentedProxy(null, m_recorder, MyClass[].class);
      fail("Expected NotWrappableTypeException");
    }
    catch (final NotWrappableTypeException e) {
    }

    verifyNoMoreInteractions(m_recorder);
  }

  @Test public void testWithNoPackage() throws Exception {

    final BlockingClassLoader blockingClassLoader =
      new BlockingClassLoader(singleton(AnotherClass.class.getName()),
                              Collections.<String>emptySet(),
                              Collections.<String>emptySet(),
                              true);

    final NoPackageURLClassLoader cl =
      new NoPackageURLClassLoader(
        ((URLClassLoader)getClass().getClassLoader()).getURLs(),
        blockingClassLoader);

    final Class<?> noPackageClass = cl.loadClass(AnotherClass.class.getName());
    final Method noPackageMethod = noPackageClass.getMethod("getOne");

    assertEquals(1, noPackageMethod.invoke(null));

    final Object result =
      m_instrumenter.createInstrumentedProxy(null, m_recorder, noPackageClass);
    assertSame(noPackageClass, result);

    assertEquals(1, noPackageMethod.invoke(null));
    verify(m_recorder).start();
    verify(m_recorder).end(true);
    verifyNoMoreInteractions(m_recorder);
  }

  @Test public void testInstrumentExceptionHandling() throws Exception {

    final IOOperation op = mock(IOOperation.class);
    when(op.run()).thenThrow(new SocketTimeoutException());

    final MyClass c = new MyClass();

    assertEquals(1, c.topLevelExceptionHandler(op));

    m_instrumenter.createInstrumentedProxy(null, m_recorder, c);

    assertEquals(1, c.topLevelExceptionHandler(op));

    verify(m_recorder).start();
    verify(m_recorder).end(true);
  }

  private static class NoPackageURLClassLoader extends URLClassLoader {

    public NoPackageURLClassLoader(final URL[] urls, final ClassLoader parent) {
      super(urls, parent);
    }

    @Override
    protected Package getPackage(final String name) {
      return null;
    }
  }
}

