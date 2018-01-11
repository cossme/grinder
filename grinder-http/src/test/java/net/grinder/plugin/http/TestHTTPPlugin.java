// Copyright (C) 2008 - 2013 Philip Aston
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

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;

import net.grinder.common.GrinderException;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.PluginContainerScopeTunnel;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginThreadListener;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.script.Statistics;
import net.grinder.statistics.StatisticsServices;
import net.grinder.util.BlockingClassLoader;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;


/**
 * Unit tests for {@link HTTPPlugin}.
 *
 * @author Philip Aston
 */
public class TestHTTPPlugin {

  @Mock private PluginProcessContext m_pluginProcessContext;
  @Mock private ScriptContext m_scriptContext;
  @Mock private Statistics m_statistics;
  @Mock private Logger m_logger;
  @Mock private StatisticsServices m_statisticsServices;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(m_scriptContext.getStatistics()).thenReturn(m_statistics);
  }

  private static void usePlugin(final ClassLoader loader) throws Exception {
    // We use via HTTPRequest because we need a public method to cross the
    // classloader boundary.
    Class.forName(HTTPRequest.class.getName(), true, loader);
  }

  @Test public void testInitialiseWithBadHTTPClient() throws Exception {

    final URLClassLoader blockingLoader =
      new BlockingClassLoader(singleton("HTTPClient.RetryModule"),
                              new HashSet<String>(
                                  asList(HTTPPlugin.class.getName() + "*",
                                         HTTPRequest.class.getName() + "*")),
                              Collections.<String>emptySet(),
                              false);
    try {
      // Initialise.
      blockingLoader.loadClass(HTTPPlugin.class.getName())
        .getConstructor(PluginProcessContext.class, ScriptContext.class)
        .newInstance(m_pluginProcessContext, m_scriptContext);

      usePlugin(blockingLoader);

      fail();
    }
    catch (final ExceptionInInitializerError e) {
      // EIIE ->  PluginException -> ClassNotFoundException
      assertTrue(e.getCause().getCause() instanceof ClassNotFoundException);
    }
  }

  @Test public void testGetPluginNotInitialised() throws Exception {

    final URLClassLoader blockingLoader =
      new BlockingClassLoader(Collections.<String>emptySet(),
                              new HashSet<String>(
                                  asList(HTTPPlugin.class.getName() + "*",
                                         HTTPRequest.class.getName() + "*")),
                              Collections.<String>emptySet(),
                              false);
    try {
      usePlugin(blockingLoader);

      fail();
    }
    catch (final ExceptionInInitializerError e) {
      assertTrue(e.getCause() instanceof PluginException);
    }
  }

  @Test public void testInitializeWithBadStatistics() throws Exception {

    final GrinderException grinderException = new GrinderException("Hello") {};

    doThrow(grinderException).when(m_statistics)
      .registerDataLogExpression(isA(String.class), isA(String.class));

    final HTTPPlugin plugin = new HTTPPlugin(m_pluginProcessContext,
                                             m_scriptContext);

    try {
      plugin.ensureInitialised();
      fail();
    }
    catch (final PluginException e) {
      assertSame(grinderException, e.getCause());
    }
  }

  @Test public void testCreateThreadListener() throws Exception {
    final HTTPPlugin plugin =
        new HTTPPlugin(m_pluginProcessContext, m_scriptContext);

    final PluginThreadListener threadListener = plugin.createThreadListener();

    assertNotNull(threadListener);
  }

  @Test public void testRegistration() throws EngineException {
    new PluginContainerScopeTunnel(m_logger, m_scriptContext, null);

    verify(m_logger).info(isA(String.class),
                          contains(HTTPPlugin.class.getName()));
  }
}
