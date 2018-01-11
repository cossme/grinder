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

package net.grinder.engine.process;

import junit.framework.TestCase;

import net.grinder.common.Test;
import net.grinder.common.StubTest;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.DispatchContext.DispatchStateException;
import net.grinder.script.InvalidContextException;
import net.grinder.script.Statistics.StatisticsForTest;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.StatisticsSetFactory;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.testutility.Time;
import net.grinder.util.StandardTimeAuthority;


/**
 * Unit test case for <code>TestData</code>.
 *
 * @author Philip Aston
 */
public class TestTestData extends TestCase {

  private static final StatisticsIndexMap.LongSampleIndex s_timedTestsIndex;
  private static final StatisticsIndexMap.LongIndex s_untimedTestsIndex;

  static {
    final StatisticsIndexMap indexMap =
      StatisticsServicesImplementation.getInstance().getStatisticsIndexMap();

    s_timedTestsIndex= indexMap.getLongSampleIndex("timedTests");
    s_untimedTestsIndex= indexMap.getLongIndex("untimedTests");
  }


  private final StatisticsSetFactory m_statisticsSetFactory =
    StatisticsServicesImplementation.getInstance().getStatisticsSetFactory();

  private final RandomStubFactory<Instrumenter> m_instrumenterStubFactory =
    RandomStubFactory.create(Instrumenter.class);
  private final Instrumenter m_instrumenter =
    m_instrumenterStubFactory.getStub();

  private final RandomStubFactory<TestStatisticsHelper>
    m_testStatisticsHelperStubFactory =
      RandomStubFactory.create(TestStatisticsHelper.class);
  private final TestStatisticsHelper m_testStatisticsHelper =
    m_testStatisticsHelperStubFactory.getStub();

  private final StubThreadContextLocator m_threadContextLocator =
    new StubThreadContextLocator();
  private final RandomStubFactory<ThreadContext> m_threadContextStubFactory =
    RandomStubFactory.create(ThreadContext.class);
  private final ThreadContext m_threadContext =
    m_threadContextStubFactory.getStub();

  private final StandardTimeAuthority m_timeAuthority =
    new StandardTimeAuthority();

  public void testCreateProxy() throws Exception {
    final Test test1 = new StubTest(1, "test1");

    final TestData testData =
      new TestData(null, m_statisticsSetFactory, null,
                   m_timeAuthority, m_instrumenter, test1);

    final Object original = new Object();

    testData.createProxy(original);

    m_instrumenterStubFactory.assertSuccess(
      "createInstrumentedProxy", test1, testData, original);
    m_instrumenterStubFactory.assertNoMoreCalls();
  }

