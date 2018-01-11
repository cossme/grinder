// Copyright (C) 2004 - 2012 Philip Aston
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

package net.grinder.engine.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import net.grinder.common.UncheckedInterruptedException;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.engine.common.EngineException;
import net.grinder.testutility.RedirectStandardStreams;
import net.grinder.util.thread.Condition;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;


/**
 *  Unit tests for <code>WorkerLauncher</code>.
 *
 * @author Philip Aston
 */
public class TestWorkerLauncher {

  private static final String s_testClasspath =
    System.getProperty("java.class.path");

  private final MyCondition m_condition = new MyCondition();
  @Mock private Logger m_logger;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test public void testConstructor() throws Exception {
    final WorkerLauncher workerLauncher1 =
      new WorkerLauncher(0, null, null, null);

    assertTrue(workerLauncher1.allFinished());
    workerLauncher1.shutdown();

    final WorkerLauncher workerLauncher2 =
      new WorkerLauncher(10, null, null, null);

    assertFalse(workerLauncher2.allFinished());

    workerLauncher2.destroyAllWorkers();
    workerLauncher2.shutdown();
  }

  @Test public void testStartSomeProcesses() throws Exception {

    final MyWorkerFactory myProcessFactory = new MyWorkerFactory();

    final WorkerLauncher workerLauncher =
      new WorkerLauncher(5, myProcessFactory, m_condition, m_logger);

    m_condition.waitFor(workerLauncher);
    assertFalse(m_condition.isFinished());

    assertEquals(0, myProcessFactory.getNumberOfProcesses());

    workerLauncher.startSomeWorkers(1);

    assertEquals(1, myProcessFactory.getNumberOfProcesses());

    assertFalse(workerLauncher.allFinished());
    assertEquals(System.out, myProcessFactory.getLastOutputStream());
    assertEquals(System.err, myProcessFactory.getLastErrorStream());

    assertEquals(1, myProcessFactory.getChildProcesses().size());

    verify(m_logger).info("worker process-0 started");

    workerLauncher.startSomeWorkers(10);
    assertEquals(5, myProcessFactory.getNumberOfProcesses());

    verify(m_logger).info("worker process-1 started");
    verify(m_logger).info("worker process-2 started");
    verify(m_logger).info("worker process-3 started");
    verify(m_logger).info("worker process-4 started");

    assertEquals(5, myProcessFactory.getChildProcesses().size());

    assertFalse(workerLauncher.allFinished());

    final Worker[] processes =
      myProcessFactory.getChildProcesses().toArray(new Worker[0]);

    sendTerminationMessage(processes[0]);
    sendTerminationMessage(processes[2]);

    assertFalse(workerLauncher.allFinished());
    assertFalse(m_condition.isFinished());

    sendTerminationMessage(processes[1]);
    sendTerminationMessage(processes[3]);
    sendTerminationMessage(processes[4]);

    // Can't be bothered to add another layer of synchronisation, just
    // spin.
    while (!m_condition.isFinished()) {
      Thread.sleep(20);
    }

    assertTrue(workerLauncher.allFinished());
    workerLauncher.shutdown();
  }

  private void sendTerminationMessage(Worker process) {
    final PrintWriter processStdin =
      new PrintWriter(process.getCommunicationStream());

    processStdin.print("Foo\n");
    processStdin.flush();
  }

  @Test public void testStartAllProcesses() throws Exception {

    final MyWorkerFactory myProcessFactory = new MyWorkerFactory();

    final WorkerLauncher workerLauncher =
      new WorkerLauncher(9, myProcessFactory, m_condition, m_logger);

    m_condition.waitFor(workerLauncher);
    assertFalse(m_condition.isFinished());

    assertEquals(0, myProcessFactory.getNumberOfProcesses());

    workerLauncher.startAllWorkers();

    assertEquals(9, myProcessFactory.getNumberOfProcesses());

    assertFalse(workerLauncher.allFinished());
    assertEquals(System.out, myProcessFactory.getLastOutputStream());
    assertEquals(System.err, myProcessFactory.getLastErrorStream());

    assertEquals(9, myProcessFactory.getChildProcesses().size());

    final Worker[] processes =
      myProcessFactory.getChildProcesses().toArray(new Worker[0]);

    sendTerminationMessage(processes[0]);
    sendTerminationMessage(processes[6]);
    sendTerminationMessage(processes[5]);
    sendTerminationMessage(processes[2]);
    sendTerminationMessage(processes[7]);

    assertFalse(workerLauncher.allFinished());
    assertFalse(m_condition.isFinished());

    sendTerminationMessage(processes[8]);
    sendTerminationMessage(processes[1]);
    sendTerminationMessage(processes[3]);
    sendTerminationMessage(processes[4]);

    // Can't be bothered to add another layer of synchronisation, just
    // spin.
    while (!m_condition.isFinished()) {
      Thread.sleep(20);
    }

    assertTrue(workerLauncher.allFinished());
    workerLauncher.shutdown();
  }

