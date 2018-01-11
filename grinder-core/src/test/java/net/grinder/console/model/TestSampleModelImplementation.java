// Copyright (C) 2008 - 2013 Philip Aston
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

import static java.util.Collections.emptySet;
import static java.util.Collections.sort;
import static net.grinder.console.model.SampleModel.State.Value.IgnoringInitialSamples;
import static net.grinder.console.model.SampleModel.State.Value.Recording;
import static net.grinder.console.model.SampleModel.State.Value.Stopped;
import static net.grinder.console.model.SampleModel.State.Value.WaitingForFirstReport;
import static net.grinder.testutility.AssertUtilities.asSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

import net.grinder.common.StubTest;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.model.SampleModel.AbstractListener;
import net.grinder.console.model.SampleModel.Listener;
import net.grinder.console.model.SampleModel.State;
import net.grinder.statistics.StatisticExpression;
import net.grinder.statistics.StatisticsIndexMap.LongIndex;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.StubTimer;
import net.grinder.translation.Translations;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

/**
 * Unit tests for {@link SampleModelImplementation}.
 *
 * @author Philip Aston
 */
public class TestSampleModelImplementation extends AbstractJUnit4FileTestCase {

  private ConsoleProperties m_consoleProperties;

  private final StatisticsServices m_statisticsServices =
      StatisticsServicesImplementation.getInstance();

  private StubTimer m_timer;

  @Mock
  private Translations m_translations;

  @Mock
  private Listener m_listener;

  @Captor
  private ArgumentCaptor<Set<net.grinder.common.Test>> m_testSetCaptor;

  @Captor
  private ArgumentCaptor<ModelTestIndex> m_modelTestIndexCaptor;

  @Mock
  private ErrorHandler m_errorHandler;

  @Captor
  private ArgumentCaptor<StatisticsSet> m_statisicsSetCaptor1;

  @Captor
  private ArgumentCaptor<StatisticsSet> m_statisicsSetCaptor2;

  @Mock
  private SampleListener m_totalSampleListener;

  private final net.grinder.common.Test m_test1 = new StubTest(1, "test 1");

  private final net.grinder.common.Test m_test2 = new StubTest(2, "test 2");

  private final net.grinder.common.Test m_test3 = new StubTest(3, "test 3");

  private final net.grinder.common.Test m_test4 = new StubTest(4, "test 4");

  private SampleModelImplementation m_sampleModelImplementation;

  @Before
  public void setUp() throws Exception {
    initMocks(this);

    when(m_translations.translate("console.state/ignoring-samples"))
        .thenReturn("whatever");
    when(m_translations.translate("console.state/waiting-for-samples"))
        .thenReturn("waiting, waiting, waiting");
    when(m_translations.translate("console.state/collection-stopped"))
        .thenReturn("done");
    when(m_translations.translate("console.state/capturing-samples"))
        .thenReturn("running");

    m_timer = new StubTimer();
    m_consoleProperties =
        new ConsoleProperties(m_translations,
          new File(getDirectory(), "props"));

    m_sampleModelImplementation =
        new SampleModelImplementation(m_consoleProperties,
          m_statisticsServices,
          m_timer,
          m_translations,
          m_errorHandler);

    m_sampleModelImplementation.addTotalSampleListener(m_totalSampleListener);
  }

  @After
  public void cancelTimer() throws Exception {
    m_timer.cancel();
  }

  @Test
  public void testConstruction() throws Exception {
    final StatisticExpression tpsExpression =
        m_sampleModelImplementation.getTPSExpression();
    assertNotNull(tpsExpression);
    assertSame(tpsExpression, m_sampleModelImplementation.getTPSExpression());

    final StatisticExpression peakTPSExpression =
        m_sampleModelImplementation.getPeakTPSExpression();
    assertNotNull(peakTPSExpression);
    assertSame(
      peakTPSExpression, m_sampleModelImplementation.getPeakTPSExpression());
    assertNotSame(tpsExpression, peakTPSExpression);

    final StatisticsSet totalCumulativeStatistics =
        m_sampleModelImplementation.getTotalCumulativeStatistics();
    assertNotNull(totalCumulativeStatistics);
    assertSame(totalCumulativeStatistics,
      m_sampleModelImplementation.getTotalCumulativeStatistics());

    final StatisticsSet totalLatestStatistics =
        m_sampleModelImplementation.getTotalLatestStatistics();
    assertNotNull(totalLatestStatistics);
    assertSame(totalLatestStatistics,
      m_sampleModelImplementation.getTotalLatestStatistics());

    final State state = m_sampleModelImplementation.getState();
    assertEquals(WaitingForFirstReport, state.getValue());
    assertEquals("waiting, waiting, waiting", state.getDescription());
    assertNull(m_timer.getLastScheduledTimerTask());
  }

