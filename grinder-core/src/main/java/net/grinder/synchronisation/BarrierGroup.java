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

import net.grinder.communication.CommunicationException;
import net.grinder.synchronisation.messages.BarrierIdentity;


/**
 * A barrier group.
 *
 * <p>
 * A group has a number of barriers, {@code N}, and a number of waiters, {@code
 * W}. At any time, {@code 0 <= W < N} or {@code 0 == W == N}.
 * </p>
 *
 * <p>
 * One or more listeners can be registered with the group.
 * </p>
 *
 * <p>
 * Whenever {@code N} is positive and becomes equal to {@code W}, {@code W} will
 * be reset to {@code 0} and all the listeners will be notified.
 * </p>
 *
 * <p>
 * If {@link #removeBarriers} reduces {@code N} to {@code 0}, the instance will
 * become invalid. This allows implementations to remove barrier groups that are
 * no longer in use.
 * </p>
 *
 * <p>
 * This interface tracks the identity of waiting barriers through
 * {@link BarrierIdentity}. Doing so allows communication with the
 * {@link net.grinder.script.Barrier} API to be asynchronous.
 * </p>
 *
 * @author Philip Aston
 * @see net.grinder.script.Barrier
 */
public interface BarrierGroup {

  /**
   * Callback used to notify listeners.
   */
  interface Listener {
    void awaken(Set<BarrierIdentity> waiters);
  }

  /**
   * Add a listener.
   *
   * @param listener The listener.
   */
  void addListener(Listener listener);

  /**
   * Remove a listener.
   *
   * @param listener The listener.
   */
  void removeListener(Listener listener);

  /**
   * Increase the number of barriers in the group.
   *
   * @throws CommunicationException
   *           If the operation could not be completed due to a network
   *           communication problem. The barrier group is left in an unknown
   *           state.
   * @throws IllegalStateException
   *           If the barrier group is invalid. A group becomes invalid if
   *           {@link #removeBarriers} removes all of the barriers.
   */
  void addBarrier() throws CommunicationException;

  /**
   * Decrease the number of barriers in the group.
   *
   * @param n
   *          The number of barriers to remove.
   *
   * @throws CommunicationException
   *           If the operation could not be completed due to a network
   *           communication problem. The barrier group is left in an unknown
   *           state.
   * @throws IllegalStateException
   *           If {@code n > N - W}.
   */
  void removeBarriers(long n) throws CommunicationException;

  /**
   * Add a waiter.
   *
   * @param barrierIdentity Identifies the barrier.
   *
   * @throws CommunicationException
   *           If the operation could not be completed due to a network
   *           communication problem. The barrier group is left in an unknown
   *           state.
   * @throws IllegalStateException
   *           If {@code N == 0}, or the group is invalid.
   */
  void addWaiter(BarrierIdentity barrierIdentity) throws CommunicationException;

  /**
   * Cancel a waiter.
   *
   * <p>Does nothing if the {@code barrierIdentity} refers to an unknown waiter.
   * This copes with the following cases:
   * <ul>
   * <li>Cancel received asynchronously after barrier has been triggered.</li>
   * <li>Cancel received without a call to {@link #addWaiter}.</li>
   * </ul>
   * </p>
   *
   * @param barrierIdentity Identifies the barrier.
   *
   * @throws CommunicationException
   *           If the operation could not be completed due to a network
   *           communication problem. The barrier group is left in an unknown
   *           state.
   */
  void cancelWaiter(BarrierIdentity barrierIdentity)
    throws CommunicationException;

  /**
   * Cancels all waiters and removes all barriers.
   *
   * @throws CommunicationException
   *           If the operation could not be completed due to a network
   *           communication problem. The barrier group is left in an unknown
   *           state.
   */
  void cancelAll() throws CommunicationException;

  /**
   * Return the name of the barrier group.
   *
   * @return The barrier group name.
   */
  String getName();
}
