// Copyright (C) 2008 - 2013 Philip Aston
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
import static org.mockito.Answers.RETURNS_MOCKS;
import static org.mockito.Mockito.when;
import net.grinder.common.SSLContextFactory;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.script.Statistics;
import net.grinder.util.InsecureSSLContextFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@link HTTPPluginControl}.
 *
 * @author Philip Aston
 */
public class TestHTTPPluginControl {

  private final SSLContextFactory m_sslContextFactory =
      new InsecureSSLContextFactory();

  @Mock private PluginProcessContext m_pluginProcessContext;
  @Mock(answer = RETURNS_MOCKS) private ScriptContext m_scriptContext;
  @Mock private Statistics m_statistics;
  @Mock private HTTPClient.HTTPConnection.TimeAuthority m_timeAuthority;

  private HTTPPlugin m_httpPlugin;

  @Before public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(m_scriptContext.getStatistics()).thenReturn(m_statistics);

    m_httpPlugin = new HTTPPlugin(m_pluginProcessContext,
                                  m_scriptContext);
  }

  @Test public void testHTTPPluginControl() throws Exception {

    final HTTPPluginThreadState threadState =
      new HTTPPluginThreadState(m_sslContextFactory,
                                null,
                                m_timeAuthority);

    when(m_pluginProcessContext.getPluginThreadListener(m_httpPlugin))
      .thenReturn(threadState);

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
