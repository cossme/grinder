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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static net.grinder.testutility.AssertUtilities.assertContains;
import static net.grinder.testutility.AssertUtilities.assertNotEquals;

import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;

import net.grinder.testutility.IsolatedObjectFactory;

import org.junit.Test;


/**
 *  Unit test case for {@code Connector}.
 *
 * @author Philip Aston
 */
public class TestConnector {

  @Test public void testConnnect() throws Exception {
    final SocketAcceptorThread socketAcceptor = SocketAcceptorThread.create();

    final Connector connector =
      new Connector(socketAcceptor.getHostName(), socketAcceptor.getPort(),
                    ConnectionType.WORKER);

    final Socket localSocket = connector.connect();

    socketAcceptor.join();

    final Socket serverSocket = socketAcceptor.getAcceptedSocket();
    final InputStream inputStream = serverSocket.getInputStream();

    assertEquals(ConnectionType.WORKER,
                 Connector.read(inputStream).getConnectionType());

    final byte[] text = "Hello".getBytes();

    localSocket.getOutputStream().write(text);

    for (int i=0; i<text.length; ++i) {
      assertEquals(text[i], inputStream.read());
    }

    socketAcceptor.close();

    try {
      connector.connect();
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    //For some reason, this connection is sometimes established.
    final Connector badConnector =
      new Connector("this is not a host name", 1234, ConnectionType.AGENT);

    try {
      badConnector.connect();
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }
  }

  @Test public void testBadRead() throws Exception {
    final PipedOutputStream out = new PipedOutputStream();
    final PipedInputStream in = new PipedInputStream(out);

    for (int x = 0; x < 100; ++x) {
      out.write(99);
    }

    try {
      Connector.read(in);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    final ObjectOutputStream objectStream = new ObjectOutputStream(out);
    objectStream.writeObject(ConnectionType.WORKER);
    objectStream.write(99);
    objectStream.writeObject(null);

    try {
      Connector.read(in);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    while (in.available() > 0) {
      in.read();
    }

    objectStream.writeObject(ConnectionType.WORKER);
    objectStream.writeObject(IsolatedObjectFactory.getIsolatedObject());

    try {
      Connector.read(in);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }
  }

  @Test public void testEquality() throws Exception {
    final Connector connector =
      new Connector("a", 1234, ConnectionType.WORKER);

    assertEquals(connector.hashCode(), connector.hashCode());
    assertEquals(connector, connector);
    assertNotEquals(connector, null);
    assertNotEquals(connector, this);

    final Connector[] equal = {
      new Connector("a", 1234, ConnectionType.WORKER),
    };

    final Connector[] notEqual = {
      new Connector("a", 6423, ConnectionType.WORKER),
      new Connector("b", 1234, ConnectionType.WORKER),
      new Connector("a", 1234, ConnectionType.AGENT),
    };

    for (int i = 0; i < equal.length; ++i) {
      assertEquals(connector.hashCode(), equal[i].hashCode());
      assertEquals(connector, equal[i]);
    }

    for (int i = 0; i < notEqual.length; ++i) {
      assertNotEquals(connector, notEqual[i]);
    }
  }

  @Test public void testGetEndpointAsString() throws Exception {
    assertEquals(
      "a:1234",
      new Connector("a", 1234, ConnectionType.WORKER).getEndpointAsString());

    final String description =
      new Connector("", 1234, ConnectionType.WORKER).getEndpointAsString();

    assertContains(description, "localhost");
    assertContains(description, "1234");
  }
}
