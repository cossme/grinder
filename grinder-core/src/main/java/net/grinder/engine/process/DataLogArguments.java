// Copyright (C) 2012 Philip Aston
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

import net.grinder.common.Test;
import net.grinder.statistics.StatisticsSet;


/**
 * The data logger provides this additional information for use by custom
 * appender configurations in the first SLF4J argument.
 *
 * @author Philip Aston
 */
public final class DataLogArguments {

  private final int m_threadNumber;
  private final int m_runNumber;
  private final Test m_test;
  private final long m_timeSinceExecutionStart;
  private final StatisticsSet m_statistics;

  /**
   * Constructor.
   *
   * @param threadNumber Worker thread number.
   * @param runNumber Run number.
   * @param test The test.
   * @param statistics
   * @param timeSinceExecutionStart
   */
  DataLogArguments(int threadNumber,
                   int runNumber,
                   Test test,
                   long timeSinceExecutionStart,
                   StatisticsSet statistics) {
    m_threadNumber = threadNumber;
    m_runNumber = runNumber;
    m_test = test;
    m_timeSinceExecutionStart = timeSinceExecutionStart;
    m_statistics = statistics;
  }

  /**
   * Worker thread number.
   *
   * @return The thread number.
   */
  public int getThreadNumber() {
    return m_threadNumber;
  }

  /**
   * The run number.
   *
   * @return The run number.
   */
  public int getRunNumber() {
    return m_runNumber;
  }

  /**
   * The test.
   *
   * @return The test.
   */
  public Test getTest() {
    return m_test;
  }

  /**
   * Time since the worker process started.
   *
   * @return The time, in milliseconds.
   */
  public long getTimeSinceExecutionStart() {
    return m_timeSinceExecutionStart;
  }

  /**
   * The statistics for the test execution.
   *
   * @return The statistics.
   */
  public StatisticsSet getStatistics() {
    return m_statistics;
  }
}
