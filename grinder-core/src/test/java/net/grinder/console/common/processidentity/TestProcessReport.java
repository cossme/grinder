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

package net.grinder.console.common.processidentity;

import java.util.Comparator;

import junit.framework.TestCase;
import net.grinder.common.processidentity.AgentIdentity;
import net.grinder.common.processidentity.AgentProcessReport;
import net.grinder.common.processidentity.ProcessReport;
import net.grinder.engine.agent.StubAgentIdentity;


/**
 * Unit tests for {@link ProcessReport}.
 *
 * @author Philip Aston
 */
public class TestProcessReport extends TestCase {

  public void testStateThenNameComparator() throws Exception {
    final Comparator<ProcessReport> comparator =
      new ProcessReport.StateThenNameThenNumberComparator();

    final AgentIdentity agentIdentity1 =
      new StubAgentIdentity("my agent");

    final AgentProcessReport agentProcessReport1 =
      new StubAgentProcessReport(agentIdentity1,
                                 ProcessReport.State.RUNNING);

    assertEquals(0,
      comparator.compare(agentProcessReport1, agentProcessReport1));

    final AgentProcessReport agentProcessReport2 =
      new StubAgentProcessReport(agentIdentity1,
                                 ProcessReport.State.FINISHED);

    assertTrue(
      comparator.compare(agentProcessReport1, agentProcessReport2) < 0);

    assertTrue(
      comparator.compare(agentProcessReport2, agentProcessReport1) > 0);

    final AgentIdentity agentIdentity2 =
      new StubAgentIdentity("zzzagent");

    final AgentProcessReport agentProcessReport3 =
      new StubAgentProcessReport(agentIdentity2,
                                 ProcessReport.State.FINISHED);

    assertTrue(
      comparator.compare(agentProcessReport3, agentProcessReport2) > 0);

    assertTrue(
      comparator.compare(agentProcessReport2, agentProcessReport3) < 0);
  }
}
