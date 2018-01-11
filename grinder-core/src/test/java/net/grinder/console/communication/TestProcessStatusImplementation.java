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

package net.grinder.console.communication;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import junit.framework.TestCase;
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
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.AllocateLowestNumber;


/**
 * Unit test case for {@link ProcessStatusImplementation}.
 *
 * @author Philip Aston
 */
public class TestProcessStatusImplementation extends TestCase {
  private final ProcessReportComparator m_processReportComparator =
    new ProcessReportComparator();

  private final Comparator<ProcessReports> m_processReportsComparator =
    new ProcessReportsComparator();

  private final MyTimer m_timer = new MyTimer();

  private final RandomStubFactory<AllocateLowestNumber>
    m_allocateLowestNumberStubFactory =
      RandomStubFactory.create(AllocateLowestNumber.class);
  private final AllocateLowestNumber m_allocateLowestNumber =
    m_allocateLowestNumberStubFactory.getStub();

  protected void tearDown() {
    m_timer.cancel();
  }

  public void testConstruction() throws Exception {
    new ProcessStatusImplementation(m_timer, m_allocateLowestNumber);

    assertEquals(2, m_timer.getNumberOfScheduledTasks());

    m_allocateLowestNumberStubFactory.assertNoMoreCalls();
  }

  public void testUpdate() throws Exception {

    final RandomStubFactory<ProcessControl.Listener> listenerStubFactory =
      RandomStubFactory.create(ProcessControl.Listener.class);

    final ProcessStatusImplementation processStatusSet =
      new ProcessStatusImplementation(m_timer, m_allocateLowestNumber);

    final TimerTask updateTask = m_timer.getTaskByPeriod(500L);

    processStatusSet.addListener(listenerStubFactory.getStub());

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

    final StubAgentIdentity agentIdentity = new StubAgentIdentity("agent");
    final WorkerIdentity workerIdentity =
      agentIdentity.createWorkerIdentity();

    final WorkerProcessReport workerProcessReport =
      new StubWorkerProcessReport(workerIdentity,
                                  ProcessReport.State.RUNNING,
                                  3,
                                  5);

    processStatusSet.addWorkerStatusReport(workerProcessReport);
    m_allocateLowestNumberStubFactory.assertSuccess("add", Object.class);
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

    updateTask.run();
    final CallData callData =
      listenerStubFactory.assertSuccess(
        "update",
        new ProcessControl.ProcessReports[0].getClass());

    final ProcessControl.ProcessReports[] processReportsArray =
      (ProcessControl.ProcessReports[])callData.getParameters()[0];

    assertEquals(1, processReportsArray.length);
    final WorkerProcessReport[] workerProcessReports =
      processReportsArray[0].getWorkerProcessReports();
    assertEquals(1, workerProcessReports.length);
    assertEquals(workerProcessReport, workerProcessReports[0]);

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();
  }

