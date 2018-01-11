// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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

package net.grinder.statistics;


/**
 * Common queries against the standard statistics.
 *
 * @author Philip Aston
 */
public final class TestStatisticsQueries {

  private final StatisticsIndexMap.LongIndex m_errorsIndex;
  private final StatisticsIndexMap.LongIndex m_untimedTestsIndex;
  private final StatisticsIndexMap.LongSampleIndex m_timedTestsIndex;

  /**
   * Constructor.
   *
   * @param statisticsIndexMap The index map to use.
   */
  TestStatisticsQueries(StatisticsIndexMap statisticsIndexMap) {
    m_errorsIndex = statisticsIndexMap.getLongIndex("errors");
    m_untimedTestsIndex = statisticsIndexMap.getLongIndex("untimedTests");
    m_timedTestsIndex = statisticsIndexMap.getLongSampleIndex("timedTests");
  }

  /**
   * Return the number of tests. This is equal to the sum of the
   * <em>timedTests</em> <em>count</em> value and the
   * <em>untimedTests</em> value.
   *
   * @param statistics The statistics to query.
   * @return a <code>long</code> value
   */
  public long getNumberOfTests(StatisticsSet statistics) {
    return
      statistics.getCount(m_timedTestsIndex) +
      statistics.getValue(m_untimedTestsIndex);
  }

  /**
   * Return the value of the <em>errors</em> statistic.
   *
   * @param statistics The statistics to query.
   * @return a <code>long</code> value
   */
  public long getNumberOfErrors(StatisticsSet statistics) {
    return statistics.getValue(m_errorsIndex);
  }

  /**
   * Return the value obtained by dividing the <em>timedTests</em> sample
   * statistics <em>total</em> attribute by its <em>count</em> attribute.
   *
   * @param statistics The statistics to query.
   * @return a <code>double</code> value
   */
  public double getAverageTestTime(StatisticsSet statistics) {
    final long count = statistics.getCount(m_timedTestsIndex);

    return
      count == 0 ?
      Double.NaN : statistics.getSum(m_timedTestsIndex) / (double)count;
  }
}
