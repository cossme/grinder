// Copyright (C) 2000 - 2012 Philip Aston
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import net.grinder.statistics.StatisticExpressionFactoryImplementation.ParseContext.ParseException;

import org.junit.Before;
import org.junit.Test;


/**
 * Unit tests for {@link StatisticExpressionFactoryImplementation}.
 *
 * @author Philip Aston
 * @see StatisticsSet
 */
public class TestStatisticExpressionFactory {

  private StatisticExpressionFactory m_factory;
  private StatisticsIndexMap m_indexMap;
  private StatisticsSet m_statistics;

  @Before public void setUp() throws Exception {
    final StatisticsServices statisticsServices =
      StatisticsServicesImplementation.getInstance();
    m_factory = statisticsServices.getStatisticExpressionFactory();
    m_indexMap = statisticsServices.getStatisticsIndexMap();
    m_statistics = new StatisticsSetImplementation(m_indexMap);

    m_statistics.addValue(m_indexMap.getLongIndex("userLong0"), 1);
    m_statistics.addValue(m_indexMap.getLongIndex("userLong1"), 2);
  }

  @Test public void testConstant() throws Exception {
    final StatisticExpression longExpression = m_factory.createConstant(-22);

    myAssertEquals(-22, longExpression);
    assertTrue(!longExpression.isDouble());

    final StatisticExpression doubleExpression = m_factory.createConstant(2.3);

    myAssertEquals(2.3d, doubleExpression);
    assertTrue(doubleExpression.isDouble());

    myAssertEquals(0, m_factory.createExpression("0"));
    myAssertEquals(99d, m_factory.createExpression("99f"));

    try {
      m_factory.createExpression("1 2");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }
  }

  @Test public void testPrimitive() throws Exception {
    final StatisticExpression expression =
      m_factory.createPrimitive(m_indexMap.getLongIndex("userLong0"));

    myAssertEquals(1, expression);
    assertTrue(!expression.isDouble());

    final StatisticsIndexMap.DoubleIndex anotherIndex = m_indexMap
        .getDoubleIndex("userDouble4");
    assertNotNull(anotherIndex);

    final StatisticExpression doubleExpresson = m_factory
        .createExpression("  userDouble4");

    myAssertEquals(0d, doubleExpresson);
    assertTrue(doubleExpresson.isDouble());

    myAssertEquals(2, m_factory.createExpression("userLong1"));

    try {
      m_factory.createExpression("");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }

    try {
      m_factory.createExpression("userLong0 userLong1");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }

    try {
      m_factory.createExpression("(timedTests)");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }

    try {
      m_factory.createExpression("Madeup");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }
  }

  @Test public void testSum() throws Exception {
    final StatisticExpression[] expressions = {
        m_factory.createExpression("userLong0"),
        m_factory.createExpression("userLong1"),
        m_factory.createExpression("userLong1"), };

    final StatisticExpression expression = m_factory.createSum(expressions);

    myAssertEquals(5, expression);
    assertTrue(!expression.isDouble());

    myAssertEquals(2, m_factory.createExpression("(+ userLong0 userLong0)"));

    myAssertEquals(4, m_factory
        .createExpression("(+ userLong0 userLong1 userLong0)"));

    myAssertEquals(5, m_factory
        .createExpression("(+ userLong0 (+ userLong0 userLong1) userLong0)"));

    myAssertEquals(0, m_factory.createExpression("(+)"));

    myAssertEquals(1, m_factory.createExpression("(+ userLong0)"));

    try {
      m_factory.createExpression("(+ userLong0 timedTests)");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }
  }

  @Test public void testNegation() throws Exception {
    myAssertEquals(-2,
      m_factory.createNegation(m_factory.createExpression("userLong1")));

    myAssertEquals(0.0,
                   m_factory.createNegation(m_factory.createConstant(0.0)));

    myAssertEquals(100,
                   m_factory.createNegation(m_factory.createConstant(-100)));

    myAssertEquals(-1, m_factory.createExpression("(- 1)"));

    try {
      m_factory.createExpression("(-)");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }

    try {
      m_factory.createExpression("(-1)");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }
  }

