// Copyright (C) 2005 - 2008 Philip Aston
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

import net.grinder.common.processidentity.AgentIdentity;
import net.grinder.common.processidentity.WorkerIdentity;


/**
 * Agent process identity.
 *
 * Non-final so unit tests can extend.
 *
 * @author Philip Aston
 */
class AgentIdentityImplementation
  extends AbstractProcessIdentityImplementation implements AgentIdentity {

  private static final long serialVersionUID = 2;

  private int m_number = -1;
  private int m_nextWorkerNumber = 0;

  /**
   * Constructor.
   *
   * @param name The public name of the agent.
   */
  AgentIdentityImplementation(String name) {
    super(name);
  }

  /**
   * Return the console allocated agent number.
   *
   * @return The number.
   */
  public int getNumber() {
    return m_number;
  }
  /**
   * Set the console allocated agent number.
   *
   * @param number The number.
   */
  public void setNumber(int number) {
    m_number = number;
  }

  /**
   * Create a worker identity.
   *
   * @return The worker identity.
   */
  WorkerIdentityImplementation createWorkerIdentity() {
    return new WorkerIdentityImplementation(getName(), m_nextWorkerNumber++);
  }

  /**
   * Worker process identity.
   *
   * @author Philip Aston
   */
  final class WorkerIdentityImplementation
    extends AbstractProcessIdentityImplementation
    implements WorkerIdentity {

    private static final long serialVersionUID = 3;
    private int m_number;

    private WorkerIdentityImplementation(String agentName, int number) {
      super(agentName + "-" + number);
      m_number = number;
    }

    public AgentIdentity getAgentIdentity() {
      return AgentIdentityImplementation.this;
    }

    public int getNumber() {
      return m_number;
    }
  }
}
