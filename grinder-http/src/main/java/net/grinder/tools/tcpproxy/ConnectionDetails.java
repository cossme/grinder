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


/**
 * Class that represents a TCP connection.
 *
 * @see #equals for identity semantics.
 *
 * @author Philip Aston
 */
public final class ConnectionDetails {
  private final EndPoint m_localEndPoint;
  private final EndPoint m_remoteEndPoint;
  private final boolean m_isSecure;
  private final String m_connectionDescription;

  private final ConnectionDetails m_origin;

  /**
   * Creates a new <code>ConnectionDetails</code> instance.
   *
   * @param localEndPoint Local host and port.
   * @param remoteEndPoint Remote host and port.
   * @param isSecure Whether the connection is secure.
   * @throws IllegalArgumentException If local and remote details are the same.
   */
  public ConnectionDetails(EndPoint localEndPoint, EndPoint remoteEndPoint,
                           boolean isSecure) {

    m_localEndPoint = localEndPoint;
    m_remoteEndPoint = remoteEndPoint;
    m_isSecure = isSecure;
    m_origin = this;

    m_connectionDescription =
      createConnectionDescription(localEndPoint, remoteEndPoint, isSecure);
  }

  private ConnectionDetails(EndPoint localEndPoint, EndPoint remoteEndPoint,
    boolean isSecure, ConnectionDetails origin) {

    m_localEndPoint = localEndPoint;
    m_remoteEndPoint = remoteEndPoint;
    m_isSecure = isSecure;
    m_origin = origin;

    m_connectionDescription =
      createConnectionDescription(localEndPoint, remoteEndPoint, isSecure);
  }

  private String createConnectionDescription(EndPoint localEndPoint,
    EndPoint remoteEndPoint, boolean isSecure) {
    final int c = localEndPoint.compareTo(remoteEndPoint);

    if (c == 0) {
      throw new IllegalArgumentException(
        "Local and remote sockets are the same");
    }

    if (c < 0) {
      return localEndPoint + "|" + remoteEndPoint + "|" + isSecure;
    }
    else {
      return remoteEndPoint + "|" + localEndPoint + "|" + isSecure;
    }
  }

  /**
   * String representation of the connection.
   *
   * @return a <code>String</code> value
   */
  public String toString() {
    return m_localEndPoint + "->" + m_remoteEndPoint;
  }

  /**
   * Accessor.
   *
   * @return a <code>boolean</code> value
   */
  public boolean isSecure() {
    return m_isSecure;
  }

  /**
   * Accessor.
   *
   * @return a <code>String</code> value
   */
  public EndPoint getRemoteEndPoint() {
    return m_remoteEndPoint;
  }

  /**
   * Accessor.
   *
   * @return a <code>String</code> value
   */
  public EndPoint getLocalEndPoint() {
    return m_localEndPoint;
  }

  /**
   * Equality. Two ConnectionDetails are equal if and only if all of the
   * following are true:
   *
   * <ul>
   * <li>they represent the same ordered pair of end points</li>
   * <li>they are identical, or derived from, the same ConnectionDetails</li>
   * </ul>
   *
   * <p>
   * It follows that <code>cd.equals(cd.getOtherEnd())</code> is false;
   * <code>cd.equals(new ConnectionDetails(cd.getLocalEndPoint(),
   * cd.getRemoteEndPoint(), cd.isSecure)</code>
   * is false; <code>cd.equals(cd.getOtherEnd().getOtherEnd())</code>
   * is true.
   * </p>
   *
   * @param other
   *            an <code>Object</code> value
   * @return <code>true</code> => <code>other</code> is equal to this
   *         object.
   */
  public boolean equals(Object other) {

    if (other == this) {
      return true;
    }

    if (other == null || other.getClass() != ConnectionDetails.class) {
      return false;
    }

    final ConnectionDetails otherConnectionDetails =
      (ConnectionDetails)other;

    // We use the reference identity of m_origin to determine whether we
    // are derived from the same ConnectionIdentity.
    return m_origin == otherConnectionDetails.m_origin &&
           getLocalEndPoint().equals(otherConnectionDetails.getLocalEndPoint());
  }

  /**
   * Implement {@link Object#hashCode}.
   *
   * @return The hash code.
   */
  public int hashCode() {
    return m_connectionDescription.hashCode();
  }

  /**
   * Return a <code>String</code> that represents the connection.
   * <code>ConnectionDetails</code> representing either end of the
   * same connection always return the same thing.
   *
   * @return Represents the connection.
   */
  public String getConnectionIdentity() {
    return m_connectionDescription;
  }

  /**
   * Return a <code>ConnectionDetails</code> representing the other
   * end of the connection.
   *
   * @return The other end of the connection to this.
   */
  public ConnectionDetails getOtherEnd() {
    return new ConnectionDetails(
      getRemoteEndPoint(), getLocalEndPoint(), isSecure(), m_origin);
  }
}
