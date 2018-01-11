// Copyright (C) 2005 - 2013 Philip Aston
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;



/**
 * Unit test case for {@link CompositeFilter}.
 *
 * @author Philip Aston
 */
public class TestCompositeFilter {

  private final MyFilterStubFactory m_filter1StubFactory =
    new MyFilterStubFactory();

  private final TCPProxyFilter m_filter1 = m_filter1StubFactory.getStub();

  final MyFilterStubFactory m_filter2StubFactory =
    new MyFilterStubFactory();

  final TCPProxyFilter m_filter2 = m_filter2StubFactory.getStub();

  private final ConnectionDetails m_connectionDetails =
    new ConnectionDetails(new EndPoint("foo", 1),
                          new EndPoint("bah", 2),
                          false);

  @Test
  public void testGetFilters() throws Exception {
    final CompositeFilter composite = new CompositeFilter();

    assertEquals(0, composite.getFilters().length);

    composite.add(m_filter1);

    assertArrayEquals(
      new Object[] { m_filter1 }, composite.getFilters());

    composite.add(m_filter2);

    assertArrayEquals(
      new Object[] { m_filter1, m_filter2 }, composite.getFilters());
  }

  @Test
  public void testHandle() throws Exception {
    final CompositeFilter composite = new CompositeFilter();

    final byte[] buffer = new byte[100];
    final byte[] buffer2 = new byte[20];

    assertNull(composite.handle(m_connectionDetails, buffer, 10));

    composite.add(m_filter1);
    composite.add(m_filter2);
    composite.add(m_filter1);

    m_filter2StubFactory.setResult(buffer2);

    composite.handle(m_connectionDetails, buffer, 10);

    m_filter1StubFactory.assertSuccess("handle",
                                       m_connectionDetails,
                                       buffer,
                                       new Integer(10));

    m_filter2StubFactory.assertSuccess("handle",
                                       m_connectionDetails,
                                       buffer,
                                       new Integer(100));

    m_filter1StubFactory.assertSuccess("handle",
                                       m_connectionDetails,
                                       buffer2,
                                       new Integer(20));

    m_filter1StubFactory.assertNoMoreCalls();
    m_filter2StubFactory.assertNoMoreCalls();

    m_filter2StubFactory.setResult(null);

    composite.handle(m_connectionDetails, buffer, 10);

    m_filter1StubFactory.assertSuccess("handle",
                                       m_connectionDetails,
                                       buffer,
                                       new Integer(10));

    m_filter2StubFactory.assertSuccess("handle",
                                       m_connectionDetails,
                                       buffer,
                                       new Integer(100));

    m_filter1StubFactory.assertSuccess("handle",
                                       m_connectionDetails,
                                       buffer,
                                       new Integer(100));
  }

  @Test
  public void testConnectionOpened() throws Exception {
    final CompositeFilter composite = new CompositeFilter();
    composite.add(m_filter1);
    composite.add(m_filter2);
    composite.add(m_filter1);

    composite.connectionOpened(m_connectionDetails);

    m_filter1StubFactory.assertSuccess("connectionOpened", m_connectionDetails);
    m_filter2StubFactory.assertSuccess("connectionOpened", m_connectionDetails);
    m_filter1StubFactory.assertSuccess("connectionOpened", m_connectionDetails);
    m_filter1StubFactory.assertNoMoreCalls();
    m_filter2StubFactory.assertNoMoreCalls();
  }

  @Test
  public void testConnectionClosed() throws Exception {
    final CompositeFilter composite = new CompositeFilter();
    composite.add(m_filter1);
    composite.add(m_filter2);
    composite.add(m_filter1);

    composite.connectionClosed(m_connectionDetails);

    m_filter1StubFactory.assertSuccess("connectionClosed", m_connectionDetails);
    m_filter2StubFactory.assertSuccess("connectionClosed", m_connectionDetails);
    m_filter1StubFactory.assertSuccess("connectionClosed", m_connectionDetails);
    m_filter1StubFactory.assertNoMoreCalls();
    m_filter2StubFactory.assertNoMoreCalls();
  }

  @Test
  public void testToString() throws Exception {
    final CompositeFilter composite = new CompositeFilter();
    composite.add(m_filter1);
    composite.add(m_filter2);
    composite.add(m_filter1);

    final String s = composite.toString();
    assertNotNull(s);
  }
}
