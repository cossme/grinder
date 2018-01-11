// Copyright (C) 2008 - 2013 Philip Aston
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

package net.grinder.console;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.Collections;
import java.util.Timer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.MessageDispatchRegistry.Handler;
import net.grinder.console.client.ConsoleConnection;
import net.grinder.console.client.ConsoleConnectionFactory;
import net.grinder.console.common.Resources;
import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.console.communication.server.DispatchClientCommands;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.SampleModel;
import net.grinder.console.model.SampleModelViews;
import net.grinder.messages.console.RegisterExpressionViewMessage;
import net.grinder.messages.console.RegisterTestsMessage;
import net.grinder.messages.console.ReportStatisticsMessage;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.translation.Translations;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;


/**
 * Unit tests for {@link ConsoleFoundation}.
 *
 * @author Philip Aston
 */
public class TestConsoleFoundation extends AbstractJUnit4FileTestCase {

  @Mock private MessageDispatchRegistry m_messageDispatchRegistry;
  @Mock private ConsoleCommunication m_consoleCommunication;
  @Mock private Logger m_logger;

  @Mock private Resources m_resources;

  @Mock private Translations m_translations;

  @Captor private ArgumentCaptor<Handler<Message>> m_handlerCaptor;

  private final ExecutorService m_executor = Executors.newCachedThreadPool();

  @Before public void setUp() {
    initMocks(this);

    when(m_consoleCommunication.getMessageDispatchRegistry())
      .thenReturn(m_messageDispatchRegistry);

    when(m_translations.translate(isA(String.class)))
      .thenAnswer(new Answer<String>() {
        @Override
        public String answer(final InvocationOnMock invocation) {
          return (String) invocation.getArguments()[0];
        }});
  }

  @Override
  @After public void tearDown() {
    m_executor.shutdownNow();
  }

  @Test public void testConstruction() throws Exception {

    final ConsoleFoundation consoleFoundation =
        new ConsoleFoundation(m_resources, m_translations, m_logger, true);

    verify(m_logger).info(isA(String.class)); // Text UI version.
    verifyNoMoreInteractions(m_logger);
    consoleFoundation.shutdown();
  }

  @Test public void testSimpleRun() throws Exception {

    final Timer timer = new Timer(true);

    final ConsoleProperties consoleProperties =
      new ConsoleProperties(m_translations, new File(getDirectory(), "props"));

    // Figure out free local ports.

    final ServerSocket serverSocket =
      new ServerSocket(0, 50, InetAddress.getLocalHost());
    final String hostName = serverSocket.getInetAddress().getHostName();
    final int port = serverSocket.getLocalPort();

    final ServerSocket serverSocket2 =
        new ServerSocket(0, 50, InetAddress.getLocalHost());
    final int port2 = serverSocket2.getLocalPort();

    serverSocket.close();
    serverSocket2.close();

    consoleProperties.setConsoleHost(hostName);
    consoleProperties.setConsolePort(port);
    consoleProperties.setHttpHost(hostName);
    consoleProperties.setHttpPort(port2);

    final ConsoleFoundation foundation =
      new ConsoleFoundation(m_resources,
                            m_translations,
                            m_logger,
                            false,
                            timer,
                            consoleProperties);

    verify(m_logger).info(isA(String.class)); // Text UI version.
    verifyNoMoreInteractions(m_logger);

    final Future<?> runTask = m_executor.submit(new Runnable() {
      @Override
      public void run() { foundation.run(); }
    });

    connectToConsole(hostName, port);

    foundation.shutdown();

    runTask.get(1, TimeUnit.SECONDS);

    verifyNoMoreInteractions(m_logger);
  }

  private void connectToConsole(final String hostName, final int port)
      throws Exception {

    final ConsoleConnectionFactory ccf = new ConsoleConnectionFactory();

    final Callable<Void> connect = new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          final ConsoleConnection client = ccf.connect(hostName, port);
          assertEquals(0, client.getNumberOfAgents());
          client.close();
          return null;
        }
      };

    final int retries = 3;

    for (int i = 0; i < retries; ++i) {
      final Future<Void> task = m_executor.submit(connect);

      try {
        task.get(1, TimeUnit.SECONDS);
      }
      catch (final ExecutionException e) {
        if (i == retries - 1) {
          throw e;
        }

        Thread.sleep(50);
      }
    }
  }

  @Test public void testWireMessageDispatch() throws Exception {

    final SampleModel sampleModel = mock(SampleModel.class);

    final SampleModelViews sampleModelViews = mock(SampleModelViews.class);

    final DispatchClientCommands dispatchClientCommands =
      new DispatchClientCommands(null, null, null);

    new ConsoleFoundation.WireMessageDispatch(m_consoleCommunication,
                                              sampleModel,
                                              sampleModelViews,
                                              dispatchClientCommands);

    verify(m_messageDispatchRegistry).set(eq(RegisterTestsMessage.class),
                                          m_handlerCaptor.capture());

    final Collection<net.grinder.common.Test> tests = Collections.emptySet();
    m_handlerCaptor.getValue().handle(new RegisterTestsMessage(tests));

    verify(sampleModel).registerTests(tests);

    verify(m_messageDispatchRegistry).set(eq(ReportStatisticsMessage.class),
                                          m_handlerCaptor.capture());

    final TestStatisticsMap delta = new TestStatisticsMap();
    m_handlerCaptor.getValue().handle(new ReportStatisticsMessage(delta));

    verify(sampleModel).addTestReport(delta);

    verify(m_messageDispatchRegistry).set(
      eq(RegisterExpressionViewMessage.class), m_handlerCaptor.capture());

    final ExpressionView expressionView =
      StatisticsServicesImplementation.getInstance()
      .getStatisticExpressionFactory().createExpressionView(
        "blah", "userLong0", false);
    m_handlerCaptor.getValue().handle(
      new RegisterExpressionViewMessage(expressionView));

    verify(sampleModelViews).registerStatisticExpression(expressionView);

    verifyNoMoreInteractions(sampleModel,
                             sampleModelViews);
  }
}
