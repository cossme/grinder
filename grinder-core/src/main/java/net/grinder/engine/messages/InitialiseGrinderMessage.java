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

package net.grinder.engine.messages;

import net.grinder.common.GrinderProperties;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.communication.Message;
import net.grinder.engine.common.ScriptLocation;


/**
 * Message used by the agent to initialise the worker processes.
 *
 * @author Philip Aston
 */
public final class InitialiseGrinderMessage implements Message {

  private static final long serialVersionUID = 5L;

  private final WorkerIdentity m_workerIdentity;
  private final WorkerIdentity m_firstWorkerIdentity;
  private final boolean m_reportToConsole;
  private final ScriptLocation m_script;
  private final GrinderProperties m_properties;

  /**
   * Constructor.
   *
   * @param workerIdentity Worker process identity.
   * @param firstWorkerIdentity The identity of the first worker of this
   *  generation.
   * @param reportToConsole Whether or not the worker process should
   * report to the console.
   * @param script The script to run.
   * @param properties Properties from the agent.
   */
  public InitialiseGrinderMessage(WorkerIdentity workerIdentity,
                                  WorkerIdentity firstWorkerIdentity,
                                  boolean reportToConsole,
                                  ScriptLocation script,
                                  GrinderProperties properties) {
    m_workerIdentity = workerIdentity;
    m_firstWorkerIdentity = firstWorkerIdentity;
    m_reportToConsole = reportToConsole;
    m_script = script;
    m_properties = properties;
  }

  /**
   * Accessor for the worker identity.
   *
   * @return The worker identity.
   */
  public WorkerIdentity getWorkerIdentity() {
    return m_workerIdentity;
  }

  /**
   * Accessor for the first worker identity.
   *
   * @return The worker identity.
   */
  public WorkerIdentity getFirstWorkerIdentity() {
    return m_firstWorkerIdentity;
  }

  /**
   * Accessor.
   *
   * @return Whether or not the worker process should report to the
   * console.
   */
  public boolean getReportToConsole() {
    return m_reportToConsole;
  }

  /**
   * Accessor.
   *
   * @return The script file to run.
   */
  public ScriptLocation getScript() {
    return m_script;
  }

  /**
   * Accessor.
   *
   * @return Properties from the agent.
   */
  public GrinderProperties getProperties() {
    return m_properties;
  }
}
