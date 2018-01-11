// Copyright (C) 2004 - 2013 Philip Aston
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

package net.grinder.console.communication;

import static net.grinder.testutility.FileUtilities.createRandomFile;
import static net.grinder.testutility.SocketUtilities.findFreePort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.grinder.common.GrinderProperties;
import net.grinder.common.TimeAuthority;
import net.grinder.common.UncheckedInterruptedException;
import net.grinder.common.processidentity.AgentIdentity;
import net.grinder.common.processidentity.ProcessReport;
import net.grinder.common.processidentity.ProcessReport.State;
import net.grinder.communication.Address;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchRegistry.Handler;
import net.grinder.communication.SendToEveryoneAddress;
import net.grinder.communication.StreamSender;
import net.grinder.communication.StubConnector;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.communication.ProcessControl.ProcessReports;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.messages.agent.CacheHighWaterMark;
import net.grinder.messages.agent.ClearCacheMessage;
import net.grinder.messages.agent.DistributeFileMessage;
import net.grinder.messages.agent.DistributionCacheCheckpointMessage;
import net.grinder.messages.agent.ResetGrinderMessage;
import net.grinder.messages.agent.StartGrinderMessage;
import net.grinder.messages.agent.StopGrinderMessage;
import net.grinder.messages.agent.StubCacheHighWaterMark;
import net.grinder.messages.console.AgentAddress;
import net.grinder.messages.console.AgentProcessReportMessage;
import net.grinder.messages.console.WorkerAddress;
import net.grinder.messages.console.WorkerProcessReportMessage;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.EchoTranslations;
import net.grinder.testutility.StubTimer;
import net.grinder.translation.Translations;
import net.grinder.util.FileContents;
import net.grinder.util.StandardTimeAuthority;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


/**
 * Unit test case for {@link ConsoleCommunicationImplementation}. Also tests
 * {@link ProcessControlImplementation} and
 * {@link DistributionControlImplementation}.
 *
 * @author Philip Aston
 */
