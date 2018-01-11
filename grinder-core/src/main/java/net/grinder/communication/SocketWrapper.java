// Copyright (C) 2005 - 2012 Philip Aston
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import net.grinder.common.Closer;
import net.grinder.util.ListenerSupport;


/**
 * Wrapper for a {@link Socket} that is a {@link ResourcePool.Resource} and
 * understands our connection close protocol.
 *
 * <p>
 * Client classes that access the sockets streams through
 * {@link #getInputStream} or {@link #getOutputStream}, and that do not
 * otherwise know they have exclusive access, should synchronise on the
 * particular stream object while they use it.
 * </p>
 *
 * @author Philip Aston
 */
class SocketWrapper implements ResourcePool.Resource {

  private final Socket m_socket;
  private final ConnectionIdentity m_connectionIdentity;
  private final InputStream m_inputStream;
  private final OutputStream m_outputStream;

  private final ListenerSupport<ClosedListener> m_closedListeners =
    new ListenerSupport<ClosedListener>();

  private final ListenerSupport.Informer<ClosedListener> m_closedInformer =
    new ListenerSupport.Informer<ClosedListener>() {
      public void inform(ClosedListener l) { l.socketClosed(); }
    };

  private Address m_address;

  /**
   * Constructor.
   *
   * @param socket
   *          Socket to wrap. If the caller maintains any references to the
   *          socket, if should synchronise access to the socket streams as
   *          described in {@link SocketWrapper}.
   * @throws CommunicationException
   *           If an error occurred.
   */
  public SocketWrapper(Socket socket) throws CommunicationException {
    m_socket = socket;

    try {
      m_inputStream = m_socket.getInputStream();
      m_outputStream = m_socket.getOutputStream();

      m_connectionIdentity =
        new ConnectionIdentity(m_socket.getInetAddress(),
                               m_socket.getPort(),
                               System.currentTimeMillis());
    }
    catch (IOException e) {
      Closer.close(m_socket);

      throw new CommunicationException("Could not establish communication", e);
    }
  }

  /**
   * Close the SocketWrapper and its underlying resources.
   *
   * <p>No need to synchronise access to the close, isClosed - they should be
   * thread safe. Also, we're careful not to hold locks around the listener
   * notification.</p>
   */
  public void close() {
    if (!m_socket.isClosed()) {
      // Java provides no way for socket code to enquire whether the
      // peer has closed the connection. We make an effort to tell the
      // peer.
      synchronized (m_outputStream) {
        new StreamSender(m_outputStream).shutdown();
      }

      Closer.close(m_socket);

      // Close before informing listeners to prevent recursion.
      m_closedListeners.apply(m_closedInformer);
    }
  }

  /**
   * Return whether the socket is closed.
   *
   * @return {@code true} If the socket is closed.
   */
  public boolean isClosed() {
    return m_socket.isClosed();
  }

  public ConnectionIdentity getConnectionIdentity() {
    return m_connectionIdentity;
  }

  /**
   * See note in {@link SocketWrapper} class documentation about the need
   * to synchronise around any usage of the returned {@code InputStream}.
   *
   * @return The input stream.
   */
  public InputStream getInputStream() {
    return m_inputStream;
  }

  /**
   * See note in {@link SocketWrapper} class documentation about the need
   * to synchronise around any usage of the returned {@code OutputStream}.
   *
   * @return The output stream.
   */
  public OutputStream getOutputStream() {
    return m_outputStream;
  }

  /**
   * Socket event notification interface.
   */
  public interface ClosedListener {
    void socketClosed();
  }

  public void addClosedListener(ClosedListener listener) {
    m_closedListeners.add(listener);
  }

  /**
   * Set an external object that identifies this socket. This can be used in
   * conjunction with {@link FanOutServerSender#send} to address a particular
   * target.
   *
   * @param address
   *            The address. We only care about its equality semantics.
   */
  public void setAddress(Address address) {
    m_address = address;
  }

  /**
   * Return the address for this socket.
   *
   * @return The address, or {@code null} if no address has been set.
   */
  public Address getAddress() {
    return m_address;
  }
}