  @Test public void testDestroyAllProcesses() throws Exception {

    final MyWorkerFactory myProcessFactory = new MyWorkerFactory();

    final WorkerLauncher workerLauncher =
      new WorkerLauncher(4, myProcessFactory, m_condition, m_logger);

    m_condition.waitFor(workerLauncher);
    assertFalse(m_condition.isFinished());

    assertEquals(0, myProcessFactory.getNumberOfProcesses());

    final RedirectStandardStreams redirectStreams =
      new RedirectStandardStreams() {
        protected void runWithRedirectedStreams() throws Exception {
          workerLauncher.startAllWorkers();
        }
      };

    redirectStreams.run();

    assertEquals(4, myProcessFactory.getNumberOfProcesses());

    assertFalse(workerLauncher.allFinished());
    assertEquals(4, myProcessFactory.getChildProcesses().size());

    final Worker[] processes =
      myProcessFactory.getChildProcesses().toArray(new Worker[0]);

    sendTerminationMessage(processes[1]);
    sendTerminationMessage(processes[3]);

    assertFalse(workerLauncher.allFinished());
    assertFalse(m_condition.isFinished());

    workerLauncher.destroyAllWorkers();

    // Can't be bothered to add another layer of synchronisation, just
    // spin.
    while (!m_condition.isFinished()) {
      Thread.sleep(20);
    }

    assertTrue(workerLauncher.allFinished());
    workerLauncher.shutdown();
  }

  @Test public void testInteruptedWaitFor() throws Exception {

    final WorkerIdentity workerIdentity =
      new AgentIdentityImplementation("test").createWorkerIdentity();

    final WorkerFactory workerFactory = mock(WorkerFactory.class);
    final Worker worker = mock(Worker.class);
    when(worker.getIdentity()).thenReturn(workerIdentity);

    when(workerFactory.create(isA(OutputStream.class),
                              isA(OutputStream.class))).thenReturn(worker);

    final WorkerLauncher workerLauncher =
      new WorkerLauncher(1, workerFactory, m_condition, m_logger);

    doThrow(new UncheckedInterruptedException(new InterruptedException()))
      .when(worker).waitFor();

    workerLauncher.startAllWorkers();

    verify(worker).getIdentity();
    verify(worker, timeout(1000)).waitFor();
    verify(worker, timeout(1000)).destroy();

    verifyNoMoreInteractions(worker);
  }

  @Test public void testRejectedExecution() throws Exception {

    final WorkerIdentity workerIdentity =
      new AgentIdentityImplementation("test").createWorkerIdentity();

    final WorkerFactory workerFactory = mock(WorkerFactory.class);
    final Worker worker = mock(Worker.class);
    when(worker.getIdentity()).thenReturn(workerIdentity);

    when(workerFactory.create(isA(OutputStream.class),
                              isA(OutputStream.class))).thenReturn(worker);

    final ExecutorService executor = mock(ExecutorService.class);

    final WorkerLauncher workerLauncher =
      new WorkerLauncher(executor, 1, workerFactory, m_condition, m_logger);

    doThrow(new RejectedExecutionException())
      .when(executor).execute(isA(Runnable.class));

    final boolean result = workerLauncher.startSomeWorkers(1);
    assertFalse(result);

    verify(m_logger).error(eq("Failed to wait for test-0"),
                           isA(Exception.class));
  }

  private static class MyCondition extends Condition {
    private boolean m_finished;

    public synchronized void waitFor(final WorkerLauncher workerLauncher) {

      m_finished = false;

      new Thread() {
        public void run() {
          try {
            synchronized (MyCondition.this) {
              while (!workerLauncher.allFinished()) {
                MyCondition.this.wait();
              }
            }

            m_finished = true;
          }
          catch (InterruptedException e) {
          }
        }
      }.start();
    }

    public boolean isFinished() {
      return m_finished;
    }
  }

  private static class MyWorkerFactory implements WorkerFactory {

    private int m_numberOfProcesses = 0;
    private OutputStream m_lastOutputStream;
    private OutputStream m_lastErrorStream;
    private ArrayList<Worker> m_childProcesses = new ArrayList<Worker>();
    private StubAgentIdentity m_agentIdentity =
      new StubAgentIdentity("process");

    public Worker create(OutputStream outputStream, OutputStream errorStream)
      throws EngineException {

      m_lastOutputStream = outputStream;
      m_lastErrorStream = errorStream;

      final CommandLine commandLine =
        new MyCommandLine("java",
                          "-classpath",
                          s_testClasspath,
                          EchoClass.class.getName());

      final Worker childProcess =
        new ProcessWorker(m_agentIdentity.createWorkerIdentity(),
                          commandLine,
                          outputStream,
                          errorStream);

      ++m_numberOfProcesses;
      m_childProcesses.add(childProcess);

      return childProcess;
    }

    public int getNumberOfProcesses() {
      return m_numberOfProcesses;
    }

    public OutputStream getLastOutputStream() {
      return m_lastOutputStream;
    }

    public OutputStream getLastErrorStream() {
      return m_lastErrorStream;
    }

    public ArrayList<Worker> getChildProcesses() {
      return m_childProcesses;
    }
  }
}
