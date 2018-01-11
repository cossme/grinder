// Copyright (C) 2000 - 2009 Philip Aston
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

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Class that represents the endpoint of a TCP connection. One day,
 * when we can depend on JDK 1.4, this may be replaced by
 * <code>java.net.InetSocketAddress</code>.
 *
 * @author <a href="mailto:paston@bea.com">Philip Aston</a>
 */
public final class EndPoint implements Comparable<EndPoint> {

  private final String m_host;
  private final int m_port;
  private final int m_hashCode;

  /**
   * Constructor.
   *
   * @param host Host name or IP address.
   * @param port Port.
   */
  public EndPoint(String host, int port) {

    m_host = host.toLowerCase();
    m_port = port;

    m_hashCode = m_host.hashCode() ^ m_port;
  }

  /**
   * Constructor.
   *
   * <p>In a perfect world, EndPoint would use
   * <code>InetAddress</code> in its internal representation.</p>
   *
   * @param address Address.
   * @param port Port
   */
  public EndPoint(InetAddress address, int port) {
    this(address.getHostName(), port);
  }

  /**
   * Accessor.
   *
   * @return Host name or IP address.
   */
  public String getHost() {
    return m_host;
  }

  /**
   * Accessor.
   *
   * @return an <code>int</code> value
   */
  public int getPort() {
    return m_port;
  }

  /**
   * Value based equality.
   *
   * @param other an <code>Object</code> value
   * @return <code>true</code> => <code>other</code> is equal to this object.
   */
  public boolean equals(Object other) {

    if (other == this) {
      return true;
    }

    if (other == null || other.getClass() != EndPoint.class) {
      return false;
    }

    final EndPoint otherEndPoint = (EndPoint)other;

    return
      hashCode() == otherEndPoint.hashCode() &&
      getPort() == otherEndPoint.getPort() &&
      getHost().equals(otherEndPoint.getHost());
  }

  /**
   * Implement {@link Object#hashCode}.
   *
   * @return The hash code.
   */
  public int hashCode() {
    return m_hashCode;
  }

  /**
   * String representation.
   *
   * @return The string.
   */
  public String toString()  {
    return m_host + ":" + m_port;
  }

  /**
   * Implement <code>Comparable</code> so that we can order pairs of
   * EndPoint's consistently.
   *
   * @param otherEndPoint Object to be compared.
   * @return A negative integer, zero, or a positive integer as this
   * object is less than, equal to, or greater than the specified
   * object.
   */
  public int compareTo(EndPoint otherEndPoint) {
    final int c = getHost().compareTo(otherEndPoint.getHost());

    if (c != 0) {
      return c;
    }
    else {
      return getPort() - otherEndPoint.getPort();
    }
  }

  /**
   * Return an <code>EndPoint</code> describing the remote (client)
   * side of the given socket.
   *
   * @param socket The socket.
   * @return The end point.
   */
  public static EndPoint clientEndPoint(Socket socket) {
    return new EndPoint(socket.getInetAddress(), socket.getPort());
  }

  /**
   * Return an <code>EndPoint</code> describing the local (server)
   * side of the given server socket.
   *
   * @param socket The server socket.
   * @return The end point.
   */
  public static EndPoint serverEndPoint(ServerSocket socket) {
    return new EndPoint(socket.getInetAddress(), socket.getLocalPort());
  }
}
