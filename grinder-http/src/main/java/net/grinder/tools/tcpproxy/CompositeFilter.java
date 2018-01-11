// Copyright (C) 2003 - 2011 Philip Aston
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

import java.util.ArrayList;
import java.util.List;


/**
 * Composite TCPProxyFilter.
 *
 * @author Philip Aston
 */
public final class CompositeFilter implements TCPProxyFilter {

  private final List<TCPProxyFilter> m_filters =
    new ArrayList<TCPProxyFilter>();

  /**
   * {@inheritDoc}
   */
  @Override public byte[] handle(ConnectionDetails connectionDetails,
                                 byte[] originalBuffer,
                                 int bytesRead)
    throws FilterException {

    byte[] nextBuffer = originalBuffer;
    int nextBytesRead = bytesRead;

    for (TCPProxyFilter filter : m_filters) {
      final byte[] buffer =
        filter.handle(connectionDetails, nextBuffer, nextBytesRead);

      if (buffer != null) {
        nextBuffer = buffer;
        nextBytesRead = buffer.length;
      }
    }

    return nextBuffer != originalBuffer ? nextBuffer : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void connectionOpened(final ConnectionDetails connectionDetails)
    throws FilterException {

    for (TCPProxyFilter filter : m_filters) {
      filter.connectionOpened(connectionDetails);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void connectionClosed(final ConnectionDetails connectionDetails)
    throws FilterException {

    for (TCPProxyFilter filter : m_filters) {
      filter.connectionClosed(connectionDetails);
    }
  }

  /**
   * Add a filter to the composite.
   *
   * @param filter The filter.
   */
  public void add(TCPProxyFilter filter) {
    m_filters.add(filter);
  }

  /**
   * Access to composed filters.
   *
   * @return The filters.
   */
  TCPProxyFilter[] getFilters() {
    return m_filters.toArray(new TCPProxyFilter[m_filters.size()]);
  }

  /**
   * Describe the filter.
   *
   * @return The description.
   */
  public String toString() {
    final StringBuilder result = new StringBuilder();

    for (TCPProxyFilter filter : m_filters) {
      if (result.length() > 0) {
        result.append(", ");
      }

      result.append(filter.getClass().getSimpleName());
    }

    return result.toString();
  }
}

