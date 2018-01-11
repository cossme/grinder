// Copyright (C) 2004 - 2011 Philip Aston
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import net.grinder.common.ThreadLifeCycleListener;
import net.grinder.engine.common.EngineException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginRegistry;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.util.TimeAuthority;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;


/**
 * Unit test case for <code>PluginRegistry</code>.
 *
 * @author Philip Aston
 */
public class TestPluginRegistryImplementation {

  @Mock private Logger m_logger;
  @Mock private ScriptContext m_scriptContext;
  @Mock private TimeAuthority m_timeAuthority;
  @Mock private GrinderPlugin m_grinderPlugin;
  @Captor private ArgumentCaptor<RegisteredPlugin> m_pluginCaptor;

  private final ThreadContextLocator m_threadContextLocator =
    new StubThreadContextLocator();

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test public void testConstructorAndSingleton() throws Exception {
    final PluginRegistry pluginRegistry =
      new PluginRegistryImplementation(
        m_logger, m_scriptContext, m_threadContextLocator,
        StatisticsServicesImplementation.getInstance(), m_timeAuthority);

    assertSame(pluginRegistry, PluginRegistry.getInstance());
  }

  @Test public void testRegister() throws Exception {
    final PluginRegistry pluginRegistry =
      new PluginRegistryImplementation(
        m_logger, m_scriptContext, m_threadContextLocator,
        StatisticsServicesImplementation.getInstance(), m_timeAuthority);

    pluginRegistry.register(m_grinderPlugin);

    verify(m_grinderPlugin).initialize(m_pluginCaptor.capture());

    final RegisteredPlugin registeredPlugin = m_pluginCaptor.getValue();
    assertSame(m_scriptContext, registeredPlugin.getScriptContext());
    assertSame(m_timeAuthority, registeredPlugin.getTimeAuthority());

    verify(m_logger).info(contains("registered plug-in"),
                          contains("GrinderPlugin"));

    pluginRegistry.register(m_grinderPlugin);

    verifyNoMoreInteractions(m_grinderPlugin, m_logger);
  }

  @Test public void testRegisterWithBadPlugin() throws Exception {
    final PluginRegistry pluginRegistry =
      new PluginRegistryImplementation(
        m_logger, m_scriptContext, m_threadContextLocator,
        StatisticsServicesImplementation.getInstance(), m_timeAuthority);

    final PluginException initialiseException = new PluginException("barf");

    doThrow(initialiseException)
      .when(m_grinderPlugin).initialize(m_pluginCaptor.capture());

    try {
      pluginRegistry.register(m_grinderPlugin);
      fail("Expected EngineException");
    }
    catch (EngineException e) {
      assertSame(initialiseException, e.getCause());
    }
  }

  @Test public void testListeners() throws Exception {
    final PluginRegistryImplementation pluginRegistry =
      new PluginRegistryImplementation(
        m_logger, m_scriptContext, m_threadContextLocator,
        StatisticsServicesImplementation.getInstance(), m_timeAuthority);

    final ThreadContext threadContext = mock(ThreadContext.class);

    pluginRegistry.threadCreated(threadContext);

    final ArgumentCaptor<ThreadLifeCycleListener> listenerCaptor =
      ArgumentCaptor.forClass(ThreadLifeCycleListener.class);

    verify(threadContext)
      .registerThreadLifeCycleListener(listenerCaptor.capture());

    final ThreadLifeCycleListener threadListener = listenerCaptor.getValue();

    assertNotNull(threadListener);

    threadListener.beginThread();
    threadListener.beginRun();
    threadListener.endRun();
    threadListener.endThread();

    pluginRegistry.register(m_grinderPlugin);

    verify(m_grinderPlugin).initialize(m_pluginCaptor.capture());

    threadListener.beginThread();

    verify(m_grinderPlugin).createThreadListener(threadContext);

    verify(threadContext, times(2))
      .registerThreadLifeCycleListener(any(ThreadLifeCycleListener.class));

    threadListener.beginRun();
    threadListener.endRun();
    threadListener.endThread();
    threadListener.beginShutdown();

    // No-op.
    pluginRegistry.threadStarted(threadContext);

    verifyNoMoreInteractions(threadContext);
  }
}
