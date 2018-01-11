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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.common.processidentity.ProcessIdentity;
import net.grinder.common.processidentity.ProcessReport;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.console.common.processidentity.StubAgentProcessReport;
import net.grinder.console.common.processidentity.StubWorkerProcessReport;
import net.grinder.console.communication.ProcessControl.ProcessReports;
import net.grinder.console.communication.ProcessStatusImplementation.AgentAndWorkers;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.messages.console.AgentAndCacheReport;
import net.grinder.util.AllocateLowestNumber;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit test case for {@link ProcessStatusImplementation}.
 *
 * @author Philip Aston
 */
public class TestProcessStatusImplementation {
  private final ProcessReportComparator m_processReportComparator =
    new ProcessReportComparator();

  private final Comparator<ProcessReports> m_processReportsComparator =
    new ProcessReportsComparator();

  private final MyTimer m_timer = new MyTimer();

  @Mock
  private AllocateLowestNumber m_allocateLowestNumber;

  @Mock
  private ProcessControl.Listener m_listener;

  @Captor
  private ArgumentCaptor<ProcessControl.ProcessReports[]> m_reportsCaptor;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void tearDown() {
    m_timer.cancel();
  }

  @Test public void testConstruction() throws Exception {
    new ProcessStatusImplementation(m_timer, m_allocateLowestNumber);

    assertEquals(2, m_timer.getNumberOfScheduledTasks());

    verifyNoMoreInteractions(m_allocateLowestNumber);
  }

  @Test public void testUpdate() throws Exception {

    final ProcessStatusImplementation processStatusSet =
      new ProcessStatusImplementation(m_timer, m_allocateLowestNumber);

    final TimerTask updateTask = m_timer.getTaskByPeriod(500L);

    processStatusSet.addListener(m_listener);

    updateTask.run();

    updateTask.run();

    final StubAgentIdentity agentIdentity = new StubAgentIdentity("agent");
    final WorkerIdentity workerIdentity =
      agentIdentity.createWorkerIdentity();

    final WorkerProcessReport workerProcessReport =
      new StubWorkerProcessReport(workerIdentity,
                                  ProcessReport.State.RUNNING,
                                  3,
                                  5);

    processStatusSet.addWorkerStatusReport(workerProcessReport);
    verify(m_allocateLowestNumber).add(any());

    updateTask.run();

    verify(m_listener).update(m_reportsCaptor.capture());

    final ProcessControl.ProcessReports[] processReportsArray =
      m_reportsCaptor.getValue();

    assertEquals(1, processReportsArray.length);
    final WorkerProcessReport[] workerProcessReports =
      processReportsArray[0].getWorkerProcessReports();
    assertEquals(1, workerProcessReports.length);
    assertEquals(workerProcessReport, workerProcessReports[0]);

    updateTask.run();

    verifyNoMoreInteractions(m_listener, m_allocateLowestNumber);
  }