  @Test
  public void testRegisterTests() throws Exception {

    m_sampleModelImplementation.addModelListener(m_listener);

    final Set<net.grinder.common.Test> emptySet = emptySet();
    m_sampleModelImplementation.registerTests(emptySet);

    final List<net.grinder.common.Test> testList =
        new ArrayList<net.grinder.common.Test>() {
          {
            add(m_test2);
            add(m_test1);
            add(m_test3);
          }
        };

    m_sampleModelImplementation.registerTests(testList);

    verify(m_listener).newTests(m_testSetCaptor.capture(),
      m_modelTestIndexCaptor.capture());

    final Set<net.grinder.common.Test> callbackTestSet =
        m_testSetCaptor.getValue();
    assertTrue(testList.containsAll(callbackTestSet));
    assertTrue(callbackTestSet.containsAll(testList));

    final ModelTestIndex modelIndex = m_modelTestIndexCaptor.getValue();
    assertEquals(testList.size(), modelIndex.getNumberOfTests());
    assertEquals(testList.size(), modelIndex.getAccumulatorArray().length);

    sort(testList);

    for (int i = 0; i < modelIndex.getNumberOfTests(); ++i) {
      assertEquals(testList.get(i), modelIndex.getTest(i));
    }

    final List<net.grinder.common.Test> testList2 =
        new ArrayList<net.grinder.common.Test>() {
          {
            add(m_test2);
            add(m_test4);
          }
        };

    reset(m_listener);

    m_sampleModelImplementation.registerTests(testList2);

    verify(m_listener).newTests(m_testSetCaptor.capture(),
      m_modelTestIndexCaptor.capture());

    final Set<net.grinder.common.Test> expectedNewTests =
        new HashSet<net.grinder.common.Test>() {
          {
            add(m_test4);
          }
        };

    final Set<net.grinder.common.Test> callbackTestSet2 =
        m_testSetCaptor.getValue();
    assertTrue(expectedNewTests.containsAll(callbackTestSet2));
    assertTrue(callbackTestSet2.containsAll(expectedNewTests));

    final ModelTestIndex modelIndex2 = m_modelTestIndexCaptor.getValue();
    assertEquals(4, modelIndex2.getNumberOfTests());
    assertEquals(4, modelIndex2.getAccumulatorArray().length);

    m_sampleModelImplementation.registerTests(testList2);

    verifyNoMoreInteractions(m_listener);
  }

  @Test
  public void testWaitingToStopped() throws Exception {

    m_sampleModelImplementation.addModelListener(m_listener);

    final State state = m_sampleModelImplementation.getState();
    assertEquals(WaitingForFirstReport, state.getValue());
    assertEquals("waiting, waiting, waiting", state.getDescription());

    verifyNoMoreInteractions(m_listener);

    m_sampleModelImplementation.stop();

    verify(m_listener).stateChanged();

    final State stoppedState = m_sampleModelImplementation.getState();
    assertEquals(Stopped, stoppedState.getValue());
    assertEquals("done", stoppedState.getDescription());

    m_sampleModelImplementation.addTestReport(new TestStatisticsMap());

    final State stoppedState2 = m_sampleModelImplementation.getState();
    assertEquals(Stopped, stoppedState2.getValue());
    assertEquals("done", stoppedState2.getDescription());

    assertNull(m_timer.getLastScheduledTimerTask());
  }

