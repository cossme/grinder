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
 * Unit tests for {@link ConnectionCache}.
 *
 * @author Philip Aston
 */
public class TestConnectionCache extends TestCase {

  final RandomStubFactory<ConnectionHandlerFactory>
    m_connectionHandlerFactoryStubFactory =
      RandomStubFactory.create(ConnectionHandlerFactory.class);
  final ConnectionHandlerFactory m_connectionHandlerFactory =
    m_connectionHandlerFactoryStubFactory.getStub();

  private final ConnectionDetails m_connectionDetails =
    new ConnectionDetails(
      new EndPoint("hostA", 80),
      new EndPoint("hostB", 80),
      false);

  private final ConnectionDetails m_connectionDetails2 =
    new ConnectionDetails(
      new EndPoint("hostA", 8080),
      new EndPoint("hostB", 80),
      false);

  public void testConstructAndDispose() throws Exception {
    final ConnectionCache connectionMap =
      new ConnectionCache(m_connectionHandlerFactory);

    connectionMap.dispose();

    m_connectionHandlerFactoryStubFactory.assertNoMoreCalls();
  }

  public void testConnectionCache() throws Exception {
    final ConnectionCache connectionCache = new ConnectionCache(
      m_connectionHandlerFactory);

    connectionCache.open(m_connectionDetails);
    m_connectionHandlerFactoryStubFactory.assertSuccess("create",
      m_connectionDetails);

    try {
      connectionCache.open(m_connectionDetails);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
    }

    connectionCache.close(m_connectionDetails);

    try {
      connectionCache.close(m_connectionDetails);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
    }

    try {
      connectionCache.request(m_connectionDetails, new byte[100], 56);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
    }

    try {
      connectionCache.response(m_connectionDetails.getOtherEnd(),
        new byte[100], 56);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
    }

    connectionCache.dispose();

    final ConnectionCache connectionCache2 = new ConnectionCache(
      m_connectionHandlerFactory);

    connectionCache2.open(m_connectionDetails);
    m_connectionHandlerFactoryStubFactory.assertSuccess("create",
      m_connectionDetails);

    connectionCache2.request(m_connectionDetails, new byte[10], 0);
    connectionCache2.request(m_connectionDetails, new byte[20], 0);

    try {
      connectionCache.request(m_connectionDetails2, new byte[10], 0);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
    }

    connectionCache2.response(m_connectionDetails.getOtherEnd(), new byte[20],
      0);

    try {
      connectionCache.response(m_connectionDetails2.getOtherEnd(),
        new byte[10], 0);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
    }

    connectionCache2.dispose(); // Closes all handlers.

    try {
      connectionCache.request(m_connectionDetails, new byte[10], 5);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
    }

    try {
      connectionCache.response(m_connectionDetails.getOtherEnd(), new byte[10],
        5);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
    }

    m_connectionHandlerFactoryStubFactory.assertNoMoreCalls();
  }
}
