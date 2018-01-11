// Copyright (C) 2008 - 2012 Philip Aston
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

import static net.grinder.console.model.SampleModel.State.Value.IgnoringInitialSamples;
import static net.grinder.console.model.SampleModel.State.Value.Recording;
import static net.grinder.console.model.SampleModel.State.Value.Stopped;
import static net.grinder.console.model.SampleModel.State.Value.WaitingForFirstReport;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

import net.grinder.common.StubTest;
import net.grinder.common.Test;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.common.StubResources;
import net.grinder.console.model.SampleModel.AbstractListener;
import net.grinder.console.model.SampleModel.Listener;
import net.grinder.console.model.SampleModel.State;
import net.grinder.statistics.StatisticExpression;
import net.grinder.statistics.StatisticsIndexMap.LongIndex;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.testutility.StubTimer;


/**
 * Unit tests for {@link SampleModelImplementation}.
 *
 * @author Philip Aston
 */
public class TestSampleModelImplementation extends AbstractFileTestCase {

  private final Resources m_resources = new StubResources<String>(
    new HashMap<String, String>() {{
      put("state.ignoring.label", "whatever");
      put("state.waiting.label", "waiting, waiting, waiting");
      put("state.stopped.label", "done");
      put("state.capturing.label", "running");
    }}
  );

  private ConsoleProperties m_consoleProperties;

  private final StatisticsServices m_statisticsServices =
    StatisticsServicesImplementation.getInstance();

  private StubTimer m_timer;

  private final RandomStubFactory<Listener> m_listenerStubFactory =
    RandomStubFactory.create(SampleModel.Listener.class);
  private final SampleModel.Listener m_listener =
    m_listenerStubFactory.getStub();

