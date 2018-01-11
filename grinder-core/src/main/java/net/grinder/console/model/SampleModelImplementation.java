// Copyright (C) 2001 - 2013 Philip Aston
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import net.grinder.common.GrinderException;
import net.grinder.common.Test;
import net.grinder.console.common.ErrorHandler;
import net.grinder.statistics.PeakStatisticExpression;
import net.grinder.statistics.StatisticExpression;
import net.grinder.statistics.StatisticExpressionFactory;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.translation.Translations;
import net.grinder.util.ListenerSupport;


/**
 * Collate test reports into samples and distribute to listeners.
 *
 * <p>
 * When notifying listeners of changes to the number of tests we send copies of
 * the new index arrays. This helps because most listeners are Swing dispatched
 * and so can't guarantee the model is in a reasonable state when they call
 * back.
 * </p>
 *
 * @author Philip Aston
 */
public final class SampleModelImplementation implements SampleModel {

  private final ConsoleProperties m_properties;
  private final StatisticsServices m_statisticsServices;
  private final Timer m_timer;
  private final ErrorHandler m_errorHandler;

  private final String m_stateIgnoringString;
  private final String m_stateWaitingString;
  private final String m_stateStoppedString;
  private final String m_stateCapturingString;
  private final String m_unknownTestString;

  /**
   * The current test set. A TreeSet is used to maintain the test
   * order. Guarded by itself.
   */
  private final Set<Test> m_tests = new TreeSet<Test>();

  private final ListenerSupport<Listener> m_listeners =
    new ListenerSupport<Listener>();

  private final StatisticsIndexMap.LongIndex m_periodIndex;
  private final StatisticExpression m_tpsExpression;
  private final PeakStatisticExpression m_peakTPSExpression;

  private final SampleAccumulator m_totalSampleAccumulator;

  /**
   * A {@link SampleAccumulator} for each test. Guarded by itself.
   */
  private final Map<Test, SampleAccumulator> m_accumulators =
    Collections.synchronizedMap(new HashMap<Test, SampleAccumulator>());

  // Guarded by this.
  private InternalState m_state;

