// Copyright (C) 2000 Phil Dawes
// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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

import java.io.IOException;
import java.net.Socket;


/**
 * Socket factory interface for SSL sockets.
 *
 * @author Philip Aston
 */
public interface TCPProxySSLSocketFactory extends TCPProxySocketFactory {

  /**
   *<p>Factory method for client sockets that are layered over existing
   * sockets.</p>
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
  Socket createClientSocket(Socket existingSocket, EndPoint remoteEndPoint)
    throws IOException;
}