  public void testUpdateWithManyProcessStatusesAndFlush() throws Exception {
    final RandomStubFactory<ProcessControl.Listener> listenerStubFactory =
      RandomStubFactory.create(ProcessControl.Listener.class);

    final ProcessStatusImplementation processStatus =
      new ProcessStatusImplementation(m_timer, m_allocateLowestNumber);

    final TimerTask updateTask = m_timer.getTaskByPeriod(500L);
    final TimerTask flushTask = m_timer.getTaskByPeriod(2000L);

    processStatus.addListener(listenerStubFactory.getStub());

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

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

    for (int i = 0; i < workerProcessReportArray.length; ++i) {
      processStatus.addWorkerStatusReport(workerProcessReportArray[i]);
    }

    assertEquals(2, processStatus.getNumberOfLiveAgents());
    m_allocateLowestNumberStubFactory.assertSuccess("add", Object.class);
    m_allocateLowestNumberStubFactory.assertSuccess("add", Object.class);
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

    updateTask.run();

    final CallData callData =
      listenerStubFactory.assertSuccess(
        "update",
        new ProcessControl.ProcessReports[0].getClass());

    final ProcessControl.ProcessReports[] processReports =
      (ProcessControl.ProcessReports[])callData.getParameters()[0];
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

    AssertUtilities.assertArraysEqual(expectedAgent1WorkerProcessReports,
                                      agent1WorkerReports);

    final WorkerProcessReport[] agent2WorkerReports =
      processReports[1].getWorkerProcessReports();
    Arrays.sort(agent2WorkerReports, m_processReportComparator);

    final WorkerProcessReport[] expectedAgent2WorkerProcessReports = {
        new StubWorkerProcessReport(
          workerIdentityB1, ProcessReport.State.RUNNING, 1, 1),
      };

    AssertUtilities.assertArraysEqual(expectedAgent2WorkerProcessReports,
                                      agent2WorkerReports);

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();

    // Nothing's changed, reports are new, first flush should do nothing.
    flushTask.run();
    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

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

    for (int i = 0; i < processStatusArray2.length; ++i) {
      processStatus.addWorkerStatusReport(processStatusArray2[i]);
    }

    assertEquals(3, processStatus.getNumberOfLiveAgents());
    m_allocateLowestNumberStubFactory.assertSuccess("add", Object.class);
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

    processStatus.addAgentStatusReport(
      new StubAgentProcessReport(agentIdentityA,
                                 ProcessReport.State.RUNNING));
    processStatus.addAgentStatusReport(
      new StubAgentProcessReport(agentIdentityB,
                                 ProcessReport.State.RUNNING));

    assertEquals(3, processStatus.getNumberOfLiveAgents());
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

    // Second flush will remove processes that haven't reported.
    // It won't remove any agents, because there's been at least one
    // report for each.
    flushTask.run();
    updateTask.run();

    final CallData callData2 =
      listenerStubFactory.assertSuccess(
        "update",
        new ProcessControl.ProcessReports[0].getClass());

    final ProcessControl.ProcessReports[] processReports2 =
      (ProcessControl.ProcessReports[])callData2.getParameters()[0];
    Arrays.sort(processReports2, m_processReportsComparator);

    assertEquals(3, processReports2.length);
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

    final WorkerProcessReport[] expectedAgent1WorkerProcessReports2 = {
      new StubWorkerProcessReport(
        workerIdentityA1, ProcessReport.State.RUNNING, 5, 10),
    };

    AssertUtilities.assertArraysEqual(
      expectedAgent1WorkerProcessReports2,
      processReports2[0].getWorkerProcessReports());

    final WorkerProcessReport[] expectedAgent2WorkerProcessReports2 = {
      new StubWorkerProcessReport(
        workerIdentityB1, ProcessReport.State.RUNNING, 1, 1),
    };

    AssertUtilities.assertArraysEqual(
      expectedAgent2WorkerProcessReports2,
      processReports2[1].getWorkerProcessReports());

    final WorkerProcessReport[] expectedAgent3WorkerProcessReports2 = {
      new StubWorkerProcessReport(
        workerIdentityC1, ProcessReport.State.FINISHED, 1, 1),
    };

    AssertUtilities.assertArraysEqual(
      expectedAgent3WorkerProcessReports2,
      processReports2[2].getWorkerProcessReports());

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();

    // Third flush.
    flushTask.run();

    assertEquals(0, processStatus.getNumberOfLiveAgents());
    m_allocateLowestNumberStubFactory.assertSuccess("remove", Object.class);
    m_allocateLowestNumberStubFactory.assertSuccess("remove", Object.class);
    m_allocateLowestNumberStubFactory.assertSuccess("remove", Object.class);
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();
  }

  public void testAgentAndWorkers() throws Exception {
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

    public void schedule(TimerTask timerTask, long delay, long period) {
      assertEquals(0, delay);

      m_taskByPeriod.put(new Long(period), timerTask);
      ++m_numberOfScheduledTasks;
    }

    public TimerTask getTaskByPeriod(long period) {
      return m_taskByPeriod.get(new Long(period));
    }

    public int getNumberOfScheduledTasks() {
      return m_numberOfScheduledTasks;
    }
  }


  private static final class ProcessReportComparator
    implements Comparator<ProcessReport> {

    public int compare(ProcessReport processReport1,
                       ProcessReport processReport2) {
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

    public int compare(ProcessReports o1, ProcessReports o2) {
      return m_processReportComparator.compare(o1.getAgentProcessReport(),
                                               o2.getAgentProcessReport());
    }
  }
}
