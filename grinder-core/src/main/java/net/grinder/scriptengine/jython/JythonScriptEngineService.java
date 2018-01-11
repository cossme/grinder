// Copyright (C) 2001 - 2013 Philip Aston
// Copyright (C) 2005 Martin Wagner
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

package net.grinder.scriptengine.jython;

import java.util.ArrayList;
import java.util.List;

import net.grinder.common.GrinderProperties;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.DCRContext;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.ScriptEngineService;
import net.grinder.scriptengine.jython.instrumentation.dcr.Jython22Instrumenter;
import net.grinder.scriptengine.jython.instrumentation.dcr.Jython25Instrumenter;
import net.grinder.util.FileExtensionMatcher;
import net.grinder.util.weave.WeavingException;


/**
 * Jython {@link ScriptEngineService} implementation.
 *
 * @author Philip Aston
 */
public final class JythonScriptEngineService implements ScriptEngineService {

  private final FileExtensionMatcher m_pyFileMatcher =
    new FileExtensionMatcher(".py");

  private final DCRContext m_dcrContext;

  /**
   * Constructor.
   *
   * @param properties Properties.
   * @param dcrContext DCR context.
   * @param scriptLocation Script location.
   */
  public JythonScriptEngineService(final GrinderProperties properties,
                                   final DCRContext dcrContext,
                                   final ScriptLocation scriptLocation) {
    m_dcrContext = dcrContext;
  }

  /**
   * Constructor used when DCR is unavailable.
   */
  public JythonScriptEngineService() {
    m_dcrContext = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override public List<Instrumenter> createInstrumenters()
    throws EngineException {

    final List<Instrumenter> instrumenters = new ArrayList<Instrumenter>();

    if (m_dcrContext != null) {
      if (instrumenters.size() == 0) {
        try {
          instrumenters.add(new Jython25Instrumenter(m_dcrContext));
        }
        catch (final WeavingException e) {
          // Jython 2.5 not available, try Jython 2.1/2.2.
          try {
            instrumenters.add(new Jython22Instrumenter(m_dcrContext));
          }
          catch (final WeavingException e2) {
            throw new EngineException(
              "Could not select an appropriate instrumenter for the " +
              "version of Jython", e);
          }
        }
      }
    }

    return instrumenters;
  }

  /**
   * {@inheritDoc}
   */
  @Override public ScriptEngine createScriptEngine(final ScriptLocation script)
    throws EngineException {

    if (m_pyFileMatcher.accept(script.getFile())) {
      return new JythonScriptEngine(script);
    }

    return null;
  }
}
