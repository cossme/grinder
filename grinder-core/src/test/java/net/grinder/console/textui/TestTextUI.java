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

package net.grinder.console.textui;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import net.grinder.common.processidentity.ProcessReport;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.processidentity.StubAgentProcessReport;
import net.grinder.console.common.processidentity.StubWorkerProcessReport;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.ProcessControl.ProcessReports;
import net.grinder.console.communication.StubProcessReports;
import net.grinder.console.model.SampleModel;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.translation.Translations;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;


/**
 * Unit tests for {@link TextUI}.
 *
 * @author Philip Aston
 */
public class TestTextUI {

  @Mock private Translations m_translations;

  @Before public void setUp() {
    initMocks(this);

    when(m_translations.translate("console.state/worker-threads", (short)3, (short)6))
      .thenReturn("(3/6 strings)");

    when(m_translations.translate("console.state/finished"))
     .thenReturn("done");

    when(m_translations.translate("console.state/no-connected-agents"))
     .thenReturn("No Agents!");

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

    when(m_translations.translate("console.term/finished"))
      .thenReturn("done");
  }

  @Mock private Logger m_logger;

  private final RandomStubFactory<ProcessControl> m_processControlStubFactory =
    RandomStubFactory.create(ProcessControl.class);
  private final ProcessControl m_processControl =
    m_processControlStubFactory.getStub();

  private final RandomStubFactory<SampleModel> m_sampleModelStubFactory =
    RandomStubFactory.create(SampleModel.class);
  private final SampleModel m_sampleModel =
    m_sampleModelStubFactory.getStub();

  @Test public void testErrorHandler() throws Exception {
    final TextUI textUI =
      new TextUI(m_translations, m_processControl, m_sampleModel, m_logger);
    verify(m_logger).info(contains("The Grinder"));

    final ErrorHandler errorHandler = textUI.getErrorHandler();

    errorHandler.handleErrorMessage("I let down their tyres");
    verify(m_logger).error("I let down their tyres");

    errorHandler.handleErrorMessage("with matches", "seeyamate");
    verify(m_logger).error(matches(".*seeyamate.*with matches.*"));

    final RuntimeException exception = new RuntimeException("wild dogs");
    errorHandler.handleException(exception);
    verify(m_logger).error("wild dogs", exception);

    errorHandler.handleException(exception, "the residents");
    verify(m_logger).error("the residents", exception);

    errorHandler.handleInformationMessage("austin maxi");
    verify(m_logger).info("austin maxi");

    Runtime.getRuntime().removeShutdownHook(textUI.getShutdownHook());
    verifyNoMoreInteractions(m_logger);

    m_sampleModelStubFactory.assertSuccess(
      "addModelListener", SampleModel.Listener.class);
  }

  @Test public void testProcessStatusListener() throws Exception {
    final TextUI textUI =
      new TextUI(m_translations, m_processControl, m_sampleModel, m_logger);
    verify(m_logger).info(contains("The Grinder"));

    final CallData processsControlCall =
      m_processControlStubFactory.assertSuccess(
        "addProcessStatusListener", ProcessControl.Listener.class);
    final ProcessControl.Listener processListener =
      (ProcessControl.Listener)processsControlCall.getParameters()[0];

    Runtime.getRuntime().removeShutdownHook(textUI.getShutdownHook());

    final ProcessReports[] reports1 = new ProcessReports[0];
    processListener.update(reports1);
    verify(m_logger).info("<no agents!>");

    processListener.update(reports1);

    processListener.update(reports1);

    final StubAgentIdentity agentIdentity1 = new StubAgentIdentity("agent1");
    final StubAgentProcessReport agentReport1 =
      new StubAgentProcessReport(agentIdentity1, ProcessReport.State.RUNNING);

    final WorkerProcessReport workerProcessReport1 =
      new StubWorkerProcessReport(agentIdentity1.createWorkerIdentity(),
                                  ProcessReport.State.RUNNING, 3, 6);

    final WorkerProcessReport workerProcessReport2 =
      new StubWorkerProcessReport(agentIdentity1.createWorkerIdentity(),
                                  ProcessReport.State.FINISHED, 0, 6);

    processListener.update(
      new ProcessReports[] {
        new StubProcessReports(agentReport1,
                               new WorkerProcessReport[] {
                                 workerProcessReport1,
                                 workerProcessReport2,
                               }),
      });

    verify(m_logger).info(
      "AG agent1 [plugged in] " +
      "{ WK agent1-0 [rolling (3/6 strings)], WK agent1-1 [fini] }");

    processListener.update(
      new ProcessReports[] {
        new StubProcessReports(agentReport1,
                               new WorkerProcessReport[] {
                                 workerProcessReport2,
                                 workerProcessReport1,
                               }),
      });

    final StubAgentIdentity agentIdentity2 = new StubAgentIdentity("agent2");
    final StubAgentProcessReport agentReport2 =
      new StubAgentProcessReport(agentIdentity2, ProcessReport.State.FINISHED);

    processListener.update(
      new ProcessReports[] {
          new StubProcessReports(agentReport2,
            new WorkerProcessReport[] { }),
        new StubProcessReports(agentReport1,
                               new WorkerProcessReport[] {
                                 workerProcessReport2,
                                 workerProcessReport1,
                               }),
      });

    verify(m_logger).info(
      "AG agent1 [plugged in] " +
      "{ WK agent1-0 [rolling (3/6 strings)], WK agent1-1 [fini] }, " +
      "AG agent2 [that's all folks]");

    verifyNoMoreInteractions(m_logger);
  }

  @Test public void testSampleModelListener() throws Exception {
    new TextUI(m_translations, m_processControl, m_sampleModel, m_logger);
    verify(m_logger).info(contains("The Grinder"));

    final Object[] addListenerParameters =
      m_sampleModelStubFactory.assertSuccess(
        "addModelListener", SampleModel.Listener.class).getParameters();

    final SampleModel.Listener listener =
      (SampleModel.Listener)addListenerParameters[0];

    m_sampleModelStubFactory.setResult(
      "getState",
      new SampleModel.State() {
          @Override
          public String getDescription() {
            return "no pressure son";
          }

          @Override
          public Value getValue() {
            return Value.Stopped;
          }

          @Override
          public long getSampleCount() {
            return -1;
          }
        } );


    listener.newSample();
    listener.newTests(null, null);
    listener.resetTests();


    listener.stateChanged();
    verify(m_logger).info("no pressure son");

    verifyNoMoreInteractions(m_logger);
  }

  @Test public void testShutdownHook() throws Exception {
    final TextUI textUI =
      new TextUI(m_translations, m_processControl, m_sampleModel, m_logger);

    verify(m_logger).info(contains("The Grinder"));

    final Thread shutdownHook = textUI.getShutdownHook();
    assertTrue(Runtime.getRuntime().removeShutdownHook(shutdownHook));

    shutdownHook.run();
    verify(m_logger).info("done");

    shutdownHook.run();
    verifyNoMoreInteractions(m_logger);
  }
}
