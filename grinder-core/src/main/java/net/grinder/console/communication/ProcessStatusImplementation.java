// Copyright (C) 2001 - 2012 Philip Aston
// Copyright (C) 2001, 2002 Dirk Feufel
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import net.grinder.common.processidentity.AgentIdentity;
import net.grinder.common.processidentity.ProcessIdentity;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.console.communication.ProcessControl.Listener;
import net.grinder.messages.agent.CacheHighWaterMark;
import net.grinder.messages.console.AgentAddress;
import net.grinder.messages.console.AgentAndCacheReport;
import net.grinder.util.AllocateLowestNumber;
import net.grinder.util.ListenerSupport;


/**
 * Handles process status information.
 *
 * @author Dirk Feufel
 * @author Philip Aston
 */
final class ProcessStatusImplementation {

  /**
   * Period at which to update the listeners.
   */
  private static final long UPDATE_PERIOD = 500;

  /**
   * We keep a record of processes for a few seconds after they have been
   * terminated.
   *
   * Every FLUSH_PERIOD, process statuses are checked. Those haven't reported
   * for a while are marked and are discarded if they still haven't been
   * updated by the next FLUSH_PERIOD.
   */
  private static final long FLUSH_PERIOD = 2000;

  /**
   * Map of agent identities to AgentAndWorkers instances. Access is
   * synchronised on the map itself.
   */
  private final Map<ProcessIdentity, AgentAndWorkers>
    m_agentIdentityToAgentAndWorkers =
      new HashMap<ProcessIdentity, AgentAndWorkers>();

  /**
   * We have exclusive write access to m_agentNumberMap.We rely on our
   * synchronisation on m_agentIdentityToAgentAndWorkers to avoid
   * race conditions where the timer might otherwise remove an agent
   * immediately after a new report has just arrived.
   */
  private final AllocateLowestNumber m_agentNumberMap;

  private final ListenerSupport<Listener> m_listeners =
    new ListenerSupport<Listener>();

  private volatile boolean m_newData = false;

  /**
   * Constructor.
   *
   * @param timer Timer which can be used to schedule housekeeping tasks.
   * @param agentNumberMap Map of {@link AgentIdentity}s to integers.
   */
  public ProcessStatusImplementation(Timer timer,
                                     AllocateLowestNumber agentNumberMap) {
    m_agentNumberMap = agentNumberMap;
    timer.schedule(
      new TimerTask() {
        public void run() { update(); }
      },
      0, UPDATE_PERIOD);

    timer.schedule(
      new TimerTask() {
        public void run() {
          synchronized (m_agentIdentityToAgentAndWorkers) {
            purge(m_agentIdentityToAgentAndWorkers);
          }
        }
      },
      0, FLUSH_PERIOD);
  }

  /**
   * Add a new listener.
   *
   * @param listener A listener.
   */
  public void addListener(Listener listener) {
    m_listeners.add(listener);
  }

  /**
   * How many agents are live?
   *
   * @return The number of agents.
   */
  public int getNumberOfLiveAgents() {
    synchronized (m_agentIdentityToAgentAndWorkers) {
      return m_agentIdentityToAgentAndWorkers.size();
    }
  }

  private void update() {
    if (!m_newData) {
      return;
    }

    m_newData = false;

    final AgentAndWorkers[] processStatuses;

    synchronized (m_agentIdentityToAgentAndWorkers) {
      processStatuses =
        m_agentIdentityToAgentAndWorkers.values().toArray(
          new AgentAndWorkers[m_agentIdentityToAgentAndWorkers.size()]);
    }

    m_listeners.apply(
      new ListenerSupport.Informer<Listener>() {
        public void inform(Listener l) { l.update(processStatuses); }
      });
  }

  private AgentAndWorkers getAgentAndWorkers(AgentIdentity agentIdentity) {

    synchronized (m_agentIdentityToAgentAndWorkers) {
      final AgentAndWorkers existing =
        m_agentIdentityToAgentAndWorkers.get(agentIdentity);

      if (existing != null) {
        return existing;
      }

      final AgentAndWorkers created = new AgentAndWorkers(agentIdentity);
      m_agentIdentityToAgentAndWorkers.put(agentIdentity, created);

      m_agentNumberMap.add(agentIdentity);

      return created;
    }
  }

  /**
   * Add an agent status report.
   *
   * @param agentProcessStatus Process status.
   */
  public void addAgentStatusReport(AgentAndCacheReport agentProcessStatus) {

    final AgentAndWorkers agentAndWorkers =
      getAgentAndWorkers(agentProcessStatus.getAgentIdentity());

    agentAndWorkers.setAgentProcessStatus(agentProcessStatus);

    m_newData = true;
  }

