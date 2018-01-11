// Copyright (C) 2006 - 2013 Philip Aston
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
import net.grinder.script.InvalidContextException;
import net.grinder.script.NoSuchStatisticException;
import net.grinder.script.Statistics.StatisticsForTest;
import net.grinder.statistics.ImmutableStatisticsSet;
import net.grinder.statistics.StatisticsIndexMap.DoubleIndex;
import net.grinder.statistics.StatisticsIndexMap.LongIndex;
import net.grinder.statistics.StatisticsSet;


/**
 * StatisticsForTestImplementation.
 *
 * <p>Starts off associated with a dispatch context and the context's mutable
 * statistics set, until freeze is called after which it is a wrapper around
 * a read-only statistics set.
 *
 * @author Philip Aston
 */
final class StatisticsForTestImplementation implements StatisticsForTest {

  private final TestStatisticsHelper m_testStatisticsHelper;
  private final Test m_test;

  /* Class invariant:
   *    if  m_dispatchContext != null:
   *        m_mutableStatistics != null
   *        m_immutableStatistics == m_mutableStatistics
   *    else:
   *        m_mutableStatistics == null
   *        m_immutableStatistics != null
   */
  private final ImmutableStatisticsSet m_immutableStatistics;
  private DispatchContext m_dispatchContext;
  private StatisticsSet m_statistics;

  public StatisticsForTestImplementation(
    final DispatchContext dispatchContext,
    final TestStatisticsHelper testStatisticsHelper,
    final StatisticsSet statistics) {

    m_testStatisticsHelper = testStatisticsHelper;
    m_test = dispatchContext.getTest();
    m_dispatchContext = dispatchContext;
    m_statistics = statistics;
    m_immutableStatistics = m_statistics;
  }

  public void freeze() {
    // Disassociate this object from the dispatcher.
    m_dispatchContext = null;
    m_statistics = null;
  }

  private StatisticsSet getStatisticsChecked() throws InvalidContextException {
    if (m_statistics == null) {
      throw new InvalidContextException(
        "Statistics for test invocation have been reported and cannot be " +
        "updated. Perhaps you should have called setDelayReports(true)?");
    }

    return m_statistics;
  }

  public StatisticsSet getStatistics() {
    return m_statistics;
  }

  private ImmutableStatisticsSet getImmutableStatistics() {
    return m_immutableStatistics;
  }

  @Override
  public Test getTest() {
    return m_test;
  }

  @Override
  public void setLong(final String statisticName, final long value)
    throws InvalidContextException, NoSuchStatisticException {

    getStatisticsChecked().setValue(getLongIndex(statisticName), value);
  }

  @Override
  public void setDouble(final String statisticName, final double value)
    throws InvalidContextException, NoSuchStatisticException {

    getStatisticsChecked().setValue(getDoubleIndex(statisticName), value);
  }

  @Override
  public void addLong(final String statisticName, final long value)
    throws InvalidContextException, NoSuchStatisticException {

    getStatisticsChecked().addValue(getLongIndex(statisticName), value);
  }

  @Override
  public void addDouble(final String statisticName, final double value)
    throws InvalidContextException, NoSuchStatisticException {

    getStatisticsChecked().addValue(getDoubleIndex(statisticName), value);
  }

  @Override
  public long getLong(final String statisticName)
      throws NoSuchStatisticException {
    return getImmutableStatistics().getValue(getLongIndex(statisticName));
  }

  @Override
  public double getDouble(final String statisticName)
    throws NoSuchStatisticException {

    return getImmutableStatistics().getValue(getDoubleIndex(statisticName));
  }

  @Override
  public void setSuccess(final boolean success) throws InvalidContextException {
    m_testStatisticsHelper.setSuccess(getStatisticsChecked(), success);
  }

  @Override
  public boolean getSuccess() {
    return m_testStatisticsHelper.getSuccess(getImmutableStatistics());
  }

  @Override
  public long getTime() {
    if (m_dispatchContext != null) {
      return m_dispatchContext.getElapsedTime();
    }
    else {
      return m_testStatisticsHelper.getTestTime(getImmutableStatistics());
    }
  }

  private DoubleIndex getDoubleIndex(final String statisticName)
    throws NoSuchStatisticException {

    final DoubleIndex index =
      m_testStatisticsHelper.getStatisticsIndexMap().getDoubleIndex(
        statisticName);

    if (index == null) {
      throw new NoSuchStatisticException(
        "'" + statisticName + "' is not a basic double statistic.");
    }

    return index;
  }

  private LongIndex getLongIndex(final String statisticName)
    throws NoSuchStatisticException {

    final LongIndex index =
      m_testStatisticsHelper.getStatisticsIndexMap().getLongIndex(
        statisticName);

    if (index == null) {
      throw new NoSuchStatisticException(
        "'" + statisticName + "' is not a basic long statistic.");
    }

    return index;
  }

  private StopWatch getPauseTimer() throws InvalidContextException {
    if (m_dispatchContext == null) {
      throw new InvalidContextException("The test is no longer in progress.");
    }

    return m_dispatchContext.getPauseTimer();
  }

  @Override
  public void pauseClock() throws InvalidContextException {
    getPauseTimer().start();
  }

  @Override
  public void resumeClock() throws InvalidContextException {
    getPauseTimer().stop();
  }
}