  public void testDispatch() throws Exception {
    final Test test1 = new StubTest(1, "test1");

    final TestData testData =
      new TestData(m_threadContextLocator,
                   m_statisticsSetFactory,
                   m_testStatisticsHelper,
                   m_timeAuthority,
                   m_instrumenter,
                   test1);

    assertSame(test1, testData.getTest());
    final StatisticsSet statistics = testData.getTestStatistics();
    assertNotNull(statistics);

    // 1. Happy case.
    try {
      testData.start();
      fail("Expected EngineException");
    }
    catch (EngineException e) {
      AssertUtilities.assertContains(e.getMessage(), "Only Worker Threads");
    }

    m_threadContextLocator.set(m_threadContext);

    testData.start();

    m_threadContextStubFactory.assertSuccess("getDispatchResultReporter");
    final DispatchContext dispatchContext =
      (DispatchContext) m_threadContextStubFactory.assertSuccess(
      "pushDispatchContext", DispatchContext.class).getParameters()[0];
    m_threadContextStubFactory.assertNoMoreCalls();

    testData.end(true);

    m_threadContextStubFactory.assertSuccess("popDispatchContext");
    m_threadContextStubFactory.assertNoMoreCalls();

    // Test statistics not updated until we report.
    m_testStatisticsHelperStubFactory.assertNoMoreCalls();
    m_testStatisticsHelperStubFactory.setResult("getSuccess", Boolean.TRUE);

    // Calling report() is the only way to reset the dispatcher.
    dispatchContext.report();

    m_testStatisticsHelperStubFactory.assertSuccess(
      "recordTest", StatisticsSet.class, Long.class);

    m_testStatisticsHelperStubFactory.assertSuccess(
      "getSuccess", StatisticsSet.class);

    m_testStatisticsHelperStubFactory.assertNoMoreCalls();

    // 2. Nested case.
    testData.start();

    m_threadContextStubFactory.assertSuccess(
      "pushDispatchContext", DispatchContext.class);
    m_threadContextStubFactory.assertNoMoreCalls();

    testData.start();
    m_threadContextStubFactory.assertNoMoreCalls();

    testData.end(true);
    m_threadContextStubFactory.assertNoMoreCalls();

    testData.end(true);

    m_threadContextStubFactory.assertSuccess("popDispatchContext");
    m_threadContextStubFactory.assertNoMoreCalls();

    // Test statistics not updated until we report.
    m_testStatisticsHelperStubFactory.assertNoMoreCalls();

    dispatchContext.report();

    m_testStatisticsHelperStubFactory.assertSuccess(
      "recordTest", StatisticsSet.class, Long.class);

    m_testStatisticsHelperStubFactory.assertSuccess(
      "getSuccess", StatisticsSet.class);

    m_testStatisticsHelperStubFactory.assertNoMoreCalls();

    // 3. Unhappy case.
    testData.start();
    testData.end(false);

    // The dispatcher's statistics (not the test statistics) are
    // marked bad.
    final StatisticsSet dispatcherStatistics =
      (StatisticsSet)
      m_testStatisticsHelperStubFactory.assertSuccess(
        "setSuccess", StatisticsSet.class, Boolean.class).getParameters()[0];
    assertNotSame(statistics, dispatcherStatistics);
    m_testStatisticsHelperStubFactory.assertNoMoreCalls();

    // 3b. Unhappy case with pause timer left running.
    dispatchContext.report();
    m_testStatisticsHelperStubFactory.resetCallHistory();

    testData.start();
    dispatchContext.getPauseTimer().start();

    testData.end(false);

    // The dispatcher's statistics (not the test statistics) are
    // marked bad.
    final StatisticsSet dispatcherStatistics2 =
      (StatisticsSet)
      m_testStatisticsHelperStubFactory.assertSuccess(
        "setSuccess", StatisticsSet.class, Boolean.class).getParameters()[0];
    assertNotSame(statistics, dispatcherStatistics2);
    m_testStatisticsHelperStubFactory.assertNoMoreCalls();

    // 4. Assertion failures.
    try {
      testData.start();
      fail("Expected DispatchStateException");
    }
    catch (DispatchStateException e) {
    }

    // 5. Lets test the reporting with an error.
    m_testStatisticsHelperStubFactory.setResult("getSuccess", Boolean.FALSE);

    dispatchContext.report();

    m_testStatisticsHelperStubFactory.assertSuccess(
      "recordTest", StatisticsSet.class, Long.class);

    m_testStatisticsHelperStubFactory.assertSuccess(
      "getSuccess", dispatcherStatistics);

    m_testStatisticsHelperStubFactory.assertSuccess(
      "incrementErrors", statistics);

    m_testStatisticsHelperStubFactory.assertNoMoreCalls();
  }

