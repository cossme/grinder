package net.grinder.console.synchronisation;

import java.util.Set;

import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.synchronisation.LocalBarrierGroups;
import net.grinder.synchronisation.BarrierGroup.Listener;
import net.grinder.synchronisation.messages.BarrierIdentity;
import net.grinder.synchronisation.messages.OpenBarrierMessage;


/**
 * Centralised record of distributed barriers.
 *
 * @author Philip Aston
 */
final class ConsoleBarrierGroups extends LocalBarrierGroups {

  private final ConsoleCommunication m_communication;

  /**
   * Constructor.
   *
   * @param communication Console communication.
   */
  public ConsoleBarrierGroups(ConsoleCommunication communication) {
    m_communication = communication;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected BarrierGroupImplementation createBarrierGroup(final String name) {
    final BarrierGroupImplementation group = super.createBarrierGroup(name);

    group.addListener(new Listener() {
        public void awaken(Set<BarrierIdentity> waiters) {
          m_communication.sendToAgents(new OpenBarrierMessage(name, waiters));
        }
      });

    return group;
  }
}
