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

package net.grinder.scriptengine;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import net.grinder.script.Test.InstrumentationFilter;
import net.grinder.scriptengine.CompositeInstrumenter;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.Recorder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@link CompositeInstrumenter}.
 *
 * @author Philip Aston
 */
public class TestCompositeInstrumenter {

  @Mock private Recorder m_recorder;
  @Mock private Instrumenter m_instrumenter1;
  @Mock private Instrumenter m_instrumenter2;
  @Mock private InstrumentationFilter m_filter;
  @Mock private net.grinder.common.Test m_test;

  private Object m_target = new Object();

  @Before public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test public void testCreateInstrumentedProxy() throws Exception {

    when(m_instrumenter2.createInstrumentedProxy(m_test, m_recorder, m_target))
      .thenReturn(m_target);

    final Instrumenter instrumenter =
      new CompositeInstrumenter(m_instrumenter1, m_instrumenter2);

    instrumenter.createInstrumentedProxy(m_test, m_recorder, m_target);

    final InOrder inOrder = inOrder(m_instrumenter1, m_instrumenter2);

    inOrder.verify(m_instrumenter1)
      .createInstrumentedProxy(m_test, m_recorder, m_target);
    inOrder.verify(m_instrumenter2)
      .createInstrumentedProxy(m_test, m_recorder, m_target);

    verifyNoMoreInteractions(m_instrumenter1, m_instrumenter2);
  }

  @Test public void testCreateInstrumentedProxyWithNull() throws Exception {
    final Instrumenter instrumenter =
      new CompositeInstrumenter(asList(m_instrumenter1, m_instrumenter2));

    final Object result =
      instrumenter.createInstrumentedProxy(m_test, m_recorder, null);
    assertNull(result);
  }

  @Test public void testCreateInstrumentedProxyFailure() throws Exception {
    final Instrumenter instrumenter =
      new CompositeInstrumenter(asList(m_instrumenter1, m_instrumenter2));

    final Object result =
      instrumenter.createInstrumentedProxy(m_test, m_recorder, m_target);
    assertNull(result);
  }

  @Test public void testInstrument() throws Exception {
    when(m_instrumenter2.instrument(m_test, m_recorder, m_target))
      .thenReturn(true);

    final Instrumenter instrumenter =
      new CompositeInstrumenter(asList(m_instrumenter1, m_instrumenter2));

    instrumenter.instrument(m_test, m_recorder, m_target);

    final InOrder inOrder = inOrder(m_instrumenter1, m_instrumenter2);

    inOrder.verify(m_instrumenter1)
      .instrument(m_test, m_recorder, m_target, Instrumenter.ALL_INSTRUMENTATION);
    inOrder.verify(m_instrumenter2)
      .instrument(m_test, m_recorder, m_target, Instrumenter.ALL_INSTRUMENTATION);

    verifyNoMoreInteractions(m_instrumenter1, m_instrumenter2);
  }

  @Test public void testInstrumentWithNull() throws Exception {
    final Instrumenter instrumenter =
      new CompositeInstrumenter(asList(m_instrumenter1, m_instrumenter2));

    final boolean result =
      instrumenter.instrument(m_test, m_recorder, null, m_filter);
    assertFalse(result);
  }

  @Test public void testCreateInstrumentedFailure() throws Exception {
    final Instrumenter instrumenter =
      new CompositeInstrumenter(asList(m_instrumenter1, m_instrumenter2));

    final boolean result =
      instrumenter.instrument(m_test, m_recorder, null, m_filter);
    assertFalse(result);
  }

  @Test public void testGetDescription() throws Exception {
    when(m_instrumenter1.getDescription()).thenReturn("I1");
    when(m_instrumenter2.getDescription()).thenReturn("I2");

    assertEquals("I1; I2",
                 new CompositeInstrumenter(m_instrumenter1, m_instrumenter2)
                 .getDescription());

    assertEquals("I1",
                 new CompositeInstrumenter(m_instrumenter1).getDescription());

    assertEquals("", new CompositeInstrumenter().getDescription());

    when(m_instrumenter2.getDescription()).thenReturn(null);

    assertEquals("I1",
                 new CompositeInstrumenter(m_instrumenter1, m_instrumenter2)
                 .getDescription());
  }
}
