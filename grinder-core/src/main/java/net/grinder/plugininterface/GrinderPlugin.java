// Copyright (C) 2000 Paco Gomez
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

package net.grinder.plugininterface;


/**
 * Interface that a plugin should implement.
 *
 * <p>
 * The Grinder discovers plugin implementations from the {@code
 * META-INF/net.grinder.plugin} resource files. Each plugin should
 * register its class name as a line in such a resource file.
 * </p>
 *
 * <p>
 * Each plugin is injected with framework services using PicoContainer.
 * The use of PicoContainer should be transparent; implementations simply need
 * to declare the services they require as constructor parameters.
 * </p>
 *
 * <p>
 * Available services include:
 * </p>
 *
 * <ul>
 * <li>{@link PluginProcessContext}</li>
 * <li>{@link net.grinder.script.ScriptContext}</li>
 * </ul>
 *
 * @author Philip Aston
 */
public interface GrinderPlugin {

  /**
   * All resources with this name are loaded to discover implementations.
   */
  String RESOURCE_NAME = "META-INF/net.grinder.plugin";

  /**
   * This method is called from each new worker thread.
   *
   * <p>The plugin should implement this method to a return a handler object
   * that receives thread specific events. It may be useful to add
   * thread-specific state to this object.
   *
   * <p>A worker thread can retrieve its handler for a particular
   *  plug-in using
   * {@link PluginProcessContext#getPluginThreadListener(GrinderPlugin)}.</p>
   *
   * @return A {@code PluginThreadListener} implementation.
   * @throws PluginException If an error occurs.
   */
  PluginThreadListener createThreadListener() throws PluginException;
}
