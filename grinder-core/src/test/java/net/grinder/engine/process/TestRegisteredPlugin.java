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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import net.grinder.common.ThreadLifeCycleListener;
import net.grinder.engine.common.EngineException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.plugininterface.PluginThreadListener;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.TimeAuthority;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;


/**
 * Unit test case for {@code RegisteredPlugin}.
 *
 * @author Philip Aston
 */
public class TestRegisteredPlugin {

  @Mock private Logger m_logger;
  @Mock private ThreadContext m_threadContext;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test public void testConstructorAndSimpleAccessors() throws Exception {

    final RandomStubFactory<GrinderPlugin> pluginStubFactory =
      RandomStubFactory.create(GrinderPlugin.class);
    final GrinderPlugin plugin = pluginStubFactory.getStub();

    final RandomStubFactory<ScriptContext> scriptContextStubFactory =
      RandomStubFactory.create(ScriptContext.class);
    final ScriptContext scriptContext =
      scriptContextStubFactory.getStub();

    final RandomStubFactory<TimeAuthority> timeAuthorityStubFactory =
      RandomStubFactory.create(TimeAuthority.class);
    final TimeAuthority timeAuthority =
      timeAuthorityStubFactory.getStub();

    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    final RegisteredPlugin registeredPlugin =
      new RegisteredPlugin(plugin,
                           scriptContext,
                           threadContextLocator,
                           StatisticsServicesImplementation.getInstance(),
                           timeAuthority,
                           m_logger);

    assertSame(scriptContext, registeredPlugin.getScriptContext());
    assertSame(StatisticsServicesImplementation.getInstance(),
               registeredPlugin.getStatisticsServices());
    assertSame(timeAuthority, registeredPlugin.getTimeAuthority());
  }

  @Test public void testGetPluginThreadListener() throws Exception {

    final GrinderPluginStubFactory grinderPluginStubFactory =
      new GrinderPluginStubFactory();
    final GrinderPlugin grinderPlugin =
      grinderPluginStubFactory.getStub();

    final RandomStubFactory<ScriptContext> scriptContextStubFactory =
      RandomStubFactory.create(ScriptContext.class);

    final RandomStubFactory<TimeAuthority> timeAuthorityStubFactory =
      RandomStubFactory.create(TimeAuthority.class);

    final StubThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    final RegisteredPlugin registeredPlugin =
      new RegisteredPlugin(grinderPlugin,
                           scriptContextStubFactory.getStub(),
                           threadContextLocator,
                           StatisticsServicesImplementation.getInstance(),
                           timeAuthorityStubFactory.getStub(),
                           m_logger);

    try {
      registeredPlugin.getPluginThreadListener();
      fail("Expected EngineException");
    }
    catch (EngineException e) {
    }

    threadContextLocator.set(m_threadContext);

    grinderPluginStubFactory.setThrowExceptionFromCreateThreadListener(true);

    try {
      registeredPlugin.getPluginThreadListener();
      fail("Expected EngineException");
    }
    catch (EngineException e) {
    }

    verify(m_threadContext).getLogMarker();

    grinderPluginStubFactory.assertException("createThreadListener",
                                             PluginException.class,
                                             PluginThreadContext.class);

    grinderPluginStubFactory.assertNoMoreCalls();

    grinderPluginStubFactory.setThrowExceptionFromCreateThreadListener(false);

    final PluginThreadListener pluginThreadListener1 =
      registeredPlugin.getPluginThreadListener();

    grinderPluginStubFactory.assertSuccess(
      "createThreadListener", PluginThreadContext.class);

    final PluginThreadListener pluginThreadListener2 =
      registeredPlugin.getPluginThreadListener();

    verify(m_threadContext)
      .registerThreadLifeCycleListener(isA(ThreadLifeCycleListener.class));

    grinderPluginStubFactory.assertNoMoreCalls();

    assertSame(pluginThreadListener1, pluginThreadListener2);

    final PluginThreadListener pluginThreadListener3 =
      registeredPlugin.createPluginThreadListener(m_threadContext);

    assertSame(pluginThreadListener1, pluginThreadListener3);

    verifyNoMoreInteractions(m_threadContext);
  }

  public static class GrinderPluginStubFactory
    extends RandomStubFactory<GrinderPlugin> {
    private boolean m_throwExceptionFromCreateThreadListener;
    private final GrinderPlugin m_delegateStub;

    public GrinderPluginStubFactory() {
      super(GrinderPlugin.class);
      m_delegateStub = RandomStubFactory.create(GrinderPlugin.class).getStub();
    }

    void setThrowExceptionFromCreateThreadListener(boolean b) {
      m_throwExceptionFromCreateThreadListener = b;
    }

    public PluginThreadListener override_createThreadListener(
      Object proxy, PluginThreadContext pluginThreadContext)
      throws PluginException {

      if (m_throwExceptionFromCreateThreadListener) {
        throw new PluginException("Eat me");
      }

      return m_delegateStub.createThreadListener(pluginThreadContext);
    }
  }
}
