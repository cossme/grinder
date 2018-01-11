// Copyright (C) 2005 - 2011 Philip Aston
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

import static net.grinder.testutility.SocketUtilities.findFreePort;

import java.io.File;
import java.io.FileOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLServerSocket;

import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.util.StreamCopier;


/**
 * Unit test case for {@link TCPProxySocketFactoryImplementation} and
 * {@link TCPProxySSLSocketFactoryImplementation}.
 *
 * @author Philip Aston
 */
public class TestTCPProxySocketFactories extends AbstractFileTestCase {

  private int m_freeLocalPort;

  protected void setUp() throws Exception {
    super.setUp();

    m_freeLocalPort = findFreePort();
  }

  public void testTCPProxySocketFactoryImplementation() throws Exception {
    final TCPProxySocketFactory socketFactory =
      new TCPProxySocketFactoryImplementation();

    final ServerSocket createdServerSocket =
      socketFactory.createServerSocket(new EndPoint("localhost",
                                                    m_freeLocalPort),
                                       100);

    final Socket createdLocalSocket =
      socketFactory.createClientSocket(new EndPoint("localhost",
                                                    m_freeLocalPort));
    createdServerSocket.close();
    createdLocalSocket.close();
  }

  public void testTCPProxySSLSocketFactoryImplementation() throws Exception {
    final TCPProxySSLSocketFactory socketFactory =
      new TCPProxySSLSocketFactoryImplementation();

    final ServerSocket createdServerSocket =
      socketFactory.createServerSocket(new EndPoint("localhost",
                                                    m_freeLocalPort),
                                       100);

    assertTrue(createdServerSocket instanceof SSLServerSocket);

    final Socket createdLocalSocket =
      socketFactory.createClientSocket(new EndPoint("localhost",
                                                    m_freeLocalPort));
    createdServerSocket.close();

    final Socket createdLocalSocket2 =
      socketFactory.createClientSocket(createdLocalSocket,
                                       new EndPoint("localhost",
                                                    m_freeLocalPort));

    createdLocalSocket2.close();
  }

  public void testTCPProxySSLSocketFactoryImplementationWithFileStore()
    throws Exception {

    final File keyStoreFile = new File(getDirectory(), "keystore");

    new StreamCopier(2000, true).copy(
      getClass().getResourceAsStream("resources/default.keystore"),
      new FileOutputStream(keyStoreFile));

    final TCPProxySSLSocketFactory socketFactory =
      new TCPProxySSLSocketFactoryImplementation(keyStoreFile,
                                                 "passphrase".toCharArray(),
                                                 null);

    final ServerSocket createdServerSocket =
      socketFactory.createServerSocket(new EndPoint("localhost",
                                                    m_freeLocalPort),
                                       100);

    assertTrue(createdServerSocket instanceof SSLServerSocket);

    final Socket createdLocalSocket =
      socketFactory.createClientSocket(new EndPoint("localhost",
                                                    m_freeLocalPort));
    createdServerSocket.close();
    createdLocalSocket.close();

    final TCPProxySSLSocketFactory socketFactory2 =
      new TCPProxySSLSocketFactoryImplementation(keyStoreFile,
                                                 "passphrase".toCharArray(),
                                                 "jks");

    final ServerSocket createdServerSocket2 =
      socketFactory2.createServerSocket(new EndPoint("localhost",
                                                     m_freeLocalPort),
                                       100);

    assertTrue(createdServerSocket2 instanceof SSLServerSocket);

    final Socket createdLocalSocket2 =
      socketFactory2.createClientSocket(new EndPoint("localhost",
                                                     m_freeLocalPort));
    createdServerSocket2.close();
    createdLocalSocket2.close();
  }

  public void testUnhappyCases() throws Exception {

    final TCPProxySocketFactory socketFactory =
      new TCPProxySocketFactoryImplementation();

    try {
      socketFactory.createClientSocket(
        new EndPoint("localhost", m_freeLocalPort));
      fail("Expected VerboseConnectException");
    }
    catch (VerboseConnectException e) {
      assertTrue(e.getCause() instanceof ConnectException);
    }

    final TCPProxySSLSocketFactory sslSocketFactory =
      new TCPProxySSLSocketFactoryImplementation();

    try {
      sslSocketFactory.createClientSocket(
        new EndPoint("localhost", m_freeLocalPort));
      fail("Expected VerboseConnectException");
    }
    catch (VerboseConnectException e) {
      assertTrue(e.getCause() instanceof ConnectException);
    }
  }
}
