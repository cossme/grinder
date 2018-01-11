// Copyright (C) 2009 - 2011 Philip Aston
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

package net.grinder.engine.process;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import net.grinder.script.NonInstrumentableTypeException;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.script.Test.InstrumentationFilter;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.Recorder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@link MasterInstrumenter}.
 *
 * @author Philip Aston
 */
public class TestMasterInstrumenter {

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
      new MasterInstrumenter(asList(m_instrumenter1, m_instrumenter2));

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
      new MasterInstrumenter(asList(m_instrumenter1, m_instrumenter2));

    try {
      instrumenter.createInstrumentedProxy(m_test, m_recorder, null);
      fail("Expected NotWrappableTypeException");
    }
    catch (NotWrappableTypeException e) {
    }
  }

  @Test public void testCreateInstrumentedProxyFailure() throws Exception {
    final Instrumenter instrumenter =
      new MasterInstrumenter(asList(m_instrumenter1, m_instrumenter2));

    try {
      instrumenter.createInstrumentedProxy(m_test, m_recorder, m_target);
      fail("Expected NotWrappableTypeException");
    }
    catch (NotWrappableTypeException e) {
    }
  }

  @Test public void testInstrument() throws Exception {
    when(m_instrumenter2.instrument(m_test, m_recorder, m_target, m_filter))
      .thenReturn(true);

    final Instrumenter instrumenter =
      new MasterInstrumenter(asList(m_instrumenter1, m_instrumenter2));

    instrumenter.instrument(m_test, m_recorder, m_target, m_filter);

    final InOrder inOrder = inOrder(m_instrumenter1, m_instrumenter2);

    inOrder.verify(m_instrumenter1)
      .instrument(m_test, m_recorder, m_target, m_filter);
    inOrder.verify(m_instrumenter2)
      .instrument(m_test, m_recorder, m_target, m_filter);

    verifyNoMoreInteractions(m_instrumenter1, m_instrumenter2);
  }

  @Test public void testInstrumentWithNull() throws Exception {
    final Instrumenter instrumenter =
      new MasterInstrumenter(asList(m_instrumenter1, m_instrumenter2));

    try {
      instrumenter.instrument(m_test, m_recorder, null, null);
      fail("Expected NonInstrumentableTypeException");
    }
    catch (NonInstrumentableTypeException e) {
    }
  }

  @Test public void testCreateInstrumentedFailure() throws Exception {
    final Instrumenter instrumenter =
      new MasterInstrumenter(asList(m_instrumenter1, m_instrumenter2));

    try {
      instrumenter.instrument(m_test, m_recorder, m_target, null);
      fail("Expected NonInstrumentableTypeException");
    }
    catch (NonInstrumentableTypeException e) {
    }
  }

  @Test public void testGetDescription() throws Exception {
    when(m_instrumenter1.getDescription()).thenReturn("I1");
    when(m_instrumenter2.getDescription()).thenReturn("I2");

    final Instrumenter instrumenter =
      new MasterInstrumenter(asList(m_instrumenter1, m_instrumenter2));

    assertEquals("I1; I2", instrumenter.getDescription());

    final Instrumenter instrumenter2 =
      new MasterInstrumenter(asList(m_instrumenter1));

    assertEquals("I1", instrumenter2.getDescription());

    final List<Instrumenter> e = emptyList();

    assertEquals("NO INSTRUMENTER COULD BE LOADED",
                 new MasterInstrumenter(e).getDescription());
  }
}
