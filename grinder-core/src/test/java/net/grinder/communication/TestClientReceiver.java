// Copyright (C) 2003 - 2012 Philip Aston
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import net.grinder.communication.Connector.ConnectDetails;

import org.junit.Test;


/**
 *  Unit tests for {@link ClientReceiver}.
 *
 * @author Philip Aston
 */
public class TestClientReceiver {

  @Test public void testReceive() throws Exception {

    final SocketAcceptorThread socketAcceptor = SocketAcceptorThread.create();

    final Connector connector =
      new Connector(socketAcceptor.getHostName(), socketAcceptor.getPort(),
                    ConnectionType.AGENT);

    final Address address = new StubAddress();

    final Receiver clientReceiver = ClientReceiver.connect(connector, address);

    socketAcceptor.join();

    final Socket acceptedSocket = socketAcceptor.getAcceptedSocket();

    assertConnection(
      acceptedSocket.getInputStream(), ConnectionType.AGENT, address);

    final OutputStream socketOutput = acceptedSocket.getOutputStream();

    final SimpleMessage message1 = new SimpleMessage();

    final ObjectOutputStream objectStream1 =
      new ObjectOutputStream(socketOutput);
    objectStream1.writeObject(message1);
    objectStream1.flush();

    final SimpleMessage message2 = new SimpleMessage();

    final ObjectOutputStream objectStream2 =
      new ObjectOutputStream(socketOutput);
    objectStream2.writeObject(message2);
    objectStream2.flush();

    final Message receivedMessage1 = clientReceiver.waitForMessage();
    final Message receivedMessage2 = clientReceiver.waitForMessage();

    assertEquals(message1, receivedMessage1);
    assertEquals(message2, receivedMessage2);

    socketAcceptor.close();

    try {
      ClientReceiver.connect(connector, address);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }
  }

  private void assertConnection(InputStream in,
                                ConnectionType type,
                                Address address) throws Exception {
    final ConnectDetails details = Connector.read(in);
    assertEquals(type, details.getConnectionType());
    assertEquals(address, details.getAddress());
  }

  @Test public void testShutdown() throws Exception {

    final SocketAcceptorThread socketAcceptor = SocketAcceptorThread.create();

    final Connector connector =
      new Connector(socketAcceptor.getHostName(), socketAcceptor.getPort(),
                    ConnectionType.AGENT);

    final Address address = new StubAddress();

    final Receiver clientReceiver = ClientReceiver.connect(connector, address);

    socketAcceptor.join();

    final Socket acceptedSocket = socketAcceptor.getAcceptedSocket();
    assertConnection(
      acceptedSocket.getInputStream(), ConnectionType.AGENT, address);

    final OutputStream socketOutput = acceptedSocket.getOutputStream();

    final SimpleMessage message1 = new SimpleMessage();

    final ObjectOutputStream objectStream1 =
      new ObjectOutputStream(socketOutput);
    objectStream1.writeObject(message1);
    objectStream1.flush();

    final Message receivedMessage = clientReceiver.waitForMessage();
    assertNotNull(receivedMessage);

    clientReceiver.shutdown();

    assertNull(clientReceiver.waitForMessage());

    socketAcceptor.close();
  }

  @Test public void testCloseCommunicationMessage() throws Exception {

    final SocketAcceptorThread socketAcceptor = SocketAcceptorThread.create();

    final Connector connector =
      new Connector(socketAcceptor.getHostName(), socketAcceptor.getPort(),
                    ConnectionType.AGENT);

    final Address address = new StubAddress();

    final Receiver clientReceiver = ClientReceiver.connect(connector, address);

    socketAcceptor.join();

    final Socket acceptedSocket = socketAcceptor.getAcceptedSocket();

    assertConnection(
      acceptedSocket.getInputStream(), ConnectionType.AGENT, address);

    final OutputStream socketOutput = acceptedSocket.getOutputStream();

    final SimpleMessage message1 = new SimpleMessage();

    final ObjectOutputStream objectStream1 =
      new ObjectOutputStream(socketOutput);
    objectStream1.writeObject(message1);
    objectStream1.flush();

    final Message receivedMessage = clientReceiver.waitForMessage();
    assertNotNull(receivedMessage);

    final Message closeCommunicationMessage = new CloseCommunicationMessage();

    final ObjectOutputStream objectStream2 =
      new ObjectOutputStream(socketOutput);
    objectStream2.writeObject(closeCommunicationMessage);
    objectStream2.flush();

    assertNull(clientReceiver.waitForMessage());

    socketAcceptor.close();
  }
}

