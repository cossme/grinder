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

import net.grinder.common.StubTest;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.TestStatisticsMap;
import junit.framework.TestCase;


/**
 * Unit test case for <code>TestStatisticsHelperImplementation</code>.
 *
 * @author Philip Aston
 */
public class TestTestStatisticsHelperImplementation extends TestCase {

  private final StatisticsServices m_statisticsServices =
    StatisticsServicesImplementation.getInstance();

  private final StatisticsIndexMap m_indexMap =
    m_statisticsServices.getStatisticsIndexMap();

  private final StatisticsIndexMap.LongIndex m_errorsIndex =
    m_indexMap.getLongIndex("errors");

  private final StatisticsIndexMap.LongSampleIndex m_timedTestsIndex =
    m_indexMap.getLongSampleIndex("timedTests");

  private final StatisticsIndexMap.LongIndex m_untimedTestsIndex =
    m_indexMap.getLongIndex("untimedTests");


  public void testSuccessMethods() throws Exception {
    final TestStatisticsHelper helper =
      new TestStatisticsHelperImplementation(m_indexMap);

    assertSame(m_indexMap, helper.getStatisticsIndexMap());

    final StatisticsSet statistics =
      m_statisticsServices.getStatisticsSetFactory().create();

    assertTrue(helper.getSuccess(statistics));

    statistics.setValue(m_errorsIndex, 1);
    assertFalse(helper.getSuccess(statistics));

    statistics.setValue(m_errorsIndex, 2);
    assertFalse(helper.getSuccess(statistics));

    statistics.setValue(m_errorsIndex, 0);
    assertTrue(helper.getSuccess(statistics));

    helper.setSuccess(statistics, false);
    assertFalse(helper.getSuccess(statistics));

    helper.setSuccess(statistics, true);
    assertTrue(helper.getSuccess(statistics));

    assertEquals(0, statistics.getValue(m_errorsIndex));
  }

  public void testRecordTest() throws Exception {

    final StatisticsSet statistics =
      m_statisticsServices.getStatisticsSetFactory().create();

    final TestStatisticsHelper helper =
      new TestStatisticsHelperImplementation(m_indexMap);

    helper.recordTest(statistics, 1234);
    assertEquals(0, statistics.getValue(m_errorsIndex));
    assertEquals(1234, statistics.getSum(m_timedTestsIndex));

    statistics.setValue(m_untimedTestsIndex, 1);
    helper.recordTest(statistics, 999);
    assertEquals(0, statistics.getValue(m_errorsIndex));
    assertEquals(999, statistics.getSum(m_timedTestsIndex));
    assertEquals(0, statistics.getValue(m_untimedTestsIndex));

    statistics.setValue(m_errorsIndex, 2);
    helper.recordTest(statistics, 1234);
    assertEquals(1, statistics.getValue(m_errorsIndex));
    assertEquals(1234, statistics.getSum(m_timedTestsIndex));
    assertEquals(0, statistics.getValue(m_untimedTestsIndex));
  }

  public void testRemoveTestTimeFromSample() throws Exception {

    final TestStatisticsHelper helper =
      new TestStatisticsHelperImplementation(m_indexMap);

    final StatisticsSet statistics1 =
      m_statisticsServices.getStatisticsSetFactory().create();
    statistics1.addSample(m_timedTestsIndex, 21321);
    statistics1.addSample(m_timedTestsIndex, 1231);

    assertEquals(21321 + 1231, helper.getTestTime(statistics1));

    final StatisticsSet statistics2 =
      m_statisticsServices.getStatisticsSetFactory().create();
    statistics2.setValue(m_untimedTestsIndex, 1);
    statistics2.addSample(m_timedTestsIndex, 18782);
    statistics2.setValue(m_errorsIndex, 1);

    assertEquals(18782, helper.getTestTime(statistics2));

    final TestStatisticsMap sample = new TestStatisticsMap();
    sample.put(new StubTest(1, ""), statistics1);
    sample.put(new StubTest(2, ""), statistics2);

    helper.removeTestTimeFromSample(sample);

    assertEquals(0, statistics1.getValue(m_errorsIndex));
    assertEquals(2, statistics1.getValue(m_untimedTestsIndex));
    assertEquals(0, statistics1.getCount(m_timedTestsIndex));
    assertEquals(0, statistics1.getSum(m_timedTestsIndex));
    assertEquals(0, helper.getTestTime(statistics1));

    assertEquals(1, statistics2.getValue(m_errorsIndex));
    assertEquals(2, statistics2.getValue(m_untimedTestsIndex));
    assertEquals(0, statistics2.getCount(m_timedTestsIndex));
    assertEquals(0, statistics2.getSum(m_timedTestsIndex));
    assertEquals(0, helper.getTestTime(statistics2));
  }

  public void testIncrementErrors() throws Exception {

    final StatisticsSet statistics =
      m_statisticsServices.getStatisticsSetFactory().create();

    final TestStatisticsHelper helper =
      new TestStatisticsHelperImplementation(m_indexMap);

    helper.incrementErrors(statistics);
    helper.incrementErrors(statistics);
    helper.incrementErrors(statistics);
    assertEquals(3, statistics.getValue(m_errorsIndex));
  }
}
