// Copyright (C) 2005 - 2012 Philip Aston
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import net.grinder.common.StubTest;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesTestFactory;
import net.grinder.statistics.StatisticsSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;


/**
 * Unit tests for {@link ThreadDataLogger}.
 *
 * @author Philip Aston
 */
public class TestThreadDataLogger {

  @Mock private Logger m_dataLogger;
  @Captor private ArgumentCaptor<DataLogArguments> m_argumentCaptor;

  private final net.grinder.common.Test m_test1 = new StubTest(1, "T1");
  private final net.grinder.common.Test m_test3 = new StubTest(3, "T3");

  private final StatisticsServices m_statisticsServices =
    StatisticsServicesTestFactory.createTestInstance();

  private StatisticsIndexMap.LongIndex s_errorsIndex;
  private StatisticsIndexMap.LongSampleIndex s_timedTestsIndex;
  private StatisticsIndexMap.DoubleIndex s_userDouble0Index;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);

    final StatisticsIndexMap indexMap =
      m_statisticsServices.getStatisticsIndexMap();

    s_errorsIndex = indexMap.getLongIndex("errors");
    s_timedTestsIndex = indexMap.getLongSampleIndex("timedTests");
    s_userDouble0Index = indexMap.getDoubleIndex("userDouble0");
  }

  @Test public void testReport() throws Exception {

    final ThreadDataLogger ThreadDataLogger =
      new ThreadDataLogger(
          m_dataLogger,
          m_statisticsServices.getDetailStatisticsView().getExpressionViews(),
          33);

    final StatisticsSet statistics =
      m_statisticsServices.getStatisticsSetFactory().create();

    statistics.addSample(s_timedTestsIndex, 99);

    ThreadDataLogger.report(10, m_test1, 123L, statistics);

    verify(m_dataLogger).info(eq("33, 10, 1, 123, 99, 0"),
                              m_argumentCaptor.capture());

    final DataLogArguments arguments = m_argumentCaptor.getValue();

    assertEquals(33, arguments.getThreadNumber());
    assertEquals(10, arguments.getRunNumber());
    assertSame(m_test1, arguments.getTest());
    assertEquals(123L, arguments.getTimeSinceExecutionStart());
    assertSame(statistics, arguments.getStatistics());

    ThreadDataLogger.report(10, m_test1, 125L, statistics);

    verify(m_dataLogger).info(eq("33, 10, 1, 125, 99, 0"),
                              m_argumentCaptor.capture());

    ThreadDataLogger.report(11, m_test3, 300L, statistics);

    verify(m_dataLogger).info(eq("33, 11, 3, 300, 99, 0"),
                              m_argumentCaptor.capture());

    statistics.reset();
    statistics.setValue(s_errorsIndex, 1);

    ThreadDataLogger.report(11, m_test3, 301L, statistics);

    verify(m_dataLogger).info(eq("33, 11, 3, 301, 0, 1"),
                              m_argumentCaptor.capture());
  }

  @Test public void testReportCustomViews() throws Exception {

    m_statisticsServices.getDetailStatisticsView().add(
      m_statisticsServices.getStatisticExpressionFactory()
      .createExpressionView("foo", "userDouble0", false));

    final StatisticsSet statistics =
      m_statisticsServices.getStatisticsSetFactory().create();

    statistics.addSample(s_timedTestsIndex, 5);
    statistics.addValue(s_userDouble0Index, 1.5);

    final ThreadDataLogger ThreadDataLogger2 =
      new ThreadDataLogger(
          m_dataLogger,
          m_statisticsServices.getDetailStatisticsView().getExpressionViews(),
          33);

    ThreadDataLogger2.report(11, m_test3, 530L, statistics);

    verify(m_dataLogger).info(eq("33, 11, 3, 530, 5, 0, 1.5"),
                              m_argumentCaptor.capture());
  }
}
