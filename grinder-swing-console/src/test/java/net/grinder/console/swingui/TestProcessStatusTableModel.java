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

package net.grinder.console.swingui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import net.grinder.common.processidentity.ProcessReport;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.console.common.processidentity.StubAgentProcessReport;
import net.grinder.console.common.processidentity.StubWorkerProcessReport;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.ProcessControl.ProcessReports;
import net.grinder.console.communication.StubProcessReports;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.translation.Translations;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;


/**
 * Unit tests for {@link ProcessStatusTableModel}.
 *
 * @author Philip Aston
 */
public class TestProcessStatusTableModel {

  @Mock private Translations m_translations;

  @Before public void setUp() {
    initMocks(this);

    when(m_translations.translate("console.process/name"))
      .thenReturn("NaMe");

    when(m_translations.translate("console.process/type"))
      .thenReturn("type");

    when(m_translations.translate("console.process/state"))
      .thenReturn("STATE");

    when(m_translations.translate("console.process/label"))
      .thenReturn("workers");

    when(m_translations.translate("console.term/threads"))
      .thenReturn("threads");

    when(m_translations.translate("console.state/running-agent"))
      .thenReturn("connected");

    when(m_translations.translate("console.state/finished-agent"))
      .thenReturn("disconnected");

    when(m_translations.translate("console.state/finished"))
      .thenReturn("finished");

    when(m_translations.translate("console.term/agent"))
      .thenReturn("Agent");

    when(m_translations.translate("console.term/worker"))
      .thenReturn("Worker");
  }

  final RandomStubFactory<ProcessControl> m_processControlStubFactory =
    RandomStubFactory.create(ProcessControl.class);
  final ProcessControl m_processControl = m_processControlStubFactory.getStub();

  @Test public void testConstruction() throws Exception {

    final int[] swingDispatcherCallCount = new int[1];

    final SwingDispatcherFactory swingDispatcherFactory =
      new SwingDispatcherFactory() {
        public <T> T create(Class<T> clazz, T delegate) {
          ++swingDispatcherCallCount[0];
          return delegate;
        }
      };

    final ProcessStatusTableModel processStatusTableModel =
      new ProcessStatusTableModel(m_translations,
                                  m_processControl,
                                  swingDispatcherFactory);

    m_processControlStubFactory.assertSuccess(
      "addProcessStatusListener", ProcessControl.Listener.class);
    m_processControlStubFactory.assertNoMoreCalls();

    assertEquals(3, processStatusTableModel.getColumnCount());
    assertEquals(0, processStatusTableModel.getRowCount());
    assertEquals("", processStatusTableModel.getValueAt(0, 0));
    assertEquals("NaMe", processStatusTableModel.getColumnName(0));
    assertEquals("type", processStatusTableModel.getColumnName(1));
    assertEquals("STATE", processStatusTableModel.getColumnName(2));

    assertFalse(processStatusTableModel.isBold(0, 0));
    assertNull(processStatusTableModel.getForeground(0, 0));
    assertNull(processStatusTableModel.getBackground(0, 0));

    assertEquals(1, swingDispatcherCallCount[0]);
  }

  @Test public void testWithData() throws Exception {
    final SwingDispatcherFactory swingDispatcherFactory =
      new SwingDispatcherFactory() {
        public <T> T create(Class<T> clazz, T delegate) { return delegate; }
      };

    final ProcessStatusTableModel processStatusTableModel =
      new ProcessStatusTableModel(m_translations,
                                  m_processControl,
                                  swingDispatcherFactory);

    final ProcessControl.Listener listener =
      (ProcessControl.Listener)
      m_processControlStubFactory.assertSuccess("addProcessStatusListener",
                                                ProcessControl.Listener.class)
     .getParameters()[0];

    assertEquals(0, processStatusTableModel.getRowCount());

    final ProcessReports[] nullReport = new ProcessReports[0];
    listener.update(nullReport);

    assertEquals(1, processStatusTableModel.getRowCount());
    assertEquals("", processStatusTableModel.getValueAt(0, 0));
    assertEquals("0 workers", processStatusTableModel.getValueAt(0, 1));
    assertEquals("0/0 threads", processStatusTableModel.getValueAt(0, 2));
    assertEquals("?", processStatusTableModel.getValueAt(0, 3));
    assertEquals("", processStatusTableModel.getValueAt(1, 3));

    final StubAgentIdentity agentIdentity1 = new StubAgentIdentity("agent1");
    final StubAgentProcessReport agentReport1 =
      new StubAgentProcessReport(agentIdentity1, ProcessReport.State.RUNNING);

    final WorkerProcessReport workerProcessReport1 =
      new StubWorkerProcessReport(agentIdentity1.createWorkerIdentity(),
                                  ProcessReport.State.RUNNING, 3, 6);

    final WorkerProcessReport workerProcessReport2 =
      new StubWorkerProcessReport(agentIdentity1.createWorkerIdentity(),
                                  ProcessReport.State.FINISHED, 0, 6);

    final ProcessReports[] someData = new ProcessReports[] {
        new StubProcessReports(agentReport1,
          new WorkerProcessReport[] {
            workerProcessReport1,
            workerProcessReport2,
          }),
    };

    listener.update(someData);

    assertEquals(4, processStatusTableModel.getRowCount());
    assertEquals("agent1", processStatusTableModel.getValueAt(0, 0));
    assertEquals("Agent", processStatusTableModel.getValueAt(0, 1));
    assertEquals("connected", processStatusTableModel.getValueAt(0, 2));
    assertEquals("?", processStatusTableModel.getValueAt(0, 3));
    assertEquals("?", processStatusTableModel.getValueAt(1, 3));
    assertEquals("  agent1-1", processStatusTableModel.getValueAt(2, 0));
    assertEquals("Worker", processStatusTableModel.getValueAt(2, 1));
    assertEquals("finished", processStatusTableModel.getValueAt(2, 2));
    assertEquals("?", processStatusTableModel.getValueAt(2, 3));
    assertEquals("2 workers", processStatusTableModel.getValueAt(3, 1));
    assertEquals("3/12 threads", processStatusTableModel.getValueAt(3, 2));

    m_processControlStubFactory.assertNoMoreCalls();
  }
}
