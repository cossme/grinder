// Copyright (C) 2012 Philip Aston
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

import static net.grinder.testutility.SocketUtilities.findFreePort;

import net.grinder.util.StandardTimeAuthority;

import org.junit.After;
import org.junit.Before;

/**
 * Abstract unit test cases for socket based {@link Sender} and {@link Receiver}
 * implementations.
 *
 * @author Philip Aston
 */
public abstract class AbstractSenderAndReceiverSocketTests
  extends AbstractSenderAndReceiverTests {

  private ConnectionType m_connectionType;
  private Acceptor m_acceptor;
  private Connector m_connector;

  @Before public final void initialiseSockets() throws Exception {

    final int port = findFreePort();

    m_connectionType = ConnectionType.AGENT;
    m_connector = new Connector("localhost", port, m_connectionType);
    m_acceptor =
        new Acceptor("localhost", port, 1, new StandardTimeAuthority());
  }

  @After public void stopAcceptor() throws Exception {
    if (m_acceptor != null) {
      m_acceptor.shutdown();
    }
  }

  protected final Acceptor getAcceptor() throws Exception {
    return m_acceptor;
  }

  protected final ConnectionType getConnectionType() throws Exception {
    return m_connectionType;
  }

  protected final Connector getConnector() throws Exception {
    return m_connector;
  }
}
