// Copyright (C) 2011 - 2013 Philip Aston
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

package net.grinder.script;

import java.util.concurrent.TimeUnit;

import net.grinder.common.GrinderException;


/**
 * Distributed synchronisation interface that allows worker threads to
 * coordinate their actions.
 *
 * <p>
 * A {@code Barrier} is created with a particular barrier name. A thread calls
 * {@link #await} to wait for all other threads that have created a barrier with
 * the same barrier name to also call {@link #await}. For each barrier name, The
 * Grinder tracks the total number of barriers created {@code N}, and the number
 * of barriers waiting {@code W}. Whenever the two numbers are equal, the calls
 * to {@link #await} complete and the threads can continue.
 * </p>
 *
 * <p>
 * Each cooperating thread should create its own {@code Barrier} instance using
 * an agreed barrier name. Unlike the standard Java library's
 * {@link java.util.concurrent.CyclicBarrier}, only a single at any one time
 * thread may call {@code await} on a {@code Barrier} instance. This class is
 * thread safe; it may be useful {@link #cancel} a barrier from another thread.
 * </p>
 *
 * <h3>Design note</h3>
 * <p>
 * This interface differs significantly from
 * {@link java.util.concurrent.CyclicBarrier}. This API supports a distributed
 * implementation, where only a central coordinator knows the total number of
 * instances. Communication between different nodes uses distributed events,
 * rather than thread synchronisation primitives, and results in a more loosely
 * coupled approach. The number of parties participating for a given barrier
 * name can change dynamically. There is no concept of a broken barrier.
 * </p>
 *
 * @author Philip Aston
 * @since 3.6
 */
public interface Barrier {

  /**
   * Wait until all other barriers with the same name have invoked
   * {@link #await}.
   *
   * <p>
   * If this barrier is not the last with the name to call {@code await} and has
   * not been {@link #cancel cancelled}, this method blocks until one of the
   * following happens:
   *
   * <ul>
   * <li>{@code await} is called for the last barrier.</li>
   * <li>All non-waiting barriers with the same name are {@link #cancel
   * cancelled}.</li>
   * <li>This barrier is {@link #cancel cancelled}.</li>
   * <li>Some other thread {@link java.lang.Thread#interrupt() interrupts} the
   * current thread. In this case, the barrier instance will be
   * {@link #cancel cancelled}.</li>
   * </ul>
   * </p>
   *
   * <p>
   * If this barrier is the last with the name to call {@code await}, the method
   * will not block. All {@code await} calls made by other threads for barriers
   * with the same name will complete.
   * </p>
   *
   * @throws CancelledBarrierException
   *           If this barrier has been {@link #cancel() cancelled}.
   * @throws GrinderException
   *           If the operation failed due to a network issue.
   * @throws IllegalStateException
   *           If some other thread has called {@code await} on this barrier
   *           instance.
   * @throws net.grinder.common.UncheckedInterruptedException
   *           If the current thread is interrupted while waiting.
   */
  void await() throws GrinderException, CancelledBarrierException;

  /**
   * Version of {@link #await()} that allows a timeout to be specified.
   *
   * <p>
   * If the specified timeout elapses while the thread is waiting, the method
   * will return {@code false}, and the barrier instance will be
   * {@link #cancel() cancelled}.
   * </p>
   *
   * @param timeout
   *          The time to wait for the barrier.
   * @param unit
   *          The time unit of the {@code timeout} parameter.
   * @return {@code false} if and only if the waiting time detectably elapsed
   *         before return from the method.
   * @throws CancelledBarrierException
   *           If this barrier has been {@link #cancel() cancelled}.
   * @throws IllegalStateException
   *           If some other thread has called {@code await} on this barrier
   *           instance.
   * @throws GrinderException
   *           If the operation failed due to a network issue.
   * @throws net.grinder.common.UncheckedInterruptedException
   *           If the current thread is interrupted while waiting.
   */
  boolean await(long timeout, TimeUnit unit) throws GrinderException,
                                                    CancelledBarrierException;

  /**
   * <p>Equivalent to {@code await(timeout, TimeUnit.MILLISECONDS)}.</p>
   *
   * @param timeout
   *          The time to wait for the barrier.
   * @return {@code false} if and only if the waiting time detectably elapsed
   *         before return from the method.
   * @throws CancelledBarrierException
   *           If this barrier has been {@link #cancel() cancelled}.
   * @throws IllegalStateException
   *           If some other thread has called {@code await} on this barrier
   *           instance.
   * @throws GrinderException
   *           If the operation failed for some other reason.
   * @throws net.grinder.common.UncheckedInterruptedException
   *           If the current thread is interrupted while waiting.
   */
  boolean await(long timeout) throws GrinderException,
                                     CancelledBarrierException;

  /**
   * Cancel this {@code Barrier} and reduce the total number of instances for
   * the barrier name. If another thread is waiting on this barrier instance,
   * it will receive a {@link CancelledBarrierException}. Otherwise, if this
   * was the only idle barrier, the others with the same name will be awoken.
   *
   * <p>
   * Subsequent calls to {@link #await} for this {@code Barrier} will result
   * in an {@link CancelledBarrierException}.
   * </p>
   *
   * @throws GrinderException
   *           If the operation failed due to a network issue.
   */
  void cancel() throws GrinderException;

  /**
   * Return the name of the barrier.
   *
   * @return The barrier name.
   */
  String getName();
}