  @Test
  public void testUpdateWithManyProcessStatusesAndFlush() throws Exception {
    final ProcessStatusImplementation processStatus =
      new ProcessStatusImplementation(m_timer, m_allocateLowestNumber);

    final TimerTask updateTask = m_timer.getTaskByPeriod(500L);
    final TimerTask flushTask = m_timer.getTaskByPeriod(2000L);

    processStatus.addListener(m_listener);

    updateTask.run();

    final StubAgentIdentity agentIdentityA =
      new StubAgentIdentity("Agent A");
    final WorkerIdentity workerIdentityA1 =
      agentIdentityA.createWorkerIdentity();
    final WorkerIdentity workerIdentityA2 =
      agentIdentityA.createWorkerIdentity();
    final WorkerIdentity workerIdentityA3 =
      agentIdentityA.createWorkerIdentity();
    final WorkerIdentity workerIdentityA4 =
        agentIdentityA.createWorkerIdentity();
    assertEquals(3, workerIdentityA4.getNumber());

    final StubAgentIdentity agentIdentityB =
      new StubAgentIdentity("Agent B");
    final WorkerIdentity workerIdentityB1 =
      agentIdentityB.createWorkerIdentity();
    assertEquals(0, workerIdentityB1.getNumber());

    final WorkerProcessReport[] workerProcessReportArray = {
      new StubWorkerProcessReport(
        workerIdentityA3, ProcessReport.State.STARTED, 1, 1),
      new StubWorkerProcessReport(
        workerIdentityA4, ProcessReport.State.STARTED, 1, 1),
      new StubWorkerProcessReport(
        workerIdentityA3, ProcessReport.State.RUNNING, 5, 10),
      new StubWorkerProcessReport(
        workerIdentityB1, ProcessReport.State.RUNNING, 1, 1),
      new StubWorkerProcessReport(
        workerIdentityA2, ProcessReport.State.FINISHED, 1, 1),
      new StubWorkerProcessReport(
        workerIdentityA1, ProcessReport.State.FINISHED, 3, 10),
    };

    for (final WorkerProcessReport element : workerProcessReportArray) {
      processStatus.addWorkerStatusReport(element);
    }

    assertEquals(2, processStatus.getNumberOfLiveAgents());
    verify(m_allocateLowestNumber, times(2)).add(any());

    updateTask.run();

    verify(m_listener).update(m_reportsCaptor.capture());

    final ProcessControl.ProcessReports[] processReports =
      m_reportsCaptor.getValue();
    Arrays.sort(processReports, m_processReportsComparator);

    assertEquals(2, processReports.length);

    final WorkerProcessReport[] agent1WorkerReports =
      processReports[0].getWorkerProcessReports();
    Arrays.sort(agent1WorkerReports, m_processReportComparator);

    final WorkerProcessReport[] expectedAgent1WorkerProcessReports = {
      new StubWorkerProcessReport(
        workerIdentityA4, ProcessReport.State.STARTED, 1, 1),
      new StubWorkerProcessReport(
        workerIdentityA3, ProcessReport.State.RUNNING, 5, 10),
      new StubWorkerProcessReport(
        workerIdentityA1, ProcessReport.State.FINISHED, 3, 10),
      new StubWorkerProcessReport(
        workerIdentityA2, ProcessReport.State.FINISHED, 1, 1),
    };

    assertArrayEquals(expectedAgent1WorkerProcessReports,
      agent1WorkerReports);

    final WorkerProcessReport[] agent2WorkerReports =
      processReports[1].getWorkerProcessReports();
    Arrays.sort(agent2WorkerReports, m_processReportComparator);

    final WorkerProcessReport[] expectedAgent2WorkerProcessReports = {
        new StubWorkerProcessReport(
          workerIdentityB1, ProcessReport.State.RUNNING, 1, 1),
      };

    assertArrayEquals(
      expectedAgent2WorkerProcessReports,
      agent2WorkerReports);

    updateTask.run();

    // Nothing's changed, reports are new, first flush should do nothing.
    flushTask.run();
    updateTask.run();

    final StubAgentIdentity agentIdentityC =
      new StubAgentIdentity("Agent C");
    final WorkerIdentity workerIdentityC1 =
      agentIdentityC.createWorkerIdentity();

    final WorkerProcessReport[] processStatusArray2 = {
      new StubWorkerProcessReport(
        workerIdentityB1, ProcessReport.State.RUNNING, 1, 1),
      new StubWorkerProcessReport(
        workerIdentityA1, ProcessReport.State.RUNNING, 5, 10),
      new StubWorkerProcessReport(
        workerIdentityC1, ProcessReport.State.FINISHED, 1, 1),
    };

    for (final WorkerProcessReport element : processStatusArray2) {
      processStatus.addWorkerStatusReport(element);
    }

    assertEquals(3, processStatus.getNumberOfLiveAgents());
    verify(m_allocateLowestNumber, times(3)).add(any());

    processStatus.addAgentStatusReport(
      new StubAgentProcessReport(agentIdentityA,
                                 ProcessReport.State.RUNNING));
    processStatus.addAgentStatusReport(
      new StubAgentProcessReport(agentIdentityB,
                                 ProcessReport.State.RUNNING));

    assertEquals(3, processStatus.getNumberOfLiveAgents());

    verifyNoMoreInteractions(m_listener);
    reset(m_listener);

    // Second flush will remove processes that haven't reported.
    // It won't remove any agents, because there's been at least one
    // report for each.
    flushTask.run();
    updateTask.run();

    verify(m_listener).update(m_reportsCaptor.capture());

    final ProcessControl.ProcessReports[] processReports2 =
      m_reportsCaptor.getValue();
    Arrays.sort(processReports2, m_processReportsComparator);

    assertEquals(3, processReports2.length);

    final WorkerProcessReport[] expectedAgent1WorkerProcessReports2 = {
      new StubWorkerProcessReport(
        workerIdentityA1, ProcessReport.State.RUNNING, 5, 10),
    };

    assertArrayEquals(
      expectedAgent1WorkerProcessReports2,
      processReports2[0].getWorkerProcessReports());

    final WorkerProcessReport[] expectedAgent2WorkerProcessReports2 = {
      new StubWorkerProcessReport(
        workerIdentityB1, ProcessReport.State.RUNNING, 1, 1),
    };

    assertArrayEquals(
      expectedAgent2WorkerProcessReports2,
      processReports2[1].getWorkerProcessReports());

    final WorkerProcessReport[] expectedAgent3WorkerProcessReports2 = {
      new StubWorkerProcessReport(
        workerIdentityC1, ProcessReport.State.FINISHED, 1, 1),
    };

    assertArrayEquals(
      expectedAgent3WorkerProcessReports2,
      processReports2[2].getWorkerProcessReports());

    updateTask.run();

    // Third flush.
    flushTask.run();

    assertEquals(0, processStatus.getNumberOfLiveAgents());
    verify(m_allocateLowestNumber, times(3)).remove(any());
    verifyNoMoreInteractions(m_listener, m_allocateLowestNumber);
  }

