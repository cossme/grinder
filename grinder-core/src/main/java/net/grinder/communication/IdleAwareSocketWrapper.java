// Copyright (C) 2012 - 2013 Philip Aston
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
import java.net.Socket;

import net.grinder.common.TimeAuthority;

/**
 * A socket wrapper that tracks how long it has been idle.
 *
 * @author Philip Aston
 */
final class IdleAwareSocketWrapper extends SocketWrapper {

  private final TimeAuthority m_timeAuthority;

  // Guarded by parent's inputStream. The time at which a socket was first found
  // to be idle, or -1.
  private long m_idleStart;


  /**
   * Constructor.
   *
   * @param socket
   *          Socket to wrap. If the caller maintains any references to the
   *          socket, if should synchronise access to the socket streams as
   *          described in {@link SocketWrapper}.
   * @param timeAuthority
   *          Something that knows the time.
   * @param idlePollTimeout
   *          The duration in milliseconds after which a socket with no
   *          available input data is closed. This is checked in
   *          {@link #hasData()}.
   * @throws CommunicationException
   *           If an error occurred.
   */
  public IdleAwareSocketWrapper(final Socket socket,
                                final TimeAuthority timeAuthority)
    throws CommunicationException {

    super(socket);

    m_timeAuthority = timeAuthority;
    m_idleStart = -1;

  }

  /**
   * Check whether the socket's input stream has any data.
   *
   * <p>
   * Java only detects that a peer has closed a connection if we do a blocking
   * read, or attempt to send data. To avoid zombie sockets in a
   * {@code CLOSE_WAIT} state, if repeated calls this method detect that the
   * socket has had no input data for more than inactiveClientTimeOut, the
   * socket will be closed. We expect the caller to clean up using a
   * {@link ClosedListener}.
   * </p>
   *
   * @param inactiveClientTimeOut
   *          Time out in milliseconds for connections that present no data.
   * @return {@code true} If there is data available to be read from the input
   *         stream.
   * @throws IOException
   *           If the socket was closed at the start of the call.
   */
  public boolean hasData(final long inactiveClientTimeOut) throws IOException {

    if (isClosed()) {
      throw new IOException("Socket is closed");
    }

    final InputStream inputStream = getInputStream();

    synchronized (inputStream) {
      if (inputStream.available() > 0) {
        m_idleStart = -1;
        return true;
      }

      final long now = m_timeAuthority.getTimeInMilliseconds();

      if (m_idleStart == -1) {
        m_idleStart = now;
      }
      else if (m_idleStart + inactiveClientTimeOut < now) {
        close();
      }

      return false;
    }
  }
}
