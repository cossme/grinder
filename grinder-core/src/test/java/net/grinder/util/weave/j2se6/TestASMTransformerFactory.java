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

package net.grinder.util.weave.j2se6;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.grinder.util.weave.ClassSource;
import net.grinder.util.weave.ParameterSource;
import net.grinder.util.weave.Weaver.TargetSource;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.agent.ExposeInstrumentation;
import net.grinder.util.weave.j2se6.DCRWeaver.ClassFileTransformerFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


/**
 * Unit tests for {@link ASMTransformerFactory}.
 *
 * @author Philip Aston
 */
public class TestASMTransformerFactory {

  private final PointCuts m_pointCuts = new PointCuts();

  @Mock
  private  PointCutRegistry m_pointCutRegistry;

  @Before
  public void setUp() throws Exception {
    initMocks(this);

    when(
      m_pointCutRegistry.getConstructorPointCutsForClass(isA(String.class))
      ).thenAnswer(new Answer<Map<Constructor<?>, List<WeavingDetails>>>() {

        @Override
        public Map<Constructor<?>, List<WeavingDetails>>
          answer(final InvocationOnMock invocation) throws Throwable {
          return m_pointCuts.getConstructor((String)invocation.getArguments()[0]);
        }});

    when(
      m_pointCutRegistry.getMethodPointCutsForClass(isA(String.class))
      ).thenAnswer(new Answer<Map<Method, List<WeavingDetails>>>() {

        @Override
        public Map<Method, List<WeavingDetails>>
          answer(final InvocationOnMock invocation) throws Throwable {
          return m_pointCuts.getMethod((String)invocation.getArguments()[0]);
        }});
  }

  @After public void tearDown() throws Exception {
    verifyNoMoreInteractions(s_callRecorder);
    reset(s_callRecorder);
  }

  @Test
  public void testFactory() throws Exception {
    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    assertNotNull(transformerFactory.create(m_pointCutRegistry));
  }

  @Test
  public void testFactoryWithBadAdvice() throws Exception {
    try {
      new ASMTransformerFactory(BadAdvice1.class);
      fail("Expected WeavingException");
    }
    catch (final WeavingException e) {
      assertTrue(e.getCause() instanceof NoSuchMethodException);
    }

    try {
      new ASMTransformerFactory(BadAdvice2.class);
      fail("Expected WeavingException");
    }
    catch (final WeavingException e) {
      assertTrue(e.getCause() instanceof NoSuchMethodException);
    }

    try {
      new ASMTransformerFactory(BadAdvice3.class);
      fail("Expected WeavingException");
    }
    catch (final WeavingException e) {
    }

    try {
      new ASMTransformerFactory(BadAdvice4.class);
      fail("Expected WeavingException");
    }
    catch (final WeavingException e) {
    }
  }

  private static final Instrumentation getInstrumentation() {
    final Instrumentation instrumentation =
      ExposeInstrumentation.getInstrumentation();

    assertNotNull(
      "Instrumentation is not available, " +
      "please add -javaagent:grinder-agent.jar to the command line",
      instrumentation);

    return instrumentation;
  }

  @Test
  public void testWithAgent() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();
    assertTrue(instrumentation.isRetransformClassesSupported());

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCuts.addMethod(A.class, "m1", "loc1");
    m_pointCuts.addMethod(A.class, "m2", "loc2");
    m_pointCuts.addMethod(A.class, "m4", "loc4");

    final ClassFileTransformer transformer =
      transformerFactory.create(m_pointCutRegistry);

    final A anotherA = new A();
    anotherA.m1();
    verifyNoMoreInteractions(s_callRecorder);

    instrumentation.addTransformer(transformer, true);
    instrumentation.retransformClasses(new Class[] { A.class, A2.class });

    final A a = new A();
    assertEquals(1, a.m1());

    verify(s_callRecorder).enter(a, "loc1");
    verify(s_callRecorder).exit(a, "loc1", true);

    anotherA.m1();
    verify(s_callRecorder).enter(anotherA, "loc1");
    verify(s_callRecorder).exit(anotherA, "loc1", true);

    try {
      a.m2();
      fail("Expected RuntimeException");
    }
    catch (final RuntimeException e) {
    }

    verify(s_callRecorder).enter(a, "loc2");
    verify(s_callRecorder).exit(a, "loc2", false);

    verifyNoMoreInteractions(s_callRecorder);
    reset(s_callRecorder);

    try {
      a.m4();
      fail("Expected RuntimeException");
    }
    catch (final RuntimeException e) {
    }

    verify(s_callRecorder).enter(a, "loc4");
    verify(s_callRecorder).enter(a, "loc2");
    verify(s_callRecorder).exit(a, "loc2", false);
    verify(s_callRecorder).exit(a, "loc4", false);

