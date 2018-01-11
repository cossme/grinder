// Copyright (C) 2000 Phil Dawes
// Copyright (C) 2000 - 2008 Philip Aston
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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocketFactory;

import HTTPClient.HTTPConnection;

import net.grinder.common.Closer;
import net.grinder.common.SSLContextFactory.SSLContextFactoryException;
import net.grinder.util.InsecureSSLContextFactory;


/**
 * {@link TCPProxySocketFactory} for SSL connections.
 *
 * <p>ARGHH. I hate JSSE.</p>
 *
 * <p>The JSSE docs rabbit on about being able to create factories
 * with the required parameters, this is a lie. Where is
 * "SSL[Server]SocketFactory.setEnabledCipherSuites()"? Hence the need
 * for our own abstract factories.</p>
 *
 * <p>We can't install our own TrustManagerFactory without messing
 * with the security properties file. Hence we create our own
 * SSLContext and initialise it. </p>
 *
 * - PhilA
 *
 * @author Philip Aston
 * @author Phil Dawes
 */
public final class TCPProxySSLSocketFactoryImplementation
  implements TCPProxySSLSocketFactory {

  private final ServerSocketFactory m_serverSocketFactory;
  private final SSLSocketFactory m_clientSocketFactory;

  /**
   * Construct a TCPProxySSLSocketFactoryImplementation that uses the
   * specified key store.
   *
   * @param keyStoreFile Key store file.
   * @param keyStorePassword Key store password, or <code>null</code>
   * if no password.
   * @param keyStoreType Key store type, or <code>null</code> if the
   * default keystore type should be used.
   * @exception IOException If an I/O error occurs.
   * @exception GeneralSecurityException If a security error occurs.
   * @exception SSLContextFactoryException If SSLContext could not be created.
   */
  public TCPProxySSLSocketFactoryImplementation(File keyStoreFile,
                                                char[] keyStorePassword,
                                                String keyStoreType)
    throws IOException, GeneralSecurityException, SSLContextFactoryException {

    this(new FileInputStream(keyStoreFile),
         keyStoreType != null ? keyStoreType : KeyStore.getDefaultType(),
         keyStorePassword);
  }

  /**
   * Construct a TCPProxySSLSocketFactoryImplementation that uses the
   * built-in key store.
   *
   * @exception IOException If an I/O error occurs.
   * @exception GeneralSecurityException If a security error occurs.
   * @exception SSLContextFactoryException If SSLContext could not be created.
   */
  public TCPProxySSLSocketFactoryImplementation()
    throws IOException, GeneralSecurityException, SSLContextFactoryException {

    this(TCPProxySSLSocketFactoryImplementation.class.getResourceAsStream(
           "resources/default.keystore"),
         "jks",
         "passphrase".toCharArray());
  }

  private TCPProxySSLSocketFactoryImplementation(
    InputStream keyStoreInputStream,
    String keyStoreType,
    char[] keyStorePassword)
    throws IOException, GeneralSecurityException, SSLContextFactoryException {

    try {
      final InsecureSSLContextFactory sslContextFactory =
        new InsecureSSLContextFactory(keyStoreInputStream,
                                      keyStorePassword,
                                      keyStoreType);

      final SSLContext sslContext = sslContextFactory.getSSLContext();

      m_clientSocketFactory = sslContext.getSocketFactory();
      m_serverSocketFactory = sslContext.getServerSocketFactory();
    }
    finally {
      Closer.close(keyStoreInputStream);
    }
  }

  /**
   * Factory method for server sockets.
   *
   * @param localEndPoint Local host and port.
   * @param timeout Socket timeout.
   * @return A new <code>ServerSocket</code>.
   * @exception IOException If an error occurs.
   */
  public ServerSocket createServerSocket(EndPoint localEndPoint, int timeout)
    throws IOException {

    final SSLServerSocket socket =
      (SSLServerSocket)m_serverSocketFactory.createServerSocket(
        localEndPoint.getPort(), 50,
        InetAddress.getByName(localEndPoint.getHost()));

    socket.setSoTimeout(timeout);

    socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
    socket.setEnabledProtocols(socket.getSupportedProtocols());

    return socket;
  }

  /**
   * Factory method for client sockets.
   *
   * @param remoteEndPoint Remote host and port.
   * @return A new <code>Socket</code>.
   * @exception IOException If an error occurs.
   */
  public Socket createClientSocket(EndPoint remoteEndPoint)
    throws IOException {

    final SSLSocket socket;

    try {
      socket = (SSLSocket)m_clientSocketFactory.createSocket(
        remoteEndPoint.getHost(), remoteEndPoint.getPort());
    }
    catch (ConnectException e) {
      throw new VerboseConnectException(e, "SSL end point " + remoteEndPoint);
    }

    socket.setEnabledCipherSuites(HTTPConnection.getSSLCipherSuites());
    socket.setEnabledProtocols(HTTPConnection.getSSLProtocols());

    return socket;
  }

  /**
   * <p>Factory method for client sockets that are layered over
   * existing sockets. Used to establish HTTPS proxy connections.</p>
   *
   * <p>The SSL socket takes ownership of the existing socket; when
   * the SSL socket is closed, the existing socket will also be
   * closed.</p>
   *
   * @param existingSocket The existing socket.
   * @param remoteEndPoint Remote host and port. Not the proxy. As far as I
   * can gather, the JSSE does not use this information.
   * @return A new <code>Socket</code>.
   * @exception IOException If an error occurs.
   */
  public Socket createClientSocket(Socket existingSocket,
                                   EndPoint remoteEndPoint)
    throws IOException {

    final SSLSocket socket =
      (SSLSocket)m_clientSocketFactory.createSocket(
        existingSocket, remoteEndPoint.getHost(), remoteEndPoint.getPort(),
        true);

    socket.setEnabledCipherSuites(HTTPConnection.getSSLCipherSuites());
    socket.setEnabledProtocols(HTTPConnection.getSSLProtocols());

    return socket;
  }
}

