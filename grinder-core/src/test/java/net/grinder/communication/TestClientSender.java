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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import net.grinder.communication.BlockingSender.NoResponseException;
import net.grinder.util.StreamCopier;

import org.junit.Test;


/**
 *  Unit test case for {@code ClientSender}.
 *
 * @author Philip Aston
 */
public class TestClientSender {

  @Test public void testSend() throws Exception {

    final SocketAcceptorThread socketAcceptor = SocketAcceptorThread.create();

    final Connector connector =
      new Connector(socketAcceptor.getHostName(), socketAcceptor.getPort(),
                    ConnectionType.AGENT);

    final Sender clientSender = ClientSender.connect(connector, null);

    socketAcceptor.join();

    final SimpleMessage message1 = new SimpleMessage();
    final SimpleMessage message2 = new SimpleMessage();

    clientSender.send(message1);
    clientSender.send(message2);

    final InputStream socketInput =
      socketAcceptor.getAcceptedSocket().getInputStream();

    assertEquals(ConnectionType.AGENT,
                 Connector.read(socketInput).getConnectionType());

    // Need an ObjectInputStream for every message. See note in
    // ClientSender.writeMessage.
    final ObjectInputStream inputStream1 = new ObjectInputStream(socketInput);
    final Object o1 = inputStream1.readObject();

    final ObjectInputStream inputStream2 = new ObjectInputStream(socketInput);
    final Object o2 = inputStream2.readObject();

    assertEquals(message1, o1);
    assertEquals(message2, o2);

    assertEquals(0, socketInput.available());

    socketAcceptor.close();

    try {
      ClientReceiver.connect(connector, new StubAddress());
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }
  }

  @Test public void testShutdown() throws Exception {

    final SocketAcceptorThread socketAcceptor = SocketAcceptorThread.create();

    final Connector connector =
      new Connector(socketAcceptor.getHostName(), socketAcceptor.getPort(),
                    ConnectionType.AGENT);

    final Sender clientSender = ClientSender.connect(connector, null);

    socketAcceptor.join();

    final Message message = new SimpleMessage();

    clientSender.send(message);

    clientSender.shutdown();

    try {
      clientSender.send(message);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    final InputStream socketInput =
      socketAcceptor.getAcceptedSocket().getInputStream();

    assertEquals(ConnectionType.AGENT,
                 Connector.read(socketInput).getConnectionType());

    final ObjectInputStream inputStream1 = new ObjectInputStream(socketInput);
    final Object o1 = inputStream1.readObject();
    assertNotNull(o1);

    final ObjectInputStream inputStream2 = new ObjectInputStream(socketInput);
    final Object o2 = inputStream2.readObject();

    assertTrue(o2 instanceof CloseCommunicationMessage);

    socketAcceptor.close();
  }

  @Test public void testShutdownPeerDifferently() throws Exception {
    final SocketAcceptorThread socketAcceptor = SocketAcceptorThread.create();

    final Connector connector =
      new Connector(socketAcceptor.getHostName(), socketAcceptor.getPort(),
                    ConnectionType.AGENT);

    final ClientSender clientSender = ClientSender.connect(connector, null);

    socketAcceptor.join();

    new StreamSender(socketAcceptor.getAcceptedSocket().getOutputStream())
      .writeMessage(new CloseCommunicationMessage());

    try {
      clientSender.blockingSend(new SimpleMessage());
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    socketAcceptor.close();
  }

  @Test public void testWithPairedClientReceiver() throws Exception {
    final SocketAcceptorThread socketAcceptor = SocketAcceptorThread.create();

    final Connector connector =
      new Connector(socketAcceptor.getHostName(), socketAcceptor.getPort(),
                    ConnectionType.AGENT);

    final ClientReceiver clientReceiver =
      ClientReceiver.connect(connector, new StubAddress());
    final ClientSender clientSender = ClientSender.connect(clientReceiver);

    socketAcceptor.join();

    // Wire up the remote end to simply copy the bytes back to us.
    final Socket remoteSocket = socketAcceptor.getAcceptedSocket();
    final InputStream inputStream = remoteSocket.getInputStream();
    assertEquals(ConnectionType.AGENT,
                 Connector.read(inputStream).getConnectionType());

    new Thread(
      new StreamCopier(1000, true).getRunnable(inputStream,
                                               remoteSocket.getOutputStream()),
      "Echo stream").start();

    final Message message = new SimpleMessage();
    clientSender.send(message);
    final Message receivedMessage = clientReceiver.waitForMessage();

    assertEquals(receivedMessage, message);

    clientReceiver.shutdown();
  }

  @Test public void testWithBadPairedClientReceiver() throws Exception {
    final SocketAcceptorThread socketAcceptor = SocketAcceptorThread.create();

    final Connector connector =
      new Connector(socketAcceptor.getHostName(), socketAcceptor.getPort(),
                    ConnectionType.AGENT);

    final ClientReceiver clientReceiver =
      ClientReceiver.connect(connector, new StubAddress());
    clientReceiver.shutdown();

    // The connection health is not checked on connect().
    final ClientSender sender = ClientSender.connect(clientReceiver);

    try {
      sender.send(null);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }
  }

  @Test public void testBlockingSend() throws Exception {

    final SocketAcceptorThread socketAcceptor = SocketAcceptorThread.create();

    final Connector connector =
      new Connector(socketAcceptor.getHostName(), socketAcceptor.getPort(),
                    ConnectionType.AGENT);

    final BlockingSender clientSender = ClientSender.connect(connector, null);

    socketAcceptor.join();

    final Socket acceptedSocket = socketAcceptor.getAcceptedSocket();
    final InputStream socketInput = acceptedSocket.getInputStream();
    final OutputStream socketOutput = acceptedSocket.getOutputStream();

    assertEquals(ConnectionType.AGENT,
                 Connector.read(socketInput).getConnectionType());

    final SimpleMessage message1 = new SimpleMessage();

    final ReceiveOneMessageAndReply receiver1 =
      new ReceiveOneMessageAndReply(socketInput, socketOutput);
    receiver1.start();

    final Object received1 = clientSender.blockingSend(message1);
    assertEquals(message1, received1);
    receiver1.join();
    assertNull(receiver1.getException());

    final NoResponseMessage message2 = new NoResponseMessage();

    final ReceiveOneMessageAndReply receiver2 =
      new ReceiveOneMessageAndReply(socketInput, socketOutput);
    receiver2.start();

    try {
      clientSender.blockingSend(message2);
      fail("Expected NoResponseException");
    }
    catch (NoResponseException e) {
    }

    receiver2.join();
    assertNull(receiver2.getException());

    socketAcceptor.close();
  }

  private static final class ReceiveOneMessageAndReply extends Thread {

    private final InputStream m_inputStream;
    private final OutputStream m_outputStream;
    private Exception m_exception;

    public ReceiveOneMessageAndReply(InputStream inputStream,
                                     OutputStream outputStream) {
      m_inputStream = inputStream;
      m_outputStream = outputStream;
    }

    public void run() {
      // Need an ObjectInputStream for every message. See note in
      // ClientSender.writeMessage.
      try {
        final ObjectInputStream inputStream =
          new ObjectInputStream(m_inputStream);
        final MessageRequiringResponse responseSender =
          (MessageRequiringResponse)inputStream.readObject();

        assert m_inputStream.available() == 0;

        final ObjectOutputStream outputStream =
          new ObjectOutputStream(m_outputStream);
        outputStream.writeObject(responseSender.getMessage());
        outputStream.flush();
      }
      catch (Exception e) {
        m_exception = e;
      }
    }

    public Exception getException() {
      return m_exception;
    }
  }
}
