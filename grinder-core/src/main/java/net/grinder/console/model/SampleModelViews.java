// Copyright (C) 2008 Philip Aston
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

import java.text.NumberFormat;
import java.util.EventListener;

import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsView;
import net.grinder.statistics.TestStatisticsQueries;


/**
 * Statistics views to use with the statistics from the {@link SampleModel}.
 *
 * @author Philip Aston
 */
public interface SampleModelViews {

  /**
   * Returns a NumberFormat which corresponds to the user's preference.
   *
   * @return The number format.
   */
  NumberFormat getNumberFormat();

  /**
   * Get the interval statistics view for this model.
   *
   * @return The interval statistics view.
   */
  StatisticsView getIntervalStatisticsView();

  /**
   * Get the cumulative statistics view for this model.
   *
   * @return The cumulative statistics view.
   */
  StatisticsView getCumulativeStatisticsView();

  /**
   * Register new statistic expression.
   *
   * @param statisticExpression The expression.
   */
  void registerStatisticExpression(ExpressionView statisticExpression);

  /**
   * Reset the views.
   */
  void resetStatisticsViews();

  /**
   * Return an object allowing access to common functions of test statistics.
   *
   * @return The {@link TestStatisticsQueries}.
   */
  TestStatisticsQueries getTestStatisticsQueries();

  /**
   * Add a new listener.
   *
   * @param listener The listener.
   */
  void addListener(Listener listener);

  /**
   * Interface for listeners.
   */
  public interface Listener extends EventListener {

    /**
     * Called when a new statistic expression has been added to the model.
     *
     * @param statisticExpression The new statistic expression.
     */
    void newStatisticExpression(ExpressionView statisticExpression);

    /**
     * Called when existing statistics views should be discarded.
     */
    void resetStatisticsViews();
  }
}
