// Copyright (C) 2004 - 2013 Philip Aston
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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import net.grinder.engine.common.EngineException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginThreadListener;
import net.grinder.script.Grinder.ScriptContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.Marker;


/**
 * Unit tests for {@code PluginProcessContextImplementation}.
 *
 * @author Philip Aston
 */
public class TestPluginProcessContextImplementation {

  @Mock private Logger m_logger;
  @Mock private ThreadContext m_threadContext;
  @Mock private GrinderPlugin m_plugin;
  @Mock private ScriptContext m_scriptContext;
  @Mock private PluginThreadListener m_pluginThreadListener;
  @Mock private Marker m_logMarker;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(m_threadContext.getLogMarker()).thenReturn(m_logMarker);
  }

  @Test(expected=EngineException.class)
  public void testGetPluginThreadListenerNonWorkerThread() throws Exception {

    final PluginProcessContext processContext =
      new PluginProcessContextImplementation(new StubThreadContextLocator(),
                                             m_logger);

    processContext.getPluginThreadListener(m_plugin);

    verifyNoMoreInteractions(m_plugin);
  }

  @Test public void testGetPluginThreadListenerBadCreate() throws Exception {

    final StubThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    threadContextLocator.set(m_threadContext);

    final PluginException e = new PluginException("");
    when(m_plugin.createThreadListener()).thenThrow(e);

    final PluginProcessContext processContext =
      new PluginProcessContextImplementation(threadContextLocator, m_logger);

    try {
      processContext.getPluginThreadListener(m_plugin);
      fail();
    }
    catch (final EngineException e2) {
      assertSame(e, e2.getCause());
    }

    verify(m_logger).error(eq(m_logMarker), isA(String.class), eq(e));
  }

  @Test public void testGetPluginThreadListener() throws Exception {

    final StubThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    threadContextLocator.set(m_threadContext);

    final PluginProcessContext processContext =
      new PluginProcessContextImplementation(threadContextLocator, m_logger);

    when(m_plugin.createThreadListener())
    .thenReturn(m_pluginThreadListener);

    final PluginThreadListener pluginThreadListener1 =
      processContext.getPluginThreadListener(m_plugin);

    final PluginThreadListener pluginThreadListener2 =
      processContext.getPluginThreadListener(m_plugin);

    verify(m_plugin).createThreadListener();
    verify(m_threadContext)
      .registerThreadLifeCycleListener(m_pluginThreadListener);

    assertSame(pluginThreadListener1, pluginThreadListener2);

    verifyNoMoreInteractions(m_plugin, m_logger, m_threadContext);
  }
}
