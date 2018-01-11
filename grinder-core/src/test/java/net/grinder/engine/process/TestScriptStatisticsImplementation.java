// Copyright (C) 2006 - 2011 Philip Aston
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import net.grinder.communication.Sender;
import net.grinder.messages.console.RegisterExpressionViewMessage;
import net.grinder.script.InvalidContextException;
import net.grinder.script.Statistics;
import net.grinder.script.Statistics.StatisticsForTest;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesTestFactory;
import net.grinder.statistics.StatisticsView;
import net.grinder.testutility.AssertUtilities;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit test case for {@link ScriptStatisticsImplementation}.
 *
 * @author Philip Aston
 */
public class TestScriptStatisticsImplementation {

  @Mock private ThreadContext m_threadContext;
  @Mock private Sender m_sender;

  @Captor ArgumentCaptor<RegisterExpressionViewMessage> m_messageCaptor;

  private final StubThreadContextLocator m_threadContextLocator =
    new StubThreadContextLocator();

  private final StatisticsServices m_statisticsServices =
    StatisticsServicesTestFactory.createTestInstance();

  @Before public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test public void testContextChecks() throws Exception {

    final ScriptStatisticsImplementation scriptStatistics =
      new ScriptStatisticsImplementation(
        m_threadContextLocator,
        m_statisticsServices,
        m_sender);

    // 1. Null thread context.
    assertFalse(scriptStatistics.isTestInProgress());

    try {
      scriptStatistics.getForCurrentTest();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      AssertUtilities.assertContains(e.getMessage(), "worker threads");
    }

    try {
      scriptStatistics.getForLastTest();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      AssertUtilities.assertContains(e.getMessage(), "worker threads");
    }

    try {
      scriptStatistics.report();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      AssertUtilities.assertContains(e.getMessage(), "worker threads");
    }

    try {
      scriptStatistics.setDelayReports(false);
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      AssertUtilities.assertContains(e.getMessage(), "worker threads");
    }

    // 2. No last statistics, no current statistics.
    m_threadContextLocator.set(m_threadContext);
    when(m_threadContext.getStatisticsForCurrentTest()).thenReturn(null);
    when(m_threadContext.getStatisticsForLastTest()).thenReturn(null);
    assertFalse(scriptStatistics.isTestInProgress());

    scriptStatistics.setDelayReports(false);

    try {
      scriptStatistics.getForCurrentTest();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      AssertUtilities.assertContains(e.getMessage(), "no test");
    }

    try {
      scriptStatistics.getForLastTest();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      AssertUtilities.assertContains(e.getMessage(), "No tests");
    }

    // 3. No last statistics, current statistics.
    final StatisticsForTest statisticsForTest1 = mock(StatisticsForTest.class);

    when(m_threadContext.getStatisticsForCurrentTest())
      .thenReturn(statisticsForTest1);

    assertTrue(scriptStatistics.isTestInProgress());
    assertSame(statisticsForTest1, scriptStatistics.getForCurrentTest());

    try {
      scriptStatistics.getForLastTest();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      AssertUtilities.assertContains(e.getMessage(), "No tests");
    }

    // 4. Last statistics, current statistics.
    final StatisticsForTest statisticsForTest2 = mock(StatisticsForTest.class);

    when(m_threadContext.getStatisticsForLastTest())
      .thenReturn(statisticsForTest2);

    assertTrue(scriptStatistics.isTestInProgress());
    assertSame(statisticsForTest1, scriptStatistics.getForCurrentTest());
    assertSame(statisticsForTest2, scriptStatistics.getForLastTest());

    verifyNoMoreInteractions(m_sender);
  }

  @Test public void testRegisterStatisticsViews() throws Exception {

    final StubThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();
    threadContextLocator.set(m_threadContext);

    final Statistics scriptStatistics =
      new ScriptStatisticsImplementation(
        threadContextLocator,
        m_statisticsServices,
        m_sender);

    final ExpressionView expressionView =
      m_statisticsServices.getStatisticExpressionFactory()
      .createExpressionView("display", "errors", false);
    scriptStatistics.registerSummaryExpression("display", "errors");

    verify(m_sender).send(m_messageCaptor.capture());

    final RegisterExpressionViewMessage message =
      m_messageCaptor.getValue();
    assertEquals("display", message.getExpressionView().getDisplayName());
    assertEquals("errors", message.getExpressionView().getExpressionString());

    verifyNoMoreInteractions(m_sender);

    final StatisticsView summaryStatisticsView =
      m_statisticsServices.getSummaryStatisticsView();

    final ExpressionView[] summaryExpressionViews =
      summaryStatisticsView.getExpressionViews();
    assertTrue(Arrays.asList(summaryExpressionViews).contains(expressionView));

    try {
      scriptStatistics.registerDataLogExpression("display2", "untimedTests");
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
    }

    threadContextLocator.set(null);

    scriptStatistics.registerDataLogExpression("display2", "untimedTests");

    final StatisticsView detailStatisticsView =
      m_statisticsServices.getDetailStatisticsView();

    final ExpressionView[] detailExpressionViews =
      detailStatisticsView.getExpressionViews();
    assertTrue(Arrays.asList(detailExpressionViews).contains(
      m_statisticsServices.getStatisticExpressionFactory()
      .createExpressionView("display2", "untimedTests", false)));
  }
}