  /**
   * Add a worker status report.
   *
   * @param workerProcessStatus Process status.
   */
  public void addWorkerStatusReport(WorkerProcessReport workerProcessStatus) {

    final AgentIdentity agentIdentity =
      workerProcessStatus.getWorkerIdentity().getAgentIdentity();

    getAgentAndWorkers(agentIdentity).setWorkerProcessStatus(
      workerProcessStatus);

    m_newData = true;
  }

  /**
   * Callers are responsible for synchronisation.
   */
  private void purge(Map<? extends ProcessIdentity,
                     ? extends Purgable> purgableMap) {

    final Set<ProcessIdentity> zombies = new HashSet<ProcessIdentity>();

    for (Entry<? extends ProcessIdentity, ? extends Purgable> entry :
         purgableMap.entrySet()) {

      if (entry.getValue().shouldPurge()) {
        zombies.add(entry.getKey());
      }
    }

    if (zombies.size() > 0) {
      purgableMap.keySet().removeAll(zombies);
      m_newData = true;
    }
  }

  private interface Purgable {
    boolean shouldPurge();
  }

  private abstract class AbstractTimedReference implements Purgable {
    private int m_purgeDelayCount;

    public boolean shouldPurge() {
      // Processes have a short time to report - see the javadoc for
      // FLUSH_PERIOD.
      if (m_purgeDelayCount > 0) {
        return true;
      }

      ++m_purgeDelayCount;

      return false;
    }
  }

  private final class AgentReference extends AbstractTimedReference {
    private final AgentAndCacheReport m_agentProcessReport;

    AgentReference(AgentAndCacheReport agentProcessReport) {
      m_agentProcessReport = agentProcessReport;
    }

    public AgentAndCacheReport getAgentProcessReport() {
      return m_agentProcessReport;
    }

    @Override public boolean shouldPurge() {
      final boolean purge = super.shouldPurge();

      if (purge) {
        // Protected against race with add since the caller holds
        // m_agentIdentityToAgentAndWorkers, and we are about to be
        // removed from m_agentIdentityToAgentAndWorkers.
        m_agentNumberMap.remove(m_agentProcessReport.getAgentIdentity());
      }

      return purge;
    }
  }

  private final class WorkerReference extends AbstractTimedReference {
    private final WorkerProcessReport m_workerProcessReport;

    WorkerReference(WorkerProcessReport workerProcessReport) {
      m_workerProcessReport = workerProcessReport;
    }

    public WorkerProcessReport getWorkerProcessReport() {
      return m_workerProcessReport;
    }
  }

  private static final class UnknownAgentProcessReport
    implements AgentAndCacheReport {

    private final AgentAddress m_address;

    public UnknownAgentProcessReport(AgentAddress address) {
      m_address = address;
    }

    public AgentAddress getProcessAddress() {
      return m_address;
    }

    public AgentIdentity getAgentIdentity() {
      return m_address.getIdentity();
    }

    public State getState() {
      return State.UNKNOWN;
    }

    public CacheHighWaterMark getCacheHighWaterMark() {
      return null;
    }
  }

  /**
   * Implementation of {@link ProcessControl.ProcessReports}.
   *
   * Package scope for unit tests.
   */
  final class AgentAndWorkers
    implements ProcessControl.ProcessReports, Purgable {

    private volatile AgentReference m_agentReportReference;

    // Synchronise on map before accessing.
    private final Map<WorkerIdentity, WorkerReference>
      m_workerReportReferences = new HashMap<WorkerIdentity, WorkerReference>();

    AgentAndWorkers(AgentIdentity agentIdentity) {
      setAgentProcessStatus(
        new UnknownAgentProcessReport(new AgentAddress(agentIdentity)));
    }

    void setAgentProcessStatus(AgentAndCacheReport agentProcessStatus) {
      m_agentReportReference = new AgentReference(agentProcessStatus);
    }

    public AgentAndCacheReport getAgentProcessReport() {
      return m_agentReportReference.getAgentProcessReport();
    }

    void setWorkerProcessStatus(WorkerProcessReport workerProcessStatus) {

      synchronized (m_workerReportReferences) {
        m_workerReportReferences.put(workerProcessStatus.getWorkerIdentity(),
                                     new WorkerReference(workerProcessStatus));
      }
    }

    public WorkerProcessReport[] getWorkerProcessReports() {

      synchronized (m_workerReportReferences) {
        final WorkerProcessReport[] result =
          new WorkerProcessReport[m_workerReportReferences.size()];

        int i = 0;

        for (WorkerReference worker : m_workerReportReferences.values()) {
          result[i++] = worker.getWorkerProcessReport();
        }

        return result;
      }
    }

    public boolean shouldPurge() {
      synchronized (m_workerReportReferences) {
        purge(m_workerReportReferences);
      }

      return m_agentReportReference.shouldPurge();
    }
  }
}
