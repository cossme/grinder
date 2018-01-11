// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2008 Philip Aston
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

package net.grinder.messages.agent;

import net.grinder.common.GrinderProperties;
import net.grinder.communication.Message;


/**
 * Message used to start the Grinder processes.
 *
 * @author Philip Aston
 */
public final class StartGrinderMessage implements Message {

  private static final long serialVersionUID = 4L;

  private final GrinderProperties m_properties;

  private final int m_agentNumber;

  /**
   * Constructor.
   *
   * @param properties
   *            A set of properties that override values in the Agents' local
   *            files.
   * @param agentNumber
   *            The console allocated agent number.
   */
  public StartGrinderMessage(GrinderProperties properties, int agentNumber) {
    m_properties = properties;
    m_agentNumber = agentNumber;
  }

  /**
   * A set of properties that override values in the Agents' local files.
   *
   * @return The properties.
   */
  public GrinderProperties getProperties() {
    return m_properties;
  }

  /**
   * The console allocated agent number.
   *
   * @return The agent number.
   */
  public int getAgentNumber() {
    return m_agentNumber;
  }
}
