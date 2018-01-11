// Copyright (C) 2005 - 2013 Philip Aston
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

package net.grinder.engine.process;

import net.grinder.common.TimeAuthority;


/**
 * Implementation of {@link StopWatch}.
 *
 * @author Philip Aston
 */
final class StopWatchImplementation implements StopWatch {

  private final TimeAuthority m_timeAuthority;

  private long m_time;
  private long m_startTime = -1;

  public StopWatchImplementation(final TimeAuthority timeAuthority) {
    m_timeAuthority = timeAuthority;
  }

  @Override
  public void start() {
    if (isRunning()) {
      throw new StopWatchRunningException("Already running");
    }

    m_startTime = m_timeAuthority.getTimeInMilliseconds();
  }

  @Override
  public void stop() {
    if (!isRunning()) {
      throw new StopWatchNotRunningException("Not running");
    }

    m_time = m_time + m_timeAuthority.getTimeInMilliseconds() - m_startTime;
    m_startTime = -1;
  }

  @Override
  public void reset() throws StopWatchRunningException {
    if (isRunning()) {
      throw new StopWatchRunningException("Still running");
    }

    m_time = 0;
  }

  @Override
  public long getTime() {
    if (isRunning()) {
      throw new StopWatchRunningException("Still running");
    }

    return m_time;
  }

  @Override
  public boolean isRunning() {
    return m_startTime != -1;
  }

  @Override
  public void add(final StopWatch watch) {
    m_time += watch.getTime();
  }
}
