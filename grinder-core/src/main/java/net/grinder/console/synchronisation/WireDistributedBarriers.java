package net.grinder.console.synchronisation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.MessageDispatchRegistry.AbstractHandler;
import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.ProcessControl.ProcessReports;
import net.grinder.synchronisation.BarrierGroups;
import net.grinder.synchronisation.messages.AddBarrierMessage;
import net.grinder.synchronisation.messages.AddWaiterMessage;
import net.grinder.synchronisation.messages.CancelWaiterMessage;
import net.grinder.synchronisation.messages.RemoveBarriersMessage;


/**
 * Factory that wires up the support for global barriers.
 *
 * <p>Must be public for PicoContainer.</p>
 *
 * @see net.grinder.console.ConsoleFoundation
 * @author Philip Aston
 */
public class WireDistributedBarriers {
  // Guarded by self.
  private final Map<WorkerIdentity, ProcessBarrierGroups>
    m_processBarriers = new HashMap<WorkerIdentity, ProcessBarrierGroups>();

  private final BarrierGroups m_consoleBarrierGroups;

  /**
   * Constructor.
   *
   * @param communication Console communication.
   * @param processControl Console process control.
   */
  public WireDistributedBarriers(ConsoleCommunication communication,
                                 ProcessControl processControl) {
    this(communication,
         processControl,
         new ConsoleBarrierGroups(communication));
  }

  /**
   * Constructor.
   *
   * <p>Package scope for unit tests.
   *
   * @param communication Console communication.
   * @param processControl Console process control.
   * @param consoleBarrierGroups
   *          The central barrier groups, owned by the console.
   */
  WireDistributedBarriers(ConsoleCommunication communication,
                          ProcessControl processControl,
                          BarrierGroups consoleBarrierGroups) {

    m_consoleBarrierGroups = consoleBarrierGroups;

    final MessageDispatchRegistry messageDispatch =
      communication.getMessageDispatchRegistry();

    messageDispatch.set(
      AddBarrierMessage.class,
      new AbstractHandler<AddBarrierMessage>() {
        public void handle(AddBarrierMessage message)
          throws CommunicationException {

          getBarrierGroupsForProcess(message.getProcessIdentity())
            .getGroup(message.getName()).addBarrier();
        }
      });

    messageDispatch.set(
      RemoveBarriersMessage.class,
      new AbstractHandler<RemoveBarriersMessage>() {
        public void handle(RemoveBarriersMessage message)
          throws CommunicationException {

          getBarrierGroupsForProcess(message.getProcessIdentity())
            .getGroup(message.getName())
            .removeBarriers(message.getNumberOfBarriers());
        }
      });

    messageDispatch.set(
      AddWaiterMessage.class,
      new AbstractHandler<AddWaiterMessage>() {
        public void handle(AddWaiterMessage message)
          throws CommunicationException {

          getBarrierGroupsForProcess(message.getProcessIdentity())
            .getGroup(message.getName())
            .addWaiter(message.getBarrierIdentity());
        }
      });

    messageDispatch.set(
      CancelWaiterMessage.class,
      new AbstractHandler<CancelWaiterMessage>() {
        public void handle(CancelWaiterMessage message)
          throws CommunicationException {

          getBarrierGroupsForProcess(message.getProcessIdentity())
            .getGroup(message.getName())
            .cancelWaiter(message.getBarrierIdentity());
        }
      });

    processControl.addProcessStatusListener(
      new ProcessControl.Listener() {

        public void update(ProcessReports[] processReports) {
          final Set<WorkerIdentity> liveWorkers =
            new HashSet<WorkerIdentity>();

          for (ProcessReports agentReport : processReports) {
            for (WorkerProcessReport workerReport :
                agentReport.getWorkerProcessReports()) {
              liveWorkers.add(workerReport.getWorkerIdentity());
            }
          }

          final Set<Entry<WorkerIdentity, ProcessBarrierGroups>>
            dead = new HashSet<Entry<WorkerIdentity, ProcessBarrierGroups>>();

          synchronized (m_processBarriers) {
            for (Entry<WorkerIdentity, ProcessBarrierGroups> p :
                 m_processBarriers.entrySet()) {
              if (!liveWorkers.contains(p.getKey())) {
                dead.add(p);
              }
            }

            for (Entry<WorkerIdentity, ProcessBarrierGroups> p : dead) {
              m_processBarriers.remove(p.getKey());
            }
          }

          for (Entry<WorkerIdentity, ProcessBarrierGroups> p : dead) {
            try {
              p.getValue().cancelAll();
            }
            catch (CommunicationException e) {
              throw new AssertionError(e);
            }
          }
        }
      });
  }

  private BarrierGroups
    getBarrierGroupsForProcess(WorkerIdentity processIdentity) {

    synchronized (m_processBarriers) {
      final BarrierGroups existing = m_processBarriers.get(processIdentity);

      if (existing != null) {
        return existing;
      }

      final ProcessBarrierGroups newState =
        new ProcessBarrierGroups(m_consoleBarrierGroups);
      m_processBarriers.put(processIdentity, newState);

      return newState;
    }
  }
}
