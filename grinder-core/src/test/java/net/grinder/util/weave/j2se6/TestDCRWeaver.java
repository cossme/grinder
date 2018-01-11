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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.grinder.util.weave.ClassSource;
import net.grinder.util.weave.ParameterSource;
import net.grinder.util.weave.Weaver;
import net.grinder.util.weave.Weaver.TargetSource;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.j2se6.DCRWeaver.ClassFileTransformerFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit test for {@link DCRWeaver}.
 * TestDCRWeaver.
 *
 * @author Philip Aston
 */
public class TestDCRWeaver {

  @Mock private ClassFileTransformerFactory m_classFileTransformerFactory;
  @Mock private ClassFileTransformer m_transformer;
  @Mock private Instrumentation m_instrumentation;
  @Captor private ArgumentCaptor<PointCutRegistry> m_pointCutRegistryCaptor;

  @SuppressWarnings("unused")
  private void myMethod() {
  }

  @SuppressWarnings("unused")
  private void myOtherMethod() {
  }

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(m_classFileTransformerFactory.create(isA(PointCutRegistry.class)))
      .thenReturn(m_transformer);
  }

  @Test public void testMethodRegistration() throws Exception {
    final Weaver weaver = new DCRWeaver(m_classFileTransformerFactory,
                                        m_instrumentation);

    verify(m_classFileTransformerFactory)
      .create(m_pointCutRegistryCaptor.capture());
    verify(m_instrumentation).addTransformer(m_transformer, true);

    final Method method = getClass().getDeclaredMethod("myMethod");

    final String l1 = weaver.weave(method, ParameterSource.FIRST_PARAMETER);
    final String l2 = weaver.weave(method, ParameterSource.FIRST_PARAMETER);
    assertEquals(l1, l2);

    weaver.weave(method, ParameterSource.FIRST_PARAMETER);

    final PointCutRegistry pointCutRegistry =
      m_pointCutRegistryCaptor.getValue();

    final String internalClassName = getClass().getName().replace('.', '/');

    final Map<Constructor<?>, List<WeavingDetails>> constructorPointCuts =
      pointCutRegistry.getConstructorPointCutsForClass(internalClassName);

    assertNull(constructorPointCuts);

    final Map<Method, List<WeavingDetails>> methodPointCuts =
      pointCutRegistry.getMethodPointCutsForClass(internalClassName);

    assertEquals(1, methodPointCuts.size());

    final List<WeavingDetails> locations1 = methodPointCuts.get(method);
    assertEquals(1, locations1.size());
    final String location1 = locations1.get(0).getLocation();
    assertNotNull(location1);

    final Method method2 = getClass().getDeclaredMethod("myOtherMethod");

    weaver.weave(method, ParameterSource.FIRST_PARAMETER);
    weaver.weave(method2, ParameterSource.FIRST_PARAMETER);

    final Map<Method, List<WeavingDetails>> pointCuts2 =
      pointCutRegistry.getMethodPointCutsForClass(internalClassName);

    assertEquals(2, pointCuts2.size());

    final List<WeavingDetails> locations2 = pointCuts2.get(method);
    assertEquals(1, locations2.size());

    assertEquals(new WeavingDetails(location1,
                                    Collections.<TargetSource>singletonList(
                                      ParameterSource.FIRST_PARAMETER)),
                 locations2.get(0));
    assertNotNull(pointCuts2.get(method2));

    verifyNoMoreInteractions(m_classFileTransformerFactory, m_instrumentation);
  }

  @Test public void testConstructorRegistration() throws Exception {
    final Weaver weaver = new DCRWeaver(m_classFileTransformerFactory,
                                        m_instrumentation);


    verify(m_classFileTransformerFactory)
      .create(m_pointCutRegistryCaptor.capture());

    final Constructor<?> constructor = getClass().getDeclaredConstructor();

    final String l1 = weaver.weave(constructor);
    final String l2 = weaver.weave(constructor);
    assertEquals(l1, l2);

    weaver.weave(constructor);

    final PointCutRegistry pointCutRegistry =
      m_pointCutRegistryCaptor.getValue();

    final String internalClassName = getClass().getName().replace('.', '/');

    final Map<Constructor<?>, List<WeavingDetails>> constructorPointCuts =
      pointCutRegistry.getConstructorPointCutsForClass(internalClassName);

    assertEquals(1, constructorPointCuts.size());

    final Map<Method, List<WeavingDetails>> methodPointCuts =
      pointCutRegistry.getMethodPointCutsForClass(internalClassName);

    assertNull(methodPointCuts);

    final List<WeavingDetails> locations1 =
      constructorPointCuts.get(constructor);

    assertEquals(1, locations1.size());

    final String location1 = locations1.get(0).getLocation();
    assertNotNull(location1);
  }

  @Test public void testWeavingWithInstrumentation() throws Exception {
    final Weaver weaver = new DCRWeaver(m_classFileTransformerFactory,
                                        m_instrumentation);

    verify(m_classFileTransformerFactory)
      .create(m_pointCutRegistryCaptor.capture());
    verify(m_instrumentation).addTransformer(m_transformer, true);

    final Method method = getClass().getDeclaredMethod("myMethod");
    weaver.weave(method, ParameterSource.FIRST_PARAMETER);

    weaver.applyChanges();

    verify(m_instrumentation).retransformClasses(new Class[] { getClass(),});

    weaver.weave(method, ParameterSource.FIRST_PARAMETER);
    weaver.applyChanges();

    verifyNoMoreInteractions(m_classFileTransformerFactory,
                             m_instrumentation);
  }

  // Cover case where location key is different.
  @Test public void testWeavingWithInstrumentation2() throws Exception {
    final Weaver weaver = new DCRWeaver(m_classFileTransformerFactory,
                                        m_instrumentation);

    verify(m_classFileTransformerFactory)
      .create(m_pointCutRegistryCaptor.capture());
    verify(m_instrumentation).addTransformer(m_transformer, true);

    final Method method = getClass().getDeclaredMethod("myMethod");

    weaver.weave(method, ParameterSource.FIRST_PARAMETER);
    weaver.applyChanges();
    weaver.weave(method, ClassSource.CLASS);
    weaver.applyChanges();

    verify(m_instrumentation, times(2))
      .retransformClasses(new Class[] { getClass(),});

    verifyNoMoreInteractions(m_classFileTransformerFactory,
                             m_instrumentation);
  }

  @Test public void testWeavingWithBadInstrumentation() throws Exception {
    final Weaver weaver = new DCRWeaver(m_classFileTransformerFactory,
                                        m_instrumentation);

    final Method method = getClass().getDeclaredMethod("myMethod");

    weaver.weave(method, ParameterSource.FIRST_PARAMETER);
    weaver.weave(method, ParameterSource.FIRST_PARAMETER);

    final Exception uce = new UnmodifiableClassException();

    doThrow(uce)
      .when(m_instrumentation).retransformClasses(isA(Class.class));

    try {
      weaver.applyChanges();
      verifyNoMoreInteractions(m_instrumentation);
      fail("Expected WeavingException");
    }
    catch (final WeavingException e) {
      assertSame(e.getCause(), uce);
    }
  }

  @Test(expected=WeavingException.class)
  public void testInsufficientParameters() throws Exception {
    final Weaver weaver = new DCRWeaver(m_classFileTransformerFactory,
                                        m_instrumentation);

    final Method method = getClass().getDeclaredMethod("myMethod");

    weaver.weave(method, ParameterSource.SECOND_PARAMETER);
  }
}
