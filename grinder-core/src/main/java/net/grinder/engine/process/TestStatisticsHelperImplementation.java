// Copyright (C) 2006 Philip Aston
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
import net.grinder.statistics.ImmutableStatisticsSet;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.TestStatisticsMap;


/**
 * TestStatisticsHelper implementation.
 *
 * @author Philip Aston
 */
final class TestStatisticsHelperImplementation
  implements TestStatisticsHelper {

  private final StatisticsIndexMap m_statisticsIndexMap;
  private final StatisticsIndexMap.LongIndex m_errorsIndex;
  private final StatisticsIndexMap.LongIndex m_untimedTestsIndex;
  private final StatisticsIndexMap.LongSampleIndex m_timedTestsIndex;

  public TestStatisticsHelperImplementation(StatisticsIndexMap indexMap) {

    m_statisticsIndexMap = indexMap;
    m_errorsIndex = indexMap.getLongIndex("errors");
    m_untimedTestsIndex = indexMap.getLongIndex("untimedTests");
    m_timedTestsIndex = indexMap.getLongSampleIndex("timedTests");
  }

  public boolean getSuccess(ImmutableStatisticsSet statistics) {
    return statistics.getValue(m_errorsIndex) == 0;
  }

  public void setSuccess(StatisticsSet statistics, boolean success) {
    statistics.setValue(m_errorsIndex, success ? 0 : 1);
  }

  /**
   * Set the elapsed time for the test and normalise statistics.
   */
  public void recordTest(StatisticsSet statistics, long elapsedTime) {
    statistics.reset(m_timedTestsIndex);
    statistics.addSample(m_timedTestsIndex, elapsedTime);

    setSuccess(statistics, getSuccess(statistics));

    // Should only be set for statistics sent to the console.
    statistics.setValue(m_untimedTestsIndex, 0);
  }

  public long getTestTime(ImmutableStatisticsSet statistics) {
    return statistics.getSum(m_timedTestsIndex);
  }

  public void removeTestTimeFromSample(TestStatisticsMap sample) {
    sample.new ForEach() {
      public void next(Test test, StatisticsSet statistics) {
        statistics.addValue(m_untimedTestsIndex,
                            statistics.getCount(m_timedTestsIndex));
        statistics.reset(m_timedTestsIndex);
      }
    }
    .iterate();
  }

  public StatisticsIndexMap getStatisticsIndexMap() {
    return m_statisticsIndexMap;
  }

  public void incrementErrors(StatisticsSet testStatistics) {
    testStatistics.addValue(m_errorsIndex, 1);
  }
}
