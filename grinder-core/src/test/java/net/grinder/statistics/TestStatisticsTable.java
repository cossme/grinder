// Copyright (C) 2000 - 2012 Philip Aston
// Copyright (C) 2005 Martin Wagner.
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import net.grinder.common.StubTest;
import net.grinder.testutility.AssertUtilities;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Unit test case for {@link StatisticsTable}.
 *
 * @author Philip Aston
 * @see StatisticsSet
 */
public class TestStatisticsTable  {

  private TestStatisticsMap m_testStatisticsMap;
  private StatisticsIndexMap m_indexMap;

  private StatisticsView m_statisticsView;

  private final StatisticsServices m_statisticsServices =
    StatisticsServicesImplementation.getInstance();

  private final Locale m_originalDefaultLocale = Locale.getDefault();

  @Before public void setUp() throws Exception {
    Locale.setDefault(Locale.US);

    m_testStatisticsMap =
      new TestStatisticsMap(m_statisticsServices.getStatisticsSetFactory());

    m_indexMap = m_statisticsServices.getStatisticsIndexMap();

    final StatisticsIndexMap.LongIndex aIndex =
      m_indexMap.getLongIndex("userLong0");
    final StatisticsIndexMap.LongIndex bIndex =
      m_indexMap.getLongIndex("userLong1");

    final StatisticExpressionFactory statisticExpressionFactory =
      StatisticsServicesImplementation.getInstance()
      .getStatisticExpressionFactory();

    final ExpressionView[] expressionViews = {
        statisticExpressionFactory.createExpressionView("A", "userLong0", true),
        statisticExpressionFactory.createExpressionView("B", "userLong1", false),
        statisticExpressionFactory.createExpressionView("A plus B", "(+ userLong0 userLong1)", false),
        statisticExpressionFactory.createExpressionView("A divided by B", "(/ userLong0 userLong1)", false),
    };

    m_statisticsView = new StatisticsView();

    for (final ExpressionView expressionView : expressionViews) {
      m_statisticsView.add(expressionView);
    }

    final net.grinder.common.Test[] tests = {
      new StubTest(9, "Test 9"),
      new StubTest(3, null),
      new StubTest(113, "Another test"),
      new StubTest(12345678, "A test with a long name"),
    };

    final StatisticsSet[] statistics = new StatisticsSet[tests.length];

    final StatisticsSetFactory factory =
      m_statisticsServices.getStatisticsSetFactory();

    for (int i = 0; i < tests.length; ++i) {
      statistics[i] =
        new StatisticsSetImplementation(
          m_statisticsServices.getStatisticsIndexMap());
      if (i != 2) {
        statistics[i].addValue(aIndex, i);
        statistics[i].addValue(bIndex, i + 1);
      }

      final StatisticsSet statistics2 = factory.create();
      statistics2.add(statistics[i]);

      m_testStatisticsMap.put(tests[i], statistics2);
    }
  }

  @After public void tearDown() throws Exception {
    Locale.setDefault(m_originalDefaultLocale);
  }

  @Test public void testStatisticsTable() throws Exception {
    final StringWriter expected = new StringWriter();
    final PrintWriter in = new PrintWriter(expected);

    in.println("             A            B            A plus B     A divided by ");
    in.println("                                                    B            ");
    in.println();
    in.println("Test 3       1            2            3            0.50         ");
    in.println("Test 9       0            1            1            0.00          \"Test 9\"");
    in.println("Test 113     0            0            0            -             \"Another test\"");
    in.println("Test 12345678 3            4            7            0.75          \"A test with a long name\"");
    in.println();
    in.println("Totals       4            7            11           0.57         ");
    in.close();

    final StatisticsTable table =
      new StatisticsTable(m_statisticsView, m_indexMap, m_testStatisticsMap);

    final StringWriter output = new StringWriter();
    final PrintWriter out = new PrintWriter(output);
    table.print(out, 1234);
    out.close();

    AssertUtilities.assertContains(
      output.getBuffer().toString(),
      expected.getBuffer().toString());
  }

  @Test public void testStatisticsTableWithCompositeTests() throws Exception {
    final StatisticsSet statistics =
      m_statisticsServices.getStatisticsSetFactory().create();
    statistics.setValue(
      m_statisticsServices.getStatisticsIndexMap().getLongIndex("userLong1"), 1);
    statistics.setIsComposite();
    m_testStatisticsMap.put(new StubTest(4, "T4"), statistics);

    final StringWriter expected = new StringWriter();
    final PrintWriter in = new PrintWriter(expected);

    in.println("             A            B            A plus B     A divided by ");
    in.println("                                                    B            ");
    in.println();
    in.println("Test 3       1            2            3            0.50         ");
    in.println("(Test 4      0            1            1            0.00)         \"T4\"");
    in.println("Test 9       0            1            1            0.00          \"Test 9\"");
    in.println("Test 113     0            0            0            -             \"Another test\"");
    in.println("Test 12345678 3            4            7            0.75          \"A test with a long name\"");
    in.println();
    in.println("Totals       4            7            11           0.57         ");
    in.println("             (0)                                                 ");
    in.close();

    final StatisticsTable table =
      new StatisticsTable(m_statisticsView, m_indexMap, m_testStatisticsMap);

    final StringWriter output = new StringWriter();
    final PrintWriter out = new PrintWriter(output);
    table.print(out, 1234);
    out.close();

    AssertUtilities.assertContains(
      output.getBuffer().toString(),
      expected.getBuffer().toString());
  }
}
