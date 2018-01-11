// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2009 Philip Aston
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

import java.util.Comparator;

import net.grinder.statistics.StatisticsSet;


/**
 * Unit test case for <code>StatisticsView</code>.
 *
 * @author Philip Aston
 * @see StatisticsSet
 */
public class TestStatisticsView extends TestCase {

  private ExpressionView[] m_views;

  private int m_numberOfUniqueViews;

  protected void setUp() throws Exception {
    final StatisticExpressionFactory statisticExpressionFactory =
      StatisticsServicesImplementation.getInstance().getStatisticExpressionFactory();

    m_views = new ExpressionView[] {
      statisticExpressionFactory.createExpressionView("One", "(+ userLong0 userLong1)", false),
      statisticExpressionFactory.createExpressionView("Two", "userLong0", false),
      statisticExpressionFactory.createExpressionView("Three", "(+ userLong0 userLong1)", false),
      statisticExpressionFactory.createExpressionView("Four", "userLong1", false),
      statisticExpressionFactory.createExpressionView("One", "(+ userLong0 userLong1)", false),
    };

    m_numberOfUniqueViews = 4;
  }

  public void testGetExpressionViews() throws Exception {
    final StatisticsView statisticsView = new StatisticsView();

    assertEquals(0, statisticsView.getExpressionViews().length);

    for (int i = 0; i < m_views.length; ++i) {
      statisticsView.add(m_views[i]);
    }

    final ExpressionView[] expressionViews = statisticsView
        .getExpressionViews();

    assertEquals(m_numberOfUniqueViews, expressionViews.length);

    // Ordered in order of creation.
    assertEquals(m_views[0], expressionViews[0]);
    assertEquals(m_views[1], expressionViews[1]);
    assertEquals(m_views[2], expressionViews[2]);
    assertEquals(m_views[3], expressionViews[3]);

    // Again, adding in a different order.
    final StatisticsView statisticsView2 = new StatisticsView();

    for (int i = m_views.length - 1; i >= 0; --i) {
      statisticsView2.add(m_views[(2 + i) % m_views.length]);
    }

    final ExpressionView[] expressionViews2 = statisticsView
        .getExpressionViews();

    assertEquals(m_numberOfUniqueViews, expressionViews2.length);

    // Ordered in order of creation.
    assertEquals(m_views[0], expressionViews2[0]);
    assertEquals(m_views[1], expressionViews2[1]);
    assertEquals(m_views[2], expressionViews2[2]);
    assertEquals(m_views[3], expressionViews2[3]);
  }

  public void testAddStatisticsView() throws Exception {
    final StatisticsView statisticsView = new StatisticsView();

    statisticsView.add(statisticsView);
    assertEquals(0, statisticsView.getExpressionViews().length);

    final StatisticsView statisticsView2 = new StatisticsView();

    statisticsView.add(statisticsView);
    assertEquals(0, statisticsView.getExpressionViews().length);

    for (int i = 0; i < m_views.length; ++i) {
      statisticsView.add(m_views[i]);
    }

    statisticsView2.add(statisticsView);
    assertEquals(m_numberOfUniqueViews,
      statisticsView2.getExpressionViews().length);

    statisticsView2.add(statisticsView);
    assertEquals(m_numberOfUniqueViews,
      statisticsView2.getExpressionViews().length);
  }

  public void testComparator() throws Exception {
    // Test the parts of the Comparator that the normal use cases don't reach.
    final Comparator<ExpressionView> comparator =
      new StatisticsView.CreationOrderComparator();

    assertEquals(0, comparator.compare(m_views[0], m_views[0]));
  }
}
