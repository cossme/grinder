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

import static java.util.Arrays.asList;
import static net.grinder.testutility.FileUtilities.createFile;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashSet;

import net.grinder.common.ThreadLifeCycleListener;
import net.grinder.engine.common.EngineException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginThreadListener;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.util.BlockingClassLoader;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;


/**
 * Unit tests for {@link PluginContainer}.
 *
 * @author Philip Aston
 */
public class TestPluginContainer extends AbstractJUnit4FileTestCase {

  @Mock private Logger m_logger;
  @Mock private ScriptContext m_scriptContext;
  @Mock private ThreadContext m_threadContext;
  @Captor private ArgumentCaptor<ThreadLifeCycleListener> m_listenerCaptor;

  private final ThreadContextLocator m_threadContextLocator =
    new StubThreadContextLocator();

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test public void testNoImplementation() throws Exception {

    final PluginContainer pluginContainer =
        new PluginContainer(m_logger,
                            m_scriptContext,
                            m_threadContextLocator);
    pluginContainer.threadCreated(m_threadContext);

    verify(m_threadContext)
      .registerThreadLifeCycleListener(m_listenerCaptor.capture());

    m_listenerCaptor.getValue().beginThread();

    verifyNoMoreInteractions(m_logger, m_threadContext);
  }

  @Test public void testSimpleImplementation() throws Exception {

    final PluginContainer pluginContainer =
        new PluginContainer(m_logger,
                            m_scriptContext,
                            m_threadContextLocator,
                            resource("RegisterSimplePlugin"));

    verify(m_logger).info(isA(String.class), contains("SimpleGrinderPlugin"));

    pluginContainer.threadCreated(m_threadContext);

    verify(m_threadContext)
      .registerThreadLifeCycleListener(m_listenerCaptor.capture());

    final PluginThreadListener myListener = mock(PluginThreadListener.class);
    SimpleGrinderPlugin.setResult(myListener);

    m_listenerCaptor.getValue().beginThread();

    verify(m_threadContext).registerThreadLifeCycleListener(myListener);
    verify(myListener).beginThread();

    verifyNoMoreInteractions(m_logger, m_threadContext, myListener);
  }

  @Test public void testUnknownImplementationStandardResourceName()
      throws Exception {

    final File path = new File(getDirectory(), "cp");

    createFile(new File(path, GrinderPlugin.RESOURCE_NAME),
               "bobbins");

    try {
      new PluginContainer(m_logger,
                          m_scriptContext,
                          m_threadContextLocator,
                          resource("RegisterUnknownPlugin"));
      fail();
    }
    catch (final EngineException e) {
      assertTrue(e.getCause() instanceof ClassNotFoundException);
    }
  }

  private String resource(final String s) {
    return getClass().getPackage().getName().replace('.',  '/') + "/" + s;
  }

  @Test public void testSimpleImplementationStandardResourceName()
      throws Exception {

    final File path = new File(getDirectory(), "cp");

    createFile(new File(path, GrinderPlugin.RESOURCE_NAME),
               SimpleGrinderPlugin.class.getName());

    final ClassLoader loader =
      new BlockingClassLoader(
         asList(path.toURI().toURL()),
         Collections.<String>emptySet(),
         new HashSet<String>(
             asList(PluginContainer.class.getPackage().getName() + "*",
                    PluginProcessContext.class.getPackage().getName() + "*")),
         new HashSet<String>(
             asList(ThreadContext.class.getName(),
                    ThreadContextLocator.class.getName())
             ),
         true);

    final Class<?> c =
      loader.loadClass(PluginContainerScopeTunnel.class.getName());

    final Constructor<?> constructor =
      c.getConstructor(Logger.class,
                       ScriptContext.class,
                       ThreadContextLocator.class);

    constructor.newInstance(m_logger,
                            m_scriptContext,
                            m_threadContextLocator);

    verify(m_logger).info(isA(String.class), contains("SimpleGrinderPlugin"));
    verifyNoMoreInteractions(m_logger, m_threadContext);
  }
}