  public void testDispatchContext() throws Exception {
    final Test test1 = new StubTest(1, "test1");

    // We need a real helper here, not a stub.
    final TestStatisticsHelper testStatisticsHelper =
      new TestStatisticsHelperImplementation(
        StatisticsServicesImplementation.getInstance().getStatisticsIndexMap());

    final TestData testData =
      new TestData(m_threadContextLocator,
                   m_statisticsSetFactory,
                   testStatisticsHelper,
                   m_timeAuthority,
                   m_instrumenter,
                   test1);

    assertSame(test1, testData.getTest());
    final StatisticsSet statistics = testData.getTestStatistics();
    assertNotNull(statistics);

    m_threadContextLocator.set(m_threadContext);

    final long beforeTime = System.currentTimeMillis();

    testData.start();

    m_threadContextStubFactory.assertSuccess("getDispatchResultReporter");
    final DispatchContext dispatchContext =
      (DispatchContext)
      m_threadContextStubFactory.assertSuccess(
        "pushDispatchContext", DispatchContext.class).getParameters()[0];
    m_threadContextStubFactory.assertNoMoreCalls();

    testData.end(true);

    m_threadContextStubFactory.assertSuccess("popDispatchContext");
    m_threadContextStubFactory.assertNoMoreCalls();

    // Test statistics not updated until we report.
    assertEquals(0, statistics.getCount(s_timedTestsIndex));

    assertSame(test1, dispatchContext.getTest());

    final StatisticsForTest dispatchStatisticsForTest =
      dispatchContext.getStatisticsForTest();
    assertSame(dispatchStatisticsForTest,
      dispatchContext.getStatisticsForTest());
    assertEquals(test1, dispatchStatisticsForTest.getTest());
    assertNotNull(dispatchStatisticsForTest);

    assertNotNull(dispatchContext.getPauseTimer());

    final long elapsedTime = dispatchContext.getElapsedTime();
    assertTrue(elapsedTime >= 0);
    assertTrue(elapsedTime <= System.currentTimeMillis() - beforeTime);

    dispatchContext.getPauseTimer().add(new StopWatch(){

      public void start() { }
      public void stop() { }
      public void reset() { }
      public void add(StopWatch watch) { }

      public long getTime() throws StopWatchRunningException {
        return 1000;
      }
      public boolean isRunning() {
        return false;
      }
    });

    // Call will take much less than a second, so we get 0.
    assertEquals(0, dispatchContext.getElapsedTime());

    assertEquals(0, dispatchStatisticsForTest.getLong("untimedTests"));

    // Its easier for the test to update the statistics by hand.
    dispatchStatisticsForTest.setLong("untimedTests", 2);

    dispatchContext.report();

    try {
      dispatchContext.report();
      fail("Expected DispatchStateException");
    }
    catch (DispatchStateException e) {
    }

    // report() will have updated the statistics with a single,
    // successful, timed test.
    assertEquals(1, statistics.getCount(s_timedTestsIndex));
    assertEquals(0, statistics.getValue(s_untimedTestsIndex));

    assertEquals(-1, dispatchContext.getElapsedTime());
    assertNull(dispatchContext.getStatisticsForTest());

    try {
      dispatchStatisticsForTest.setLong("untimedTests", 2);
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
    }

    testData.start();

    assertTrue(dispatchContext.getElapsedTime() < 20);
    try {
      Thread.sleep(50);
    }
    catch (InterruptedException e) {
      fail(e.getMessage());
    }

    assertTrue(dispatchContext.getElapsedTime() >=
               50 - Time.J2SE_TIME_ACCURACY_MILLIS);

    testData.end(true);

    final long elapsedTime2 = dispatchContext.getElapsedTime();
    assertTrue(elapsedTime2 >= 50 - Time.J2SE_TIME_ACCURACY_MILLIS);
    assertTrue(elapsedTime2 <= 200); // Pause timer was reset after last call.

    assertFalse(statistics.isComposite());
    dispatchContext.setHasNestedContexts();
    assertTrue(statistics.isComposite());
  }

  public void testDispatchForBug1593169() throws Exception {
    final TestData testData =
      new TestData(m_threadContextLocator,
                   m_statisticsSetFactory,
                   m_testStatisticsHelper,
                   m_timeAuthority,
                   m_instrumenter,
                   new StubTest(1, "test1"));


    m_threadContextLocator.set(m_threadContext);

    final ShutdownException se = new ShutdownException("Bang");
    m_threadContextStubFactory.setThrows("pushDispatchContext", se);

    try {
      testData.start();
      fail("Expected ShutdownException");
    }
    catch (ShutdownException e) {
    }

    m_threadContextStubFactory.assertSuccess("getDispatchResultReporter");
    m_threadContextStubFactory.assertException("pushDispatchContext",
                                               se,
                                               DispatchContext.class);
    m_threadContextStubFactory.assertNoMoreCalls();
  }
}
