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


import static net.grinder.testutility.SocketUtilities.findFreePort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import net.grinder.common.TimeAuthority;
import net.grinder.common.UncheckedInterruptedException;
import net.grinder.util.StandardTimeAuthority;

import org.junit.Test;
import org.mockito.ArgumentCaptor;


/**
 *  Unit tests for {@code Acceptor}.
 *
 * @author Philip Aston
 */
public class TestAcceptor {

  private final TimeAuthority m_timeAuthority = new StandardTimeAuthority();

  @Test public void testConstructor() throws Exception {

    final List<String> testAddresses = new ArrayList<String>();

    // Loop back.
    testAddresses.add(InetAddress.getByName(null).getHostName());

    // All interfaces.
    testAddresses.add("");

    // All interfaces.
    testAddresses.add(CommunicationDefaults.ALL_INTERFACES);

    for (final InetAddress localAddress :
      InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())) {
      testAddresses.add(localAddress.getHostName());
    }

    final int port = findFreePort();

    for (final String testAddresse : testAddresses) {
      final Acceptor acceptor =
          new Acceptor(testAddresse, port, 2, m_timeAuthority);
      assertEquals(port, acceptor.getPort());
      assertNull(acceptor.peekPendingException());
      acceptor.shutdown();

      // Should also be able to use a OS allocated port.
      final Acceptor acceptor2 =
          new Acceptor(testAddresse, 0, 2, m_timeAuthority);
      assertEquals(port, acceptor.getPort());
      assertNull(acceptor2.peekPendingException());
      acceptor2.shutdown();
    }

    final ServerSocket usedSocket = new ServerSocket(0);
    final int usedPort = usedSocket.getLocalPort();

    for (final String testAddresse : testAddresses) {
      try {
        new Acceptor(testAddresse, usedPort, 1, m_timeAuthority);
        fail("Expected CommunicationException");
      }
      catch (final CommunicationException e) {
      }
    }

    usedSocket.close();
  }

  @Test public void testGetSocketSet() throws Exception {

    final Acceptor acceptor = createAcceptor(2);

    assertEquals(0, acceptor.getNumberOfConnections());

    final Acceptor.Listener listener = mock(Acceptor.Listener.class);

    acceptor.addListener(ConnectionType.WORKER, listener);

    final ResourcePool controlSocketSet =
      acceptor.getSocketSet(ConnectionType.AGENT);

    assertNotNull(controlSocketSet);
    assertTrue(controlSocketSet.reserveNext().isSentinel());

    final Connector controlConnector =
      new Connector("localhost", acceptor.getPort(), ConnectionType.AGENT);

    final Connector reportConnector =
      new Connector("localhost", acceptor.getPort(), ConnectionType.WORKER);

    controlConnector.connect();
    controlConnector.connect();
    reportConnector.connect();

    // Sleep until we've accepted both control connections and our
    // listener has been notified. Give up after a few seconds.
    for (int i = 0; controlSocketSet.countActive() != 2 && i < 10; ++i) {
      Thread.sleep(i * i * 10);
    }

    final ArgumentCaptor<ConnectionIdentity> identityCaptor =
        ArgumentCaptor.forClass(ConnectionIdentity.class);

    verify(listener, timeout(1000))
      .connectionAccepted(eq(ConnectionType.WORKER),
                          identityCaptor.capture());

    assertSame(controlSocketSet,
               acceptor.getSocketSet(ConnectionType.AGENT));

    final List<?> controlSocketResources = controlSocketSet.reserveAll();
    assertEquals(2, controlSocketResources.size());

    // Now do a similar checks with report socket set.
    final ResourcePool reportSocketSet =
      acceptor.getSocketSet(ConnectionType.WORKER);

    for (int i=0; reportSocketSet.countActive() != 1 && i<10; ++i) {
      Thread.sleep(i * i * 10);
    }

    assertEquals(3, acceptor.getNumberOfConnections());

    assertSame(reportSocketSet, acceptor.getSocketSet(ConnectionType.WORKER));

    final List<?> reportSocketResources = reportSocketSet.reserveAll();
    assertEquals(1, reportSocketResources.size());

    acceptor.shutdown();

    assertEquals(0, acceptor.getNumberOfConnections());

    verify(listener)
    .connectionClosed(ConnectionType.WORKER,
                      identityCaptor.getValue());
  }

  private Acceptor createAcceptor(final int numberOfThreads) throws Exception {
    // Figure out a free local port.
    final ServerSocket serverSocket = new ServerSocket(0);
    final int port = serverSocket.getLocalPort();
    serverSocket.close();

    return new Acceptor("", port, numberOfThreads, m_timeAuthority);
  }

  @Test public void testShutdown() throws Exception {

    final Acceptor acceptor = createAcceptor(3);

    final ResourcePool socketSet =
      acceptor.getSocketSet(ConnectionType.AGENT);

    final Connector connector =
      new Connector("localhost", acceptor.getPort(), ConnectionType.AGENT);

    connector.connect();

    // Sleep until we've accepted the connection. Give up after a few
    // seconds.
    for (int i = 0; socketSet.countActive() != 1 && i < 10; ++i) {
      Thread.sleep(i * i * 10);
    }

    assertNull(acceptor.peekPendingException());

    acceptor.shutdown();

    try {
      acceptor.getSocketSet(ConnectionType.AGENT);
      fail("Expected Acceptor.ShutdownException");
    }
    catch (final Acceptor.ShutdownException e) {
    }

    assertTrue(socketSet.reserveNext().isSentinel());
  }

  @Test public void testGetPendingException() throws Exception {

    final Acceptor acceptor = createAcceptor(3);

    // Non blocking.
    assertNull(acceptor.peekPendingException());

    // Create a couple of problems.
    final Socket socket = new Socket("localhost", acceptor.getPort());

    for (int i = 0; i < 10; ++i) {
      socket.getOutputStream().write(99);
    }

    socket.getOutputStream().flush();

    final Socket socket2 = new Socket("localhost", acceptor.getPort());

    for (int i = 0; i < 10; ++i) {
      socket2.getOutputStream().write(99);
    }

    socket2.getOutputStream().flush();

    // Blocking, so we don't need to do fancy synchronisation to
    // ensure acceptor has encountered the problems.
    assertTrue(acceptor.getPendingException()
               instanceof CommunicationException);

    assertTrue(acceptor.getPendingException()
               instanceof CommunicationException);

    assertNull(acceptor.peekPendingException());

    acceptor.shutdown();

    assertNull(acceptor.getPendingException());
  }

  @Test public void testGetPendingExceptionInterrupted() throws Exception {
    final Acceptor acceptor = createAcceptor(3);

    Thread.currentThread().interrupt();

    try {
      acceptor.getPendingException();
      fail("Expected UncheckedInterruptedException");
    }
    catch (final UncheckedInterruptedException e) {
    }
  }
}