  @Test
  public void testWaitingToTriggeredToCapturingToStopped()
      throws Exception {

    final TestStatisticsMap testStatisticsMap = new TestStatisticsMap();

    m_sampleModelImplementation.addModelListener(m_listener);

    final State waitingState = m_sampleModelImplementation.getState();
    assertEquals(WaitingForFirstReport, waitingState.getValue());
    assertEquals("waiting, waiting, waiting", waitingState.getDescription());

    verifyNoMoreInteractions(m_listener);

    m_consoleProperties.setIgnoreSampleCount(10);

    m_sampleModelImplementation.addTestReport(testStatisticsMap);

    final State triggeredState = m_sampleModelImplementation.getState();
    assertEquals(IgnoringInitialSamples, triggeredState.getValue());
    assertEquals("whatever: 1", triggeredState.getDescription());

    final TimerTask triggeredSampleTask = m_timer.getLastScheduledTimerTask();
    triggeredSampleTask.run();

    assertEquals("whatever: 2", triggeredState.getDescription());

    m_sampleModelImplementation.addTestReport(testStatisticsMap);
    m_sampleModelImplementation.addTestReport(testStatisticsMap);
    m_sampleModelImplementation.addTestReport(testStatisticsMap);
    triggeredSampleTask.run();

    assertEquals("whatever: 3",
      m_sampleModelImplementation.getState().getDescription());

    for (int i = 0; i < 4; ++i) {
      triggeredSampleTask.run();
    }

    assertEquals("whatever: 7",
      m_sampleModelImplementation.getState().getDescription());

    triggeredSampleTask.run();

    assertEquals("whatever: 8",
      m_sampleModelImplementation.getState().getDescription());
    assertEquals(IgnoringInitialSamples,
      m_sampleModelImplementation.getState().getValue());

    for (int i = 0; i < 3; ++i) {
      m_sampleModelImplementation.addTestReport(testStatisticsMap);
      triggeredSampleTask.run();
    }

    final State capturingState = m_sampleModelImplementation.getState();
    assertEquals(Recording, capturingState.getValue());
    assertEquals("running: 1", capturingState.getDescription());

    m_sampleModelImplementation.addTestReport(testStatisticsMap);

    assertEquals("running: 1",
      m_sampleModelImplementation.getState().getDescription());

    final TimerTask capturingSampleTask = m_timer.getLastScheduledTimerTask();
    assertNotSame(triggeredSampleTask, capturingSampleTask);
    capturingSampleTask.run();

    assertEquals("running: 2",
      m_sampleModelImplementation.getState().getDescription());

    m_sampleModelImplementation.addTestReport(testStatisticsMap);
    capturingSampleTask.run();

    assertEquals("running: 3",
      m_sampleModelImplementation.getState().getDescription());

    m_consoleProperties.setCollectSampleCount(2);
    capturingSampleTask.run();

    assertEquals("done",
      m_sampleModelImplementation.getState().getDescription());

    capturingSampleTask.run();
    assertEquals("done",
      m_sampleModelImplementation.getState().getDescription());
  }

  @Test
  public void testReset() throws Exception {

    m_sampleModelImplementation.addModelListener(m_listener);
    m_sampleModelImplementation.reset();

    verify(m_listener).resetTests();
  }

  @Test
  public void testSampleListenersZeroStatistics() throws Exception {

    final SampleListener sampleListener = mock(SampleListener.class);
    final SampleListener sampleListener2 = mock(SampleListener.class);

    // Adding a listener for a test that isn't registered is a no-op.
    m_sampleModelImplementation.addSampleListener(m_test1, sampleListener);

    final Set<net.grinder.common.Test> testSet = asSet(m_test2, m_test4);
    m_sampleModelImplementation.registerTests(testSet);
    m_sampleModelImplementation.addSampleListener(m_test2, sampleListener2);

    // Also no-op.
    m_sampleModelImplementation.addSampleListener(m_test3, sampleListener);

    m_sampleModelImplementation.zeroStatistics();
    verify(sampleListener2).update(m_statisicsSetCaptor1.capture(),
      m_statisicsSetCaptor2.capture());
    verify(m_totalSampleListener).update(m_statisicsSetCaptor1.capture(),
      m_statisicsSetCaptor2.capture());

    verifyNoMoreInteractions(sampleListener,
      sampleListener2,
      m_totalSampleListener);
  }

