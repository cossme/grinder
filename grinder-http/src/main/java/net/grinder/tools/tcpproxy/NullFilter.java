// Copyright (C) 2000 Phil Dawes
// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
// Copyright (C) 2003 Bertrand Ave
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


/**
 * Filter that does nothing.
 *
 * @author Philip Aston
 * @author Bertrand Ave
 */
public class NullFilter implements TCPProxyFilter {

  /**
   * Constructor.
   */
  public NullFilter() {
  }

  /**
   * Handle a message fragment from the stream.
   *
   * @param connectionDetails Describes the connection.
   * @param buffer Contains the data.
   * @param bytesRead How many bytes of data in <code>buffer</code>.
   * @return Filters can optionally return a <code>byte[]</code>
   * which will be transmitted to the server instead of
   * <code>buffer</code>.
   */
  public byte[] handle(ConnectionDetails connectionDetails, byte[] buffer,
                       int bytesRead) {
    return null;
  }

  /**
   * A new connection has been opened.
   *
   * @param connectionDetails Describes the connection.
   */
  public void connectionOpened(ConnectionDetails connectionDetails) {
  }

  /**
   * A connection has been closed.
   *
   * @param connectionDetails Describes the connection.
   */
  public void connectionClosed(ConnectionDetails connectionDetails) {
  }
}
