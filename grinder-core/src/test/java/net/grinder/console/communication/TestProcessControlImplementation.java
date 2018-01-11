// Copyright (C) 2012 Philip Aston
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.Timer;

import net.grinder.common.GrinderProperties;
import net.grinder.common.processidentity.AgentIdentity;
import net.grinder.common.processidentity.ProcessReport.State;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.MessageDispatchRegistry.Handler;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.communication.ProcessControl.ProcessReports;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.messages.agent.StartGrinderMessage;
import net.grinder.messages.agent.StubCacheHighWaterMark;
import net.grinder.messages.console.AgentAddress;
import net.grinder.messages.console.AgentProcessReportMessage;
import net.grinder.translation.Translations;
import net.grinder.util.Directory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

/**
 * Unit tests for {@link ProcessControlImplementation}.
 *
 * @author Philip Aston
 */
public class TestProcessControlImplementation {

  private final AgentIdentity m_agentIdentity =
      new StubAgentIdentity("my agent");

  private final AgentProcessReportMessage m_agentMessage =
      new AgentProcessReportMessage(State.RUNNING,
                                    new StubCacheHighWaterMark(null, 0));

  @Mock private ProcessReports m_processReports1;
  @Mock private ProcessReports m_processReports2;
  @Mock private Timer m_timer;
  @Mock private MessageDispatchRegistry m_messageDispatchRegistry;
  @Mock private ConsoleCommunication m_consoleCommunication;
  @Mock private Translations m_translations;
  @Captor ArgumentCaptor<Handler<AgentProcessReportMessage>>
    m_agentReportMessageHandlerCaptor;
  @Captor ArgumentCaptor<StartGrinderMessage> m_startGrinderMessageCaptor;

  private ProcessControl m_processControl;

  @Before public void setUp() throws Exception {
    initMocks(this);

    when(m_consoleCommunication.getMessageDispatchRegistry())
      .thenReturn(m_messageDispatchRegistry);

    m_agentMessage.setAddress(new AgentAddress(m_agentIdentity));

    m_processControl = new ProcessControlImplementation(m_timer,
                                     m_consoleCommunication,
                                     m_translations);

    verify(m_messageDispatchRegistry)
      .set(eq(AgentProcessReportMessage.class),
          m_agentReportMessageHandlerCaptor.capture());

    m_agentReportMessageHandlerCaptor.getValue().handle(m_agentMessage);
  }

  @Test public void testStartWorkerProcessesWithDistributedFiles()
      throws Exception {

    final GrinderProperties properties = new GrinderProperties();
    m_processControl.startWorkerProcessesWithDistributedFiles(new Directory(),
                                                              properties);

    verify(m_consoleCommunication).sendToAddressedAgents(
      eq(m_agentMessage.getProcessAddress()),
      m_startGrinderMessageCaptor.capture());

    final StartGrinderMessage startMessage =
        m_startGrinderMessageCaptor.getValue();
    assertEquals(0, startMessage.getAgentNumber());
    final GrinderProperties sentProperties = startMessage.getProperties();

    assertEquals(properties, sentProperties);
  }

  @Test(expected=DisplayMessageConsoleException.class)
  public void
    testStartWorkerProcessesWithDistributedFilesInvalidRelativePath()
      throws Exception {

    final GrinderProperties properties = new GrinderProperties();
    properties.setFile("grinder.script", new File("../outside"));
    m_processControl.startWorkerProcessesWithDistributedFiles(new Directory(),
                                                              properties);
  }

  @Test(expected=DisplayMessageConsoleException.class)
  public void
    testStartWorkerProcessesWidthDistributedFilesInvalidRelativePath2()
      throws Exception {

    final GrinderProperties properties = new GrinderProperties();
    properties.setAssociatedFile(new File("foo/bah").getAbsoluteFile());
    properties.setFile("grinder.script", new File("../../outside"));
    m_processControl.startWorkerProcessesWithDistributedFiles(new Directory(),
                                                              properties);
  }

  @Test public void
    testStartWorkerProcessesWithDistributedFilesKeepRelativePath()
      throws Exception {

    final GrinderProperties properties = new GrinderProperties();
    properties.setAssociatedFile(new File("one"));
    properties.setFile("grinder.script", new File("one/two"));

    m_processControl.startWorkerProcessesWithDistributedFiles(
      new Directory(new File("three").getAbsoluteFile()),
      properties);

    verify(m_consoleCommunication).sendToAddressedAgents(
      eq(m_agentMessage.getProcessAddress()),
      m_startGrinderMessageCaptor.capture());

    final StartGrinderMessage startMessage =
        m_startGrinderMessageCaptor.getValue();
    assertEquals(0, startMessage.getAgentNumber());
    final GrinderProperties sentProperties = startMessage.getProperties();

    assertEquals(properties, sentProperties);
    assertEquals(properties.getAssociatedFile(),
                 sentProperties.getAssociatedFile());
  }

  @Test public void
    testStartWorkerProcessesWithDistributedFilesAdjustRelativePath()
      throws Exception {

    final GrinderProperties properties = new GrinderProperties();
    properties.setAssociatedFile(
      new File("three/four/my.props").getAbsoluteFile());
    properties.setFile("grinder.script", new File("one/two"));

    m_processControl.startWorkerProcessesWithDistributedFiles(
      new Directory(new File("three").getAbsoluteFile()),
      properties);

    verify(m_consoleCommunication).sendToAddressedAgents(
      eq(m_agentMessage.getProcessAddress()),
      m_startGrinderMessageCaptor.capture());

    final StartGrinderMessage startMessage =
        m_startGrinderMessageCaptor.getValue();
    assertEquals(0, startMessage.getAgentNumber());
    final GrinderProperties sentProperties = startMessage.getProperties();

    assertEquals(properties, sentProperties);
    assertEquals(new File("four/my.props"), sentProperties.getAssociatedFile());
  }
}
