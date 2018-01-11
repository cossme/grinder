// Copyright (C) 2004, 2005, 2006, 2007 Philip Aston
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

import net.grinder.common.GrinderException;


/**
 * <p>
 * Plugins must register themselves with the process wide singleton instance of
 * this class.
 * </p>
 *
 * <p>
 * Plugins should register before worker threads start, otherwise thread
 * listeners will not be created for existing threads. Typically a plugin will
 * want to initialise itself when a script imports one or more of its classes. A
 * static initialiser is a good way to achieve this - see the
 * {@code net.grinder.plugin.http.HTTPPlugin} implementation for an example.
 * </p>
 *
 * @author Philip Aston
 */
public abstract class PluginRegistry {
  private static PluginRegistry s_instance;

  /**
   * Singleton accessor.
   *
   * @return The singleton.
   */
  public static final PluginRegistry getInstance() {
    return s_instance;
  }

  /**
   * Used to register a new plugin.
   *
   * @param plugin The plugin instance.
   * @exception GrinderException If an error occurs.
   */
  public abstract void register(GrinderPlugin plugin) throws GrinderException;

  /**
   * Set the singleton.
   *
   * @param pluginRegistry The singleton.
   */
  protected static final void setInstance(PluginRegistry pluginRegistry) {
    s_instance = pluginRegistry;
  }
}
