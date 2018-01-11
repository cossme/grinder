// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2013 Philip Aston
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

import net.grinder.common.Test;
import net.grinder.common.TimeAuthority;
import net.grinder.common.UncheckedGrinderException;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.DispatchContext.DispatchStateException;
import net.grinder.script.NonInstrumentableTypeException;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.script.Statistics.StatisticsForTest;
import net.grinder.script.Test.InstrumentationFilter;
import net.grinder.script.TestRegistry.RegisteredTest;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.Recorder;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.StatisticsSetFactory;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;


/**
 * Represents an individual test. Holds configuration information and
 * the tests statistics.
 *
 * Package scope.
 *
 * @author Philip Aston
 */
final class TestData implements RegisteredTest, Recorder {

  private final StatisticsSetFactory m_statisticsSetFactory;
  private final TestStatisticsHelper m_testStatisticsHelper;
  private final TimeAuthority m_timeAuthority;
  private final Instrumenter m_instrumenter;
  private final ThreadContextLocator m_threadContextLocator;
  private final Test m_test;
  private final Marker m_logMarker;

  /**
   * Cumulative statistics for our test that haven't yet been set to
   * the console.
   */
  private final StatisticsSet m_testStatistics;

  private final RecorderHolderThreadLocal m_recorderHolderTL =
    new RecorderHolderThreadLocal();

  TestData(final ThreadContextLocator threadContextLocator,
           final StatisticsSetFactory statisticsSetFactory,
           final TestStatisticsHelper testStatisticsHelper,
           final TimeAuthority timeAuthority,
           final Instrumenter instrumenter,
           final Test testDefinition) {
    m_statisticsSetFactory = statisticsSetFactory;
    m_testStatisticsHelper = testStatisticsHelper;
    m_timeAuthority = timeAuthority;
    m_instrumenter = instrumenter;
    m_threadContextLocator = threadContextLocator;
    m_test = testDefinition;
    m_testStatistics = m_statisticsSetFactory.create();

    m_logMarker = MarkerFactory.getMarker("test-" + testDefinition.getNumber());
  }

  Test getTest() {
    return m_test;
  }

  Marker getLogMarker() {
    return m_logMarker;
  }

  StatisticsSet getTestStatistics() {
    return m_testStatistics;
  }

