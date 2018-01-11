// Copyright (C) 2005 - 2009 Philip Aston
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

package net.grinder.plugin.http.tcpproxyfilter;

import junit.framework.TestCase;

import net.grinder.testutility.RandomStubFactory;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.EndPoint;


/**
 * Unit tests for {@link HTTPRequestFilter}.
 *
 * @author Philip Aston
 */
public class TestHTTPRequestFilter extends TestCase {

  private final ConnectionDetails m_connectionDetails =
    new ConnectionDetails(
      new EndPoint("hostA", 80),
      new EndPoint("hostB", 80),
      false);

  public void testDelegation() throws Exception {
    final RandomStubFactory<HTTPFilterEventListener> connectionMapStubFactory =
      RandomStubFactory.create(HTTPFilterEventListener.class);

    final HTTPRequestFilter filter =
      new HTTPRequestFilter(connectionMapStubFactory.getStub());

    connectionMapStubFactory.assertNoMoreCalls();

    filter.connectionOpened(m_connectionDetails);
    connectionMapStubFactory.assertSuccess("open", m_connectionDetails);

    final byte[] buffer = new byte[100];

    final byte[] result = filter.handle(m_connectionDetails, buffer, 56);
    assertNull(result);
    connectionMapStubFactory.assertSuccess(
      "request", m_connectionDetails, buffer, new Integer(56));

    filter.connectionClosed(m_connectionDetails);
    connectionMapStubFactory.assertSuccess("close", m_connectionDetails);

    connectionMapStubFactory.assertNoMoreCalls();
  }
}
