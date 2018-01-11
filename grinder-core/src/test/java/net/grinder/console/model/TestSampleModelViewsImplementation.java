// Copyright (C) 2008 - 2009 Philip Aston
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

package net.grinder.console.model;

import java.io.File;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.grinder.console.model.SampleModelViews.Listener;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsView;
import net.grinder.statistics.TestStatisticsQueries;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit tests for {@link SampleModelViewsImplementation}.
 *
 * @author Philip Aston
 */
public class TestSampleModelViewsImplementation extends AbstractFileTestCase {

  private final RandomStubFactory<SampleModel> m_modelStubFactory =
    RandomStubFactory.create(SampleModel.class);
  private final SampleModel m_model = m_modelStubFactory.getStub();

  private ConsoleProperties m_consoleProperties;

  protected void setUp() throws Exception {
    super.setUp();
    m_consoleProperties =
      new ConsoleProperties(null, new File(getDirectory(), "props"));
  }

  public void testConstruction() throws Exception {

    final StatisticsServices statisticsServices =
      StatisticsServicesImplementation.getInstance();

    final Set<ExpressionView> standardSummaryExpressionViews =
      new HashSet<ExpressionView>(Arrays.asList(
        statisticsServices.getSummaryStatisticsView().getExpressionViews()));

    final SampleModelViews sampleModelViews =
      new SampleModelViewsImplementation(
        m_consoleProperties,
        statisticsServices,
        m_model);

    m_modelStubFactory.assertSuccess("getPeakTPSExpression");

    final Set<ExpressionView> cumulativeViewSet =
      expressionViewsSet(sampleModelViews.getCumulativeStatisticsView());

    assertTrue(cumulativeViewSet.containsAll(standardSummaryExpressionViews));
    assertFalse(standardSummaryExpressionViews.containsAll(cumulativeViewSet));

    final Set<ExpressionView> intervalViewSet =
      expressionViewsSet(sampleModelViews.getIntervalStatisticsView());

    assertTrue(intervalViewSet.containsAll(standardSummaryExpressionViews));
    assertTrue(cumulativeViewSet.containsAll(intervalViewSet));
    assertFalse(intervalViewSet.containsAll(cumulativeViewSet));

    final ExpressionView expressionView =
      statisticsServices.getStatisticExpressionFactory().createExpressionView(
        "My view", "userLong0", false);

    assertFalse(cumulativeViewSet.contains(expressionView));

    sampleModelViews.registerStatisticExpression(expressionView);

    assertTrue(
      expressionViewsSet(sampleModelViews.getCumulativeStatisticsView())
      .contains(expressionView));

    assertTrue(
      expressionViewsSet(sampleModelViews.getIntervalStatisticsView())
      .contains(expressionView));

    sampleModelViews.resetStatisticsViews();

    assertFalse(
      expressionViewsSet(sampleModelViews.getCumulativeStatisticsView())
      .contains(expressionView));

    assertFalse(
      expressionViewsSet(sampleModelViews.getIntervalStatisticsView())
      .contains(expressionView));

    final TestStatisticsQueries statisticsQueries =
      sampleModelViews.getTestStatisticsQueries();
    assertNotNull(statisticsQueries);
    assertSame(statisticsQueries,
               statisticsServices.getTestStatisticsQueries());

    m_modelStubFactory.assertNoMoreCalls();
  }

  private HashSet<ExpressionView> expressionViewsSet(
    StatisticsView statisticsView) {

    return new HashSet<ExpressionView>(
        Arrays.asList(statisticsView.getExpressionViews()));
  }

  public void testListeners() throws Exception {
    final StatisticsServices statisticsServices =
      StatisticsServicesImplementation.getInstance();

    final SampleModelViews sampleModelViews =
      new SampleModelViewsImplementation(
        m_consoleProperties,
        statisticsServices,
        m_model);

    final RandomStubFactory<Listener> listenerStubFactory =
      RandomStubFactory.create(Listener.class);
    sampleModelViews.addListener(listenerStubFactory.getStub());

    sampleModelViews.resetStatisticsViews();
    listenerStubFactory.assertSuccess("resetStatisticsViews");

    final ExpressionView expressionView =
      statisticsServices.getStatisticExpressionFactory().createExpressionView(
        "My view", "userLong0", false);

    sampleModelViews.registerStatisticExpression(expressionView);
    listenerStubFactory.assertSuccess("newStatisticExpression", expressionView);

    listenerStubFactory.assertNoMoreCalls();
  }

  public void testNumberFormat() throws Exception {

    final SampleModelViews sampleModelViews =
      new SampleModelViewsImplementation(
        m_consoleProperties,
        StatisticsServicesImplementation.getInstance(),
        m_model);

    final NumberFormat numberFormat = sampleModelViews.getNumberFormat();

    assertNotNull(numberFormat);
    assertSame(numberFormat, sampleModelViews.getNumberFormat());

    assertEquals("1.23", numberFormat.format(1.234));

    m_consoleProperties.setSignificantFigures(4);

    final NumberFormat numberFormat4sf = sampleModelViews.getNumberFormat();
    assertNotSame(numberFormat, numberFormat4sf);
    assertEquals("1.234", numberFormat4sf.format(1.234));
  }
}
