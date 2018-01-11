// Copyright (C) 2005 Philip Aston
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

package net.grinder.tools.tcpproxy;

import junit.framework.TestCase;


/**
 * Unit test case for {@link HTTPMethodRelativeURIFilterDecorator}.
 *
 * @author Philip Aston
 */
public class TestHTTPMethodRelativeURIFilterDecorator extends TestCase {

  private final MyFilterStubFactory m_delegateFilterStubFactory =
    new MyFilterStubFactory();

  private final TCPProxyFilter m_delegateFilter =
    m_delegateFilterStubFactory.getStub();

  public void testHandle() throws Exception {

    final HTTPMethodRelativeURIFilterDecorator filter =
      new HTTPMethodRelativeURIFilterDecorator(m_delegateFilter);

    final ConnectionDetails connectionDetails =
      new ConnectionDetails(new EndPoint("a", 1), new EndPoint("b", 2), false);

    final byte[] allBytes = new byte[256];

    for (int i = 0; i < 256; ++i) {
      allBytes[i] = (byte)i;
    }

    final byte[] result0 =
      filter.handle(connectionDetails, allBytes, allBytes.length);

    assertEquals(allBytes, result0);

    final byte[] convertMe =
      "GET http://somewhere/foo HTTP/1.1\r\nBlah:lah\r\n\r\nstuff".getBytes();

    final byte[] result1 =
      filter.handle(connectionDetails, convertMe, convertMe.length);

    assertEquals("GET /foo HTTP/1.1\r\nBlah:lah\r\n\r\nstuff",
                 new String(result1));

    final byte[] dontConvertMe =
      "Lah\r\nGET http://somewhere/foo HTTP/1.1\r\nBlah:lah\r\n\r\nstuff"
      .getBytes();

    final byte[] result2 =
      filter.handle(connectionDetails, dontConvertMe, dontConvertMe.length);

    assertEquals(dontConvertMe, result2);

    m_delegateFilterStubFactory.setResult(null);

    final byte[] result3 =
      filter.handle(connectionDetails, allBytes, allBytes.length);

    assertNull(result3);

    final byte[] convertMe1 =
      "POST http://bah:99/foo?123 HTTP/1.0\r\n\r\nstuff\nxxx".getBytes();

    final byte[] result4 =
      filter.handle(connectionDetails, convertMe1, convertMe1.length);

    assertEquals("POST /foo?123 HTTP/1.0\r\n\r\nstuff\nxxx",
                 new String(result4));

    final byte[] dontConvertMe1 =
      "get http://bah:99/foo HTTP/1.1\r\nBlah:lah\r\n\r\nstuff".getBytes();

    final byte[] result5 =
      filter.handle(connectionDetails, dontConvertMe1, dontConvertMe1.length);

    assertNull(result5);
  }

  public void testOtherMethods() throws Exception {

    final HTTPMethodRelativeURIFilterDecorator filter =
      new HTTPMethodRelativeURIFilterDecorator(m_delegateFilter);

    final ConnectionDetails connectionDetails =
      new ConnectionDetails(new EndPoint("a", 1), new EndPoint("b", 2), false);

    filter.connectionOpened(connectionDetails);
    m_delegateFilterStubFactory.assertSuccess("connectionOpened",
                                              connectionDetails);
    m_delegateFilterStubFactory.assertNoMoreCalls();

    filter.connectionClosed(connectionDetails);
    m_delegateFilterStubFactory.assertSuccess("connectionClosed",
                                              connectionDetails);
    m_delegateFilterStubFactory.assertNoMoreCalls();

    m_delegateFilterStubFactory.assertNoMoreCalls();
  }
}
