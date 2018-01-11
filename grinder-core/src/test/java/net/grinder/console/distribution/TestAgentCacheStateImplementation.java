// Copyright (C) 2005 - 2012 Philip Aston
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

package net.grinder.console.distribution;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.regex.Pattern;

import net.grinder.common.processidentity.ProcessReport;
import net.grinder.console.common.processidentity.StubAgentProcessReport;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.StubProcessReports;
import net.grinder.console.communication.ProcessControl.Listener;
import net.grinder.console.communication.ProcessControl.ProcessReports;
import net.grinder.console.distribution.AgentSet.OutOfDateException;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.messages.agent.CacheHighWaterMark;
import net.grinder.messages.console.AgentAddress;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.Directory;


/**
 * Unit test for {@link AgentCacheStateImplementation}.
 *
 * @author Philip Aston
 */
public class TestAgentCacheStateImplementation extends AbstractFileTestCase {

  private final RandomStubFactory<ProcessControl> m_processControlStubFactory =
    RandomStubFactory.create(ProcessControl.class);
  private final ProcessControl m_processControl =
    m_processControlStubFactory.getStub();

  private Directory m_directory;
  private Pattern m_pattern;

  protected void setUp() throws Exception {
    m_directory = new Directory(new File(""));
    m_pattern = Pattern.compile(".*");
  }

  public void testAgentCacheStateImplementation() throws Exception {

    final UpdateableAgentCacheState cacheState =
      new AgentCacheStateImplementation(
        m_processControl, m_directory, m_pattern);

    final CacheParameters cacheParameters = cacheState.getCacheParameters();
    assertEquals(m_directory, cacheParameters.getDirectory());
    assertEquals(m_pattern.pattern(),
      cacheParameters.getFileFilterPattern().pattern());

    cacheState.setDirectory(m_directory);
    cacheState.setFileFilterPattern(m_pattern);

    assertEquals(cacheParameters, cacheState.getCacheParameters());
  }

  public void testProcessUpdates() throws Exception {

    final UpdateableAgentCacheState cacheState =
      new AgentCacheStateImplementation(
        m_processControl, m_directory, m_pattern);

    final Listener processListener =
      (Listener) m_processControlStubFactory.assertSuccess(
        "addProcessStatusListener", Listener.class).getParameters()[0];
    m_processControlStubFactory.assertNoMoreCalls();

    final RandomStubFactory<PropertyChangeListener> listenerStubFactory =
      RandomStubFactory.create(PropertyChangeListener.class);

    cacheState.addListener(listenerStubFactory.getStub());

    assertFalse(cacheState.getOutOfDate());

    processListener.update(new ProcessReports[0]);
    assertFalse(cacheState.getOutOfDate());
    listenerStubFactory.assertNoMoreCalls();

    processListener.update(new ProcessReports[0]);

    assertFalse(cacheState.getOutOfDate());
    listenerStubFactory.assertNoMoreCalls();

    final StubAgentIdentity agentIdentity1 = new StubAgentIdentity("agent1");
    final StubAgentProcessReport agentReport1 =
      new StubAgentProcessReport(agentIdentity1, ProcessReport.State.RUNNING);

    processListener.update(new ProcessReports[] {
        new StubProcessReports(agentReport1, null),
    });

    assertTrue(cacheState.getOutOfDate());

    final PropertyChangeEvent propertyChangeEvent2 =
      (PropertyChangeEvent)
      listenerStubFactory.assertSuccess(
        "propertyChange", PropertyChangeEvent.class).getParameters()[0];
    listenerStubFactory.assertNoMoreCalls();

    assertEquals("outOfDate", propertyChangeEvent2.getPropertyName());
    assertEquals(Boolean.FALSE, propertyChangeEvent2.getOldValue());
    assertEquals(Boolean.TRUE, propertyChangeEvent2.getNewValue());

    final CacheHighWaterMark highWaterMark =
      cacheState.getCacheParameters().createHighWaterMark(1000);

    agentReport1.setCacheHighWaterMark(highWaterMark);

    processListener.update(new ProcessReports[] {
      new StubProcessReports(agentReport1, null),
    });

    assertFalse(cacheState.getOutOfDate());
    listenerStubFactory.assertSuccess(
      "propertyChange", PropertyChangeEvent.class);
    listenerStubFactory.assertNoMoreCalls();

    cacheState.setNewFileTime(1000);

    processListener.update(new ProcessReports[] {
      new StubProcessReports(agentReport1, null),
    });

    assertFalse(cacheState.getOutOfDate());
    listenerStubFactory.assertNoMoreCalls();

    cacheState.setNewFileTime(1500);

    processListener.update(new ProcessReports[] {
      new StubProcessReports(agentReport1, null),
    });

    assertTrue(cacheState.getOutOfDate());
    listenerStubFactory.assertSuccess(
      "propertyChange", PropertyChangeEvent.class);
    listenerStubFactory.assertNoMoreCalls();
  }

