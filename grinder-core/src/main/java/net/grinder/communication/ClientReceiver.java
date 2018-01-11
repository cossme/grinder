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

package net.grinder.communication;


/**
 * Manages receipt of messages from a server over a TCP connection.
 *
 * @author Philip Aston
 */
public final class ClientReceiver extends StreamReceiver {

  /**
   * Factory method that makes a TCP connection and returns a corresponding
   * <code>Receiver</code>.
   *
   * @param connector
   *            Connector to use to make the connection to the server.
   * @param address
   *            The address of this ClientReceiver - can be used with
   *            {@link FanOutServerSender#send(Address, Message)}.
   * @return The ClientReceiver.
   * @throws CommunicationException
   *             If failed to connect.
   */
  public static ClientReceiver connect(Connector connector, Address address)
    throws CommunicationException {

    return new ClientReceiver(new SocketWrapper(connector.connect(address)));
  }

  private final SocketWrapper m_socketWrapper;

  private ClientReceiver(SocketWrapper socketWrapper) {
    super(socketWrapper.getInputStream());
    m_socketWrapper = socketWrapper;
  }

  /**
   * {@inheritDoc}
   */
  @Override public void shutdown() {
    // Close the socket wrapper first as that needs to use the socket.
    m_socketWrapper.close();
    super.shutdown();
  }

  SocketWrapper getSocketWrapper() {
    return m_socketWrapper;
  }
}
