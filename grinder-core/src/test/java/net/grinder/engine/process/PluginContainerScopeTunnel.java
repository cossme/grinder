// Copyright (C) 2013 Philip Aston
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

import net.grinder.engine.common.EngineException;
import net.grinder.script.Grinder.ScriptContext;

import org.slf4j.Logger;


/**
 * Scope tunnel required by {@link TestPluginContainer} due to class loader
 * stunts.
 *
 * @author Philip Aston
 */
public class PluginContainerScopeTunnel {

  private final PluginContainer m_delegate;

  public PluginContainerScopeTunnel(
    final Logger logger,
    final ScriptContext scriptContext,
    final ThreadContextLocator threadContextLocator) throws EngineException {

     m_delegate = new PluginContainer(logger,
                                      scriptContext,
                                      threadContextLocator);
  }

  public void threadCreated(final ThreadContext threadContext) {
    m_delegate.threadCreated(threadContext);
  }
}
