// Copyright (C) 2005 - 2011 Philip Aston
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

import static net.grinder.testutility.SocketUtilities.findFreePort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import net.grinder.testutility.CallData;
import net.grinder.tools.tcpproxy.TCPProxyFilter.FilterException;
import net.grinder.util.StreamCopier;
import net.grinder.util.TerminalColour;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;


/**
 * Unit test case for {@link PortForwarderTCPProxyEngine}.
 *
 * @author Philip Aston
 */
public class TestPortForwarderTCPProxyEngine {

  private final MyFilterStubFactory m_requestFilterStubFactory =
    new MyFilterStubFactory();
  private final TCPProxyFilter m_requestFilter =
    m_requestFilterStubFactory.getStub();

  private final MyFilterStubFactory m_responseFilterStubFactory =
    new MyFilterStubFactory();
  private final TCPProxyFilter m_responseFilter =
    m_responseFilterStubFactory.getStub();

  @Mock private Logger m_logger;
  private final PrintWriter m_out = new PrintWriter(new StringWriter());

  private int m_localPort;

  @Before public void setUp() throws Exception {
    m_localPort = findFreePort();

    MockitoAnnotations.initMocks(this);

    resetLogger();
  }

  private void resetLogger() {
    reset(m_logger);
  }

  @Test public void testBadLocalPort() throws Exception {
    final ConnectionDetails badConnectionDetails =
      new ConnectionDetails(new EndPoint("fictitious-host", 111),
                            new EndPoint("to", 222),
                            false);

    try {
      new PortForwarderTCPProxyEngine(m_requestFilter,
                                      m_responseFilter,
                                      m_out,
                                      m_logger,
                                      badConnectionDetails,
                                      false,
                                      1000);
      fail("Expected UnknownHostException");
    }
    catch (UnknownHostException e) {
    }
  }

  @Test public void testTimeOut() throws Exception {

    final ConnectionDetails connectionDetails =
      new ConnectionDetails(new EndPoint("localhost", m_localPort),
                            new EndPoint("wherever", 9999),
                            false);

    final TCPProxyEngine engine =
      new PortForwarderTCPProxyEngine(m_requestFilter,
                                      m_responseFilter,
                                      m_out,
                                      m_logger,
                                      connectionDetails,
                                      false,
                                      10);

    resetLogger();

    // If this ends up spinning its probably because
    // some other test has not terminated all of its filter
    // threads correctly.
    engine.run();

    verify(m_logger).error("Listen time out");
    verifyNoMoreInteractions(m_logger);
  }

  private void engineTests(AbstractTCPProxyEngine engine,
                           ConnectionDetails connectionDetails)
    throws Exception {

    final Thread engineThread = new Thread(engine, "Run engine");
    engineThread.start();

    final Socket clientSocket =
      new Socket(engine.getListenEndPoint().getHost(),
                 engine.getListenEndPoint().getPort());

    final OutputStreamWriter outputStreamWriter =
      new OutputStreamWriter(clientSocket.getOutputStream());

    final PrintWriter clientWriter =
      new PrintWriter(outputStreamWriter, true);

    final String message =
      "This is some stuff\r\nWe expect to be echoed.\u00ff\u00fe";
    clientWriter.print(message);
    clientWriter.flush();

    final InputStream clientInputStream = clientSocket.getInputStream();

    while (clientInputStream.available() <= 0) {
      Thread.sleep(10);
    }

    final ByteArrayOutputStream response = new ByteArrayOutputStream();

    // Don't use a StreamCopier because it will block reading the
    // input stream.
    final byte[] buffer = new byte[100];

    while (clientInputStream.available() > 0) {
      final int bytesRead = clientInputStream.read(buffer, 0, buffer.length);
      response.write(buffer, 0, bytesRead);
    }

    // Not sure why, but on some JVMs, this fails if we use BAOS.toString().
    // Why should the default encoding used by OSW differ from that used
    // by BAOS?
    assertEquals(message, response.toString(outputStreamWriter.getEncoding()));

    clientSocket.close();

    engine.stop();
    engineThread.join();

    final CallData callData =
      m_requestFilterStubFactory.assertSuccess("connectionOpened",
                                               ConnectionDetails.class);

    // Check the remote endpoint and isSecure of the connection details matches
    // those of our remote endpoint.
    final ConnectionDetails localConnectionDetails =
      (ConnectionDetails)callData.getParameters()[0];

    assertEquals(connectionDetails.getRemoteEndPoint(),
      localConnectionDetails.getRemoteEndPoint());
    assertEquals(connectionDetails.isSecure(),
      localConnectionDetails.isSecure());

    m_requestFilterStubFactory.assertSuccess("handle",
                                             ConnectionDetails.class,
                                             new byte[0].getClass(),
                                             Integer.class);
    m_requestFilterStubFactory.assertSuccess("connectionClosed",
                                             ConnectionDetails.class);
    m_requestFilterStubFactory.assertNoMoreCalls();

    m_responseFilterStubFactory.assertSuccess("connectionOpened",
                                             ConnectionDetails.class);
    m_responseFilterStubFactory.assertSuccess("handle",
                                             ConnectionDetails.class,
                                             new byte[0].getClass(),
                                             Integer.class);

    m_responseFilterStubFactory.setIgnoreCallOrder(true);

    m_responseFilterStubFactory.assertSuccess(
      "connectionClosed", ConnectionDetails.class);
    m_responseFilterStubFactory.assertNoMoreCalls();

    verifyNoMoreInteractions(m_logger);

    // Stopping engine or filter again doesn't do anything.
    engine.stop();

    m_requestFilterStubFactory.assertNoMoreCalls();
    m_responseFilterStubFactory.assertNoMoreCalls();
  }

