// Copyright (C) 2002 - 2013 Philip Aston
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

import static net.grinder.util.ClassLoaderUtilities.loadRegisteredImplementations;

import java.util.List;

import net.grinder.common.SkeletonThreadLifeCycleListener;
import net.grinder.engine.common.EngineException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginThreadListener;
import net.grinder.script.Grinder.ScriptContext;

import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.behaviors.Caching;
import org.slf4j.Logger;

/**
 * Registry of live {@link GrinderPlugin} implementations. Responsible
 * for plugin process and thread initialisation.
 *
 * @author Philip Aston
 */
final class PluginContainer
  implements ProcessLifeCycleListener {

  private final MutablePicoContainer m_container =
      new DefaultPicoContainer(new Caching());

  private final PluginProcessContextImplementation m_context;

  /**
   * Constructor.
   * @throws EngineException
   */
  PluginContainer(final Logger logger,
                  final ScriptContext scriptContext,
                  final ThreadContextLocator threadContextLocator)
    throws EngineException {

    this(logger, scriptContext, threadContextLocator,
         GrinderPlugin.RESOURCE_NAME);
  }

  /**
   * Constructor.
   *
   * <p>Package scope for unit tests.</p>
   */
  PluginContainer(final Logger logger,
                  final ScriptContext scriptContext,
                  final ThreadContextLocator threadContextLocator,
                  final String resourceName)
    throws EngineException {

    m_container.addComponent(scriptContext);

    m_context =
        new PluginProcessContextImplementation(threadContextLocator, logger);
    m_container.addComponent(m_context);

    for (final Class<? extends GrinderPlugin> implementation :
      loadRegisteredImplementations(resourceName,
                                    GrinderPlugin.class)) {

      m_container.addComponent(implementation);

      logger.info("registered plug-in {}", implementation.getName());
    }

    // Instantiate all plugins.
    m_container.getComponents(GrinderPlugin.class);
  }

  @Override
  public void threadCreated(final ThreadContext threadContext) {
    threadContext.registerThreadLifeCycleListener(
      new SkeletonThreadLifeCycleListener() {
        @Override
        public void beginThread() {
          // Called from the worker thread.

          final List<GrinderPlugin> plugins =
              m_container.getComponents(GrinderPlugin.class);

          for (final GrinderPlugin plugin : plugins) {
            try {
              final PluginThreadListener l =
                  m_context.createPluginThreadListener(threadContext, plugin);

              // Delegate call.
              l.beginThread();
            }
            catch (final EngineException e) {
              // Swallow plug-in failures. We don't need the result from
              // createPluginThreadListener(), and it will have produced
              // adequate logging.
            }
          }
        }
      });
  }
}
