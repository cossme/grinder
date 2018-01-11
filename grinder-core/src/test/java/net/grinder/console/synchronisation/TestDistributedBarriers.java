// Copyright (C) 2011 - 2013 Philip Aston
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

package net.grinder.console.synchronisation;

import static net.grinder.testutility.SocketUtilities.findFreePort;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import net.grinder.common.UncheckedInterruptedException;
import net.grinder.communication.ClientReceiver;
import net.grinder.communication.ClientSender;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.Connector;
import net.grinder.communication.MessageDispatchSender;
import net.grinder.communication.MessagePump;
import net.grinder.communication.Sender;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.console.communication.ConsoleCommunicationImplementation;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.messages.console.AgentAddress;
import net.grinder.messages.console.WorkerAddress;
import net.grinder.script.Barrier;
import net.grinder.synchronisation.BarrierGroups;
import net.grinder.synchronisation.BarrierIdentityGenerator;
import net.grinder.synchronisation.BarrierImplementation;
import net.grinder.synchronisation.ClientBarrierGroups;
import net.grinder.synchronisation.messages.BarrierIdentity;
import net.grinder.synchronisation.messages.BarrierIdentity.Factory;
import net.grinder.translation.Translations;
import net.grinder.util.StandardTimeAuthority;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Concurrent tests for distributed barriers.
 *
 * @author Philip Aston
 */
public class TestDistributedBarriers {

  private static final int PROCESSES = 3;
  private static final int THREADS_PER_PROCESS = 5;
  private static final int THREADS = PROCESSES * THREADS_PER_PROCESS;
  private static final int RUNS = 3;

  @Mock private ErrorHandler m_errorHandler;
  @Mock private ProcessControl m_processControl;
  @Mock private Translations m_translations;

  private ConsoleCommunication m_communication;

  private final ExecutorService m_exector = Executors.newCachedThreadPool();
  private int m_port;

  @Before public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    final ConsoleProperties properties =
      new ConsoleProperties(m_translations, new File(""));

    m_port = findFreePort();
    properties.setConsolePort(m_port);

    m_communication =
      new ConsoleCommunicationImplementation(m_translations,
                                             properties,
                                             m_errorHandler,
                                             new StandardTimeAuthority());

    new WireDistributedBarriers(m_communication, m_processControl);

    m_exector.execute(new Runnable() {
      @Override
      public void run() {
        try {
          while (m_communication.processOneMessage()) { }
        }
        catch (final UncheckedInterruptedException e) {
          // Exit.
        }
      }
    });
  }

  @After public void shutdown() {
    m_exector.shutdownNow();
    m_communication.shutdown();
  }

  private final AtomicInteger m_n = new AtomicInteger();

  private class ClientThread implements Callable<Void> {

    private final BarrierGroups m_barrierGroups;
    private final Factory m_identityFactory;
    private final Barrier m_incrementBarrier;
    private final Barrier m_assertionBarrier;
    private final Barrier m_resetBarrier;

    private ClientThread(final BarrierGroups barrierGroups,
                         final Factory identityFactory)
      throws Exception {

      m_barrierGroups = barrierGroups;
      m_identityFactory = identityFactory;

      m_incrementBarrier =
        new BarrierImplementation(m_barrierGroups.getGroup("After Increment"),
                                  m_identityFactory);

      m_assertionBarrier =
        new BarrierImplementation(m_barrierGroups.getGroup("After assert"),
                                  m_identityFactory);

      m_resetBarrier =
        new BarrierImplementation(m_barrierGroups.getGroup("After reset"),
                                  m_identityFactory);
    }

    public void run() throws Exception {

      m_n.incrementAndGet();

      m_incrementBarrier.await();

      assertEquals(THREADS, m_n.get());

      m_assertionBarrier.await();

      m_n.set(0);

      m_resetBarrier.await();
    }


    @Override
    public Void call() throws Exception {
      for (int i = 0; i < RUNS; ++i) {
        run();
      }

      return null;
    }
  }

  private class ClientProcess {

    private final MessageDispatchSender m_messageDispatcher;
    private final MessagePump m_messagePump;
    private final StubAgentIdentity m_agentIdentity;
    private final BarrierGroups m_barrierGroups;
    private final BarrierIdentity.Factory m_identityFactory;

    public ClientProcess(final int n) throws Exception {

      m_agentIdentity = new StubAgentIdentity("agent" + n);

      final ClientReceiver agentReceiver =
        ClientReceiver.connect(new Connector("localhost",
                                             m_port,
                                             ConnectionType.AGENT),
                               new AgentAddress(m_agentIdentity));

      m_messageDispatcher = new MessageDispatchSender();

      m_messagePump = new MessagePump(agentReceiver, m_messageDispatcher, 1);

      final WorkerAddress workerAddress =
        new WorkerAddress(m_agentIdentity.createWorkerIdentity());

      final Sender workerSender =
        ClientSender.connect(new Connector("localhost",
                                           m_port,
                                           ConnectionType.WORKER),
                            workerAddress);

      m_barrierGroups =
        new ClientBarrierGroups(workerSender, m_messageDispatcher);

      m_identityFactory =
        new BarrierIdentityGenerator(workerAddress.getIdentity());
    }

    public void start() {
      m_messagePump.start();
    }

    public void stop() {
      m_messagePump.shutdown();
    }

    private ClientThread createWorker() throws Exception {

      return new ClientThread(m_barrierGroups, m_identityFactory);
    }
  }

  @Test public void testDistributed() throws Throwable {

    final List<ClientProcess> processes = new ArrayList<ClientProcess>();
    final List<ClientThread> threads = new ArrayList<ClientThread>();

    for (int i = 0; i < PROCESSES; ++i) {
      final ClientProcess agent = new ClientProcess(i);
      agent.start();
      processes.add(agent);

      for (int j = 0; j < THREADS_PER_PROCESS; ++j) {
        threads.add(agent.createWorker());
      }
    }

    try {
      for (final Future<Void> f : m_exector.invokeAll(threads)) { f.get(); }
    }
    catch (final ExecutionException e) {
      throw e.getCause();
    }

    for (final ClientProcess p : processes) { p.stop(); }
  }
}