  @Test public void testEngine() throws Exception {

    final AcceptSingleConnectionAndEcho echoer =
      new AcceptSingleConnectionAndEcho();

    final EndPoint localEndPoint = new EndPoint("localhost", m_localPort);

    final ConnectionDetails connectionDetails =
      new ConnectionDetails(localEndPoint,
                            echoer.getEndPoint(),
                            false);

    // Set the filters not to randomly generate output.
    m_requestFilterStubFactory.setResult(null);
    m_responseFilterStubFactory.setResult(null);

    final AbstractTCPProxyEngine engine =
      new PortForwarderTCPProxyEngine(m_requestFilter,
                                      m_responseFilter,
                                      m_out,
                                      m_logger,
                                      connectionDetails,
                                      false,
                                      100000);

    m_responseFilterStubFactory.assertNoMoreCalls();
    m_requestFilterStubFactory.assertNoMoreCalls();

    assertEquals(localEndPoint, engine.getListenEndPoint());
    assertNotNull(engine.getSocketFactory());
    m_requestFilterStubFactory.assertIsWrappedBy(engine.getRequestFilter());
    m_responseFilterStubFactory.assertIsWrappedBy(engine.getResponseFilter());
    assertEquals(TerminalColour.NONE, engine.getRequestColour());
    assertEquals(TerminalColour.NONE, engine.getResponseColour());

    resetLogger();
    m_requestFilterStubFactory.resetCallHistory();
    m_responseFilterStubFactory.resetCallHistory();

    engineTests(engine, connectionDetails);
  }

  @Test public void testColourEngine() throws Exception {

    final AcceptSingleConnectionAndEcho echoer =
      new AcceptSingleConnectionAndEcho();

    final EndPoint localEndPoint = new EndPoint("localhost", m_localPort);

    final ConnectionDetails connectionDetails =
      new ConnectionDetails(localEndPoint,
                            echoer.getEndPoint(),
                            true);

    // Set the filters not to randomly generate output.
    m_requestFilterStubFactory.setResult(null);
    m_responseFilterStubFactory.setResult(null);

    final AbstractTCPProxyEngine engine =
      new PortForwarderTCPProxyEngine(m_requestFilter,
                                      m_responseFilter,
                                      m_out,
                                      m_logger,
                                      connectionDetails,
                                      true,
                                      100000);

    m_responseFilterStubFactory.assertNoMoreCalls();
    m_requestFilterStubFactory.assertNoMoreCalls();

    assertEquals(localEndPoint, engine.getListenEndPoint());
    assertNotNull(engine.getSocketFactory());
    m_requestFilterStubFactory.assertIsWrappedBy(engine.getRequestFilter());
    m_responseFilterStubFactory.assertIsWrappedBy(engine.getResponseFilter());
    assertEquals(TerminalColour.RED, engine.getRequestColour());
    assertEquals(TerminalColour.BLUE, engine.getResponseColour());

    resetLogger();
    m_requestFilterStubFactory.resetCallHistory();
    m_responseFilterStubFactory.resetCallHistory();

    engineTests(engine, connectionDetails);
  }

  @Test public void testOutputStreamFilterTeeWithBadFilters() throws Exception {

    final EndPoint localEndPoint = new EndPoint("localhost", m_localPort);

    final ConnectionDetails connectionDetails =
      new ConnectionDetails(localEndPoint,
                            new EndPoint("bah", 456),
                            false);

    final AbstractTCPProxyEngine engine =
      new PortForwarderTCPProxyEngine(m_requestFilter,
                                      m_responseFilter,
                                      m_out,
                                      m_logger,
                                      connectionDetails,
                                      true,
                                      100000);

    final AbstractTCPProxyEngine.OutputStreamFilterTee filterTee =
      engine.new OutputStreamFilterTee(connectionDetails,
                                       new ByteArrayOutputStream(),
                                       new BadFilter(),
                                       TerminalColour.NONE);

    resetLogger();

    filterTee.connectionOpened();
    verify(m_logger).error(contains("Problem"), isA(FilterException.class));

    filterTee.connectionClosed();
    verify(m_logger, times(2))
      .error(contains("Problem"), isA(FilterException.class));

    filterTee.handle(new byte[0], 0);
    verify(m_logger, times(3))
      .error(contains("Problem"), isA(FilterException.class));

    verifyNoMoreInteractions(m_logger);
  }

  private static final class BadFilter implements TCPProxyFilter {

    public byte[] handle(ConnectionDetails connectionDetails,
                         byte[] buffer,
                         int bytesRead)
      throws FilterException {
      throw new FilterException("Problem", null);
    }

    public void connectionOpened(ConnectionDetails connectionDetails)
      throws FilterException {
      throw new FilterException("Problem", null);
    }

    public void connectionClosed(ConnectionDetails connectionDetails)
      throws FilterException {
      throw new FilterException("Problem", null);
    }
  }

  private static final class AcceptSingleConnectionAndEcho implements Runnable {
    private final ServerSocket m_serverSocket;

    public AcceptSingleConnectionAndEcho() throws IOException {
      m_serverSocket = new ServerSocket(0);
      new Thread(this, getClass().getName()).start();
    }

    public EndPoint getEndPoint() {
      return EndPoint.serverEndPoint(m_serverSocket);
    }

    public void run() {
      try {
        final Socket socket = m_serverSocket.accept();

        new StreamCopier(1000, true).copy(socket.getInputStream(),
                                          socket.getOutputStream());
      }
      catch (IOException e) {
        System.err.println("Got a " + e.getMessage());
      }
    }
  }
}