    m_pointCuts.addMethod(A.class, "m1", "locX");

    instrumentation.retransformClasses(new Class[] { A.class, A2.class });

    a.m1();
    // We only support than one advice per method.
    verify(s_callRecorder).enter(a, "loc1");
    verify(s_callRecorder).enter(a, "locX");
    verify(s_callRecorder).exit(a, "locX", true);
    verify(s_callRecorder).exit(a, "loc1", true);

    instrumentation.removeTransformer(transformer);
  }

  @Test
  public void testTwoTransformations() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCuts.addMethod(A.class, "m1", "loc1");
    m_pointCuts.addMethod(A.class, "m2", "loc2");

    final ClassFileTransformer transformer1 =
      transformerFactory.create(m_pointCutRegistry);
    final ClassFileTransformer transformer2 =
      transformerFactory.create(m_pointCutRegistry);

    instrumentation.addTransformer(transformer1, true);
    instrumentation.addTransformer(transformer2, true);

    instrumentation.retransformClasses(new Class[] { A.class, });

    final A a = new A();
    assertEquals(1, a.m1());

    verify(s_callRecorder, times(2)).enter(a, "loc1");
    verify(s_callRecorder, times(2)).exit(a, "loc1", true);

    try {
      a.m2();
      fail("Expected RuntimeException");
    }
    catch (final RuntimeException e) {
    }

    verify(s_callRecorder, times(2)).enter(a, "loc2");
    verify(s_callRecorder, times(2)).exit(a, "loc2", false);

    assertTrue(instrumentation.removeTransformer(transformer2));
    assertTrue(instrumentation.removeTransformer(transformer1));
    instrumentation.retransformClasses(new Class[] { A.class, });

    instrumentation.removeTransformer(transformer1);
    instrumentation.removeTransformer(transformer2);
  }

  @Test
  public void testSerializationNotBroken() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCuts.addMethod(SerializableA.class, "m1", "loc1");

    final ClassFileTransformer transformer =
      transformerFactory.create(m_pointCutRegistry);

    final SerializableA a = new SerializableA();

    assertEquals(1, a.m1());

    final byte[] originalBytes = serialize(a);

    instrumentation.addTransformer(transformer, true);
    instrumentation.retransformClasses(new Class[] { SerializableA.class, });

    assertEquals(1, a.m1());

    verify(s_callRecorder).enter(a, "loc1");
    verify(s_callRecorder).exit(a, "loc1", true);

    final byte[] bytes = serialize(a);

    assertArrayEquals(originalBytes, bytes);

    instrumentation.removeTransformer(transformer);
  }

  @Test
  public void testConstructors() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCuts.addConstructor(
      A2.class, A2.class.getDeclaredConstructor(Integer.TYPE), "loc1");

    final ClassFileTransformer transformer =
      transformerFactory.create(m_pointCutRegistry);

    new A2(1);

    instrumentation.addTransformer(transformer, true);
    instrumentation.retransformClasses(new Class[] { A2.class, });

    new A2(1);

    verify(s_callRecorder).enter(A2.class, "loc1");
    verify(s_callRecorder).exit(A2.class, "loc1", true);

    new A3();

    m_pointCuts.addConstructor(
      A3.class, A3.class.getDeclaredConstructor(), "loc2");
    instrumentation.retransformClasses(new Class[] { A3.class, A2.class });

    new A3();

    verify(s_callRecorder).enter(A3.class, "loc2");
    verify(s_callRecorder).exit(A3.class, "loc2", true);

    instrumentation.removeTransformer(transformer);
  }

  @Test
  public void testOverloading() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCuts.addConstructor(
      A4.class, A4.class.getDeclaredConstructor(), "loc1");
    m_pointCuts.addConstructor(
      A4.class, A4.class.getDeclaredConstructor(String.class), "loc2");

    m_pointCuts.addMethod(
      A4.class, A4.class.getDeclaredMethod("m1", Integer.TYPE), "loc3");
    m_pointCuts.addMethod(
      A4.class, A4.class.getDeclaredMethod("m1", String.class), "loc4");

    final ClassFileTransformer transformer =
      transformerFactory.create(m_pointCutRegistry);

    instrumentation.addTransformer(transformer, true);
    instrumentation.retransformClasses(new Class[] { A4.class, });

    final A4 a = new A4("abc");

    verify(s_callRecorder).enter(A4.class, "loc2");
    verify(s_callRecorder).enter(A4.class, "loc1");
    verify(s_callRecorder).exit(A4.class, "loc1", true);
    verify(s_callRecorder).exit(A4.class, "loc2", true);

    a.m1(1);

    verify(s_callRecorder).enter(a, "loc3");
    verify(s_callRecorder).enter(a, "loc4");
    verify(s_callRecorder).exit(a, "loc4", true);
    verify(s_callRecorder).exit(a, "loc3", true);

    instrumentation.removeTransformer(transformer);
  }

  @Test
  public void testStaticMethods() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCuts.addMethod(
      A2.class, A2.class.getDeclaredMethod("m3"), "loc1");

    final ClassFileTransformer transformer =
      transformerFactory.create(m_pointCutRegistry);

    instrumentation.addTransformer(transformer, true);
    instrumentation.retransformClasses(new Class[] { A2.class, });

    assertEquals(3, A2.m3());

    verify(s_callRecorder).enter(A2.class, "loc1");
    verify(s_callRecorder).exit(A2.class, "loc1", true);

    instrumentation.removeTransformer(transformer);
  }

  @Test
  public void testVariedByteCode() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCuts.addConstructor(
      A5.class, A5.class.getDeclaredConstructor(Integer.TYPE), "loc1");

    m_pointCuts.addMethod(
      A5.class, A5.class.getDeclaredMethod("m1", Integer.TYPE), "loc3");
    m_pointCuts.addMethod(
      A5.class, A5.class.getDeclaredMethod("m1", String.class), "loc4");
    m_pointCuts.addMethod(
      A5.class, A5.class.getDeclaredMethod("m2"), "loc5");
    m_pointCuts.addMethod(
      A5.class, A5.class.getDeclaredMethod("m3"), "loc6");

    final ClassFileTransformer transformer =
      transformerFactory.create(m_pointCutRegistry);

    instrumentation.addTransformer(transformer, true);
    instrumentation.retransformClasses(new Class[] { A5.class, });

    final A5 a = new A5(10);

    verify(s_callRecorder).enter(A5.class, "loc1");
    verify(s_callRecorder).exit(A5.class, "loc1", true);

    assertEquals(-11d, a.m1(1), 0.01d);

    verify(s_callRecorder).enter(a, "loc3");
    verify(s_callRecorder).enter(a, "loc4");
    verify(s_callRecorder).exit(a, "loc4", true);
    verify(s_callRecorder).exit(a, "loc3", true);

    instrumentation.removeTransformer(transformer);
  }

  @Test
  public void testTargetSource() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCuts.addMethod(
      A4.class,
      A4.class.getDeclaredMethod("m1", String.class),
      "X",
      ParameterSource.SECOND_PARAMETER);

    final ClassFileTransformer transformer =
      transformerFactory.create(m_pointCutRegistry);

    instrumentation.addTransformer(transformer, true);
    instrumentation.retransformClasses(new Class[] { A4.class, });

    final A4 a = new A4();

    a.m1("Hello");

    verify(s_callRecorder).enter("Hello", "X");
    verify(s_callRecorder).exit("Hello", "X", true);

    m_pointCuts.addMethod(
      A4.class,
      A4.class.getDeclaredMethod("m1", String.class),
      "Y",
      ParameterSource.FIRST_PARAMETER);

    instrumentation.retransformClasses(new Class[] { A4.class, });

    a.m1("Goodbye");

    verify(s_callRecorder).enter("Goodbye", "X");
    verify(s_callRecorder).enter(a, "Y");
    verify(s_callRecorder).exit(a, "Y", true);
    verify(s_callRecorder).exit("Goodbye", "X", true);

    instrumentation.removeTransformer(transformer);
  }

  private static final byte[] serialize(final Object a) throws IOException {
    final ByteArrayOutputStream byteOutputStream =
      new ByteArrayOutputStream();

    final ObjectOutputStream objectOutputStream =
      new ObjectOutputStream(byteOutputStream);

    objectOutputStream.writeObject(a);
    objectOutputStream.close();

    return byteOutputStream.toByteArray();
  }

  public static final class A {
    public int m1() {
      return 1;
    }

    private void m2() {
      throw new RuntimeException("Test");
    }

    public static int m3() {
      return 2;
    }

    public void m4() {
      m2();
    }
  }

  public static final class A2 {
    public A2(final int x) {
    }

    protected int m1() {
      return 1;
    }

    public void m2() {
    }

    public static int m3() {
      return 3;
    }
  }

  public static final class A3 {
  }

  public static final class A4 {
    private A4() {
    }

    protected A4(final String a) {
      this();
    }

    public void m1(final int a) {
      m1(Integer.toString(a));
    }

    private void m1(final String a) {
    }
  }

  public static final class A5 {
    private float m_y;

    private A5(int x) {
      ++x;

      switch (x) {
        case 99:
          m_y = x;
          break;
        default:
          m_y = -x;
      }
    }

    public double m1(final int a) {
      final int[][] x = new int[3][2];

      // Hacked at this until the compile generated LOOKUPSWITCH
      // instead of TABLESWITCH.
      switch (a) {
        case 1: return m1(Integer.toString(a));
        case 2: return 1;
        case 30: return 1;
        case 4: return 1;
        case -1: return 1;
        default: return x[0][0];
      }
    }

    private float m1(final String a) {
      return m_y;
    }

    long m2() {
      return 2;
    }

    Object m3() {
      return this;
    }
  }

  public static final class SerializableA implements Serializable {
    public int m1() {
      return 1;
    }
  }

  private static interface CallRecorderI {

    void enter(
      final Object reference,
      final String location);

    void enter(
      final Object reference,
      final Object reference2,
      final String location);

    void exit(
      final Object reference,
      final String location,
      final boolean success);

    void exit(
      final Object reference,
      final Object reference2,
      final String location,
      final boolean success);
  }

  private static final CallRecorderI s_callRecorder =
      Mockito.mock(CallRecorderI.class);

  public static final class MyAdvice {
    public static void enter(final Object reference, final String location) {
      s_callRecorder.enter(reference, location);
    }

    public static void enter(final Object reference,
                             final Object reference2,
                             final String location) {
      s_callRecorder.enter(reference, reference2, location);
    }

    public static void exit(final Object reference,
                            final String location,
                            final boolean success) {

      s_callRecorder.exit(reference, location, success);
    }

    public static void exit(final Object reference,
                            final Object reference2,
                            final String location,
                            final boolean success) {

      s_callRecorder.exit(reference, reference2, location, success);
    }
  }

  public static final class BadAdvice1 {
  }

  public static final class BadAdvice2 {
    public static void enter(final Object reference, final String location) { }

    public static void exit(final Object reference, final String location) { }
  }

  public static final class BadAdvice3 {
    public static void enter(final Object reference, final String location) { }

    public void enter(final Object reference,
                      final Object reference2,
                      final String location) { }

    public static void exit(final Object reference,
                            final String location,
                            final boolean success) { }

    public static void exit(final Object reference,
                            final Object reference2,
                            final String location,
                            final boolean success) { }
  }

  public static final class BadAdvice4 {
    public static void enter(final Object reference, final String location) { }

    public void exit(
      final Object reference,
      final String location,
      final boolean success) { }
  }

  private static final class PointCuts {

    private final Map<String, Map<Constructor<?>, List<WeavingDetails>>>
      m_constructors =
        new HashMap<String, Map<Constructor<?>, List<WeavingDetails>>>();

    private final Map<String, Map<Method, List<WeavingDetails>>> m_methods =
      new HashMap<String, Map<Method, List<WeavingDetails>>>();


    public Map<Constructor<?>, List<WeavingDetails>>
      getConstructor(final String key) {
      return m_constructors.get(key);
    }

    public Map<Method, List<WeavingDetails>> getMethod(final String key) {
      return m_methods.get(key);
    }

    public void addConstructor(final Class<?> theClass,
                               final Constructor<?> constructor,
                               final String location) {
      addMember(theClass, constructor, location, m_constructors, null);
    }

    /**
     * Convenience for methods that have no parameters.
     */
    public void addMethod(final Class<?> theClass,
                          final String methodName,
                          final String location)
      throws SecurityException, NoSuchMethodException {

      addMethod(theClass,
                theClass.getDeclaredMethod(methodName),
                location);
    }

    public void addMethod(final Class<?> theClass,
                          final Method method,
                          final String location) {
      addMethod(theClass, method, location, null);
    }

    public void addMethod(final Class<?> theClass,
                          final Method method,
                          final String location,
                          final TargetSource source) {
      addMember(theClass, method, location, m_methods, source);
    }

    public <T extends Member> void addMember(
      final Class<?> theClass,
      final T member,
      final String location,
      final Map<String, Map<T, List<WeavingDetails>>> members,
      TargetSource source) {

      final String internalClassName = theClass.getName().replace('.', '/');

      final Map<T, List<WeavingDetails>> forClass;

      final Map<T, List<WeavingDetails>> existing =
        members.get(internalClassName);

      if (existing != null) {
        forClass = existing;
      }
      else {
        forClass = new HashMap<T, List<WeavingDetails>>();
        members.put(internalClassName, forClass);
      }

      if (source == null) {

        if (Modifier.isStatic(member.getModifiers()) ||
            member instanceof Constructor<?>) {
          source = ClassSource.CLASS;
        }
        else {
          source = ParameterSource.FIRST_PARAMETER;
        }
      }

      getList(forClass, member).add(
        new WeavingDetails(location,
                           Collections.<TargetSource>singletonList(source)));
    }
  }

  private static <K, V> List<V> getList(
    final Map<K, List<V>> map,
    final K key) {

    final List<V> existing = map.get(key);

    if (existing == null) {
      final List<V> list = new ArrayList<V>();
      map.put(key, list);
      return list;
    }
    else {
      return existing;
    }
  }
}
