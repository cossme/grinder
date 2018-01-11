// Copyright (C) 2008 - 2014 Philip Aston
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import net.grinder.common.processidentity.AgentProcessReport;
import net.grinder.common.processidentity.ProcessReport;
import net.grinder.common.processidentity.ProcessReport.State;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.console.common.ProcessReportDescriptionFactory.ProcessDescription;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.translation.Translations;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;


/**
 * Unit tests for {@link ProcessReportDescriptionFactory}.
 *
 * @author Philip Aston
 */
public class TestProcessReportDescriptionFactory {

  @Mock private Translations m_translations;

  @Before public void setUp() {
    initMocks(this);

    when(m_translations.translate("console.state/worker-threads", (short)10, (short)21))
      .thenReturn("(10 out of 21 strings)");

    when(m_translations.translate("console.term/agent"))
      .thenReturn("AG");

    when(m_translations.translate("console.term/worker"))
      .thenReturn("WK");

    when(m_translations.translate("console.state/started"))
      .thenReturn("hot to trot");

    when(m_translations.translate("console.state/running"))
      .thenReturn("rolling");

    when(m_translations.translate("console.state/running-agent"))
      .thenReturn("plugged in");

    when(m_translations.translate("console.state/finished"))
      .thenReturn("fini");

    when(m_translations.translate("console.state/finished-agent"))
      .thenReturn("that's all folks");

    when(m_translations.translate("console.state/unknown"))
      .thenReturn("huh");
  }

  @Test public void testWithAgentProcessReport() throws Exception {
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
      new ProcessReportDescriptionFactory(m_translations);

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

  @Test public void testWithWorkerProcessReport() throws Exception {
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
      new ProcessReportDescriptionFactory(m_translations);

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

    assertEquals("rolling (10 out of 21 strings)", description2.getState());

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
