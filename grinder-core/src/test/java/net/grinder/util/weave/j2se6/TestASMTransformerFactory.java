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

package net.grinder.util.weave.j2se6;

import static net.grinder.testutility.AssertUtilities.assertArraysEqual;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.grinder.testutility.CallData;
import net.grinder.testutility.CallRecorder;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.weave.Weaver.TargetSource;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.agent.ExposeInstrumentation;
import net.grinder.util.weave.j2se6.DCRWeaver.ClassFileTransformerFactory;


/**
 * Unit tests for {@link ASMTransformerFactory}.
 *
 * @author Philip Aston
 */
public class TestASMTransformerFactory extends TestCase {

  private final PointCutRegistryStubFactory m_pointCutRegistryStubFactory =
    new PointCutRegistryStubFactory();
  private final PointCutRegistry m_pointCutRegistry =
    m_pointCutRegistryStubFactory.getStub();

  @Override protected void tearDown() throws Exception {
    super.tearDown();
    m_pointCutRegistryStubFactory.clear();
  }

  private static final CallRecorder s_callRecorder = new CallRecorder();

  public void testFactory() throws Exception {
    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    assertNotNull(transformerFactory.create(m_pointCutRegistry));
  }

  public void testFactoryWithBadAdvice() throws Exception {
    try {
      new ASMTransformerFactory(BadAdvice1.class);
      fail("Expected WeavingException");
    }
    catch (WeavingException e) {
      assertTrue(e.getCause() instanceof NoSuchMethodException);
    }

    try {
      new ASMTransformerFactory(BadAdvice2.class);
      fail("Expected WeavingException");
    }
    catch (WeavingException e) {
      assertTrue(e.getCause() instanceof NoSuchMethodException);
    }

    try {
      new ASMTransformerFactory(BadAdvice3.class);
      fail("Expected WeavingException");
    }
    catch (WeavingException e) {
    }

    try {
      new ASMTransformerFactory(BadAdvice4.class);
      fail("Expected WeavingException");
    }
    catch (WeavingException e) {
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

  public void testWithAgent() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();
    assertTrue(instrumentation.isRetransformClassesSupported());

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCutRegistryStubFactory.addMethod(A.class, "m1", "loc1");
    m_pointCutRegistryStubFactory.addMethod(A.class, "m2", "loc2");
    m_pointCutRegistryStubFactory.addMethod(A.class, "m4", "loc4");

    final ClassFileTransformer transformer =
      transformerFactory.create(m_pointCutRegistry);

    final A anotherA = new A();
    anotherA.m1();
    s_callRecorder.assertNoMoreCalls();

    instrumentation.addTransformer(transformer, true);
    instrumentation.retransformClasses(new Class[] { A.class, A2.class });

    final A a = new A();
    assertEquals(1, a.m1());

    s_callRecorder.assertSuccess("enter", a, "loc1");
    s_callRecorder.assertSuccess("exit", a, "loc1", true);
    s_callRecorder.assertNoMoreCalls();

    anotherA.m1();
    s_callRecorder.assertSuccess("enter", anotherA, "loc1");
    s_callRecorder.assertSuccess("exit", anotherA, "loc1", true);
    s_callRecorder.assertNoMoreCalls();

    try {
      a.m2();
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
    }

    s_callRecorder.assertSuccess("enter", a, "loc2");
    s_callRecorder.assertSuccess("exit", a, "loc2", false);
    s_callRecorder.assertNoMoreCalls();

    try {
      a.m4();
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
    }

    s_callRecorder.assertSuccess("enter", a, "loc4");
    s_callRecorder.assertSuccess("enter", a, "loc2");
    s_callRecorder.assertSuccess("exit", a, "loc2", false);
    s_callRecorder.assertSuccess("exit", a, "loc4", false);
    s_callRecorder.assertNoMoreCalls();

    m_pointCutRegistryStubFactory.addMethod(A.class, "m1", "locX");

    instrumentation.retransformClasses(new Class[] { A.class, A2.class });

    a.m1();
    // We only support more than one advice per method.
    s_callRecorder.assertSuccess("enter", a, "loc1");
    s_callRecorder.assertSuccess("enter", a, "locX");
    s_callRecorder.assertSuccess("exit", a, "locX", true);
    s_callRecorder.assertSuccess("exit", a, "loc1", true);
    s_callRecorder.assertNoMoreCalls();

    instrumentation.removeTransformer(transformer);
  }

  public void testTwoTransformations() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCutRegistryStubFactory.addMethod(A.class, "m1", "loc1");
    m_pointCutRegistryStubFactory.addMethod(A.class, "m2", "loc2");

    final ClassFileTransformer transformer1 =
      transformerFactory.create(m_pointCutRegistry);
    final ClassFileTransformer transformer2 =
      transformerFactory.create(m_pointCutRegistry);

    instrumentation.addTransformer(transformer1, true);
    instrumentation.addTransformer(transformer2, true);

    instrumentation.retransformClasses(new Class[] { A.class, });


    final A a = new A();
    assertEquals(1, a.m1());

    s_callRecorder.assertSuccess("enter", a, "loc1");
    s_callRecorder.assertSuccess("enter", a, "loc1");
    s_callRecorder.assertSuccess("exit", a, "loc1", true);
    s_callRecorder.assertSuccess("exit", a, "loc1", true);
    s_callRecorder.assertNoMoreCalls();

    try {
      a.m2();
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
    }

    s_callRecorder.assertSuccess("enter", a, "loc2");
    s_callRecorder.assertSuccess("enter", a, "loc2");
    s_callRecorder.assertSuccess("exit", a, "loc2", false);
    s_callRecorder.assertSuccess("exit", a, "loc2", false);
    s_callRecorder.assertNoMoreCalls();

    assertTrue(instrumentation.removeTransformer(transformer2));
    assertTrue(instrumentation.removeTransformer(transformer1));
    instrumentation.retransformClasses(new Class[] { A.class, });
    s_callRecorder.assertNoMoreCalls();

    instrumentation.removeTransformer(transformer1);
    instrumentation.removeTransformer(transformer2);
  }

