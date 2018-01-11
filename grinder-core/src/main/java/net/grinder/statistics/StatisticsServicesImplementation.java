// Copyright (C) 2005 - 2011 Philip Aston
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
 * Singleton that is the point of entry for {@link StatisticsServices}.
 *
 * @author Philip Aston
 */
public final class StatisticsServicesImplementation
  implements StatisticsServices {

  private static final StatisticsServicesImplementation s_instance;

  static {
    final StatisticsIndexMap statisticsIndexMap = new StatisticsIndexMap();
    final StatisticExpressionFactory statisticExpressionFactory =
      new StatisticExpressionFactoryImplementation(statisticsIndexMap);

    s_instance = new StatisticsServicesImplementation(
                   new CommonStatisticsViews(statisticExpressionFactory),
                   statisticExpressionFactory,
                   new StatisticsSetFactory(statisticsIndexMap),
                   statisticsIndexMap,
                   new TestStatisticsQueries(statisticsIndexMap));
  }

  /**
   * Singleton accessor.
   *
   * @return The singleton.
   */
  public static StatisticsServices getInstance() {
    return s_instance;
  }

  private final CommonStatisticsViews m_commonStatisticsViews;
  private final StatisticExpressionFactory m_statisticExpressionFactory;
  private final StatisticsSetFactory m_statisticsSetFactory;
  private final StatisticsIndexMap m_statisticsIndexMap;
  private final TestStatisticsQueries m_testStatisticsQueries;

  StatisticsServicesImplementation(
    CommonStatisticsViews commonStatisticsViews,
    StatisticExpressionFactory statisticExpressionFactory,
    StatisticsSetFactory statisticsSetFactory,
    StatisticsIndexMap statisticsIndexMap,
    TestStatisticsQueries testStatisticsQueries) {

    m_commonStatisticsViews = commonStatisticsViews;
    m_statisticExpressionFactory = statisticExpressionFactory;
    m_statisticsSetFactory = statisticsSetFactory;
    m_statisticsIndexMap = statisticsIndexMap;
    m_testStatisticsQueries = testStatisticsQueries;
  }

  /**
   * {@inheritDoc}
   */
  @Override public StatisticsView getDetailStatisticsView() {
    return m_commonStatisticsViews.getDetailStatisticsView();
  }

  /**
   * {@inheritDoc}
   */
  @Override public StatisticsView getSummaryStatisticsView() {
    return m_commonStatisticsViews.getSummaryStatisticsView();
  }

  /**
   * {@inheritDoc}
   */
  @Override public StatisticExpressionFactory getStatisticExpressionFactory() {
    return m_statisticExpressionFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override public StatisticsSetFactory getStatisticsSetFactory() {
    return m_statisticsSetFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override public StatisticsIndexMap getStatisticsIndexMap() {
    return m_statisticsIndexMap;
  }

  /**
   * {@inheritDoc}
   */
  @Override public TestStatisticsQueries getTestStatisticsQueries() {
    return m_testStatisticsQueries;
  }

  /**
   * {@inheritDoc}
   */
  @Override public StatisticExpression getTPSExpression() {
    return m_commonStatisticsViews.getTPSExpression();
  }
}
