// Copyright (C) 2000 Phil Dawes
// Copyright (C) 2000 - 2012 Philip Aston
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

import net.grinder.common.GrinderException;


/**
 * Interface that TCP Proxy filters must implement.
 *
 * <p>
 * In addition, filters are registered with a PicoContainer. They can learn
 * about a {@code Logger}, other filters, and custom components (see the
 * TCPProxy {@code -component} option) through constructor injection. Filters
 * can also implement the PicoContainer life cycle methods
 * {@link org.picocontainer.Startable} and {@link org.picocontainer.Disposable}.
 * </p>
 *
 * <p>
 * Well behaved filters should not swallow {@link InterruptedException}s.
 * Ideally, they should convert {@link InterruptedException}s to
 * {@link net.grinder.common.UncheckedInterruptedException}s; but alternatively
 * they could reinstate the Thread's interrupted status.
 * </p>
 *
 * <p>
 * The {@link ConnectionDetails#equals equality semantics} of the
 * {@code ConnectionDetails} passed to each method can be used to uniquely
 * identify a connection.
 * </p>
 *
 * @author Philip Aston
 * @author Bertrand Ave
 */
public interface TCPProxyFilter {

  /**
   * Handle a message fragment from the stream.
   *
   * @param connectionDetails Describes the connection.
   * @param buffer Contains the data.
   * @param bytesRead How many bytes of data in <code>buffer</code>.
   * @return Filters can optionally return a <code>byte[]</code>
   * which will be transmitted to the server instead of
   * <code>buffer</code>.
   * @throws TCPProxyFilter.FilterException If an error occurs.
   */
  byte[] handle(ConnectionDetails connectionDetails, byte[] buffer,
                int bytesRead)
    throws TCPProxyFilter.FilterException;

  /**
   * A new connection has been opened.
   *
   * @param connectionDetails Describes the connection.
   * @throws TCPProxyFilter.FilterException If an error occurs.
   */
  void connectionOpened(ConnectionDetails connectionDetails)
  throws TCPProxyFilter.FilterException;

  /**
   * A connection has been closed.
   *
   * @param connectionDetails Describes the connection.
   * @throws TCPProxyFilter.FilterException If an error occurs.
   */
  void connectionClosed(ConnectionDetails connectionDetails)
  throws TCPProxyFilter.FilterException;

  /**
   * Exception type for filter problems.
   */
  class FilterException extends GrinderException {
    public FilterException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}