  final RandomStubFactory<ErrorHandler> m_errorHandlerStubFactory =
    RandomStubFactory.create(ErrorHandler.class);
  final ErrorHandler m_errorHandler =
    m_errorHandlerStubFactory.getStub();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m_timer = new StubTimer();
    m_consoleProperties =
      new ConsoleProperties(null, new File(getDirectory(), "props"));
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    m_timer.cancel();
  }

  public void testConstruction() throws Exception {
    final SampleModelImplementation sampleModelImplementation =
      new SampleModelImplementation(m_consoleProperties,
                                    m_statisticsServices,
                                    m_timer,
                                    m_resources,
                                    m_errorHandler);

    final StatisticExpression tpsExpression =
      sampleModelImplementation.getTPSExpression();
    assertNotNull(tpsExpression);
    assertSame(tpsExpression, sampleModelImplementation.getTPSExpression());

    final StatisticExpression peakTPSExpression =
      sampleModelImplementation.getPeakTPSExpression();
    assertNotNull(peakTPSExpression);
    assertSame(
      peakTPSExpression, sampleModelImplementation.getPeakTPSExpression());
    assertNotSame(tpsExpression, peakTPSExpression);

    final StatisticsSet totalCumulativeStatistics =
      sampleModelImplementation.getTotalCumulativeStatistics();
    assertNotNull(totalCumulativeStatistics);
    assertSame(totalCumulativeStatistics,
               sampleModelImplementation.getTotalCumulativeStatistics());

    final StatisticsSet totalLatestStatistics =
    	      sampleModelImplementation.getTotalLatestStatistics();
    	    assertNotNull(totalLatestStatistics);
    	    assertSame(totalLatestStatistics,
    	               sampleModelImplementation.getTotalLatestStatistics());

    final State state = sampleModelImplementation.getState();
    assertEquals(WaitingForFirstReport, state.getValue());
    assertEquals("waiting, waiting, waiting", state.getDescription());
    assertNull(m_timer.getLastScheduledTimerTask());
  }

  @SuppressWarnings("unchecked")
  public void testRegisterTests() throws Exception {
    final SampleModelImplementation sampleModelImplementation =
      new SampleModelImplementation(m_consoleProperties,
                                    m_statisticsServices,
                                    m_timer,
                                    m_resources,
                                    m_errorHandler);

    sampleModelImplementation.addModelListener(m_listener);

    final Set<Test> emptySet = Collections.emptySet();
    sampleModelImplementation.registerTests(emptySet);
    m_listenerStubFactory.assertNoMoreCalls();

    final Test test1 = new StubTest(1, "test 1");
    final Test test2 = new StubTest(2, "test 2");
    final Test test3 = new StubTest(3, "test 3");
    final Test test4 = new StubTest(4, "test 4");

    final List<Test> testList = new ArrayList<Test>() { {
      add(test2);
      add(test1);
      add(test3);
    } };

    sampleModelImplementation.registerTests(testList);

    final Object[] callbackParameters = m_listenerStubFactory.assertSuccess(
      "newTests", Set.class, ModelTestIndex.class).getParameters();
    m_listenerStubFactory.assertNoMoreCalls();

    Collections.sort(testList);

    final Set<Test> callbackTestSet = (Set<Test>)callbackParameters[0];
    assertTrue(testList.containsAll(callbackTestSet));
    assertTrue(callbackTestSet.containsAll(testList));

    final ModelTestIndex modelIndex = (ModelTestIndex)callbackParameters[1];
    assertEquals(testList.size(), modelIndex.getNumberOfTests());
    assertEquals(testList.size(), modelIndex.getAccumulatorArray().length);

    for (int i = 0; i < modelIndex.getNumberOfTests(); ++i) {
      assertEquals(testList.get(i), modelIndex.getTest(i));
    }

    final List<Test> testList2 = new ArrayList<Test>() { {
      add(test2);
      add(test4);
    } };

    sampleModelImplementation.registerTests(testList2);

    final Object[] callbackParameters2 = m_listenerStubFactory.assertSuccess(
      "newTests", Set.class, ModelTestIndex.class).getParameters();
    m_listenerStubFactory.assertNoMoreCalls();

    final Set<Test> expectedNewTests = new HashSet<Test>() { {
      add(test4);
    } };

    final Set<Test> callbackTestSet2 = (Set<Test>)callbackParameters2[0];
    assertTrue(expectedNewTests.containsAll(callbackTestSet2));
    assertTrue(callbackTestSet2.containsAll(expectedNewTests));

    final ModelTestIndex modelIndex2 = (ModelTestIndex)callbackParameters2[1];
    assertEquals(4, modelIndex2.getNumberOfTests());
    assertEquals(4, modelIndex2.getAccumulatorArray().length);

    sampleModelImplementation.registerTests(testList2);
    m_listenerStubFactory.assertNoMoreCalls();
  }

  public void testWaitingToStopped() throws Exception {
    final SampleModelImplementation sampleModelImplementation =
      new SampleModelImplementation(m_consoleProperties,
                                    m_statisticsServices,
                                    m_timer,
                                    m_resources,
                                    m_errorHandler);

    sampleModelImplementation.addModelListener(m_listener);

    final State state = sampleModelImplementation.getState();
    assertEquals(WaitingForFirstReport, state.getValue());
    assertEquals("waiting, waiting, waiting", state.getDescription());

    m_listenerStubFactory.assertNoMoreCalls();


    sampleModelImplementation.stop();

    m_listenerStubFactory.assertSuccess("stateChanged");

    final State stoppedState = sampleModelImplementation.getState();
    assertEquals(Stopped, stoppedState.getValue());
    assertEquals("done", stoppedState.getDescription());


    sampleModelImplementation.addTestReport(new TestStatisticsMap());

    final State stoppedState2 = sampleModelImplementation.getState();
    assertEquals(Stopped, stoppedState2.getValue());
    assertEquals("done", stoppedState2.getDescription());


    assertNull(m_timer.getLastScheduledTimerTask());
  }

  public void testWaitingToTriggeredToCapturingToStopped() throws Exception {
    final SampleModelImplementation sampleModelImplementation =
      new SampleModelImplementation(m_consoleProperties,
                                    m_statisticsServices,
                                    m_timer,
                                    m_resources,
                                    m_errorHandler);

    final TestStatisticsMap testStatisticsMap = new TestStatisticsMap();


    sampleModelImplementation.addModelListener(m_listener);

    final State waitingState = sampleModelImplementation.getState();
    assertEquals(WaitingForFirstReport, waitingState.getValue());
    assertEquals("waiting, waiting, waiting", waitingState.getDescription());

    m_listenerStubFactory.assertNoMoreCalls();


    m_consoleProperties.setIgnoreSampleCount(10);

    sampleModelImplementation.addTestReport(testStatisticsMap);

    final State triggeredState = sampleModelImplementation.getState();
    assertEquals(IgnoringInitialSamples, triggeredState.getValue());
    assertEquals("whatever 1", triggeredState.getDescription());


    final TimerTask triggeredSampleTask = m_timer.getLastScheduledTimerTask();
    triggeredSampleTask.run();

    assertEquals("whatever 2", triggeredState.getDescription());


    sampleModelImplementation.addTestReport(testStatisticsMap);
    sampleModelImplementation.addTestReport(testStatisticsMap);
    sampleModelImplementation.addTestReport(testStatisticsMap);
    triggeredSampleTask.run();

    assertEquals("whatever 3",
                 sampleModelImplementation.getState().getDescription());


    for (int i = 0; i < 4; ++i) {
      triggeredSampleTask.run();
    }

    assertEquals("whatever 7",
      sampleModelImplementation.getState().getDescription());


    triggeredSampleTask.run();

    assertEquals("whatever 8",
      sampleModelImplementation.getState().getDescription());
    assertEquals(IgnoringInitialSamples, sampleModelImplementation.getState().getValue());

    for (int i = 0; i < 3; ++i) {
      sampleModelImplementation.addTestReport(testStatisticsMap);
      triggeredSampleTask.run();
    }

    final State capturingState = sampleModelImplementation.getState();
    assertEquals(Recording, capturingState.getValue());
    assertEquals("running 1", capturingState.getDescription());


    sampleModelImplementation.addTestReport(testStatisticsMap);

    assertEquals("running 1",
      sampleModelImplementation.getState().getDescription());


    final TimerTask capturingSampleTask = m_timer.getLastScheduledTimerTask();
    assertNotSame(triggeredSampleTask, capturingSampleTask);
    capturingSampleTask.run();

    assertEquals("running 2",
      sampleModelImplementation.getState().getDescription());


    sampleModelImplementation.addTestReport(testStatisticsMap);
    capturingSampleTask.run();

    assertEquals("running 3",
      sampleModelImplementation.getState().getDescription());


    m_consoleProperties.setCollectSampleCount(2);
    capturingSampleTask.run();

    assertEquals("done", sampleModelImplementation.getState().getDescription());


    capturingSampleTask.run();
    assertEquals("done", sampleModelImplementation.getState().getDescription());
  }

  public void testReset() throws Exception {
    final SampleModelImplementation sampleModelImplementation =
      new SampleModelImplementation(m_consoleProperties,
                                    m_statisticsServices,
                                    m_timer,
                                    m_resources,
                                    m_errorHandler);

    sampleModelImplementation.addModelListener(m_listener);
    sampleModelImplementation.reset();

    m_listenerStubFactory.assertSuccess("resetTests");
  }

  public void testSampleListeners() throws Exception {
    final SampleModelImplementation sampleModelImplementation =
      new SampleModelImplementation(m_consoleProperties,
                                    m_statisticsServices,
                                    m_timer,
                                    m_resources,
                                    m_errorHandler);

    final RandomStubFactory<SampleListener> totalSampleListenerStubFactory =
      RandomStubFactory.create(SampleListener.class);
    sampleModelImplementation.addTotalSampleListener(
      totalSampleListenerStubFactory.getStub());

    final Test test1 = new StubTest(1, "test 1");
    final Test test2 = new StubTest(2, "test 2");
    final Test test3 = new StubTest(3, "test 3");
    final Test test4 = new StubTest(4, "test 4");

    final RandomStubFactory<SampleListener> sampleListenerStubFactory =
      RandomStubFactory.create(SampleListener.class);
    final SampleListener sampleListener =
      sampleListenerStubFactory.getStub();

    // Adding a listener for a test that isn't registered is a no-op.
    sampleModelImplementation.addSampleListener(test1, sampleListener);


    final Set<Test> testSet = new HashSet<Test>() { {
      add(test2);
      add(test4);
    } };

    sampleModelImplementation.registerTests(testSet);
    sampleModelImplementation.addSampleListener(test2, sampleListener);


    sampleModelImplementation.reset();
    sampleListenerStubFactory.assertNoMoreCalls();


    final TestStatisticsMap testReports = new TestStatisticsMap();
    final StatisticsSet statistics1 =
      m_statisticsServices.getStatisticsSetFactory().create();
    final LongIndex userLong0 =
      m_statisticsServices.getStatisticsIndexMap().getLongIndex("userLong0");
    final StatisticsSet statistics2 =
      m_statisticsServices.getStatisticsSetFactory().create();
    statistics1.setValue(userLong0, 99);
    statistics2.setValue(userLong0, 1);
    statistics2.setIsComposite();
    testReports.put(test2, statistics1);
    testReports.put(test3, statistics2);
    testReports.put(test4, statistics2);


    sampleModelImplementation.registerTests(testSet);
    sampleModelImplementation.addSampleListener(test2, sampleListener);
    sampleModelImplementation.addTestReport(testReports);

    totalSampleListenerStubFactory.assertNoMoreCalls();
    sampleListenerStubFactory.assertNoMoreCalls();


    final TimerTask capturingTask = m_timer.getLastScheduledTimerTask();
    capturingTask.run();

    final Object[] updateParameters =
      sampleListenerStubFactory.assertSuccess(
        "update", StatisticsSet.class, StatisticsSet.class).getParameters();
    sampleListenerStubFactory.assertNoMoreCalls();

    assertEquals(99, ((StatisticsSet)updateParameters[0]).getValue(userLong0));
    assertEquals(99, ((StatisticsSet)updateParameters[1]).getValue(userLong0));

    final Object[] totalParameters =
      totalSampleListenerStubFactory.assertSuccess(
        "update", StatisticsSet.class, StatisticsSet.class).getParameters();
    totalSampleListenerStubFactory.assertNoMoreCalls();

    assertEquals(99, ((StatisticsSet)totalParameters[0]).getValue(userLong0));
    assertEquals(99, ((StatisticsSet)totalParameters[1]).getValue(userLong0));


    capturingTask.run();

    final Object[] updateParameters2 =
      sampleListenerStubFactory.assertSuccess(
        "update", StatisticsSet.class, StatisticsSet.class).getParameters();
    sampleListenerStubFactory.assertNoMoreCalls();

    assertEquals(0, ((StatisticsSet)updateParameters2[0]).getValue(userLong0));
    assertEquals(99, ((StatisticsSet)updateParameters2[1]).getValue(userLong0));

    final Object[] totalParameters2 =
      totalSampleListenerStubFactory.assertSuccess(
        "update", StatisticsSet.class, StatisticsSet.class).getParameters();
    totalSampleListenerStubFactory.assertNoMoreCalls();

    assertEquals(0, ((StatisticsSet)totalParameters2[0]).getValue(userLong0));
    assertEquals(99, ((StatisticsSet)totalParameters2[1]).getValue(userLong0));


    // Now put into the triggered state.
    sampleModelImplementation.start();
    m_consoleProperties.setIgnoreSampleCount(10);
    statistics1.setValue(userLong0, 3);

    sampleModelImplementation.addTestReport(testReports);

    final TimerTask triggeredTask = m_timer.getLastScheduledTimerTask();
    triggeredTask.run();

    final Object[] updateParameters3 =
      sampleListenerStubFactory.assertSuccess(
        "update", StatisticsSet.class, StatisticsSet.class).getParameters();
    sampleListenerStubFactory.assertNoMoreCalls();

    assertEquals(3, ((StatisticsSet)updateParameters3[0]).getValue(userLong0));
    assertEquals(0, ((StatisticsSet)updateParameters3[1]).getValue(userLong0));

    final Object[] totalParameters3 =
      totalSampleListenerStubFactory.assertSuccess(
        "update", StatisticsSet.class, StatisticsSet.class).getParameters();
    totalSampleListenerStubFactory.assertNoMoreCalls();

    assertEquals(3, ((StatisticsSet)totalParameters3[0]).getValue(userLong0));
    assertEquals(0, ((StatisticsSet)totalParameters3[1]).getValue(userLong0));
  }

  public void testAbstractListener() {
    // An exercise in coverage.
    final AbstractListener listener = new AbstractListener() {};

    listener.newTests(null, null);
    listener.resetTests();
    listener.newSample();
    listener.stateChanged();
  }
}
