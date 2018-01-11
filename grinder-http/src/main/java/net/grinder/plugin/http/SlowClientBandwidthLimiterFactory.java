// Copyright (C) 2006 - 2008 Philip Aston
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

package net.grinder.plugin.http;

import net.grinder.util.Sleeper;
import net.grinder.util.Sleeper.ShutdownException;
import HTTPClient.HTTPConnection.BandwidthLimiter;
import HTTPClient.HTTPConnection.BandwidthLimiterFactory;


/**
 * BandwidthLimiterFactory that creates a
 * {@link HTTPClient.HTTPConnection.BandwidthLimiter}
 * that restricts the bandwidth of the data transfered through the buffer in
 * {@link HTTPClient.HTTPConnection.BandwidthLimiter#maximumBytes} by sleeping.
 *
 * @author Philip Aston
 */
final class SlowClientBandwidthLimiterFactory
  implements BandwidthLimiterFactory {

  private final Sleeper m_sleeper;
  private final int m_targetBPS;
  private final int m_bufferIncrement;

  public SlowClientBandwidthLimiterFactory(Sleeper sleeper, int targetBPS) {
    m_sleeper = sleeper;
    m_targetBPS = targetBPS;

    // We must use a larger buffer increment for higher BPS rates due to the
    // precision to which we can sleep (maybe ~10ms). Limiting for higher
    // BPS rates will only apply to large messages, but that's not something
    // we can help.
    //
    // I considered adjusting the buffer increment based on the target baud, or
    // dynamically based on the measured performance. I discounted this because
    // there's no obvious algorithm, and its likely to cause non-linear
    // behaviour due to external influences such as the MTU size. Also, having
    // the increment too small will increase the work that we have to do within
    // The Grinder, which might significantly skew timings.
    m_bufferIncrement = Math.max(100, m_targetBPS / 500);
  }

  public BandwidthLimiter create() {
    return new SlowClientBandwidthLimiter();
  }

  private final class SlowClientBandwidthLimiter implements BandwidthLimiter {

    // Too large and the instantaneous bandwidth varies too much.
    // Too small and we don't reach the target fast enough.
    private static final float DAMPING_FACTOR = 0.5f;

    private long m_startTime;
    private int m_sleepTime;
    private float m_damping;

    public int maximumBytes(int position) {

      final long now = m_sleeper.getTimeInMilliseconds();

      if (position == 0) {
        m_startTime = now;

        // Set the initial sleep time to 0 so we start pumping bytes straight
        // away.
        m_sleepTime = 0;

        // Set the second sleep time based on the first lot of bytes transfered.
        // The damping is 2 to account for the initial call.
        m_damping = 2;
      }
      else {
        final long expectedTime = (long)position * 8 * 1000 / m_targetBPS;
        final long actualTime = now - m_startTime;
        m_sleepTime += (expectedTime - actualTime) * m_damping;

        if (m_sleepTime < 0) {
          m_sleepTime = 0;
        }

        m_damping = DAMPING_FACTOR;
      }

      try {
        m_sleeper.sleepNormal(m_sleepTime, 0);
      }
      catch (ShutdownException e) {
        // Don't propagate exception - the thread will work out it's shutdown
        // soon enough.
      }

      // Allow m_bufferIncrement bytes to be read.
      return m_bufferIncrement;
    }
  }
}
