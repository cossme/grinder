// Copyright (C) 2004 - 2012 Philip Aston
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

import java.net.InetAddress;


/**
 * Value object that represents the identity of an accepted
 * connection.
 *
 * @author Philip Aston
 */
public final class ConnectionIdentity {

  private final InetAddress m_inetAddress;
  private final int m_port;
  private final long m_connectionTime;

  /**
   * Constructor.
   *
   * @param inetAddress TCP address of connection.
   * @param port TCP port of connection.
   * @param connectionTime Connection time - milliseconds since the
   * Epoch.
   */
  ConnectionIdentity(InetAddress inetAddress, int port, long connectionTime) {
    m_inetAddress = inetAddress;
    m_port = port;
    m_connectionTime = connectionTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override public int hashCode() {
    return (int)m_connectionTime ^ m_port;
  }

  /**
   * {@inheritDoc}
   */
  @Override public boolean equals(Object o) {

    if (o == this) {
      return true;
    }

    if (o == null || o.getClass() != ConnectionIdentity.class) {
      return false;
    }

    final ConnectionIdentity other = (ConnectionIdentity) o;

    return
      m_connectionTime == other.m_connectionTime &&
      m_port == other.m_port &&
      m_inetAddress.equals(other.m_inetAddress);
  }
}
