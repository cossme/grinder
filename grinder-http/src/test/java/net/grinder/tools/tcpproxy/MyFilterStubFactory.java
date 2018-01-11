// Copyright (C) 2005 - 2009 Philip Aston
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

import net.grinder.testutility.RandomStubFactory;


public class MyFilterStubFactory extends RandomStubFactory<TCPProxyFilter> {
  private boolean m_resultSet;
  private byte[] m_result;

  public MyFilterStubFactory() {
    super(TCPProxyFilter.class);
  }

  public byte[] override_handle(Object proxy,
                                ConnectionDetails connectionDetails,
                                byte[] originalBuffer,
                                int bytesRead) {
    return m_resultSet ? m_result : originalBuffer;
  }

  public void setResult(byte[] result) {
    m_resultSet = true;
    m_result = result;
  }

  public void assertIsWrappedBy(TCPProxyFilter filter)
    throws Exception {

    assertNoMoreCalls();

    final ConnectionDetails connectionDetails =
      new ConnectionDetails(new EndPoint("A", 55),
                            new EndPoint("B", 80), false);

    filter.connectionOpened(connectionDetails);
    assertSuccess("connectionOpened", connectionDetails);

    filter.connectionClosed(connectionDetails);
    assertSuccess("connectionClosed", connectionDetails);

    filter.connectionOpened(connectionDetails);
    assertSuccess("connectionOpened", connectionDetails);

    filter.connectionClosed(connectionDetails);
    assertSuccess("connectionClosed", connectionDetails);

    // We don't test stop() here because our wrapper prevents it from being
    // called more than once, so we would break future tests.

    assertNoMoreCalls();
  }
}
