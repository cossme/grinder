// Copyright (C) 2001 - 2013 Philip Aston
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

package net.grinder.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.grinder.common.TimeAuthority;
import net.grinder.util.thread.Condition;

import org.slf4j.Logger;


/**
 * Manage sleeping.
 *
 * <p>Several threads can safely use the same <code>Sleeper</code>.
 * </p>
 *
 * @author Philip Aston
 */
public final class SleeperImplementation implements Sleeper {

  private static final Random s_random = new Random();
  private static final List<WeakReference<SleeperImplementation>>
    s_allSleepers = new ArrayList<WeakReference<SleeperImplementation>>();

  private final TimeAuthority m_timeAuthority;
  private final double m_factor;
  private final double m_limit9975Factor;
  private final Logger m_logger;
  private final Condition m_condition = new Condition();

  private boolean m_shutdown = false;

  /**
   * The constructor.
   *
   * @param timeAuthority An authority on the current time.
   * @param logger A logger to chat to. Pass <code>null</code> for no chat.
   * @param factor All sleep times are modified by this factor.
   * @param limit9975Factor See {@link #sleepNormal(long)}.
   */
  public SleeperImplementation(final TimeAuthority timeAuthority,
                               final Logger logger,
                               final double factor,
                               final double limit9975Factor) {

    if (factor < 0d || limit9975Factor < 0d) {
      throw new IllegalArgumentException("Factors must be positive");
    }

    synchronized (SleeperImplementation.class) {
      s_allSleepers.add(new WeakReference<SleeperImplementation>(this));
    }

    m_timeAuthority  = timeAuthority;
    m_factor = factor;
    m_limit9975Factor = limit9975Factor;
    m_logger = logger;
  }

  /**
   * Shutdown all Sleepers that are currently constructed.
   */
  public static synchronized void shutdownAllCurrentSleepers() {

    for (final WeakReference<SleeperImplementation> reference : s_allSleepers) {
      final Sleeper sleeper = reference.get();

      if (sleeper != null) {
        sleeper.shutdown();
      }
    }

    s_allSleepers.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override public void shutdown() {

    synchronized (m_condition) {
      m_shutdown = true;
      m_condition.notifyAll();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override public long getTimeInMilliseconds() {
    return m_timeAuthority.getTimeInMilliseconds();
  }

  /**
   * {@inheritDoc}
   */
  @Override public void sleepNormal(final long meanTime)
      throws ShutdownException {

    sleepNormal(meanTime, (long)((meanTime * m_limit9975Factor) / 3.0));
  }

  /**
   * {@inheritDoc}
   */
  @Override public void sleepNormal(final long meanTime, final long sigma)
    throws ShutdownException {

    checkShutdown();

    if (meanTime > 0) {
      if (sigma > 0) {
        doSleep(meanTime + (long) (s_random.nextGaussian() * sigma));
      }
      else {
        doSleep(meanTime);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override public void sleepFlat(final long maximumTime)
      throws ShutdownException {

    checkShutdown();

    if (maximumTime > 0) {
      doSleep(Math.abs(s_random.nextLong()) % maximumTime);
    }
  }

  private void doSleep(final long time) throws ShutdownException {

    final long factoredTime = (long)(time * m_factor);

    if (m_logger != null) {
      m_logger.info("sleeping for {} ms", factoredTime);
    }

    if (time > 0) {
      long currentTime = m_timeAuthority.getTimeInMilliseconds();
      final long wakeUpTime = currentTime + factoredTime;

      while (currentTime < wakeUpTime) {
        synchronized (m_condition) {
          checkShutdown();
          m_condition.waitNoInterrruptException(wakeUpTime - currentTime);
        }

        currentTime = m_timeAuthority.getTimeInMilliseconds();
      }
    }
  }

  private void checkShutdown() throws ShutdownException {

    synchronized (m_condition) {
      if (m_shutdown) {
        throw new ShutdownException("Shut down");
      }
    }
  }
}
