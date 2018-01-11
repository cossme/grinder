// Copyright (C) 2008 - 2013 Philip Aston
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

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
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.translation.Translations;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;


/**
 * Unit tests for {@link SampleModelViewsImplementation}.
 *
 * @author Philip Aston
 */
public class TestSampleModelViewsImplementation
  extends AbstractJUnit4FileTestCase {

  @Mock private SampleModel m_model;
  @Mock private Translations m_translations;

  private ConsoleProperties m_consoleProperties;

  @Before public void setUp() throws Exception {
    initMocks(this);

    m_consoleProperties =
      new ConsoleProperties(m_translations, new File(getDirectory(), "props"));
  }

  @Test public void testConstruction() throws Exception {

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

    verify(m_model).getPeakTPSExpression();

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

    verifyNoMoreInteractions(m_model);
  }

  private HashSet<ExpressionView> expressionViewsSet(
    final StatisticsView statisticsView) {

    return new HashSet<ExpressionView>(
        Arrays.asList(statisticsView.getExpressionViews()));
  }

  @Test public void testListeners() throws Exception {
    final StatisticsServices statisticsServices =
      StatisticsServicesImplementation.getInstance();

    final SampleModelViews sampleModelViews =
      new SampleModelViewsImplementation(
        m_consoleProperties,
        statisticsServices,
        m_model);

    final Listener listener = mock(Listener.class);
    sampleModelViews.addListener(listener);

    sampleModelViews.resetStatisticsViews();
    verify(listener).resetStatisticsViews();

    final ExpressionView expressionView =
      statisticsServices.getStatisticExpressionFactory().createExpressionView(
        "My view", "userLong0", false);

    sampleModelViews.registerStatisticExpression(expressionView);
    verify(listener).newStatisticExpression(expressionView);

    verifyNoMoreInteractions(listener);
  }

  @Test public void testNumberFormat() throws Exception {

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