  @Test public void testMinus() throws Exception {
    final StatisticExpression[] expressions = {
        m_factory.createExpression("userLong0"),
        m_factory.createExpression("userLong1"),
        m_factory.createExpression("userLong1"), };

    final StatisticExpression expression =
      m_factory.createMinus(m_factory.createConstant(10), expressions);

    myAssertEquals(5, expression);
    assertTrue(!expression.isDouble());

    myAssertEquals(1, m_factory.createExpression("(- userLong1 userLong0)"));

    myAssertEquals(-3, m_factory
        .createExpression("(- userLong0 userLong1 userLong1)"));

    myAssertEquals(0, m_factory
        .createExpression("(- userLong0 (- userLong0 userLong0) userLong0)"));


    myAssertEquals(-1.0, m_factory
        .createExpression("(- userLong0 (- userLong0 userDouble0) userLong0)"));

    try {
      m_factory.createExpression("(- userLong0 timedTests)");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }
  }

  @Test public void testProduct() throws Exception {
    final StatisticExpression[] expressions = {
        m_factory.createExpression("userLong0"),
        m_factory.createExpression("userLong1"),
        m_factory.createExpression("userLong1"), };

    final StatisticExpression expression = m_factory.createProduct(expressions);

    myAssertEquals(4, expression);
    assertTrue(!expression.isDouble());

    myAssertEquals(1, m_factory.createExpression("(* userLong0 userLong0)"));

    myAssertEquals(4, m_factory
        .createExpression("(* userLong0 userLong1 userLong1)"));

    myAssertEquals(8, m_factory
        .createExpression("(* userLong1 (* userLong1 userLong1) userLong0)"));

    myAssertEquals(1, m_factory.createExpression("(*)"));

    myAssertEquals(2, m_factory.createExpression("(* userLong1)"));

    try {
      m_factory.createExpression("(* timedTests timedTests)");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }
  }

  @Test public void testDivision() throws Exception {
    final StatisticExpression expression =
      m_factory.createDivision(m_factory.createExpression("userLong1"),
                               m_factory.createExpression("userLong1"));

    myAssertEquals(1, expression);
    assertTrue(expression.isDouble());

    myAssertEquals(1d, m_factory.createExpression("(/ userLong0 userLong0)"));

    myAssertEquals(0.5d, m_factory.createExpression("(/ userLong0 userLong1)"));

    myAssertEquals(2d, m_factory.createExpression("(/ userLong1 userLong0)"));

    try {
      m_factory.createExpression("(/)");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }

    try {
      m_factory.createExpression("(/ userLong0)");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }

    try {
      m_factory.createExpression("(/ userLong0 userLong0 userLong0)");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }

    try {
      m_factory.createExpression("(/ timedTests userLong0)");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }
  }

  @Test public void testDivisionZeroNumerator() throws Exception {
    myAssertEquals(Double.NaN,
                   m_factory.createExpression("(/ userLong2 userLong2)"));
  }

  @Test public void testSquareRoot() throws Exception {
    final StatisticExpression expression =
      m_factory.createSquareRoot(m_factory.createExpression("userDouble0"));

    myAssertEquals(0, expression);
    assertTrue(expression.isDouble());

    m_statistics.addValue(m_indexMap.getDoubleIndex("userDouble0"), 4);
    myAssertEquals(2, expression);

    myAssertEquals(1d, m_factory.createExpression("(sqrt 1)"));
    assertTrue(
        Double.isNaN(m_factory.createExpression("(sqrt -1)")
                     .getDoubleValue(m_statistics)));

    try {
      m_factory.createExpression("(sqrt)");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }

    try {
      m_factory.createExpression("(sqtr userDouble0 userDouble0)");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }

    try {
      m_factory.createExpression("(sqrt timedTests)");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }
  }

