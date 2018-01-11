// Copyright (C) 2000 Phil Dawes
// Copyright (C) 2000 - 2012 Philip Aston
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
import java.io.PrintWriter;
import java.net.Socket;

import org.slf4j.Logger;

import net.grinder.common.UncheckedInterruptedException;

/**
 * Simple implementation of TCPProxyEngine that connects to a single remote
 * server.
 *
 * @author Phil Dawes
 * @author Philip Aston
 */
public final class PortForwarderTCPProxyEngine extends AbstractTCPProxyEngine {

  private final ConnectionDetails m_connectionDetails;

  /**
   * Constructor.
   *
   * @param requestFilter
   *          Request filter.
   * @param responseFilter
   *          Response filter.
   * @param output
   *          Output stream.
   * @param logger
   *          Logger.
   * @param connectionDetails
   *          Connection details.
   * @param useColour
   *          Whether to use colour.
   * @param timeout
   *          Timeout for server socket in milliseconds.
   *
   * @exception IOException
   *              If an I/O error occurs.
   */
  public PortForwarderTCPProxyEngine(TCPProxyFilter requestFilter,
                                     TCPProxyFilter responseFilter,
                                     PrintWriter output,
                                     Logger logger,
                                     ConnectionDetails connectionDetails,
                                     boolean useColour,
                                     int timeout) throws IOException {

    this(new TCPProxySocketFactoryImplementation(), requestFilter,
         responseFilter, output, logger, connectionDetails, useColour, timeout);
  }

  /**
   * Constructor that allows socket factory to be specified.
   *
   * @param socketFactory
   *          Socket factory.
   * @param requestFilter
   *          Request filter.
   * @param responseFilter
   *          Response filter.
   * @param output
   *          Where to send the output.
   * @param logger
   *          Logger.
   * @param connectionDetails
   *          Connection details.
   * @param useColour
   *          Whether to use colour.
   * @param timeout
   *          Timeout for server socket in milliseconds.
   *
   * @exception IOException
   *              If an I/O error occurs.
   */
  public PortForwarderTCPProxyEngine(TCPProxySocketFactory socketFactory,
                                     TCPProxyFilter requestFilter,
                                     TCPProxyFilter responseFilter,
                                     PrintWriter output,
                                     Logger logger,
                                     ConnectionDetails connectionDetails,
                                     boolean useColour,
                                     int timeout) throws IOException {

    super(socketFactory, requestFilter, responseFilter, output, logger,
          connectionDetails.getLocalEndPoint(), useColour, timeout);

    m_connectionDetails = connectionDetails;
  }

  /**
   * Main event loop.
   */
  public void run() {

    while (true) {
      final Socket localSocket;

      try {
        localSocket = accept();
      }
      catch (IOException e) {
        UncheckedInterruptedException.ioException(e);
        logIOException(e);
        return;
      }

      final EndPoint sourceEndPoint = EndPoint.clientEndPoint(localSocket);
      final EndPoint targetEndPoint = m_connectionDetails.getRemoteEndPoint();

      try {
        final Socket remoteSocket =
            getSocketFactory().createClientSocket(targetEndPoint);

        launchThreadPair(localSocket,
                         remoteSocket,
                         sourceEndPoint,
                         targetEndPoint,
                         m_connectionDetails.isSecure());
      }
      catch (IOException e) {
        UncheckedInterruptedException.ioException(e);
        logIOException(e);

        try {
          localSocket.close();
        }
        catch (IOException closeException) {
          throw new AssertionError(closeException);
        }
      }
    }
  }
}
