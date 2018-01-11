// Copyright (C) 2002 - 2011 Philip Aston
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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import net.grinder.common.SkeletonThreadLifeCycleListener;
import net.grinder.engine.common.EngineException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginRegistry;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.statistics.StatisticsServices;
import net.grinder.util.TimeAuthority;


/**
 * Registry of live {@link GrinderPlugin} implementations. Responsible
 * for plugin process and thread initialisation.
 *
 * @author Philip Aston
 */
final class PluginRegistryImplementation
  extends PluginRegistry implements ProcessLifeCycleListener {

  private final Logger m_logger;
  private final ScriptContext m_scriptContext;
  private final ThreadContextLocator m_threadContextLocator;
  private final StatisticsServices m_statisticsServices;
  private final TimeAuthority m_timeAuthority;

  private final Map<GrinderPlugin, RegisteredPlugin> m_plugins =
    new HashMap<GrinderPlugin, RegisteredPlugin>();

  /**
   * Constructor.
   */
  PluginRegistryImplementation(Logger logger, ScriptContext scriptContext,
                               ThreadContextLocator threadContextLocator,
                               StatisticsServices statisticsServices,
                               TimeAuthority timeAuthority) {
    m_logger = logger;
    m_scriptContext = scriptContext;
    m_threadContextLocator = threadContextLocator;
    m_statisticsServices = statisticsServices;
    m_timeAuthority = timeAuthority;

    setInstance(this);
  }

  /**
   * Used to register a new plugin.
   *
   * @param plugin The plugin instance.
   * @exception EngineException if an error occurs
   */
  public void register(GrinderPlugin plugin) throws EngineException {
    synchronized (m_plugins) {
      if (!m_plugins.containsKey(plugin)) {

        final RegisteredPlugin registeredPlugin =
          new RegisteredPlugin(plugin, m_scriptContext, m_threadContextLocator,
                               m_statisticsServices, m_timeAuthority, m_logger);

        try {
          plugin.initialize(registeredPlugin);
        }
        catch (PluginException e) {
          throw new EngineException("An instance of the plug-in class '" +
                                    plugin.getClass().getName() +
                                    "' could not be initialised.", e);
        }

        m_plugins.put(plugin, registeredPlugin);
        m_logger.info("registered plug-in {}", plugin.getClass().getName());
      }
    }
  }

  public void threadCreated(final ThreadContext threadContext) {
    // A new thread has been created. Create a thread listener for each plugin.
    // We use a ThreadLifeCycleListener so the thread listener is created in the
    // worker thread.

    threadContext.registerThreadLifeCycleListener(
      new SkeletonThreadLifeCycleListener() {
        public void beginThread() {
          final RegisteredPlugin[] registeredPlugins;

          synchronized (m_plugins) {
            registeredPlugins = m_plugins.values().toArray(
                                  new RegisteredPlugin[m_plugins.size()]);
          }

          for (int i = 0; i < registeredPlugins.length; ++i) {
            try {
              registeredPlugins[i].createPluginThreadListener(threadContext);
            }
            catch (EngineException e) {
              // Swallow plug-in failures. We don't need a result from
              // createPluginThreadListener(), and it will have produced
              // adequate logging.
            }
          }
        }
      });
  }

  public void threadStarted(ThreadContext threadContext) {
  }
}
