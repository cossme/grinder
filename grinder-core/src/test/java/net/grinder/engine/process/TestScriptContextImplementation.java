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

package net.grinder.engine.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import net.grinder.common.GrinderProperties;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.script.Barrier;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.script.InvalidContextException;
import net.grinder.script.SSLControl;
import net.grinder.script.Statistics;
import net.grinder.script.TestRegistry;
import net.grinder.synchronisation.BarrierGroup;
import net.grinder.synchronisation.BarrierGroups;
import net.grinder.synchronisation.messages.BarrierIdentity;
import net.grinder.testutility.Time;
import net.grinder.util.Sleeper;
import net.grinder.util.SleeperImplementation;
import net.grinder.util.StandardTimeAuthority;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;


/**
 * Unit tests for {@code ScriptContextImplementation}.
 *
 * @author Philip Aston
 */
public class TestScriptContextImplementation {

  @Mock private ThreadContext m_threadContext;
  @Mock private Logger m_logger;
  @Mock private ThreadStarter m_threadStarter;
  @Mock private ThreadStopper m_threadStopper;
  @Mock private Statistics m_statistics;
  @Mock private SSLControl m_sslControl;
  @Mock private TestRegistry m_testRegistry;
  @Mock private BarrierGroups m_barrierGroups;
  @Mock private BarrierGroup m_barrierGroup;
  @Mock private BarrierIdentity.Factory m_identityGenerator;
  @Mock private ThreadContextLocator m_threadContextLocator;

  private ScriptContext m_scriptContext;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(m_threadContextLocator.get()).thenReturn(m_threadContext);

    when(m_barrierGroup.getName()).thenReturn("MyBarrierGroup");
    when(m_barrierGroups.getGroup("MyBarrierGroup"))
      .thenReturn(m_barrierGroup);

    m_scriptContext = new ScriptContextImplementation(
      null, null, m_threadContextLocator, null, null, null, null, null, null,
      null, m_threadStopper, m_barrierGroups, m_identityGenerator);
  }

  @Test public void testConstructorAndGetters() throws Exception {

    final GrinderProperties properties = new GrinderProperties();

    final int threadNumber = 99;
    final int runNumber = 3;

    when(m_threadContext.getThreadNumber()).thenReturn(threadNumber);
    when(m_threadContext.getRunNumber()).thenReturn(runNumber);

    final Sleeper sleeper = new SleeperImplementation(null, m_logger, 1, 0);

    final StubAgentIdentity agentIdentity =
      new StubAgentIdentity("Agent");
    final WorkerIdentity workerIdentity = agentIdentity.createWorkerIdentity();
    final WorkerIdentity firstWorkerIdentity =
      agentIdentity.createWorkerIdentity();

    final ScriptContextImplementation scriptContext =
      new ScriptContextImplementation(
        workerIdentity,
        firstWorkerIdentity,
        m_threadContextLocator,
        properties,
        m_logger,
        sleeper,
        m_sslControl,
        m_statistics,
        m_testRegistry,
        m_threadStarter,
        m_threadStopper,
        m_barrierGroups,
        m_identityGenerator);

    assertEquals(workerIdentity.getName(), scriptContext.getProcessName());
    assertEquals(workerIdentity.getNumber(),
                 scriptContext.getProcessNumber());
    assertEquals(firstWorkerIdentity.getNumber(),
                 scriptContext.getFirstProcessNumber());
    assertEquals(threadNumber, scriptContext.getThreadNumber());
    assertEquals(runNumber, scriptContext.getRunNumber());
    assertSame(m_logger, scriptContext.getLogger());
    assertSame(properties, scriptContext.getProperties());
    assertSame(m_statistics, scriptContext.getStatistics());
    assertSame(m_sslControl, scriptContext.getSSLControl());
    assertSame(m_testRegistry, scriptContext.getTestRegistry());
    assertNotNull(scriptContext.getTimeAuthority());

    when(m_threadContextLocator.get()).thenReturn(null);

    assertEquals(-1, scriptContext.getThreadNumber());
    assertEquals(-1, scriptContext.getRunNumber());
    assertEquals(m_statistics, scriptContext.getStatistics());

    assertEquals(0, scriptContext.getProcessNumber());
    assertEquals(-1, scriptContext.getAgentNumber());

    agentIdentity.setNumber(10);
    assertEquals(0, scriptContext.getProcessNumber());
    assertEquals(10, scriptContext.getAgentNumber());

    verifyNoMoreInteractions(m_threadStarter);

    scriptContext.startWorkerThread();
    verify(m_threadStarter).startThread(any());

    final Object testRunner = new Object();
    scriptContext.startWorkerThread(testRunner);
    verify(m_threadStarter).startThread(testRunner);
    verifyNoMoreInteractions(m_threadStarter);

    scriptContext.stopWorkerThread(10);
    verify(m_threadStopper).stopThread(10);
    verifyNoMoreInteractions(m_threadStopper);
  }

  @Test public void testSleep() throws Exception {

    final Sleeper sleeper =
      new SleeperImplementation(new StandardTimeAuthority(),
                                m_logger,
                                1,
                                0);

    final ScriptContextImplementation scriptContext =
      new ScriptContextImplementation(
        null, null, null, null, null, sleeper, null, null, null, null,
        null, null, null);

    assertTrue(
      new Time(50, 70) {
        @Override
        public void doIt() throws Exception  { scriptContext.sleep(50); }
      }.run());

    assertTrue(
      new Time(40, 70) {
        @Override
        public void doIt() throws Exception  { scriptContext.sleep(50, 5); }
      }.run());
  }

  @Test(expected=InvalidContextException.class)
  public void testStopThisWorkerThreadOtherThread() throws Exception {

    when(m_threadContextLocator.get()).thenReturn(null);
    m_scriptContext.stopThisWorkerThread();
  }

  @Test(expected=ShutdownException.class)
  public void testStopThisWorkerThread() throws Exception {

    m_scriptContext.stopThisWorkerThread();
  }

  @Test public void testBarrier() throws Exception {

    final Barrier globalBarrier = m_scriptContext.barrier("MyBarrierGroup");
    assertEquals("MyBarrierGroup", globalBarrier.getName());

    verify(m_identityGenerator).next();
  }

  @Test public void testStopProcessOtherThread() {
    when(m_threadContextLocator.get()).thenReturn(null);
    m_scriptContext.stopProcess();
    verify(m_threadStopper).stopProcess();
  }

  @Test(expected=ShutdownException.class)
  public void testStopProcessWorkerThread() {
    m_scriptContext.stopProcess();
    verify(m_threadStopper).stopProcess();
  }
}