  @Test public void testLongPeak() throws Exception {
    final StatisticsIndexMap.LongIndex peakIndex1 = m_indexMap
        .getLongIndex("userLong2");

    final StatisticsIndexMap.LongIndex peakIndex2 = m_indexMap
        .getLongIndex("userLong3");

    final StatisticExpression expression = m_factory.createPeak(peakIndex1,
        m_factory.createExpression("userLong1"));

    myAssertEquals(0, expression);
    assertTrue(!expression.isDouble());

    final StatisticsIndexMap.LongIndex statIndex = m_indexMap
        .getLongIndex("userLong4");

    final PeakStatisticExpression peak = m_factory.createPeak(peakIndex2,
        m_factory.createExpression("userLong4"));

    final StatisticsSet statistics =
      new StatisticsSetImplementation(m_indexMap);

    statistics.setValue(statIndex, 2);
    myAssertEquals(0, peak, statistics);
    peak.update(statistics, statistics);
    myAssertEquals(2, peak, statistics);

    statistics.setValue(statIndex, 33);
    peak.update(statistics, statistics);
    myAssertEquals(33, peak, statistics);

    statistics.setValue(statIndex, 2);
    peak.update(statistics, statistics);
    myAssertEquals(33, peak, statistics);
  }

  @Test public void testDoublePeak() throws Exception {
    final StatisticsIndexMap.DoubleIndex peakIndex1 = m_indexMap
        .getDoubleIndex("userDouble2");

    final StatisticsIndexMap.DoubleIndex peakIndex2 = m_indexMap
        .getDoubleIndex("userDouble3");

    final StatisticExpression expression = m_factory.createPeak(peakIndex1,
        m_factory.createExpression("(/ userLong1 userLong0)"));

    myAssertEquals(0, expression);
    assertTrue(expression.isDouble());

    final StatisticsIndexMap.DoubleIndex statIndex = m_indexMap
        .getDoubleIndex("userDouble4");

    final PeakStatisticExpression peak = m_factory.createPeak(peakIndex2,
        m_factory.createExpression("userDouble4"));

    final StatisticsSet statistics =
      new StatisticsSetImplementation(m_indexMap);

    statistics.setValue(statIndex, 0.5);
    myAssertEquals(0d, peak, statistics);
    peak.update(statistics, statistics);
    myAssertEquals(0.5d, peak, statistics);

    statistics.setValue(statIndex, 33d);
    peak.update(statistics, statistics);
    myAssertEquals(33d, peak, statistics);

    statistics.setValue(statIndex, -2d);
    peak.update(statistics, statistics);
    myAssertEquals(33d, peak, statistics);
  }

  @Test public void testLongSample() throws Exception {
    myAssertEquals(0, m_factory.createExpression("(count timedTests)"));
    myAssertEquals(0, m_factory.createExpression("(sum timedTests)"));
    myAssertEquals(0, m_factory.createExpression("(variance timedTests)"));

    m_statistics.addSample(m_indexMap.getLongSampleIndex("timedTests"), 2);
    m_statistics.addSample(m_indexMap.getLongSampleIndex("timedTests"), -1);

    myAssertEquals(2, m_factory.createExpression("(count timedTests)"));
    myAssertEquals(1, m_factory.createExpression("(sum timedTests)"));
    myAssertEquals(2.25, m_factory.createExpression("(variance timedTests)"));

    try {
      m_factory.createExpression("(sum userLong0)");
      fail("Expected ParseException");
    }
    catch (ParseException e) {
    }

    try {
      m_factory.createExpression("(count userLong0)");
      fail("Expected ParseException");
    }
    catch (ParseException e) {
    }
    try {
      m_factory.createExpression("(variance userLong0)");
      fail("Expected ParseException");
    }
    catch (ParseException e) {
    }
  }