  @Test public void testAgentAndWorkers() throws Exception {
    final ProcessStatusImplementation processStatusSet =
      new ProcessStatusImplementation(m_timer, m_allocateLowestNumber);

    final StubAgentIdentity agentIdentity =
      new StubAgentIdentity("agent");

    final AgentAndWorkers agentAndWorkers =
      processStatusSet.new AgentAndWorkers(agentIdentity);

    final AgentAndCacheReport initialReport =
      agentAndWorkers.getAgentProcessReport();

    assertEquals(agentIdentity, initialReport.getAgentIdentity());

    assertNull(initialReport.getCacheHighWaterMark());
  }

  private static final class MyTimer extends Timer {
    private final Map<Long, TimerTask> m_taskByPeriod =
      new HashMap<Long, TimerTask>();

    private int m_numberOfScheduledTasks;

    MyTimer() {
      super(true);
    }

    @Override
    public void schedule(final TimerTask timerTask,
                         final long delay,
                         final long period) {
      assertEquals(0, delay);

      m_taskByPeriod.put(new Long(period), timerTask);
      ++m_numberOfScheduledTasks;
    }

    public TimerTask getTaskByPeriod(final long period) {
      return m_taskByPeriod.get(new Long(period));
    }

    public int getNumberOfScheduledTasks() {
      return m_numberOfScheduledTasks;
    }
  }


  private static final class ProcessReportComparator
    implements Comparator<ProcessReport> {

    @Override
    public int compare(final ProcessReport processReport1,
                       final ProcessReport processReport2) {
      final int compareState =
        processReport1.getState().compareTo(processReport2.getState());

      if (compareState == 0) {
        final ProcessIdentity identity1 =
          processReport1.getProcessAddress().getIdentity();
        final ProcessIdentity identity2 =
          processReport2.getProcessAddress().getIdentity();

        return identity1.getName().compareTo(identity2.getName());
      }
      else {
        return compareState;
      }
    }
  }

  private final class ProcessReportsComparator
    implements Comparator<ProcessReports> {

    @Override
    public int compare(final ProcessReports o1, final ProcessReports o2) {
      return m_processReportComparator.compare(o1.getAgentProcessReport(),
                                               o2.getAgentProcessReport());
    }
  }
}
