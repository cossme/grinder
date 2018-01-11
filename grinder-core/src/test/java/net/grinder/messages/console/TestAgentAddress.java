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

package net.grinder.messages.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.grinder.common.processidentity.AgentIdentity;
import net.grinder.communication.Address;
import net.grinder.engine.agent.StubAgentIdentity;

import org.junit.Test;


/**
 * Unit tests for {@link AgentAddress}.
 *
 * @author Philip Aston
 */
public class TestAgentAddress {

  private final AgentIdentity m_agent1 = new StubAgentIdentity("agent1");
  private final AgentIdentity m_agent2 = new StubAgentIdentity("agent2");

  private final Address m_address1 = new AgentAddress(m_agent1);
  private final Address m_address1Too = new AgentAddress(m_agent1);
  private final Address m_address2 = new AgentAddress(m_agent2);

  @Test public void testIncludes() throws Exception {
    assertTrue(m_address1.includes(m_address1));
    assertTrue(m_address1.includes(m_address1Too));

    assertFalse(m_address1.includes(m_address2));

    assertFalse(m_address1.includes(null));
  }

  @Test public void testEqualityAndHashCode() throws Exception {
    assertEquals(m_address1, m_address1);
    assertEquals(m_address1, m_address1Too);

    assertFalse(m_address2.equals(m_address1));
    assertFalse(m_address1.equals(m_address2));

    assertFalse(m_address1.equals(null));

    assertFalse(m_address1.equals(this));

    assertEquals(m_address1.hashCode(), m_address1Too.hashCode());
  }
}
