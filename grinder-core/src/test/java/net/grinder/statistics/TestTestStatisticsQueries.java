// Copyright (C) 2000 Paco Gomez
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

import junit.framework.TestCase;


/**
 * Unit test case for <code>TestStatisticsImplementation</code>.
 *
 * @author Philip Aston
 * @see StatisticsSet
 */
public class TestTestStatisticsQueries extends TestCase {

  private final StatisticsServices m_statisticsServices =
    StatisticsServicesImplementation.getInstance();

  public void testSingleton() throws Exception {
    assertSame(m_statisticsServices.getTestStatisticsQueries(),
      m_statisticsServices.getTestStatisticsQueries());
  }

  public void testTestStatisticsQueries() throws Exception {
    final StatisticsIndexMap statisticsIndexMap =
      m_statisticsServices.getStatisticsIndexMap();
    final StatisticsIndexMap.LongIndex errorStatisticIndex =
      statisticsIndexMap.getLongIndex("errors");
    final StatisticsIndexMap.LongIndex untimedTestsIndex =
      statisticsIndexMap.getLongIndex("untimedTests");
    final StatisticsIndexMap.LongSampleIndex timedTestsIndex =
      statisticsIndexMap.getLongSampleIndex("timedTests");

    final StatisticsSet statistics0 =
      new StatisticsSetImplementation(
        m_statisticsServices.getStatisticsIndexMap());

    final TestStatisticsQueries queries =
      m_statisticsServices.getTestStatisticsQueries();

    assertEquals(0, queries.getNumberOfErrors(statistics0));
    assertEquals(0, queries.getNumberOfTests(statistics0));
    assertTrue(Double.isNaN(queries.getAverageTestTime(statistics0)));

    final StatisticsSet statistics1 =
      new StatisticsSetImplementation(
        m_statisticsServices.getStatisticsIndexMap());

    assertTrue(statistics0 != statistics1);
    assertEquals(statistics0, statistics1);

    statistics0.addValue(errorStatisticIndex, 1);
    assertEquals(1, queries.getNumberOfErrors(statistics0));
    assertTrue(!statistics0.equals(statistics1));

    statistics1.addValue(errorStatisticIndex, 1);
    assertEquals(statistics0, statistics1);

    statistics0.addValue(untimedTestsIndex, 1);
    assertEquals(1, queries.getNumberOfTests(statistics0));
    assertTrue(!statistics0.equals(statistics1));

    statistics1.addValue(untimedTestsIndex, 1);
    assertEquals(statistics0, statistics1);

    statistics0.addSample(timedTestsIndex, 5);
    statistics1.addSample(timedTestsIndex, 10);
    assertEquals(2, queries.getNumberOfTests(statistics0));
    assertTrue(!statistics0.equals(statistics1));

    statistics0.addSample(timedTestsIndex, 10);
    statistics1.addSample(timedTestsIndex, 5);
    assertEquals(statistics0, statistics1);
    assertEquals(7.5d, queries.getAverageTestTime(statistics1), 0.01);
  }
}
