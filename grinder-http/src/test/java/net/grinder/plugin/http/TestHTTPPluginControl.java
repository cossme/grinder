// Copyright (C) 2008 - 2012 Philip Aston
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import net.grinder.common.GrinderException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginRegistry;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.util.InsecureSSLContextFactory;
import net.grinder.util.StandardTimeAuthority;

import org.junit.Test;


/**
 * Unit tests for {@link HTTPPluginControl}.
 *
 * @author Philip Aston
 */
public class TestHTTPPluginControl {

  @Test public void testHTTPPluginControl() throws Exception {
    final HTTPPluginThreadState threadState =
      new HTTPPluginThreadState(null,
                                new InsecureSSLContextFactory(),
                                null,
                                new StandardTimeAuthority());

    final ScriptContext scriptContext =
        mock(ScriptContext.class, RETURNS_MOCKS);

    final PluginProcessContext pluginProcessContext =
        mock(PluginProcessContext.class);

    when(pluginProcessContext.getPluginThreadListener())
      .thenReturn(threadState);
    when(pluginProcessContext.getScriptContext()).thenReturn(scriptContext);

    new PluginRegistry() {
      { setInstance(this); }

      @Override
      public void register(final GrinderPlugin plugin) throws GrinderException {
        plugin.initialize(pluginProcessContext);
      }
    };

    // Sigh, if a previous test has registered a stub PluginProcessContext, we
    // need to rewire it to make this test valid. Further proof that static
    // references are evil.
    final PluginProcessContext existingMock =
        HTTPPlugin.getPlugin().getPluginProcessContext();
    if (existingMock != null &&
        existingMock != pluginProcessContext) {

      when(existingMock.getPluginThreadListener()).thenReturn(threadState);
      when(existingMock.getScriptContext()).thenReturn(scriptContext);
    }

    final HTTPPluginConnection connectionDefaults =
      HTTPPluginControl.getConnectionDefaults();

    assertNotNull(connectionDefaults);
    assertSame(connectionDefaults, HTTPPluginControl.getConnectionDefaults());

    final HTTPUtilities utilities = HTTPPluginControl.getHTTPUtilities();
    assertNotNull(utilities);

    final Object threadContext = HTTPPluginControl.getThreadHTTPClientContext();
    assertSame(threadState, threadContext);
    assertSame(threadState, HTTPPluginControl.getThreadHTTPClientContext());

    final HTTPPluginConnection connection =
      HTTPPluginControl.getThreadConnection("http://foo");
    assertSame(connection,
               HTTPPluginControl.getThreadConnection("http://foo/bah"));
    assertNotSame(connection,
                  HTTPPluginControl.getThreadConnection("http://bah"));
  }
}
