// Copyright (C) 2008 - 2012 Philip Aston
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

package net.grinder.plugin.http;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URLClassLoader;
import java.util.Collections;

import net.grinder.common.GrinderException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginRegistry;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.plugininterface.PluginThreadListener;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.script.Statistics;
import net.grinder.util.BlockingClassLoader;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@link HTTPPlugin}.
 *
 * @author Philip Aston
 */
public class TestHTTPPlugin {

  @Mock private PluginProcessContext m_pluginProcessContext;
  @Mock private ScriptContext m_scriptContext;
  @Mock private Statistics m_statistics;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(m_pluginProcessContext.getScriptContext()).thenReturn(m_scriptContext);
    when(m_scriptContext.getStatistics()).thenReturn(m_statistics);

    new PluginRegistry() {
      {
        setInstance(this);
      }

      @Override
      public void register(final GrinderPlugin plugin) throws GrinderException {
        plugin.initialize(m_pluginProcessContext);
      }
    };
  }

  @Test public void testInitialiseWithBadHTTPClient() throws Exception {

    final String pluginName = HTTPPlugin.class.getName();

    final URLClassLoader blockingLoader =
      new BlockingClassLoader(singleton("HTTPClient.RetryModule"),
                              singleton(pluginName),
                              Collections.<String>emptySet(),
                              false);

    new PluginRegistry() {
      {
        setInstance(this);
      }

      public void register(GrinderPlugin plugin) throws GrinderException {
        plugin.initialize(m_pluginProcessContext);
      }
    };

    try {
      Class.forName(pluginName, true, blockingLoader);
      fail("Expected PluginException");
    }
    catch (ExceptionInInitializerError e) {
      // EIIE ->  PluginException -> ClassNotFoundException
      assertTrue(e.getCause().getCause() instanceof ClassNotFoundException);
    }
  }

  @Test public void testInitializeWithBadStatistics() throws Exception {

    final GrinderException grinderException = new GrinderException("Hello") {};

    doThrow(grinderException).when(m_statistics)
      .registerDataLogExpression(isA(String.class), isA(String.class));

    final HTTPPlugin plugin = new HTTPPlugin();

    try {
      plugin.initialize(m_pluginProcessContext);
      fail("Expected PluginException");
    }
    catch (PluginException e) {
      assertSame(grinderException, e.getCause());
    }
  }

  @Test public void testCreateThreadListener() throws Exception {
    final HTTPPlugin plugin = new HTTPPlugin();

    plugin.initialize(m_pluginProcessContext);

    assertSame(m_pluginProcessContext, plugin.getPluginProcessContext());

    final PluginThreadContext
      pluginThreadContext = mock(PluginThreadContext.class);
    final PluginThreadListener threadListener =
      plugin.createThreadListener(pluginThreadContext);

    assertNotNull(threadListener);
  }
}
