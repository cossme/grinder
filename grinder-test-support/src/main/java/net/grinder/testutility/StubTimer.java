// Copyright (C) 2008 - 2009 Philip Aston
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

package net.grinder.testutility;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Stub implementation of {@link Timer}.
 *
 * @author Philip Aston
 */
public final class StubTimer extends Timer {
  private final Map<Long, TimerTask> m_taskByPeriod =
    new HashMap<Long, TimerTask>();
  private TimerTask m_lastScheduledTimerTask;
  private long m_lastDelay;
  private long m_lastPeriod;

  public StubTimer() {
    super(false);
    super.cancel();
  }

  public void schedule(TimerTask timerTask, long delay, long period) {
    m_lastScheduledTimerTask = timerTask;
    m_lastDelay = delay;
    m_lastPeriod = period;
    m_taskByPeriod.put(new Long(period), timerTask);
  }

  public void schedule(TimerTask timerTask, long delay) {
    m_lastScheduledTimerTask = timerTask;
    m_lastDelay = delay;
    m_lastPeriod = 0;
  }


  public TimerTask getLastScheduledTimerTask() {
    return m_lastScheduledTimerTask;
  }

  public long getLastDelay() {
    return m_lastDelay;
  }

  public long getLastPeriod() {
    return m_lastPeriod;
  }

  public TimerTask getTaskByPeriod(long period) {
    return m_taskByPeriod.get(new Long(period));
  }
}
