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

package net.grinder.synchronisation;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.grinder.common.UncheckedInterruptedException;
import net.grinder.communication.CommunicationException;
import net.grinder.script.Barrier;
import net.grinder.script.CancelledBarrierException;
import net.grinder.synchronisation.messages.BarrierIdentity;
import net.grinder.util.thread.Condition;


/**
 * Barrier implementation.
 *
 * @author Philip Aston
 */
public final class BarrierImplementation
  implements Barrier, BarrierGroup.Listener {

  private final BarrierGroup m_barrierGroup;
  private final BarrierIdentity.Factory m_identityFactory;
  private final Condition m_condition = new Condition();

  private enum State {
    Idle {
      void beginWait(final BarrierImplementation b) {
        b.changeState(Waiting);
      }

      boolean awoken(BarrierImplementation b) throws CancelledBarrierException {
        return true;
      }
    },

    Waiting {
      void beginWait(BarrierImplementation b) {
        throw new IllegalStateException("Another thread has called await()");
      }

      boolean awoken(BarrierImplementation b) throws CommunicationException {
        // Timed out.
        b.cancel();
        return false;
      }
    },

    Cancelled {
      void beginWait(BarrierImplementation b) throws CancelledBarrierException {
        throw new CancelledBarrierException("Barrier is cancelled");
      }

      void awaken(BarrierImplementation b) {
        // No-op.
      }

      boolean awoken(BarrierImplementation b) throws CancelledBarrierException {
        throw new CancelledBarrierException("Cancelled while waiting");
      }

      void cancel(BarrierImplementation b) {
        // No-op.
      }
    };

    abstract void beginWait(BarrierImplementation b)
      throws CancelledBarrierException;

    void awaken(BarrierImplementation b) {
      b.changeState(Idle);
    }

    abstract boolean awoken(BarrierImplementation barrierImplementation)
      throws CancelledBarrierException, CommunicationException;

    void cancel(BarrierImplementation b) throws CommunicationException {
      b.changeState(Cancelled);
      b.m_barrierGroup.removeListener(b);
      b.m_barrierGroup.cancelWaiter(b.m_identity);
      b.m_barrierGroup.removeBarriers(1);
    }
  }

  // Guarded by m_condition.
  private State m_state = State.Idle;

  // Guarded by m_condition.
  private BarrierIdentity m_identity;

  /**
   * Constructor.
   *
   * @param group
   *          Barrier group.
   * @param identityFactory
   *          Identity generator.
   * @throws CommunicationException
   *           If the barrier group could not be created due to a
   *           network communication problem.
   */
  public BarrierImplementation(BarrierGroup group,
                               BarrierIdentity.Factory identityFactory)
    throws CommunicationException {

    m_barrierGroup = group;
    m_identityFactory = identityFactory;
    m_identity = identityFactory.next();

    m_barrierGroup.addListener(this);

    m_barrierGroup.addBarrier();
  }

  private void changeState(State newState) {
    m_state = newState;
    m_condition.notifyAll();
  }

  // I hate Java. When can we have closures?
  private abstract class Waiter {
    public boolean await()
      throws CancelledBarrierException, CommunicationException {

      synchronized (m_condition) {
        m_state.beginWait(BarrierImplementation.this);

        m_identity = m_identityFactory.next();
        m_barrierGroup.addWaiter(m_identity);

        while (m_state == State.Waiting) {
          if (doWait()) {
            break;
          }
        }

        return m_state.awoken(BarrierImplementation.this);
      }
    }

    /**
     * @return {@code true} iff the wait completed naturally.
     */
    protected abstract boolean doWait() throws CommunicationException;
  }

  private class ForeverWaiter extends Waiter {
    @Override public boolean doWait() throws CommunicationException {

      try {
        m_condition.wait();
      }
      catch (InterruptedException e) {
        cancel();
        throw new UncheckedInterruptedException(e);
      }

      return true;
    }
  }

  private class TimedWaiter extends Waiter {
    private long m_time;

    public TimedWaiter(long timeMillis) {
      m_time = timeMillis;
    }

    @Override public boolean doWait() throws CommunicationException {
      final long start = System.currentTimeMillis();

      try {
        m_condition.wait(m_time);
      }
      catch (InterruptedException e) {
        cancel();
        throw new UncheckedInterruptedException(e);
      }

      m_time -= System.currentTimeMillis() - start;

      return m_time < 1;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override public void await()
    throws CancelledBarrierException, CommunicationException {
    new ForeverWaiter().await();
  }

  /**
   * {@inheritDoc}
   */
  @Override public boolean await(long timeout, TimeUnit unit)
    throws CancelledBarrierException, CommunicationException {

    return new TimedWaiter(Math.max(1, unit.toMillis(timeout))).await();
  }

  /**
   * {@inheritDoc}
   */
  @Override public boolean await(long timeout)
    throws CancelledBarrierException, CommunicationException {

    return await(timeout, TimeUnit.MILLISECONDS);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void awaken(Set<BarrierIdentity> waiters) {
    synchronized (m_condition) {
      if (waiters.contains(m_identity)) {
        m_state.awaken(BarrierImplementation.this);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override public void cancel() throws CommunicationException {
    synchronized (m_condition) {
      m_state.cancel(this);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override public String getName() {
    return m_barrierGroup.getName();
  }
}
