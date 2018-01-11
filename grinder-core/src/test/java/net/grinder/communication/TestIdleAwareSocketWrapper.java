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

package net.grinder.communication;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import net.grinder.common.TimeAuthority;
import net.grinder.util.StandardTimeAuthority;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 *  Unit tests for {@link IdleAwareSocketWrapper}.
 *
 * @author Philip Aston
 */
public class TestIdleAwareSocketWrapper {

  private static Acceptor s_acceptor;
  private Socket m_socket;
  @Mock private TimeAuthority m_timeAuthority;

  @BeforeClass public static void setUpAcceptor() throws Exception {
    s_acceptor = new Acceptor("localhost", 0, 1, new StandardTimeAuthority());
  }

  @AfterClass public static void shutDownAcceptor() throws Exception {
    s_acceptor.shutdown();
  }

  @Before public void createSocket() throws Exception {
    MockitoAnnotations.initMocks(this);

    m_socket = new Socket(InetAddress.getByName(null), s_acceptor.getPort());
  }

  @Test(expected=CommunicationException.class)
  public void testConstructionWithBadSocket() throws Exception {
    m_socket.close();
    new IdleAwareSocketWrapper(m_socket, m_timeAuthority);
  }

  @Test public void testHasDataNoData() throws Exception {
    final IdleAwareSocketWrapper socketWrapper =
        new IdleAwareSocketWrapper(m_socket, m_timeAuthority);

    assertFalse(socketWrapper.hasData(99));
  }

  @Test(expected = IOException.class)
  public void testHasDataSocketClosed() throws Exception {
    final IdleAwareSocketWrapper socketWrapper =
        new IdleAwareSocketWrapper(m_socket, m_timeAuthority);
    socketWrapper.close();

    socketWrapper.hasData(99);
  }

  @Test public void testHasDataTimeOut() throws Exception {

    final IdleAwareSocketWrapper socketWrapper =
        new IdleAwareSocketWrapper(m_socket,
                                   m_timeAuthority);

    when(m_timeAuthority.getTimeInMilliseconds())
      .thenReturn(1000L)
      .thenReturn(2000L);

    assertFalse(socketWrapper.hasData(123));
    assertFalse(m_socket.isClosed());

    assertFalse(socketWrapper.hasData(123));
    assertTrue(m_socket.isClosed());
  }
}
