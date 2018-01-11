// Copyright (C) 2007 - 2013 Philip Aston
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
import net.grinder.common.SSLContextFactory;
import net.grinder.util.InsecureSSLContextFactory;
import net.grinder.util.Sleeper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.URI;


/**
 * Unit tests for {@link HTTPPluginThreadState}.
 *
 * @author Philip Aston
 */
public class TestHTTPPluginThreadState {

  private final SSLContextFactory m_sslContextFactory =
      new InsecureSSLContextFactory();

  @Mock private Sleeper m_sleeper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testHTTPPluginThreadState() throws Exception {
    final HTTPPluginThreadState pluginThreadState =
      new HTTPPluginThreadState(m_sslContextFactory,
                                m_sleeper,
                                null);

    pluginThreadState.beginThread();

    pluginThreadState.beginRun();

    pluginThreadState.endRun();

    pluginThreadState.beginRun();

    final HTTPConnectionWrapper wrapper1 =
      pluginThreadState.getConnectionWrapper(new URI("http://blah.com"));

    assertNotNull(wrapper1);
    assertNotNull(m_sslContextFactory.getSSLContext().getSocketFactory());

    final HTTPConnectionWrapper wrapper2 =
      pluginThreadState.getConnectionWrapper(new URI("https://secure.com"));

    assertNotNull(wrapper2);

    final HTTPConnectionWrapper wrapper3 =
      pluginThreadState.getConnectionWrapper(new URI("http://blah.com/lah"));

    assertSame(wrapper1, wrapper3);
    assertNotSame(wrapper2, wrapper3);

    pluginThreadState.endRun();

    pluginThreadState.beginRun();

    final HTTPConnectionWrapper wrapper4 =
      pluginThreadState.getConnectionWrapper(new URI("http://blah.com/lah"));

    assertNotSame(wrapper1, wrapper4);

    pluginThreadState.endRun();

    pluginThreadState.endThread();

    pluginThreadState.beginShutdown();
  }

  @Test public void testSetResponse() throws Exception {
    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();

    try {
      final HTTPConnection connection =
        new HTTPConnection(new URI(handler.getURL()));

      final HTTPPluginThreadState pluginThreadState =
        new HTTPPluginThreadState(m_sslContextFactory,
                                  m_sleeper,
                                  null);

      final HTTPResponse response = connection.Get("foo");

      pluginThreadState.setLastResponse(response);

      assertSame(response, pluginThreadState.getLastResponse());
    }
    finally {
      handler.shutdown();
    }
  }
}