  public void testSerializationNotBroken() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCutRegistryStubFactory.addMethod(SerializableA.class, "m1", "loc1");

    final ClassFileTransformer transformer =
      transformerFactory.create(m_pointCutRegistry);

    final SerializableA a = new SerializableA();

    assertEquals(1, a.m1());
    s_callRecorder.assertNoMoreCalls();

    final byte[] originalBytes = serialize(a);

    instrumentation.addTransformer(transformer, true);
    instrumentation.retransformClasses(new Class[] { SerializableA.class, });

    assertEquals(1, a.m1());

    s_callRecorder.assertSuccess("enter", a, "loc1");
    s_callRecorder.assertSuccess("exit", a, "loc1", true);
    s_callRecorder.assertNoMoreCalls();

    final byte[] bytes = serialize(a);

    assertArraysEqual(originalBytes, bytes);

    instrumentation.removeTransformer(transformer);
  }

  public void testConstructors() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCutRegistryStubFactory.addConstructor(
      A2.class, A2.class.getDeclaredConstructor(Integer.TYPE), "loc1");

    final ClassFileTransformer transformer =
      transformerFactory.create(m_pointCutRegistry);

    new A2(1);
    s_callRecorder.assertNoMoreCalls();

    instrumentation.addTransformer(transformer, true);
    instrumentation.retransformClasses(new Class[] { A2.class, });

    new A2(1);

    s_callRecorder.assertSuccess("enter", A2.class, "loc1");
    s_callRecorder.assertSuccess("exit", A2.class, "loc1", true);
    s_callRecorder.assertNoMoreCalls();

    new A3();
    s_callRecorder.assertNoMoreCalls();

    m_pointCutRegistryStubFactory.addConstructor(
      A3.class, A3.class.getDeclaredConstructor(), "loc2");
    instrumentation.retransformClasses(new Class[] { A3.class, A2.class });

    new A3();

    s_callRecorder.assertSuccess("enter", A3.class, "loc2");
    s_callRecorder.assertSuccess("exit", A3.class, "loc2", true);
    s_callRecorder.assertNoMoreCalls();

    instrumentation.removeTransformer(transformer);
  }

  public void testOverloading() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCutRegistryStubFactory.addConstructor(
      A4.class, A4.class.getDeclaredConstructor(), "loc1");
    m_pointCutRegistryStubFactory.addConstructor(
      A4.class, A4.class.getDeclaredConstructor(String.class), "loc2");

    m_pointCutRegistryStubFactory.addMethod(
      A4.class, A4.class.getDeclaredMethod("m1", Integer.TYPE), "loc3");
    m_pointCutRegistryStubFactory.addMethod(
      A4.class, A4.class.getDeclaredMethod("m1", String.class), "loc4");

    final ClassFileTransformer transformer =
      transformerFactory.create(m_pointCutRegistry);

    instrumentation.addTransformer(transformer, true);
    instrumentation.retransformClasses(new Class[] { A4.class, });

    final A4 a = new A4("abc");

    s_callRecorder.assertSuccess("enter", A4.class, "loc2");
    s_callRecorder.assertSuccess("enter", A4.class, "loc1");
    s_callRecorder.assertSuccess("exit", A4.class, "loc1", true);
    s_callRecorder.assertSuccess("exit", A4.class, "loc2", true);
    s_callRecorder.assertNoMoreCalls();

    a.m1(1);

    s_callRecorder.assertSuccess("enter", a, "loc3");
    s_callRecorder.assertSuccess("enter", a, "loc4");
    s_callRecorder.assertSuccess("exit", a, "loc4", true);
    s_callRecorder.assertSuccess("exit", a, "loc3", true);
    s_callRecorder.assertNoMoreCalls();

    instrumentation.removeTransformer(transformer);
  }

  public void testStaticMethods() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCutRegistryStubFactory.addMethod(
      A2.class, A2.class.getDeclaredMethod("m3"), "loc1");

    final ClassFileTransformer transformer =
      transformerFactory.create(m_pointCutRegistry);

    instrumentation.addTransformer(transformer, true);
    instrumentation.retransformClasses(new Class[] { A2.class, });

    assertEquals(3, A2.m3());

    s_callRecorder.assertSuccess("enter", A2.class, "loc1");
    s_callRecorder.assertSuccess("exit", A2.class, "loc1", true);
    s_callRecorder.assertNoMoreCalls();

    instrumentation.removeTransformer(transformer);
  }

  public void testVariedByteCode() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCutRegistryStubFactory.addConstructor(
      A5.class, A5.class.getDeclaredConstructor(Integer.TYPE), "loc1");

    m_pointCutRegistryStubFactory.addMethod(
      A5.class, A5.class.getDeclaredMethod("m1", Integer.TYPE), "loc3");
    m_pointCutRegistryStubFactory.addMethod(
      A5.class, A5.class.getDeclaredMethod("m1", String.class), "loc4");
    m_pointCutRegistryStubFactory.addMethod(
      A5.class, A5.class.getDeclaredMethod("m2"), "loc5");
    m_pointCutRegistryStubFactory.addMethod(
      A5.class, A5.class.getDeclaredMethod("m3"), "loc6");

    final ClassFileTransformer transformer =
      transformerFactory.create(m_pointCutRegistry);

    instrumentation.addTransformer(transformer, true);
    instrumentation.retransformClasses(new Class[] { A5.class, });

    final A5 a = new A5(10);

    s_callRecorder.assertSuccess("enter", A5.class, "loc1");
    s_callRecorder.assertSuccess("exit", A5.class, "loc1", true);
    s_callRecorder.assertNoMoreCalls();

    assertEquals(-11d, a.m1(1), 0.01d);

    s_callRecorder.assertSuccess("enter", a, "loc3");
    s_callRecorder.assertSuccess("enter", a, "loc4");
    s_callRecorder.assertSuccess("exit", a, "loc4", true);
    s_callRecorder.assertSuccess("exit", a, "loc3", true);
    s_callRecorder.assertNoMoreCalls();

    instrumentation.removeTransformer(transformer);
  }

  public void testTargetSource() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCutRegistryStubFactory.addMethod(
      A4.class,
      A4.class.getDeclaredMethod("m1", String.class),
      "X",
      TargetSource.SECOND_PARAMETER);

    final ClassFileTransformer transformer =
      transformerFactory.create(m_pointCutRegistry);

    instrumentation.addTransformer(transformer, true);
    instrumentation.retransformClasses(new Class[] { A4.class, });

    final A4 a = new A4();

    a.m1("Hello");

    s_callRecorder.assertSuccess("enter", "Hello", "X");
    s_callRecorder.assertSuccess("exit", "Hello", "X", true);
    s_callRecorder.assertNoMoreCalls();


    m_pointCutRegistryStubFactory.addMethod(
      A4.class,
      A4.class.getDeclaredMethod("m1", String.class),
      "Y",
      TargetSource.FIRST_PARAMETER);

    instrumentation.retransformClasses(new Class[] { A4.class, });

    a.m1("Goodbye");

    s_callRecorder.assertSuccess("enter", "Goodbye", "X");
    s_callRecorder.assertSuccess("enter", a, "Y");
    s_callRecorder.assertSuccess("exit", a, "Y", true);
    s_callRecorder.assertSuccess("exit", "Goodbye", "X", true);
    s_callRecorder.assertNoMoreCalls();

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
    public A2(int x) {
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

    protected A4(String a) {
      this();
    }

    public void m1(int a) {
      m1(Integer.toString(a));
    }

    private void m1(String a) {
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

    public double m1(int a) {
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

    private float m1(String a) {
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

  public static final class MyAdvice {
    private static final Method ENTER_METHOD;
    private static final Method EXIT_METHOD;

    static {
      try {
        ENTER_METHOD = MyAdvice.class.getMethod(
          "enter", Object.class, String.class);
        EXIT_METHOD = MyAdvice.class.getMethod(
          "exit", Object.class, String.class, Boolean.TYPE);
      }
      catch (Exception e) {
        throw new ExceptionInInitializerError(e);
      }
    }

    public static void enter(Object reference, String location) {
      s_callRecorder.record(new CallData(ENTER_METHOD,
                                         null,
                                         reference,
                                         location));
    }

    public static void exit(Object reference,
                            String location,
                            boolean success) {

      s_callRecorder.record(new CallData(EXIT_METHOD,
                                         null,
                                         reference,
                                         location,
                                         success));
    }
  }

  public static final class BadAdvice1 {
  }

  public static final class BadAdvice2 {
    public static void enter(Object reference, String location) { }

    public static void exit(Object reference, String location) { }
  }

  public static final class BadAdvice3 {
    public void enter(Object reference, String location) { }

    public static void exit(Object reference,
                            String location,
                            boolean success) { }
  }

  public static final class BadAdvice4 {
    public static void enter(Object reference, String location) { }

    public void exit(Object reference, String location, boolean success) { }
  }

  public static final class PointCutRegistryStubFactory
    extends RandomStubFactory<PointCutRegistry> {

    private final Map<String, Map<Constructor<?>, List<WeavingDetails>>>
      m_constructors =
        new HashMap<String, Map<Constructor<?>, List<WeavingDetails>>>();

    private final Map<String, Map<Method, List<WeavingDetails>>> m_methods =
      new HashMap<String, Map<Method, List<WeavingDetails>>>();

    protected PointCutRegistryStubFactory() {
      super(PointCutRegistry.class);
    }

    public void clear() {
      m_constructors.clear();
      m_methods.clear();
    }

    public Map<Constructor<?>, List<WeavingDetails>>
      override_getConstructorPointCutsForClass(Object stub, String className) {
      return m_constructors.get(className);
    }

    public Map<Method, List<WeavingDetails>>
      override_getMethodPointCutsForClass(Object stub, String className) {
      return m_methods.get(className);
    }

    public void addConstructor(Class<?> theClass,
                               Constructor<?> constructor,
                               String location) {
      addMember(theClass, constructor, location, m_constructors, null);
    }

    /**
     * Convenience for methods that have no parameters.
     */
    public void addMethod(Class<?> theClass,
                          String methodName,
                          String location)
      throws SecurityException, NoSuchMethodException {

      addMethod(theClass,
                theClass.getDeclaredMethod(methodName),
                location);
    }

    public void addMethod(Class<?> theClass,
                          Method method,
                          String location) {
      addMethod(theClass, method, location, null);
    }

    public void addMethod(Class<?> theClass,
                          Method method,
                          String location,
                          TargetSource source) {
      addMember(theClass, method, location, m_methods, source);
    }

    public <T extends Member> void addMember(
      Class<?> theClass,
      T member,
      String location,
      Map<String, Map<T, List<WeavingDetails>>> members,
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
          source = TargetSource.CLASS;
        }
        else {
          source = TargetSource.FIRST_PARAMETER;
        }
      }

      getList(forClass, member).add(new WeavingDetails(location, source));
    }
  }

  private static <K, V> List<V> getList(Map<K, List<V>> map, K key) {

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
