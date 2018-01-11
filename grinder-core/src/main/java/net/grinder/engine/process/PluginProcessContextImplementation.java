// Copyright (C) 2000 - 2013 Philip Aston
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

import java.util.IdentityHashMap;
import java.util.Map;

import net.grinder.engine.common.EngineException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginThreadListener;

import org.slf4j.Logger;


/**
 * Handle returned from plugin registration.
 *
 * @author Philip Aston
 */
final class PluginProcessContextImplementation
  implements PluginProcessContext {

  private final ThreadContextLocator m_threadContextLocator;

  private final ThreadLocal<Map<GrinderPlugin, PluginThreadListener>>
    m_threadListenersThreadLocal =
      new ThreadLocal<Map<GrinderPlugin, PluginThreadListener>>() {
        @Override
        protected Map<GrinderPlugin, PluginThreadListener> initialValue() {
          return new IdentityHashMap<GrinderPlugin, PluginThreadListener>();
        }
  };

  private final Logger m_logger;

  public PluginProcessContextImplementation(
    final ThreadContextLocator threadContextLocator,
    final Logger logger) {
    m_threadContextLocator = threadContextLocator;
    m_logger = logger;
  }

  @Override
  public PluginThreadListener
    getPluginThreadListener(final GrinderPlugin plugin)
    throws EngineException {

    final ThreadContext threadContext = m_threadContextLocator.get();

    if (threadContext == null) {
      throw new EngineException("Must be called from worker thread");
    }

    return createPluginThreadListener(threadContext, plugin);
  }

  PluginThreadListener createPluginThreadListener(
    final ThreadContext threadContext, final GrinderPlugin plugin)
    throws EngineException {

    final Map<GrinderPlugin, PluginThreadListener> pluginThreadListeners =
      m_threadListenersThreadLocal.get();

    final PluginThreadListener existingPluginThreadListener =
        pluginThreadListeners.get(plugin);

    if (existingPluginThreadListener != null) {
      return existingPluginThreadListener;
    }

    final PluginThreadListener newPluginThreadListener;

    try {
      newPluginThreadListener = plugin.createThreadListener();
    }
    catch (final PluginException e) {
      m_logger.error(threadContext.getLogMarker(),
                     "Plugin could not create thread listener",
                     e);

      throw new EngineException("Plugin could not create thread listener", e);
    }

    pluginThreadListeners.put(plugin, newPluginThreadListener);

    threadContext.registerThreadLifeCycleListener(newPluginThreadListener);

    return newPluginThreadListener;
  }
}
