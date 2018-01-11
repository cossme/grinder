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
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Socket factory interface.
 *
 * @author Philip Aston
 */
public interface TCPProxySocketFactory {

  /**
   * Factory method for server sockets.
   *
   * @param localEndPoint Local host and port.
   * @param timeout Socket timeout.
   * @return A new <code>ServerSocket</code>.
   * @exception IOException If an error occurs.
   */
  ServerSocket createServerSocket(EndPoint localEndPoint, int timeout)
    throws IOException;

  /**
   * Factory method for client sockets.
   *
   * @param remoteEndPoint Remote host and port.
   * @return A new <code>Socket</code>.
   * @exception IOException If an error occurs.
   */
  Socket createClientSocket(EndPoint remoteEndPoint) throws IOException;
}

