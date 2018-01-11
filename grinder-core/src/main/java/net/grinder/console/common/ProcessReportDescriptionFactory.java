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

package net.grinder.console.common;

import net.grinder.common.processidentity.AgentIdentity;
import net.grinder.common.processidentity.AgentProcessReport;
import net.grinder.common.processidentity.WorkerProcessReport;


/**
 * Factory that converts process reports into descriptions that can be used in
 * the user interface.
 *
 * @author Philip Aston
 */
public final class ProcessReportDescriptionFactory {
  private final String m_threadsString;
  private final String m_agentString;
  private final String m_workerString;
  private final String m_stateStartedString;
  private final String m_stateRunningString;
  private final String m_stateFinishedString;
  private final String m_stateConnectedString;
  private final String m_stateDisconnectedString;
  private final String m_stateUnknownString;

  /**
   * Constructor.
   *
   * @param resources Console resources.
   */
  public ProcessReportDescriptionFactory(Resources resources) {
    m_threadsString = resources.getString("processTable.threads.label");

    m_agentString = resources.getString("processTable.agentProcess.label");
    m_workerString = resources.getString("processTable.workerProcess.label");

    m_stateStartedString = resources.getString("processState.started.label");
    m_stateRunningString = resources.getString("processState.running.label");
    m_stateFinishedString = resources.getString("processState.finished.label");
    m_stateConnectedString =
      resources.getString("processState.connected.label");
    m_stateDisconnectedString =
      resources.getString("processState.disconnected.label");
    m_stateUnknownString = resources.getString("processState.unknown.label");
  }

  /**
   * Factory method that creates a description from an agent process report.
   *
   * @param agentProcessReport The process report.
   * @return The description.
   */
  public ProcessDescription create(AgentProcessReport agentProcessReport) {

    final String state;

    switch (agentProcessReport.getState()) {
      case STARTED:
      case RUNNING:
        state = m_stateConnectedString;
        break;

      case FINISHED:
        state = m_stateDisconnectedString;
        break;

      case UNKNOWN:
      default:
        state = m_stateUnknownString;
        break;
    }

    final AgentIdentity agentIdentity = agentProcessReport.getAgentIdentity();

    final StringBuilder name = new StringBuilder(agentIdentity.getName());

    if (agentIdentity.getNumber() >= 0) {
      name.append(" (");
      name.append(m_agentString);
      name.append(" ");
      name.append(agentIdentity.getNumber());
      name.append(")");
    }

    return new ProcessDescription(name.toString(), m_agentString, state);
  }

  /**
   * Factory method that creates a description from a worker process report.
   *
   * @param workerProcessReport The process report.
   * @return The description.
   */
  public ProcessDescription create(WorkerProcessReport workerProcessReport) {

    final String state;

    switch (workerProcessReport.getState()) {
      case STARTED:
        state = m_stateStartedString;
        break;

      case RUNNING:
        state = m_stateRunningString + " (" +
                workerProcessReport.getNumberOfRunningThreads() + "/" +
                workerProcessReport.getMaximumNumberOfThreads() + " " +
                m_threadsString + ")";
        break;

      case FINISHED:
        state = m_stateFinishedString;
        break;

      default:
        state = m_stateUnknownString;
        break;
    }

    return new ProcessDescription(
      workerProcessReport.getWorkerIdentity().getName(),
      m_workerString,
      state);
  }

  /**
   * Various descriptions of the attributes of a process report.
   */
  public static final class ProcessDescription {
    private final String m_name;
    private final String m_processType;
    private final String m_state;

    private ProcessDescription(String name,
                               String processType,
                               String state) {
      m_name = name;
      m_processType = processType;
      m_state = state;
    }

    /**
     * The process name.
     *
     * @return The name.
     */
    public String getName() {
      return m_name;
    }

    /**
     * Description of the process type.
     *
     * @return The process type.
     */
    public String getProcessType() {
      return m_processType;
    }

    /**
     * Description of the process state.
     *
     * @return The process state.
     */
    public String getState() {
      return m_state;
    }

    /**
     * All descriptions in one string.
     *
     * @return The descriptions.
     */
    @Override public String toString() {
      return getProcessType() + " " + getName() + " [" + getState() + "]";
    }
  }
}