  @Test public void testDoubleSample() throws Exception {
    try {
      final StatisticsIndexMap.DoubleIndex sumIndex =
        m_indexMap.getDoubleIndex("userDouble0");
      final StatisticsIndexMap.LongIndex countIndex =
        m_indexMap.getLongIndex("userLong3");
      final StatisticsIndexMap.DoubleIndex varianceIndex =
        m_indexMap.getDoubleIndex("userDouble1");

      final StatisticsIndexMap.DoubleSampleIndex doubleSampleIndex =
        m_indexMap.createDoubleSampleIndex("testDoubleSampleStatistic",
                                           sumIndex,
                                           countIndex,
                                           varianceIndex);
      assertNotNull(doubleSampleIndex);

      myAssertEquals(0, m_factory.createExpression(
                          "(count testDoubleSampleStatistic)"));
      myAssertEquals(0, m_factory.createExpression(
                          "(sum testDoubleSampleStatistic)"));
      myAssertEquals(0, m_factory.createExpression(
                          "(variance testDoubleSampleStatistic)"));

      final StatisticsIndexMap.DoubleSampleIndex index =
        m_indexMap.getDoubleSampleIndex("testDoubleSampleStatistic");
      m_statistics.addSample(index, 2);
      m_statistics.addSample(index, -1);

      myAssertEquals(2, m_factory.createExpression(
                          "(count testDoubleSampleStatistic)"));
      myAssertEquals(1, m_factory.createExpression(
                          "(sum testDoubleSampleStatistic)"));
      myAssertEquals(2.25, m_factory.createExpression(
                             "(variance testDoubleSampleStatistic)"));

      try {
        m_factory.createExpression("(sum userDouble0)");
        fail("Expected ParseException");
      }
      catch (ParseException e) {
      }

      try {
        m_factory.createExpression("(count userDouble0)");
        fail("Expected ParseException");
      }
      catch (ParseException e) {
      }

      try {
        m_factory.createExpression("(variance userDouble0)");
        fail("Expected ParseException");
      }
      catch (ParseException e) {
      }
    }
    finally {
      m_indexMap.removeDoubleSampleIndex("testDoubleSampleStatistic");
    }
  }

  @Test public void testParseCompoundExpessions() throws Exception {
    myAssertEquals(0.5, m_factory
        .createExpression("(/ userLong0 (+ userLong0 userLong0))"));

    myAssertEquals(
        2.25d,
        m_factory.createExpression(
            "(+ userLong0 (/ userLong0 (+ userLong1 userLong1)) userLong0)"));

    myAssertEquals(
        2.25d,
        m_factory.createExpression(
            "(+ userLong0 (/ userLong0 (* userLong1 userLong1)) userLong0)"));

    myAssertEquals(
        9d,
        m_factory.createExpression(
        "(* 4 (+ userLong0 (/ userLong0 (* userLong1 userLong1)) userLong0))"));
  }

  @Test public void testParseInvalidExpessions() throws Exception {
    try {
      m_factory.createExpression("(+");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }

    try {
      m_factory.createExpression("+)");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }

    try {
      m_factory.createExpression("(/ 1 2");
      fail("Expected a ParseException");
    }
    catch (ParseException e) {
    }
  }

  @Test public void testNormaliseExpressionString() throws Exception {
    assertEquals("userLong0",
                m_factory.normaliseExpressionString(" userLong0 "));

    assertEquals("(+ userLong0 userLong1 (* userLong0 userLong1))",
                 m_factory.normaliseExpressionString(
                   "\t(+ userLong0 userLong1( \n  * userLong0 userLong1) )"));

    try {
      m_factory.normaliseExpressionString("userLong0 userLong0");
      fail("Expected ParseException");
    }
    catch (ParseException e) {
    }
  }

  private void myAssertEquals(long expected, StatisticExpression expression) {
    myAssertEquals(expected, expression, m_statistics);
  }

  private void myAssertEquals(long expected,
                              StatisticExpression expression,
                              StatisticsSet statistics) {
    assertEquals(expected, expression.getLongValue(statistics));
    myAssertEquals((double) expected, expression, statistics);
  }

  private void myAssertEquals(double expected, StatisticExpression expression) {
    myAssertEquals(expected, expression, m_statistics);
  }

  private void myAssertEquals(double expected,
                              StatisticExpression expression,
                              StatisticsSet statistics) {
    assertEquals(expected, expression.getDoubleValue(statistics), 0.00001d);
  }
}