  /**
   * Creates a new <code>SampleModelImplementation</code> instance.
   *
   * @param properties The console properties.
   * @param statisticsServices Statistics services.
   * @param timer A timer.
   * @param translations Console resources.
   * @param errorHandler Error handler
   *
   * @exception GrinderException if an error occurs
   */
  public SampleModelImplementation(final ConsoleProperties properties,
                                   final StatisticsServices statisticsServices,
                                   final Timer timer,
                                   final Translations translations,
                                   final ErrorHandler errorHandler)
    throws GrinderException {

    m_properties = properties;
    m_statisticsServices = statisticsServices;
    m_timer = timer;
    m_errorHandler = errorHandler;

    m_stateIgnoringString =
        translations.translate("console.state/ignoring-samples") + ": ";
    m_stateWaitingString =
        translations.translate("console.state/waiting-for-samples");
    m_stateStoppedString =
        translations.translate("console.state/collection-stopped");
    m_stateCapturingString =
        translations.translate("console.state/capturing-samples") + ": ";
    m_unknownTestString =
        translations.translate("console.phrase/ignoring-unknown-test");

    final StatisticsIndexMap indexMap =
      statisticsServices.getStatisticsIndexMap();

    m_periodIndex = indexMap.getLongIndex("period");

    final StatisticExpressionFactory statisticExpressionFactory =
      m_statisticsServices.getStatisticExpressionFactory();

    m_tpsExpression = statisticsServices.getTPSExpression();

    m_peakTPSExpression =
      statisticExpressionFactory.createPeak(
        indexMap.getDoubleIndex("peakTPS"), m_tpsExpression);

    m_totalSampleAccumulator =
      new SampleAccumulator(m_peakTPSExpression, m_periodIndex,
                            m_statisticsServices.getStatisticsSetFactory());

    setInternalState(new WaitingForTriggerState());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StatisticExpression getTPSExpression() {
    return m_tpsExpression;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StatisticExpression getPeakTPSExpression() {
    return m_peakTPSExpression;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerTests(final Collection<Test> tests) {
    // Need to copy collection, might be immutable.
    final Set<Test> newTests = new HashSet<Test>(tests);

    final Test[] testArray;

    synchronized (m_tests) {
      newTests.removeAll(m_tests);

      if (newTests.size() == 0) {
        // No new tests.
        return;
      }

      m_tests.addAll(newTests);

      // Create an index of m_tests sorted by test number.
      testArray = m_tests.toArray(new Test[m_tests.size()]);
    }

    final SampleAccumulator[] accumulatorArray =
      new SampleAccumulator[testArray.length];

    synchronized (m_accumulators) {
      for (final Test test : newTests) {
        m_accumulators.put(test,
                           new SampleAccumulator(
                             m_peakTPSExpression,
                             m_periodIndex,
                             m_statisticsServices.getStatisticsSetFactory()));
      }

      for (int i = 0; i < accumulatorArray.length; i++) {
        accumulatorArray[i] = m_accumulators.get(testArray[i]);
      }
    }

    final ModelTestIndex modelTestIndex =
      new ModelTestIndex(testArray, accumulatorArray);

    m_listeners.apply(
      new ListenerSupport.Informer<Listener>() {
        @Override
        public void inform(final Listener l) {
          l.newTests(newTests, modelTestIndex); }
      });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StatisticsSet getTotalCumulativeStatistics() {
    return m_totalSampleAccumulator.getCumulativeStatistics();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StatisticsSet getTotalLatestStatistics() {
    return m_totalSampleAccumulator.getLastSampleStatistics();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addModelListener(final Listener listener) {
    m_listeners.add(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addSampleListener(final Test test,
                                final SampleListener listener) {
    final SampleAccumulator sampleAccumulator = m_accumulators.get(test);

    if (sampleAccumulator != null) {
      sampleAccumulator.addSampleListener(listener);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addTotalSampleListener(final SampleListener listener) {
    m_totalSampleAccumulator.addSampleListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {

    synchronized (m_tests) {
      m_tests.clear();
    }

    m_accumulators.clear();
    m_totalSampleAccumulator.zero();

    m_listeners.apply(
      new ListenerSupport.Informer<Listener>() {
        @Override
        public void inform(final Listener l) { l.resetTests(); }
      });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void zeroStatistics() {
    zero();

    m_listeners.apply(
      new ListenerSupport.Informer<Listener>() {
        @Override
        public void inform(final Listener l) { l.stateChanged(); }
      });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    getInternalState().start();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    getInternalState().stop();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addTestReport(final TestStatisticsMap testStatisticsMap) {
    getInternalState().newTestReport(testStatisticsMap);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public State getState() {
    return getInternalState().toExternalState();
  }

  private void zero() {
    synchronized (m_accumulators) {
      for (final SampleAccumulator sampleAccumulator :
        m_accumulators.values()) {
        sampleAccumulator.zero();
      }
    }

    m_totalSampleAccumulator.zero();
  }

  private InternalState getInternalState() {
    synchronized (this) {
      return m_state;
    }
  }

  private void setInternalState(final InternalState newState) {
    synchronized (this) {
      m_state = newState;
    }

    m_listeners.apply(
      new ListenerSupport.Informer<Listener>() {
        @Override
        public void inform(final Listener l) { l.stateChanged(); }
      });
  }

  private interface InternalState {
    State toExternalState();

    void start();

    void stop();

    void newTestReport(TestStatisticsMap testStatisticsMap);
  }

  private abstract class AbstractInternalState
    implements InternalState, State {

    protected final boolean isActiveState() {
      return getInternalState() == this;
    }

    @Override
    public final State toExternalState() {
      // We don't bother cloning the state, only the description varies.
      return this;
    }

    @Override
    public final void start() {
      // Valid transition for all states.
      setInternalState(new WaitingForTriggerState());
    }

    @Override
    public final void stop() {
      // Valid transition for all states.
      setInternalState(new StoppedState());
    }

    @Override
    public long getSampleCount() {
      return -1;
    }
  }

  private final class WaitingForTriggerState extends AbstractInternalState {
    public WaitingForTriggerState() {
      zero();
    }

    @Override
    public void newTestReport(final TestStatisticsMap testStatisticsMap) {
      if (m_properties.getIgnoreSampleCount() == 0) {
        setInternalState(new CapturingState());
      }
      else {
        setInternalState(new TriggeredState());
      }

      // Ensure the the first sample is recorded.
      getInternalState().newTestReport(testStatisticsMap);
    }

    @Override
    public String getDescription() {
      return m_stateWaitingString;
    }

    @Override
    public Value getValue() {
      return Value.WaitingForFirstReport;
    }
  }

  private final class StoppedState extends AbstractInternalState {
    @Override
    public void newTestReport(final TestStatisticsMap testStatisticsMap) {
    }

    @Override
    public String getDescription() {
      return m_stateStoppedString;
    }

    @Override
    public Value getValue() {
      return Value.Stopped;
    }
  }

  private abstract class SamplingState extends AbstractInternalState {
    // Guarded by this.
    private long m_lastTime = 0;

    private volatile long m_sampleCount = 1;

    @Override
    public final void newTestReport(final TestStatisticsMap testStatisticsMap) {
      testStatisticsMap.new ForEach() {
        @Override
        public void next(final Test test, final StatisticsSet statistics) {
          final SampleAccumulator sampleAccumulator = m_accumulators.get(test);

          if (sampleAccumulator == null) {
            m_errorHandler.handleInformationMessage(
              m_unknownTestString + " " + test);
          }
          else {
            sampleAccumulator.addIntervalStatistics(statistics);

            if (shouldAccumulateSamples()) {
              sampleAccumulator.addCumulativeStaticstics(statistics);
            }

            if (!statistics.isComposite()) {
              m_totalSampleAccumulator.addIntervalStatistics(statistics);

              if (shouldAccumulateSamples()) {
                m_totalSampleAccumulator.addCumulativeStaticstics(statistics);
              }
            }
          }
        }
      }
      .iterate();
    }

    protected final void schedule() {
      synchronized (this) {
        if (m_lastTime == 0) {
          m_lastTime = System.currentTimeMillis();
        }
      }

      m_timer.schedule(
        new TimerTask() {
          @Override
          public void run() { sample(); }
        },
        m_properties.getSampleInterval());
    }

    public final void sample() {
      if (!isActiveState()) {
        return;
      }

      try {
        final long period;

        synchronized (this) {
          period = System.currentTimeMillis() - m_lastTime;
        }

        final long sampleInterval = m_properties.getSampleInterval();

        synchronized (m_accumulators) {
          for (final SampleAccumulator sampleAccumulator :
            m_accumulators.values()) {
            sampleAccumulator.fireSample(sampleInterval, period);
          }
        }

        m_totalSampleAccumulator.fireSample(sampleInterval, period);

        ++m_sampleCount;

        // I'm ignoring a minor race here: the model could have been stopped
        // after the task was started.
        // We call setInternalState() even if the InternalState hasn't
        // changed since we've altered the sample count.
        setInternalState(nextState());

        m_listeners.apply(
          new ListenerSupport.Informer<Listener>() {
            @SuppressWarnings("deprecation")
            @Override
            public void inform(final Listener l) { l.newSample(); }
          });
      }
      finally {
        synchronized (this) {
          if (isActiveState()) {
            schedule();
          }
        }
      }
    }

    @Override
    public final long getSampleCount() {
      return m_sampleCount;
    }

    protected abstract boolean shouldAccumulateSamples();

    protected abstract InternalState nextState();
  }

  private final class TriggeredState extends SamplingState {
    public TriggeredState() {
      schedule();
    }

    @Override
    protected boolean shouldAccumulateSamples() {
      return false;
    }

    @Override
    protected InternalState nextState() {
      if (getSampleCount() > m_properties.getIgnoreSampleCount()) {
        return new CapturingState();
      }

      return this;
    }

    @Override
    public String getDescription() {
      return m_stateIgnoringString + getSampleCount();
    }

    @Override
    public Value getValue() {
      return Value.IgnoringInitialSamples;
    }
  }

  private final class CapturingState extends SamplingState {
    public CapturingState() {
      zero();
      schedule();
    }

    @Override
    protected boolean shouldAccumulateSamples() {
      return true;
    }

    @Override
    protected InternalState nextState() {
      final int collectSampleCount = m_properties.getCollectSampleCount();

      if (collectSampleCount != 0 && getSampleCount() > collectSampleCount) {
        return new StoppedState();
      }

      return this;
    }

    @Override
    public String getDescription() {
      return m_stateCapturingString + getSampleCount();
    }

    @Override
    public Value getValue() {
      return Value.Recording;
    }
  }
}