public class TestConsoleCommunicationImplementation
  extends AbstractJUnit4FileTestCase {

  private final Translations m_translations = new EchoTranslations();

  @Mock private ErrorHandler m_errorHandler;
  @Mock private Handler<Message> m_messageHandler;
  private final TimeAuthority m_timeAuthority = new StandardTimeAuthority();

  private ConsoleCommunicationImplementation m_consoleCommunication;
  private ConsoleProperties m_properties;
  private ServerSocket m_usedServerSocket;
  private final ProcessMessagesThread m_processMessagesThread =
    new ProcessMessagesThread();
  private StubTimer m_timer;

  @Before public void setUp() throws Exception {
    initMocks(this);

    m_timer = new StubTimer();

    m_usedServerSocket = new ServerSocket(0, 50, InetAddress.getByName(null));

    final File file = new File(getDirectory(), "properties");
    m_properties = new ConsoleProperties(m_translations, file);

    m_properties.setConsolePort(findFreePort());

    m_consoleCommunication =
      new ConsoleCommunicationImplementation(m_translations,
                                             m_properties,
                                             m_errorHandler,
                                             m_timeAuthority,
                                             10,
                                             10000);
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();

    m_consoleCommunication.shutdown();

    m_processMessagesThread.interrupt();
    m_processMessagesThread.join();

    m_timer.cancel();

    m_usedServerSocket.close();

    waitForNumberOfConnections(0);
  }

  @Test public void testConstruction() throws Exception {
    // Need a thread to be attempting to process messages or
    // ConsoleCommunicationImplementation.reset() will not complete.
    m_processMessagesThread.start();

    // Cause the sender to be invalid.
    m_properties.setConsolePort(m_usedServerSocket.getLocalPort());

    final ConsoleCommunicationImplementation consoleCommunication =
      new ConsoleCommunicationImplementation(m_translations,
                                             m_properties,
                                             m_errorHandler,
                                             m_timeAuthority,
                                             500,
                                             10000);

    assertEquals(0, consoleCommunication.getNumberOfConnections());

    final ConsoleCommunicationImplementation consoleCommunication2 =
      new ConsoleCommunicationImplementation(m_translations,
                                             m_properties,
                                             m_errorHandler,
                                             m_timeAuthority);

    assertEquals(0, consoleCommunication2.getNumberOfConnections());
  }

  @Test public void testShutdown() throws Exception {

    m_processMessagesThread.start();

    new StubConnector(InetAddress.getByName(null).getHostName(),
                      m_properties.getConsolePort(),
                      ConnectionType.AGENT)
      .connect();

    waitForNumberOfConnections(1);

    m_consoleCommunication.shutdown();

    waitForNumberOfConnections(0);
  }

  private Message readMessage(final Socket socket) throws Exception {
    final ObjectInputStream objectStream =
      new ObjectInputStream(socket.getInputStream());

    return (Message)objectStream.readObject();
  }

  private void sendMessage(final Socket socket, final Message message)
      throws Exception {
    final ObjectOutputStream objectStream =
      new ObjectOutputStream(socket.getOutputStream());

    objectStream.writeObject(message);
    objectStream.flush();
  }

  @Test public void testWithProcessControl() throws Exception {
    // We need to associate the agent id with the connection or we'll never
    // get a start message.
    final AgentIdentity agentIdentity = new StubAgentIdentity("foo");

    final Socket socket =
      new StubConnector(InetAddress.getByName(null).getHostName(),
                        m_properties.getConsolePort(),
                        ConnectionType.AGENT)
      .connect(new AgentAddress(agentIdentity));

    waitForNumberOfConnections(1);

    final ProcessControl processControl =
      new ProcessControlImplementation(m_timer,
                                       m_consoleCommunication,
                                       m_translations);

    final CacheHighWaterMark cacheHighWaterMark =
      new StubCacheHighWaterMark("cache", 100);

    final ProcessControl.Listener listener =
      mock(ProcessControl.Listener.class);

    processControl.addProcessStatusListener(listener);
    processControl.resetWorkerProcesses();
    processControl.stopAgentAndWorkerProcesses();

    assertTrue(readMessage(socket) instanceof ResetGrinderMessage);
    assertTrue(readMessage(socket) instanceof StopGrinderMessage);

    final GrinderProperties properties = new GrinderProperties();
    properties.setProperty("foo", "bah");

    // Need a thread to be attempting to process messages.
    m_processMessagesThread.start();

    final AgentProcessReportMessage message =
      new AgentProcessReportMessage(ProcessReport.State.RUNNING,
                                    cacheHighWaterMark);

    message.setAddress(new AgentAddress(agentIdentity));

    new StreamSender(socket.getOutputStream()).send(message);

    final CountDownLatch listenerCalledLatch = new CountDownLatch(1);

    doAnswer(new Answer<Void>() {
        @Override
        public Void answer(final InvocationOnMock invocation) {
          listenerCalledLatch.countDown();
          return null;
        }
      }).when(listener).update(isA(ProcessReports[].class));

    // Repeatedly run update task until it notifies listener that the message
    // has been received.
    final TimerTask processStatusUpdateTask = m_timer.getTaskByPeriod(500L);

    do {
      processStatusUpdateTask.run();
    }
    while (!listenerCalledLatch.await(10, TimeUnit.MILLISECONDS));

    verify(listener).update(isA(ProcessReports[].class));
    verifyNoMoreInteractions(listener);

    processControl.startWorkerProcesses(properties);
    final StartGrinderMessage startGrinderMessage =
      (StartGrinderMessage)readMessage(socket);

    assertEquals(properties, startGrinderMessage.getProperties());
    assertEquals(0, startGrinderMessage.getAgentNumber());

    processControl.startWorkerProcesses(new GrinderProperties());
    final StartGrinderMessage startGrinderMessage2 =
      (StartGrinderMessage)readMessage(socket);
    assertEquals(0, startGrinderMessage2.getProperties().size());

    // This shouldn't call reset. If it does, we'll block because
    // nothing's processing the messages.
    m_properties.setIgnoreSampleCount(99);

    // Reset by changing properties and do another test.
    m_properties.setConsolePort(findFreePort());

    // Changing the port drops the existing connections.
    waitForNumberOfConnections(0);

    final Socket socket2 =
      new StubConnector(InetAddress.getByName(null).getHostName(),
                        m_properties.getConsolePort(),
                        ConnectionType.AGENT)
      .connect();

    // Make sure something is listening to our new connection.
    waitForNumberOfConnections(1);

    processControl.resetWorkerProcesses();

    assertTrue(readMessage(socket2) instanceof ResetGrinderMessage);
  }

  @Test public void testDistributionControl() throws Exception {
    final Socket socket =
      new StubConnector(InetAddress.getByName(null).getHostName(),
                        m_properties.getConsolePort(),
                        ConnectionType.AGENT)
      .connect();

    final DistributionControl distributionControl =
      new DistributionControlImplementation(m_consoleCommunication);

    final Socket socket2 =
      new StubConnector(InetAddress.getByName(null).getHostName(),
                        m_properties.getConsolePort(),
                        ConnectionType.AGENT)
      .connect();

    waitForNumberOfConnections(2);

    socket2.close();

    final Address address = new SendToEveryoneAddress();

    // Closing the socket isn't enough for the ConsoleCommunication's Sender to
    // know we've gone (and so close its end of the connection); we need to send
    // something too.
    // Sadly it appears we sometimes need to chuck more than one message the
    // socket before it figures out the other end is stuffed.
    int n = 0;

    while (m_consoleCommunication.getNumberOfConnections() != 1) {
      distributionControl.clearFileCaches(address);
      ++n;
      assertTrue(n < 10);
    }

    for (int i = 0; i < n; ++i) {
      assertTrue(readMessage(socket) instanceof ClearCacheMessage);
    }

    final File relativePath = new File("foo");
    final File fullPath = new File(getDirectory(), relativePath.getPath());
    createRandomFile(fullPath);

    final FileContents fileContents = new FileContents(getDirectory(),
      relativePath);

    distributionControl.sendFile(address, fileContents);

    assertTrue(readMessage(socket) instanceof DistributeFileMessage);
    socket.close();

    // Need a thread to be attempting to process messages or
    // ConsoleCommunicationImplementation.reset() will not complete.
    m_processMessagesThread.start();

    // Reset by changing properties and do another test.
    m_properties.setConsoleHost("localhost");

    // Reseting the properties should ditch the existing connections.
    waitForNumberOfConnections(0);

    final Socket socket3 =
      new StubConnector(InetAddress.getByName(null).getHostName(),
                        m_properties.getConsolePort(),
                        ConnectionType.AGENT)
      .connect();

    waitForNumberOfConnections(1);

    distributionControl.clearFileCaches(address);
    assertTrue(readMessage(socket3) instanceof ClearCacheMessage);

    distributionControl.setHighWaterMark(address,
                                         new StubCacheHighWaterMark("", 100));
    assertTrue(
      readMessage(socket3) instanceof DistributionCacheCheckpointMessage);
  }

  /**
   * Connections are accepted by separate threads so we need to spin a while.
   * @param n - Wait until there are this number of accepted connections.
   * @throws InterruptedException
   */
  private void waitForNumberOfConnections(final int n)
      throws InterruptedException {
    for (int retry = 0;
         m_consoleCommunication.getNumberOfConnections() != n && retry < 200;
         ++retry) {
      Thread.sleep(10);
    }

    assertEquals(n, m_consoleCommunication.getNumberOfConnections());
  }

  @Test public void testProcessOneMessage() throws Exception {
    m_consoleCommunication.getMessageDispatchRegistry()
      .addFallback(m_messageHandler);

    m_processMessagesThread.start();

    final ProcessControl processControl =
      new ProcessControlImplementation(m_timer,
                                       m_consoleCommunication,
                                       m_translations);

    assertEquals(0, processControl.getNumberOfLiveAgents());

    final StubAgentIdentity agentIdentity = new StubAgentIdentity("agent");

    final Socket agentSocket =
      new StubConnector(InetAddress.getByName(null).getHostName(),
                        m_properties.getConsolePort(),
                        ConnectionType.AGENT)
      .connect(new AgentAddress(agentIdentity));

    final AgentProcessReportMessage agentMessage =
      new AgentProcessReportMessage(State.STARTED, null);

    sendMessage(agentSocket, agentMessage);

    final WorkerProcessReportMessage message =
      new WorkerProcessReportMessage(State.STARTED,
                                     (short)0,
                                     (short)0);

    final Socket workerSocket =
      new StubConnector(InetAddress.getByName(null).getHostName(),
                        m_properties.getConsolePort(),
                        ConnectionType.WORKER)
      .connect(new WorkerAddress(agentIdentity.createWorkerIdentity()));


    sendMessage(workerSocket, message);

    sendMessage(workerSocket, new MyMessage());

    // Message instance different due to serialisation.
    verify(m_messageHandler, timeout(10000)).handle(isA(MyMessage.class));

    assertEquals(1, processControl.getNumberOfLiveAgents());

    // ConsoleCommunication should have handled the original
    // AgentProcessReportMessage and WorkerProcessReportMessage. We check here
    // so we're sure the've been processed.

    sendMessage(workerSocket, new StopGrinderMessage());

    verify(m_messageHandler, timeout(10000))
      .handle(isA(StopGrinderMessage.class));

    verifyNoMoreInteractions(m_messageHandler);
  }

  @Test public void testSendExceptions() throws Exception {
    // Need a thread to be attempting to process messages or
    // ConsoleCommunicationImplementation.reset() will not complete.
    m_processMessagesThread.start();

    // Cause the sender to be invalid.
    m_properties.setConsolePort(m_usedServerSocket.getLocalPort());

    verify(m_errorHandler)
      .handleException(isA(DisplayMessageConsoleException.class));

    m_consoleCommunication.sendToAddressedAgents(
      new AgentAddress(new StubAgentIdentity("agent")), new MyMessage());

    verify(m_errorHandler, times(2))
      .handleException(isA(DisplayMessageConsoleException.class));

    m_consoleCommunication.sendToAgents(new MyMessage());
    verify(m_errorHandler, times(3))
      .handleException(isA(DisplayMessageConsoleException.class));


    m_properties.setConsolePort(m_usedServerSocket.getLocalPort());
    final ConsoleCommunication brokenConsoleCommunication =
      new ConsoleCommunicationImplementation(m_translations,
                                             m_properties,
                                             m_errorHandler,
                                             m_timeAuthority,
                                             100,
                                             10000);

    verify(m_errorHandler, times(4))
      .handleException(isA(DisplayMessageConsoleException.class));


    brokenConsoleCommunication.sendToAddressedAgents(
      new AgentAddress(new StubAgentIdentity("agent")), new MyMessage());
    verify(m_errorHandler).handleErrorMessage(isA(String.class));

    brokenConsoleCommunication.sendToAgents(new MyMessage());
    verify(m_errorHandler, times(2)).handleErrorMessage(isA(String.class));

    verifyNoMoreInteractions(m_errorHandler);
  }

  @Test public void testErrorHandling() throws Exception {
    // Need a thread to be attempting to process messages or the
    // receiver will never be shutdown correctly.
    m_processMessagesThread.start();

    m_properties.setConsolePort(m_usedServerSocket.getLocalPort());

    verify(m_errorHandler)
      .handleException(isA(DisplayMessageConsoleException.class));

    final Address address = new SendToEveryoneAddress();

    new DistributionControlImplementation(m_consoleCommunication)
    .clearFileCaches(address);

    verify(m_errorHandler, times(2))
      .handleException(isA(DisplayMessageConsoleException.class));
    verifyNoMoreInteractions(m_errorHandler);

    final ErrorHandler errorHandler2 = mock(ErrorHandler.class);

    // Test a ConsoleCommunication with an invalid Sender.
    m_properties.setConsolePort(m_usedServerSocket.getLocalPort());
    final ConsoleCommunication brokenConsoleCommunication =
      new ConsoleCommunicationImplementation(m_translations,
                                             m_properties,
                                             errorHandler2,
                                             m_timeAuthority,
                                             100,
                                             10000);

    verify(errorHandler2)
      .handleException(isA(DisplayMessageConsoleException.class));

    new DistributionControlImplementation(brokenConsoleCommunication)
    .clearFileCaches(address);

    verify(errorHandler2).handleErrorMessage(isA(String.class));
    verifyNoMoreInteractions(errorHandler2);
  }

  @Test public void testErrorHandlingWithFurtherCommunicationProblems()
    throws Exception {

    final ServerSocket freeServerSocket = new ServerSocket(0);
    freeServerSocket.close();

    // Need a thread to be attempting to process messages or
    // ConsoleCommunicationImplementation.reset() will not complete.
    m_processMessagesThread.start();

    m_properties.setConsolePort(freeServerSocket.getLocalPort());

    final Socket socket = new Socket(freeServerSocket.getInetAddress(),
                                     freeServerSocket.getLocalPort());

    socket.getOutputStream().close();

    // Will be called via the Acceptor problem listener.
    verify(m_errorHandler, timeout(1000))
      .handleException(isA(CommunicationException.class));

    final Socket socket2 =
      new StubConnector(InetAddress.getByName(null).getHostName(),
                        m_properties.getConsolePort(),
                        ConnectionType.AGENT)
      .connect();

    socket2.getOutputStream().write(new byte[100]);

    verify(m_errorHandler, timeout(1000).times(2))
      .handleException(isA(CommunicationException.class));

    socket.close();
    socket2.close();

    verifyNoMoreInteractions(m_errorHandler);
  }

  private static final class MyMessage implements Message {
    private static final long serialVersionUID = 1L;
  }

  private final class ProcessMessagesThread extends Thread {
    public ProcessMessagesThread() {
      super("Process messages");
    }

    @Override
    public void run() {
      try {
        while (m_consoleCommunication.processOneMessage()) { }
      }
      catch (final UncheckedInterruptedException e) {
        // Time to go.
      }
    }
  }
}