  /**
   * {@inheritDoc}
   */
  @Override public Object createProxy(final Object o)
    throws NotWrappableTypeException {
    return m_instrumenter.createInstrumentedProxy(getTest(), this, o);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void instrument(final Object target)
    throws NonInstrumentableTypeException {
    m_instrumenter.instrument(getTest(), this, target);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void instrument(final Object target,
                                   final InstrumentationFilter filter)
    throws NonInstrumentableTypeException {
    m_instrumenter.instrument(getTest(), this, target, filter);
  }

  @Override
  public void start() throws EngineException {
    m_recorderHolderTL.getHolder().start();
  }

  @Override
  public void end(final boolean success) throws EngineException {
    m_recorderHolderTL.getHolder().end(success);
  }

  /**
   * Thread local storage which keeps a {@link RecorderHolder} for each
   * worker thread that has ever used this test.
   */
  private final class RecorderHolderThreadLocal {
    private final ThreadLocal<RecorderHolder> m_threadLocal =
      new ThreadLocal<RecorderHolder>() {

      @Override
      public RecorderHolder initialValue() {
        final ThreadContext threadContext = m_threadContextLocator.get();

        if (threadContext == null) {
          throw new UncheckedException("Only Worker Threads can invoke tests");
        }

        final TestRecorder recorder =
          new TestRecorder(threadContext.getDispatchResultReporter(),
                           new StopWatchImplementation(m_timeAuthority));

        return new RecorderHolder(threadContext, recorder);
      }
    };

    public RecorderHolder getHolder() throws EngineException {
      try {
        return m_threadLocal.get();
      }
      catch (final UncheckedException e) {
        throw new EngineException(e.getMessage());
      }
    }
  }

  /**
   * Cache a single {@link TestRecorder} for a particular worker thread.
   *
   * <p>
   * Ensure each worker thread ignores any nested methods instrumented with the
   * our Test. That is, if the thread makes nested calls to methods instrumented
   * with the same Test, the instrumentation is applied only at the outermost
   * stack frame,
   * </p>
   *
   * <p>
   * This prevents nested invocations for the same test/thread from being
   * recorded multiple times, making life simpler for the user, and for the
   * script engine instrumentation.
   * </p>
   */
  private static final class RecorderHolder implements Recorder {

    private final ThreadContext m_threadContext;
    private final TestRecorder m_recorder;
    private int m_nestingDepth = 0;

    public RecorderHolder(final ThreadContext threadContext,
                          final TestRecorder recorder) {
      m_threadContext = threadContext;
      m_recorder = recorder;
    }

    @Override
    public void start() throws DispatchStateException {
      if (m_nestingDepth++ == 0) {
        // Entering outer frame.
        m_threadContext.pushDispatchContext(m_recorder);
        m_recorder.start();
      }
    }

    @Override
    public void end(final boolean success)  {
      if (--m_nestingDepth == 0) {
        // Leaving outer frame.
        m_recorder.end(success);
        m_threadContext.popDispatchContext();
      }
    }
  }

  /**
   * Three states:
   * <ul>
   * <li><em>initialised</em> Start time is -1. Dispatch time is -1.
   * m_statisticsForTest is null.</li>
   * <li><em>dispatching</em> Start time is valid. Dispatch time is -1.
   * m_statisticsForTest is not null.</li>
   * <li><em>complete</em> Ready to report. Start time is valid. Dispatch
   * time is valid. m_statisticsForTest is null.</li>
   * </ul>
   *
   * {@link ThreadContextImplementation#getDispatchContext()} takes care to only
   * return references to Dispatchers that are <em>dispatching</em> or
   * <em>complete</em>.
   */
  private final class TestRecorder
    implements DispatchContext, Recorder {

    private final DispatchResultReporter m_resultReporter;
    private final StopWatch m_pauseTimer;

    private long m_startTime = -1;
    private long m_dispatchTime = -1;
    private StatisticsForTestImplementation m_statisticsForTest;

    public TestRecorder(final DispatchResultReporter resultReporter,
                           final StopWatch pauseTimer) {

      m_resultReporter = resultReporter;
      m_pauseTimer = pauseTimer;
    }

    @Override
    public void start() throws DispatchStateException {
      if (m_startTime != -1 || m_dispatchTime != -1) {
        throw new DispatchStateException("Last statistics were not reported");
      }

      m_pauseTimer.reset();

      m_statisticsForTest = new StatisticsForTestImplementation(
        this,
        m_testStatisticsHelper,
        m_statisticsSetFactory.create());

      // Make it more likely that the timed section has a "clear run".
      Thread.yield();

      m_startTime = m_timeAuthority.getTimeInMilliseconds();
    }

    @Override
    public void end(final boolean success) {
      m_dispatchTime =
        Math.max(m_timeAuthority.getTimeInMilliseconds() - m_startTime, 0);

      if (m_pauseTimer.isRunning()) {
        m_pauseTimer.stop();
      }

      if (!success && m_statisticsForTest != null) {
        // Always mark as an error if the test threw an exception.
        m_testStatisticsHelper.setSuccess(
          m_statisticsForTest.getStatistics(), false);

        // We don't log the exception. If the script doesn't handle the
        // exception it will be logged when the run is aborted,
        // otherwise we assume the script writer knows what they're
        // doing.
      }
    }

    @Override
    public void report() throws DispatchStateException {
      if (m_dispatchTime < 0) {
        throw new DispatchStateException("No statistics to report");
      }

      final StatisticsSet statistics =  m_statisticsForTest.getStatistics();

      m_testStatisticsHelper.recordTest(statistics, getElapsedTime());

      m_resultReporter.report(getTest(), m_startTime, statistics);

      if (m_testStatisticsHelper.getSuccess(statistics)) {
        getTestStatistics().add(statistics);
      }
      else {
        // If an error, we consider other information to be unreliable,
        // so do not aggregate it.
        m_testStatisticsHelper.incrementErrors(getTestStatistics());
      }

      // Disassociate ourselves from m_statisticsForTest;
      m_statisticsForTest.freeze();
      m_statisticsForTest = null;

      m_startTime = -1;
      m_dispatchTime = -1;
    }

    @Override
    public Test getTest() {
      return TestData.this.getTest();
    }

    @Override public Marker getLogMarker() {
      return TestData.this.getLogMarker();
    }

    @Override
    public StopWatch getPauseTimer() {
      return m_pauseTimer;
    }

    @Override
    public long getElapsedTime() {
      if (m_startTime == -1) {
        return -1;
      }

      final long unadjustedTime;

      if (m_dispatchTime == -1) {
        unadjustedTime = m_timeAuthority.getTimeInMilliseconds() - m_startTime;
      }
      else {
        unadjustedTime = m_dispatchTime;
      }

      return Math.max(unadjustedTime - m_pauseTimer.getTime(), 0);
    }

    @Override
    public StatisticsForTest getStatisticsForTest() {
      return m_statisticsForTest;
    }

    @Override
    public void setHasNestedContexts() {
      m_testStatistics.setIsComposite();
    }
  }

  private static final class UncheckedException
    extends UncheckedGrinderException {
    public UncheckedException(final String message) {
      super(message);
    }
  }
}
