// Copyright (C) 2008 Pawel Lacinski
// Copyright (C) 2008 - 2011 Philip Aston
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;

import net.grinder.common.GrinderException;
import net.grinder.engine.agent.TestAgentDaemon.ActionListSleeperStubFactory.SleepAction;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.util.Sleeper;
import net.grinder.util.Sleeper.ShutdownException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;


/**
 * Unit tests for {@link AgentDaemon}
 * TestAgentDaemon.
 *
 * @author
 * @author Philip Aston
 */
public class TestAgentDaemon extends AbstractJUnit4FileTestCase {

  @Mock private Logger m_logger;
  @Mock private Agent m_agent;

  @Before public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test public void testConstruction() throws Exception {
    final File propertyFile = new File(getDirectory(), "properties");
    final Agent agent = new AgentImplementation(m_logger, propertyFile, false);
    final AgentDaemon daemon = new AgentDaemon(m_logger, 1000, agent);
    daemon.shutdown();

    verify(m_logger).info(contains("finished"));
    verifyNoMoreInteractions(m_logger);
  }

  @Test public void testRun() throws Exception {
    final ActionListSleeperStubFactory sleeperStubFactory =
      new ActionListSleeperStubFactory(
        new SleepAction[] {
          new SleepAction() {
            public void sleep(long time) throws ShutdownException {
              assertEquals(1000, time);
            }
          },
          new SleepAction() {
            public void sleep(long time) throws ShutdownException {
              assertEquals(1000, time);
              throw new ShutdownException("");
            }
          }
      } );

    final AgentDaemon agentDaemon =
      new AgentDaemon(m_logger, 1000, m_agent, sleeperStubFactory.getStub());

    agentDaemon.run();

    sleeperStubFactory.assertFinished();
  }

  @Test public void testShutdownHook() throws Exception {
    final AgentDaemon agentDaemon = new AgentDaemon(m_logger, 0, m_agent);

    final Thread shutdownHook = agentDaemon.getShutdownHook();

    assertFalse(Runtime.getRuntime().removeShutdownHook(shutdownHook));
    verifyNoMoreInteractions(m_agent);

    final GrinderException runException = new GrinderException("") {};
    doThrow(runException).when(m_agent).run();

    try {
      agentDaemon.run();
      fail("Expected GrinderException");
    }
    catch (GrinderException e) {
      assertSame(runException, e);
    }
    assertTrue(Runtime.getRuntime().removeShutdownHook(shutdownHook));

    shutdownHook.run();
    verifyNoMoreInteractions(m_logger);
    verify(m_agent).shutdown();
  }

  public static class ActionListSleeperStubFactory
    // Good grief. Some horrible javac issue means we need to fully qualify
    // this.
    extends net.grinder.testutility.RandomStubFactory<
            net.grinder.util.Sleeper> {

    public interface SleepAction {
      void sleep(long time) throws ShutdownException;
    }

    private final SleepAction[] m_actions;
    private int m_nextRunnable = 0;

    public ActionListSleeperStubFactory(SleepAction[] actions) {
      super(Sleeper.class);
      m_actions = actions;
    }

    public void override_sleepNormal(Object proxy, long time)
      throws ShutdownException {
      m_actions[m_nextRunnable++].sleep(time);
    }

    public void assertFinished() {
      assertTrue("All actions complete", m_nextRunnable == m_actions.length);
    }
  }
}
