// Copyright (C) 2011 - 2012 Philip Aston
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

import java.util.ArrayList;
import java.util.List;

import net.grinder.common.GrinderProperties;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.DCRContext;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.ScriptEngineService;
import net.grinder.scriptengine.ScriptEngineService.ScriptEngine;

import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.behaviors.Caching;
import org.slf4j.Logger;


/**
 * Container for script engines.
 *
 * @author Philip Aston
 */
final class ScriptEngineContainer {

  private final MutablePicoContainer m_container =
    new DefaultPicoContainer(new Caching());

  public ScriptEngineContainer(GrinderProperties properties,
                               Logger logger,
                               DCRContext dcrContext,
                               ScriptLocation scriptLocation)
    throws EngineException {

    m_container.addComponent(properties);
    m_container.addComponent(logger);
    m_container.addComponent(scriptLocation);

    if (dcrContext != null) {
      m_container.addComponent(dcrContext);
    }

    for (Class<? extends ScriptEngineService> implementation :
      loadRegisteredImplementations(ScriptEngineService.RESOURCE_NAME,
                                    ScriptEngineService.class)) {

      m_container.addComponent(implementation);
    }
  }

  public ScriptEngine getScriptEngine(ScriptLocation script)
    throws EngineException {

    for (ScriptEngineService service :
         m_container.getComponents(ScriptEngineService.class)) {

      final ScriptEngine engine = service.createScriptEngine(script);

      if (engine != null) {
        return engine;
      }
    }

    throw new EngineException("No suitable script engine installed for '" +
                              script + "'");
  }

  public Instrumenter createInstrumenter() throws EngineException {

    final List<Instrumenter> instrumenters = new ArrayList<Instrumenter>();

    for (ScriptEngineService service :
         m_container.getComponents(ScriptEngineService.class)) {

      for (Instrumenter instrumenter : service.createInstrumenters()) {
        instrumenters.add(instrumenter);
      }
    }

    return new MasterInstrumenter(instrumenters);
  }
}
