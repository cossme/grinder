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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticExpressionFactory;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsView;
import net.grinder.statistics.TestStatisticsQueries;
import net.grinder.util.ListenerSupport;
import net.grinder.util.SignificantFigureFormat;


/**
 * Statistics views to use with the statistics from the Sample SampleModel.
 *
 * @author Philip Aston
 */
public class SampleModelViewsImplementation implements SampleModelViews {

  private final ListenerSupport<Listener> m_listeners =
    new ListenerSupport<Listener>();
  private final StatisticsServices m_statisticsServices;
  private final ExpressionView m_peakTPSExpressionView;

  // Guarded by this.
  private NumberFormat m_numberFormat;

  // Guarded by this.
  private StatisticsView m_intervalStatisticsView;

  // Guarded by this.
  private StatisticsView m_cumulativeStatisticsView;

  /**
   * Constructor.
   *
   * @param properties Console properties.
   * @param statisticsServices Statistic services.
   * @param model Sample model.
   */
  public SampleModelViewsImplementation(ConsoleProperties properties,
                                        StatisticsServices statisticsServices,
                                        SampleModel model) {

    m_statisticsServices = statisticsServices;

    m_numberFormat =
      new SignificantFigureFormat(properties.getSignificantFigures());

    final StatisticExpressionFactory statisticExpressionFactory =
      m_statisticsServices.getStatisticExpressionFactory();

    m_peakTPSExpressionView =
      statisticExpressionFactory
        .createExpressionView("Peak TPS", model.getPeakTPSExpression());

    resetStatisticsViews();

    properties.addPropertyChangeListener(
      ConsoleProperties.SIG_FIG_PROPERTY,
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent event) {
          synchronized (SampleModelViewsImplementation.this) {
            m_numberFormat =
              new SignificantFigureFormat(
                ((Integer)event.getNewValue()).intValue());
          }
        }
      });
  }

  /**
   * Reset the views.
   */
  public void resetStatisticsViews() {
    final StatisticsView summaryStatisticsView =
      m_statisticsServices.getSummaryStatisticsView();

    synchronized (this) {
      m_intervalStatisticsView = new StatisticsView();
      m_cumulativeStatisticsView = new StatisticsView();

      m_intervalStatisticsView.add(summaryStatisticsView);

      m_cumulativeStatisticsView.add(summaryStatisticsView);
      m_cumulativeStatisticsView.add(m_peakTPSExpressionView);
    }

    m_listeners.apply(
      new ListenerSupport.Informer<Listener>() {
        public void inform(Listener l) { l.resetStatisticsViews(); }
      });
  }

  /**
   * Returns a NumberFormat which corresponds to the user's preference.
   *
   * @return The number format.
   */
  public NumberFormat getNumberFormat() {
    synchronized (this) {
      return m_numberFormat;
    }
  }

  /**
   * Register new statistic expression.
   *
   * @param statisticExpression The expression.
   */
  public void registerStatisticExpression(
    final ExpressionView statisticExpression) {

    synchronized (this) {
      m_intervalStatisticsView.add(statisticExpression);
      m_cumulativeStatisticsView.add(statisticExpression);
    }

    m_listeners.apply(
      new ListenerSupport.Informer<Listener>() {
        public void inform(Listener l) {
          l.newStatisticExpression(statisticExpression);
        }
      });
  }

  /**
   * Get the cumulative statistics view for this model.
   *
   * @return The cumulative statistics view.
   */
  public StatisticsView getCumulativeStatisticsView() {
    synchronized (this) {
      return m_cumulativeStatisticsView;
    }
  }

  /**
   * Get the interval statistics view for this model.
   *
   * @return The interval statistics view.
   */
  public StatisticsView getIntervalStatisticsView() {
    synchronized (this) {
      return m_intervalStatisticsView;
    }
  }

  /**
   * Return an object allowing access to common functions of test statistics.
   *
   * @return The {@link TestStatisticsQueries}.
   */
  public TestStatisticsQueries getTestStatisticsQueries() {
    return m_statisticsServices.getTestStatisticsQueries();
  }

  /**
   * Add a new listener.
   *
   * @param listener The listener.
   */
  public void addListener(Listener listener) {
    m_listeners.add(listener);
  }
}
