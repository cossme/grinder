// Copyright (C) 2003 - 2013 Philip Aston
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.Socket;

import net.grinder.common.TimeAuthority;
import net.grinder.util.StandardTimeAuthority;

import org.junit.Test;


/**
 *  Unit tests for {@link FanOutServerSender}.
 *
 * @author Philip Aston
 */
public class TestFanOutServerSender {

  private final TimeAuthority m_timeAuthority = new StandardTimeAuthority();

  @Test public void testConstructor() throws Exception {

    final Acceptor acceptor = new Acceptor("localhost", 0, 1, m_timeAuthority);

    final FanOutServerSender serverSender =
      new FanOutServerSender(acceptor, ConnectionType.AGENT, 3);

    serverSender.shutdown();
    acceptor.shutdown();
  }

  @Test public void testSend() throws Exception {

    final Acceptor acceptor = new Acceptor("localhost", 0, 1, m_timeAuthority);

    final FanOutServerSender serverSender =
      new FanOutServerSender(acceptor, ConnectionType.AGENT, 3);

    final Socket[] socket = new Socket[5];

    for (int i=0; i<socket.length; ++i) {
      socket[i] = new Connector(InetAddress.getByName(null).getHostName(),
                                acceptor.getPort(),
                                ConnectionType.AGENT).connect();
    }

    // Sleep until we've accepted all connections. Give up after a few
    // seconds.
    final ResourcePool socketSet = acceptor.getSocketSet(ConnectionType.AGENT);

    for (int i=0; socketSet.countActive() != 5 && i<10; ++i) {
      Thread.sleep(i * i * 10);
    }

    final SimpleMessage message1 = new SimpleMessage();
    final SimpleMessage message2 = new SimpleMessage();

    serverSender.send(message1);
    serverSender.send(message2);

    for (final Socket element : socket) {
      final InputStream socketInput = element.getInputStream();

      final Message m1 = readMessage(socketInput);
      final Message m2 = readMessage(socketInput);

      assertEquals(message1, m1);
      assertEquals(message2, m2);

      assertEquals(0, socketInput.available());

      element.close();
    }

    serverSender.shutdown();
    acceptor.shutdown();
  }

  private static Message readMessage(final InputStream socketInput)
    throws Exception {

    for (int i = 0; socketInput.available() == 0 && i < 5; ++i) {
      Thread.sleep(i * i * 10);
    }

    if (socketInput.available() == 0) {
      return null;
    }

    return (Message)new ObjectInputStream(socketInput).readObject();
  }

  @Test public void testSendAddressedMessage() throws Exception {

    final Acceptor acceptor = new Acceptor("localhost", 0, 1, m_timeAuthority);

    final FanOutServerSender serverSender =
      new FanOutServerSender(acceptor, ConnectionType.AGENT, 3);

    final Socket[] socket = new Socket[5];

    for (int i = 0; i < socket.length; ++i) {
      socket[i] =
        new Connector(InetAddress.getByName(null).getHostName(),
                      acceptor.getPort(),
                      ConnectionType.AGENT)
        .connect(new StubAddress(new Integer(i)));
    }

    // Sleep until we've accepted all connections. Give up after a few
    // seconds.
    final ResourcePool socketSet = acceptor.getSocketSet(ConnectionType.AGENT);

    for (int i = 0; socketSet.countActive() != 5 && i < 10; ++i) {
      Thread.sleep(i * i * 10);
    }

    final SimpleMessage message1 = new SimpleMessage();
    final SimpleMessage message2 = new SimpleMessage();

    serverSender.send(new StubAddress(new Integer(1)), message1);
    serverSender.send(new StubAddress(new Integer(2)), message2);

    for (int i = 0; i < socket.length; ++i) {
      final InputStream socketInput = socket[i].getInputStream();

      if (i == 1) {
        final Message m = readMessage(socketInput);
        assertEquals(message1, m);
      }
      else if (i == 2) {
        final Message m = readMessage(socketInput);
        assertEquals(message2, m);
      }

      assertEquals(0, socketInput.available());

      socket[i].close();
    }

    serverSender.shutdown();

    try {
      serverSender.send(new Address() {
        @Override
        public boolean includes(final Address address) { return false; }
        },
        message1);
      fail("Expected CommunicationException");
    }
    catch (final CommunicationException e) {
    }

    acceptor.shutdown();
  }

  @Test public void testShutdown() throws Exception {

    final Acceptor acceptor = new Acceptor("localhost", 0, 1, m_timeAuthority);

    final FanOutServerSender serverSender =
      new FanOutServerSender(acceptor, ConnectionType.AGENT, 3);

    final Socket socket =
      new Connector(InetAddress.getByName(null).getHostName(),
        acceptor.getPort(),
        ConnectionType.AGENT).connect();

    // Sleep until we've accepted the connection. Give up after a few
    // seconds.
    final ResourcePool socketSet =
      acceptor.getSocketSet(ConnectionType.AGENT);

    for (int i=0; socketSet.countActive() != 1 && i<10; ++i) {
      Thread.sleep(i * i * 10);
    }

    final Message message = new SimpleMessage();
    serverSender.send(message);

    final InputStream socketStream = socket.getInputStream();

    final Object o1 = readMessage(socketStream);
    assertNotNull(o1);

    serverSender.shutdown();

    try {
      serverSender.send(message);
      fail("Expected CommunicationException");
    }
    catch (final CommunicationException e) {
    }

    try {
      final Object o2 = readMessage(socketStream);

      assertTrue(o2 instanceof CloseCommunicationMessage);
    }
    catch (final StreamCorruptedException e) {
      // Occasionally this occurs because the connection is shutdown.
      // Whatever.
    }

    acceptor.shutdown();
  }
 }