  public void testAgentSetValidity() throws Exception {
    final UpdateableAgentCacheState cacheState =
      new AgentCacheStateImplementation(
        m_processControl, m_directory, m_pattern);

    final AgentSet agentSet = cacheState.getAgentSet();

    assertNotNull(agentSet.getAddressOfAllAgents());
    assertNotNull(agentSet.getAddressOfOutOfDateAgents(123));
    assertEquals(-1, agentSet.getEarliestAgentTime());

    cacheState.setDirectory(new Directory(new File("abc")));

    try {
      agentSet.getAddressOfAllAgents();
      fail("Expected OutOfDateException");
    }
    catch (OutOfDateException e) {
    }

    try {
      agentSet.getAddressOfOutOfDateAgents(123);
      fail("Expected OutOfDateException");
    }
    catch (OutOfDateException e) {
    }

    final AgentSet agentSet2 = cacheState.getAgentSet();

    assertNotNull(agentSet2.getAddressOfAllAgents());
    assertNotNull(agentSet2.getAddressOfOutOfDateAgents(123));

    cacheState.setFileFilterPattern(Pattern.compile(".?"));

    try {
      agentSet.getAddressOfAllAgents();
      fail("Expected OutOfDateException");
    }
    catch (OutOfDateException e) {
    }

    try {
      agentSet.getAddressOfOutOfDateAgents(123);
      fail("Expected OutOfDateException");
    }
    catch (OutOfDateException e) {
    }

    assertEquals(-1, agentSet2.getEarliestAgentTime());
  }

  public void testAgentSetGetAddressOfAllAgents() throws Exception {
    final UpdateableAgentCacheState cacheState =
      new AgentCacheStateImplementation(
        m_processControl, m_directory, m_pattern);

    final Listener processListener =
      (Listener) m_processControlStubFactory.assertSuccess(
        "addProcessStatusListener", Listener.class).getParameters()[0];
    m_processControlStubFactory.assertNoMoreCalls();

    final StubAgentIdentity agentIdentity1 = new StubAgentIdentity("agent1");
    final StubAgentProcessReport agentReport1 =
      new StubAgentProcessReport(agentIdentity1, ProcessReport.State.RUNNING);

    assertFalse(cacheState.getAgentSet().getAddressOfAllAgents().includes(
      new AgentAddress(agentIdentity1)));

    processListener.update(new ProcessReports[] {
      new StubProcessReports(agentReport1, null),
    });

    assertTrue(cacheState.getAgentSet().getAddressOfAllAgents().includes(
      new AgentAddress(agentIdentity1)));
  }

  public void testAgentSetGetAddressOfOutOfDateAgents() throws Exception {
    final UpdateableAgentCacheState cacheState =
      new AgentCacheStateImplementation(
        m_processControl, m_directory, m_pattern);

    final Listener processListener =
      (Listener) m_processControlStubFactory.assertSuccess(
        "addProcessStatusListener", Listener.class).getParameters()[0];
    m_processControlStubFactory.assertNoMoreCalls();

    final StubAgentIdentity agentIdentity1 = new StubAgentIdentity("agent1");
    final StubAgentProcessReport agentReport1 =
      new StubAgentProcessReport(agentIdentity1, ProcessReport.State.RUNNING);

    assertFalse(
      cacheState.getAgentSet().getAddressOfOutOfDateAgents(100).includes(
        new AgentAddress(agentIdentity1)));

    processListener.update(new ProcessReports[] {
      new StubProcessReports(agentReport1, null),
    });

    assertTrue(
      cacheState.getAgentSet().getAddressOfOutOfDateAgents(100).includes(
        new AgentAddress(agentIdentity1)));

    final CacheHighWaterMark highWaterMark =
      cacheState.getCacheParameters().createHighWaterMark(1000);

    agentReport1.setCacheHighWaterMark(highWaterMark);

    processListener.update(new ProcessReports[] {
      new StubProcessReports(agentReport1, null),
    });

    assertTrue(
      cacheState.getAgentSet().getAddressOfOutOfDateAgents(1001).includes(
        new AgentAddress(agentIdentity1)));

    assertFalse(
      cacheState.getAgentSet().getAddressOfOutOfDateAgents(1000).includes(
        new AgentAddress(agentIdentity1)));
  }
}
