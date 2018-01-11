// Copyright (C) 2008 - 2012 Philip Aston
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

package net.grinder.console.common;

import java.util.HashMap;

import junit.framework.TestCase;
import net.grinder.common.processidentity.AgentProcessReport;
import net.grinder.common.processidentity.ProcessReport;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.common.processidentity.ProcessReport.State;
import net.grinder.console.common.ProcessReportDescriptionFactory.ProcessDescription;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit tests for {@link ProcessReportDescriptionFactory}.
 *
 * @author Philip Aston
 */
public class TestProcessReportDescriptionFactory extends TestCase {

  private Resources m_resources =
    new StubResources<String>(
      new HashMap<String, String>() { {
        put("processTable.threads.label", "strings");
        put("processTable.agentProcess.label", "AG");
        put("processTable.workerProcess.label", "WK");
        put("processState.started.label", "hot to trot");
        put("processState.running.label", "rolling");
        put("processState.connected.label", "plugged in");
        put("processState.finished.label", "fini");
        put("processState.disconnected.label", "that's all folks");
        put("processState.unknown.label", "huh");
      } }
    );

  public void testWithAgentProcessReport() throws Exception {
    final StubAgentIdentity agentIdentity =
      new StubAgentIdentity("my agent");

    final RandomStubFactory<AgentProcessReport> agentProcessReportStubFactory =
      RandomStubFactory.create(AgentProcessReport.class);
    final AgentProcessReport agentProcessReport =
      agentProcessReportStubFactory.getStub();
    agentProcessReportStubFactory.setResult("getAgentIdentity", agentIdentity);
    agentProcessReportStubFactory.setResult(
      "getState", ProcessReport.State.UNKNOWN);

    final ProcessReportDescriptionFactory processReportDescriptionFactory =
      new ProcessReportDescriptionFactory(m_resources);

    final ProcessDescription description1 =
      processReportDescriptionFactory.create(agentProcessReport);

    assertEquals("my agent", description1.getName());
    assertEquals("AG", description1.getProcessType());
    assertEquals("huh", description1.getState());
    assertEquals("AG my agent [huh]", description1.toString());

    agentProcessReportStubFactory.setResult(
      "getState", ProcessReport.State.RUNNING);

    final ProcessDescription description2 =
      processReportDescriptionFactory.create(agentProcessReport);

    assertEquals("plugged in", description2.getState());

    // Both started and running report "connected".
    agentProcessReportStubFactory.setResult(
      "getState", ProcessReport.State.STARTED);

    final ProcessDescription description3 =
      processReportDescriptionFactory.create(agentProcessReport);

    assertEquals("plugged in", description3.getState());

    agentProcessReportStubFactory.setResult(
      "getState", ProcessReport.State.FINISHED);

    final ProcessDescription description4 =
      processReportDescriptionFactory.create(agentProcessReport);

    assertEquals("that's all folks", description4.getState());

    agentProcessReportStubFactory.setResult("getState", State.UNKNOWN);

    final ProcessDescription description5 =
      processReportDescriptionFactory.create(agentProcessReport);

    assertEquals("huh", description5.getState());

    agentIdentity.setNumber(10);
    final ProcessDescription description6 =
      processReportDescriptionFactory.create(agentProcessReport);
    assertEquals("my agent (AG 10)", description6.getName());
  }

  public void testWithWorkerProcessReport() throws Exception {
    final StubAgentIdentity agentIdentity =
      new StubAgentIdentity("agent");

    final WorkerIdentity workerIdentity =
      agentIdentity.createWorkerIdentity();

    final RandomStubFactory<WorkerProcessReport>
      workerProcessReportStubFactory =
        RandomStubFactory.create(WorkerProcessReport.class);

    final WorkerProcessReport workerProcessReport =
      workerProcessReportStubFactory.getStub();
    workerProcessReportStubFactory.setResult("getWorkerIdentity",
                                             workerIdentity);
    workerProcessReportStubFactory.setResult(
      "getState", ProcessReport.State.UNKNOWN);

    final ProcessReportDescriptionFactory processReportDescriptionFactory =
      new ProcessReportDescriptionFactory(m_resources);

    final ProcessDescription description1 =
      processReportDescriptionFactory.create(workerProcessReport);

    assertEquals("agent-0", description1.getName());
    assertEquals("WK", description1.getProcessType());
    assertEquals("huh", description1.getState());
    assertEquals("WK agent-0 [huh]", description1.toString());

    workerProcessReportStubFactory.setResult(
      "getState", ProcessReport.State.RUNNING);
    workerProcessReportStubFactory.setResult(
      "getNumberOfRunningThreads", new Short((short) 10));
    workerProcessReportStubFactory.setResult(
      "getMaximumNumberOfThreads", new Short((short) 21));

    final ProcessDescription description2 =
      processReportDescriptionFactory.create(workerProcessReport);

    assertEquals("rolling (10/21 strings)", description2.getState());

    workerProcessReportStubFactory.setResult(
      "getState", ProcessReport.State.STARTED);

    final ProcessDescription description3 =
      processReportDescriptionFactory.create(workerProcessReport);

    assertEquals("hot to trot", description3.getState());

    workerProcessReportStubFactory.setResult(
      "getState", ProcessReport.State.FINISHED);

    final ProcessDescription description4 =
      processReportDescriptionFactory.create(workerProcessReport);

    assertEquals("fini", description4.getState());

    workerProcessReportStubFactory.setResult("getState", State.UNKNOWN);

    final ProcessDescription description5 =
      processReportDescriptionFactory.create(workerProcessReport);

    assertEquals("huh", description5.getState());
  }
}
