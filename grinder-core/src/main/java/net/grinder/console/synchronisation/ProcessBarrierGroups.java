// Copyright (C) 2011 Philip Aston
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

package net.grinder.console.synchronisation;

import java.util.Set;

import net.grinder.communication.CommunicationException;
import net.grinder.synchronisation.AbstractBarrierGroups;
import net.grinder.synchronisation.BarrierGroup;
import net.grinder.synchronisation.BarrierGroups;
import net.grinder.synchronisation.messages.BarrierIdentity;


/**
 * Keeps track of the barrier values for a particular worker process.
 *
 * <p>
 * Operations are delegated to the central set of barrier groups. If the worker
 * process disappears, {@link AbstractBarrierGroups#cancelAll} will clean up the
 * per-process barrier information.
 * </p>
 *
 * @author Philip Aston
 */
final class ProcessBarrierGroups extends AbstractBarrierGroups {

  private final BarrierGroups m_delegate;

  public ProcessBarrierGroups(BarrierGroups consoleBarrierGroups) {
    m_delegate = consoleBarrierGroups;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected BarrierGroupImplementation createBarrierGroup(String name) {

    final BarrierGroup delegateGroup = m_delegate.getGroup(name);

    return new BarrierGroupImplementation(name) {
      {
        delegateGroup.addListener(new Listener() {
          public void awaken(Set<BarrierIdentity> waiters) {
            clearWaiters(waiters);
          }
        });
      }

      @Override
      public void addBarrier() throws CommunicationException {

        super.addBarrier();
        delegateGroup.addBarrier();
      }

      @Override
      public void removeBarriers(long n) throws CommunicationException {

        super.removeBarriers(n);
        delegateGroup.removeBarriers(n);
      }

      @Override
      public void addWaiter(BarrierIdentity barrierIdentity)
        throws CommunicationException {

        super.addWaiter(barrierIdentity);
        delegateGroup.addWaiter(barrierIdentity);
      }

      @Override
      public void cancelWaiter(BarrierIdentity barrierIdentity)
        throws CommunicationException {

        super.cancelWaiter(barrierIdentity);
        delegateGroup.cancelWaiter(barrierIdentity);
      }
    };
  }
}
