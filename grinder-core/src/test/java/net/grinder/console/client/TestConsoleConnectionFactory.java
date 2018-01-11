// Copyright (C) 2007 - 2012 Philip Aston
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

package net.grinder.console.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.KeepAliveMessage;
import net.grinder.communication.SocketAcceptorThread;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@link ConsoleConnectionFactory}.
*
 * @author Philip Aston
 */
public class TestConsoleConnectionFactory {

  @Mock private ScheduledExecutorService m_scheduler;
  @Captor private ArgumentCaptor<Runnable> m_runnableCaptor;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test public void testConnect() throws Exception {
    final SocketAcceptorThread socketAcceptor = SocketAcceptorThread.create();

    final ConsoleConnection consoleConnection =
      new ConsoleConnectionFactory().connect(socketAcceptor.getHostName(),
                                             socketAcceptor.getPort());

    assertNotNull(consoleConnection);

    socketAcceptor.close();

    final InputStream socketInput =
      socketAcceptor.getAcceptedSocket().getInputStream();

    final ObjectInputStream objectInputStream =
      new ObjectInputStream(socketInput);

    assertEquals(ConnectionType.CONSOLE_CLIENT, objectInputStream.readObject());
  }

  @Test(expected = ConsoleConnectionException.class)
  public void testConnectBadAcceptor() throws Exception {

    final SocketAcceptorThread socketAcceptor = SocketAcceptorThread.create();

    final ConsoleConnectionFactory consoleConnectionFactory =
        new ConsoleConnectionFactory();

    final ConsoleConnection consoleConnection =
      consoleConnectionFactory.connect(
        socketAcceptor.getHostName(), socketAcceptor.getPort());

    assertNotNull(consoleConnection);

    socketAcceptor.close();

    consoleConnectionFactory.connect(socketAcceptor.getHostName(),
                                     socketAcceptor.getPort());
  }

  @Test public void testKeepAlive() throws Exception {
    final ConsoleConnectionFactory consoleConnection =
        new ConsoleConnectionFactory(m_scheduler);

    final SocketAcceptorThread socketAcceptor = SocketAcceptorThread.create();

    consoleConnection.connect(socketAcceptor.getHostName(),
                              socketAcceptor.getPort());
    socketAcceptor.close();

    verify(m_scheduler).scheduleWithFixedDelay(m_runnableCaptor.capture(),
                                               isA(Long.class),
                                               isA(Long.class),
                                               isA(TimeUnit.class));

    final InputStream socketInput =
      socketAcceptor.getAcceptedSocket().getInputStream();

    final ObjectInputStream objectInputStream =
      new ObjectInputStream(socketInput);

    assertEquals(ConnectionType.CONSOLE_CLIENT, objectInputStream.readObject());
    assertNull(objectInputStream.readObject());
    assertEquals(0, socketInput.available());

    final Runnable keepAlive = m_runnableCaptor.getValue();

    keepAlive.run();

    final ObjectInputStream objectInputStream2 =
        new ObjectInputStream(socketInput);

    assertTrue(objectInputStream2.readObject() instanceof KeepAliveMessage);

    socketAcceptor.getAcceptedSocket().close();

    try {
      // Not sure why, but we don't find out until the second message we send.
      keepAlive.run();
      keepAlive.run();
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
      assertTrue(e.getCause() instanceof CommunicationException);
    }
  }
}