  @Test
  public void testSampleListenersReset() throws Exception {

    final SampleListener sampleListener = mock(SampleListener.class);

    // Adding a listener for a test that isn't registered is a no-op.
    m_sampleModelImplementation.addSampleListener(m_test1, sampleListener);

    final Set<net.grinder.common.Test> testSet = asSet(m_test2, m_test4);

    m_sampleModelImplementation.registerTests(testSet);
    m_sampleModelImplementation.addSampleListener(m_test2, sampleListener);

    m_sampleModelImplementation.reset();

    // Reset only notifies the total statistics listener.
    verify(m_totalSampleListener).update(m_statisicsSetCaptor1.capture(),
      m_statisicsSetCaptor2.capture());

    verifyNoMoreInteractions(sampleListener, m_totalSampleListener);
  }

  @Test
  public void testSampleListeners() throws Exception {

    final SampleListener sampleListener = mock(SampleListener.class);

    final Set<net.grinder.common.Test> testSet = asSet(m_test2, m_test4);

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
    testReports.put(m_test2, statistics1);
    testReports.put(m_test3, statistics2);
    testReports.put(m_test4, statistics2);

    m_sampleModelImplementation.registerTests(testSet);
    m_sampleModelImplementation.addSampleListener(m_test2, sampleListener);
    m_sampleModelImplementation.addTestReport(testReports);

    verify(sampleListener).update(m_statisicsSetCaptor1.capture(),
      m_statisicsSetCaptor2.capture());

    verify(m_totalSampleListener).update(m_statisicsSetCaptor1.capture(),
      m_statisicsSetCaptor2.capture());

    verifyNoMoreInteractions(m_totalSampleListener, sampleListener);
    reset(sampleListener, m_totalSampleListener);

    final TimerTask capturingTask = m_timer.getLastScheduledTimerTask();
    capturingTask.run();

    verify(sampleListener).update(m_statisicsSetCaptor1.capture(),
      m_statisicsSetCaptor2.capture());

    assertEquals(99, m_statisicsSetCaptor1.getValue().getValue(userLong0));
    assertEquals(99, m_statisicsSetCaptor2.getValue().getValue(userLong0));

    verify(m_totalSampleListener).update(m_statisicsSetCaptor1.capture(),
      m_statisicsSetCaptor2.capture());

    assertEquals(99, m_statisicsSetCaptor1.getValue().getValue(userLong0));
    assertEquals(99, m_statisicsSetCaptor2.getValue().getValue(userLong0));

    reset(m_totalSampleListener, sampleListener);

    capturingTask.run();

    verify(sampleListener).update(m_statisicsSetCaptor1.capture(),
      m_statisicsSetCaptor2.capture());

    assertEquals(0, m_statisicsSetCaptor1.getValue().getValue(userLong0));
    assertEquals(99, m_statisicsSetCaptor2.getValue().getValue(userLong0));

    verify(m_totalSampleListener).update(m_statisicsSetCaptor1.capture(),
      m_statisicsSetCaptor2.capture());

    assertEquals(0, m_statisicsSetCaptor1.getValue().getValue(userLong0));
    assertEquals(99, m_statisicsSetCaptor2.getValue().getValue(userLong0));

    // Now put into the triggered state.
    m_sampleModelImplementation.start();
    m_consoleProperties.setIgnoreSampleCount(10);
    statistics1.setValue(userLong0, 3);

    reset(m_totalSampleListener, sampleListener);

    m_sampleModelImplementation.addTestReport(testReports);

    final TimerTask triggeredTask = m_timer.getLastScheduledTimerTask();
    triggeredTask.run();

    verify(sampleListener).update(m_statisicsSetCaptor1.capture(),
      m_statisicsSetCaptor2.capture());

    assertEquals(3, m_statisicsSetCaptor1.getValue().getValue(userLong0));
    assertEquals(0, m_statisicsSetCaptor2.getValue().getValue(userLong0));

    verify(m_totalSampleListener).update(m_statisicsSetCaptor1.capture(),
      m_statisicsSetCaptor2.capture());

    assertEquals(3, m_statisicsSetCaptor1.getValue().getValue(userLong0));
    assertEquals(0, m_statisicsSetCaptor2.getValue().getValue(userLong0));
  }

  @Test
  public void testAbstractListener() {
    // An exercise in coverage.
    final AbstractListener listener = new AbstractListener() {
    };

    listener.newTests(null, null);
    listener.resetTests();
    listener.newSample();
    listener.stateChanged();
  }
}
