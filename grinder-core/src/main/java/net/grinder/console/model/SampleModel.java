// Copyright (C) 2006 - 2013 Philip Aston
// Copyright (C) 2012 Marc Holden
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

import java.util.Collection;
import java.util.EventListener;
import java.util.Set;

import net.grinder.common.Test;
import net.grinder.statistics.StatisticExpression;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.TestStatisticsMap;


/**
 * Interface to {@link SampleModelImplementation}.
 *
 * @author Philip Aston
 */
public interface SampleModel {

  /**
   * Discard all of the information held by the model.
   *
   * <p>This doesn't change the model state.</p>
   */
  void reset();

  /**
   * Zero the statistics.
   *
   * @since 3.10
   */
  void zeroStatistics();

  /**
   * Start the model.
   */
  void start();

  /**
   * Stop the model.
   */
  void stop();

  /**
   * Get the current model state.
   *
   * @return The model state.
   */
  State getState();

  /**
   * A snapshot of the model state.
   *
   * @see SampleModel#getState()
   */
  interface State {
    /**
     * The primary state.
     */
    enum Value {
      /** Stopped - all test reports are ignored. */
      Stopped,

      /** Waiting for the first test report to start the sampling. */
      WaitingForFirstReport,

      /** Waiting until the configured number of samples have been ignored. */
      IgnoringInitialSamples,

      /** Capturing samples. */
      Recording;
    }

    Value getValue();

    /**
     * A presentable description of the state.
     *
     * @return The description.
     */
    String getDescription();

    /**
     * Return the sample count.
     *
     * @return The sample count.
     */
    long getSampleCount();
  }

  /**
   * Get the statistics expression for TPS.
   *
   * @return The TPS expression for this model.
   */
  StatisticExpression getTPSExpression();

  /**
   * Get the expression for peak TPS.
   *
   * @return The peak TPS expression for this model.
   */
  StatisticExpression getPeakTPSExpression();

  /**
   * Get the cumulative statistics for this model.
   *
   * @return The cumulative statistics.
   */
  StatisticsSet getTotalCumulativeStatistics();

  /**
   * Get the total statistics for the latest sample.
   *
   * @return The cumulative statistics.
   */
  StatisticsSet getTotalLatestStatistics();

  /**
   * Add a new model listener.
   *
   * @param listener The listener.
   */
  void addModelListener(Listener listener);

  /**
   * Add a new total sample listener.
   *
   * @param listener The sample listener.
   */
  void addTotalSampleListener(SampleListener listener);

  /**
   * Add a new sample listener for the specific test.
   *
   * @param test The test to add the sample listener for.
   * @param listener The sample listener.
   */
  void addSampleListener(Test test, SampleListener listener);

  /**
   * Register new tests.
   *
   * @param tests The new tests.
   */
  void registerTests(Collection<Test> tests);

  /**
   * Add a new test report.
   *
   * @param statisticsDelta The new test statistics.
   */
  void addTestReport(TestStatisticsMap statisticsDelta);


  /**
   * Interface for listeners to {@link SampleModelImplementation}.
   */
  interface Listener extends EventListener {

    /**
     * Called when the model state has changed, or there is a new
     * sample.
     */
    void stateChanged();

    /**
     * Called when the model has a new sample.
     *
     * @deprecated Deprecated for 3.12. Redundant, since {@link #stateChanged()}
     * is fired whenever there is a new sample, and fine grained notifications
     * are available via {@link SampleModel#addSampleListener} and
     * {@link SampleModel#addTotalSampleListener}.
     */
    @Deprecated
    void newSample();

    /**
     * Called when new tests have been added to the model.
     *
     * @param newTests The new tests.
     * @param modelTestIndex New index structure for the model's tests.
     */
    void newTests(Set<Test> newTests, ModelTestIndex modelTestIndex);

    /**
     * Called when existing tests and statistics views should be
     * discarded.
     */
    void resetTests();
  }

  /**
   * Skeleton implementation of {@link SampleModel.Listener}.
   */
  abstract class AbstractListener implements Listener {

    @Override
    public void newSample() { }

    @Override
    public void newTests(final Set<Test> newTests,
                         final ModelTestIndex modelTestIndex) { }

    @Override
    public void resetTests() { }

    @Override
    public void stateChanged() { }
  }
}
