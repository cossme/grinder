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


/**
 * Provides references to commonly used {@link StatisticsView}s.
 *
 * @author Philip Aston
 */
final class CommonStatisticsViews {
  private final StatisticsView m_detailStatisticsView = new StatisticsView();

  private final StatisticsView m_summaryStatisticsView =
    new StatisticsView();

  private final StatisticExpression m_tpsExpression;

  CommonStatisticsViews(StatisticExpressionFactory expressionFactory) {
    try {
      final ExpressionView[] detailExpressionViews = {
        expressionFactory.createExpressionView(
          "Test time",
          "(sum timedTests)",
          false),
        expressionFactory.createExpressionView(
          "Errors",
          "errors",
          false),
      };

      for (int i = 0; i < detailExpressionViews.length; ++i) {
        m_detailStatisticsView.add(detailExpressionViews[i]);
      }

      m_tpsExpression = expressionFactory.createExpression(
        "(* 1000 (/ (+ (count timedTests) untimedTests) period))");

      final ExpressionView[] summaryExpressionViews = {
        expressionFactory.createExpressionView(
          "Tests",
          "(+ (count timedTests) untimedTests)",
          true),
        expressionFactory.createExpressionView(
          "Errors",
          "errors",
          true),
        expressionFactory.createExpressionView(
          "Mean Test Time (ms)",
          "(/ (sum timedTests) (count timedTests))",
          false),
        expressionFactory.createExpressionView(
          "Test Time Standard Deviation (ms)",
          "(sqrt (variance timedTests))",
          false),
        expressionFactory.createExpressionView("TPS", m_tpsExpression),
      };

      for (int i = 0; i < summaryExpressionViews.length; ++i) {
        m_summaryStatisticsView.add(summaryExpressionViews[i]);
      }
    }
    catch (StatisticsException e) {
      throw new AssertionError(e);
    }
  }

  /**
   * Get the detail {@link StatisticsView}.
   *
   * @return The {@link StatisticsView}.
   */
  public StatisticsView getDetailStatisticsView() {
    return m_detailStatisticsView;
  }

  /**
   * Get the summary {@link StatisticsView}.
   *
   * @return The {@link StatisticsView}.
   */
  public StatisticsView getSummaryStatisticsView() {
    return m_summaryStatisticsView;
  }

  public StatisticExpression getTPSExpression() {
    return m_tpsExpression;
  }
}
