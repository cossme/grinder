// Copyright (C) 2006 - 2013 Philip Aston
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import net.grinder.common.GrinderProperties;
import net.grinder.common.SSLContextFactory;
import net.grinder.common.StubTest;
import net.grinder.common.ThreadLifeCycleListener;
import net.grinder.script.Statistics.StatisticsForTest;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;


/**
 * Unit tests for {@link ThreadContextImplementation}.
 *
 * @author Philip Aston
 */
public class TestThreadContextImplementation {

  @Mock private GrinderProperties m_properties;
  @Mock private SSLContextFactory m_sslContextFactory;
  @Mock private DispatchContext m_dispatchContext;
  @Mock private StatisticsForTest m_statisticsForTest;
  @Mock private Logger m_dataLogger;

  private final StatisticsServices m_statisticsServices =
    StatisticsServicesImplementation.getInstance();

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test public void testBasics() throws Exception {
    final ThreadContext threadContext =
      new ThreadContextImplementation(m_properties,
                                      m_statisticsServices,
                                      13,
                                      null);

    assertEquals(13, threadContext.getThreadNumber());
    assertEquals(-1, threadContext.getRunNumber());

    assertNull(threadContext.getThreadSSLContextFactory());
    threadContext.setThreadSSLContextFactory(m_sslContextFactory);
    assertSame(m_sslContextFactory, threadContext.getThreadSSLContextFactory());
  }

  @Test public void testDispatchResultReporter() throws Exception {

    final ThreadContext threadContext =
      new ThreadContextImplementation(m_properties,
                                      m_statisticsServices,
                                      1,
                                      m_dataLogger);

    final DispatchResultReporter dispatchResultReporter =
      threadContext.getDispatchResultReporter();

    final net.grinder.common.Test test = new StubTest(22, "test");

    final StatisticsSet statistics =
      m_statisticsServices.getStatisticsSetFactory().create();

    dispatchResultReporter.report(test, 123456, statistics);

    verify(m_dataLogger).info(and(contains("22"),
                                  contains("123456")),
                              isA(DataLogArguments.class));
  }

  @Test public void testNullDispatchResultReporter() throws Exception {

    when(m_properties.getProperty("grinder.logData")).thenReturn("false");

    final ThreadContext threadContext =
      new ThreadContextImplementation(m_properties,
                                      m_statisticsServices,
                                      1,
                                      m_dataLogger);

    final DispatchResultReporter dispatchResultReporter =
      threadContext.getDispatchResultReporter();

    final net.grinder.common.Test test = new StubTest(22, "test");

    final StatisticsSet statistics =
      m_statisticsServices.getStatisticsSetFactory().create();

    dispatchResultReporter.report(test, 123456, statistics);

    verifyNoMoreInteractions(m_dataLogger);
  }

  @Test public void testDispatchContext() throws Exception {
    final ThreadContext threadContext =
      new ThreadContextImplementation(m_properties,
                                      m_statisticsServices,
                                      1,
                                      null);

    assertNull(threadContext.getStatisticsForCurrentTest());
    assertNull(threadContext.getStatisticsForLastTest());

    try {
      threadContext.popDispatchContext();
      fail("Expected AssertionError");
    }
    catch (final AssertionError e) {
    }

    when(m_dispatchContext.getTest()).thenReturn(new StubTest(14, "test"));

    when(m_dispatchContext.getStatisticsForTest())
      .thenReturn(m_statisticsForTest);

    threadContext.pushDispatchContext(m_dispatchContext);

    verify(m_dispatchContext).getLogMarker();

    assertSame(m_statisticsForTest,
               threadContext.getStatisticsForCurrentTest());
    assertNull(threadContext.getStatisticsForLastTest());

    verify(m_dispatchContext).getStatisticsForTest();
    verifyNoMoreInteractions(m_dispatchContext);

    threadContext.popDispatchContext();

    verify(m_dispatchContext, times(2)).getStatisticsForTest();
    verify(m_dispatchContext).report();
    verifyNoMoreInteractions(m_dispatchContext);

    assertSame(m_statisticsForTest, threadContext.getStatisticsForLastTest());
    assertNull(threadContext.getStatisticsForCurrentTest());

    final DispatchContext anotherDispatchContext = mock(DispatchContext.class);
    when(anotherDispatchContext.getTest())
      .thenReturn(new StubTest(3, "another test"));

    final StopWatch stopWatch2 = mock(StopWatch.class);
    when(m_dispatchContext.getPauseTimer()).thenReturn(stopWatch2);

    final StopWatch stopWatch = mock(StopWatch.class);
    when(anotherDispatchContext.getPauseTimer()).thenReturn(stopWatch);

    threadContext.pushDispatchContext(anotherDispatchContext);
    threadContext.pushDispatchContext(m_dispatchContext);

    threadContext.popDispatchContext();

    verify(m_dispatchContext).getPauseTimer();
    verify(m_dispatchContext, times(3)).getStatisticsForTest();
    verify(m_dispatchContext, times(2)).report();

    verify(anotherDispatchContext).getPauseTimer();

    verify(stopWatch).add(stopWatch2);

    verifyNoMoreInteractions(stopWatch, stopWatch2);

    threadContext.fireBeginThreadEvent();
    threadContext.fireBeginRunEvent();
    threadContext.fireEndRunEvent();
    threadContext.fireBeginShutdownEvent();
    threadContext.fireEndThreadEvent();

    try {
      threadContext.popDispatchContext();
      fail("Expected AssertionError");
    }
    catch (final AssertionError e) {
    }
  }

  @Test public void testEvents() throws Exception {
    final ThreadLifeCycleListener threadLifeCycleListener =
      mock(ThreadLifeCycleListener.class);

    final ThreadContext threadContext =
      new ThreadContextImplementation(m_properties,
                                      m_statisticsServices,
                                      1,
                                      null);

    threadContext.registerThreadLifeCycleListener(threadLifeCycleListener);

    threadContext.fireBeginThreadEvent();
    verify(threadLifeCycleListener).beginThread();

    threadContext.fireBeginRunEvent();
    verify(threadLifeCycleListener).beginRun();

    threadContext.fireEndRunEvent();
    verify(threadLifeCycleListener).endRun();

    threadContext.fireBeginShutdownEvent();
    verify(threadLifeCycleListener).beginShutdown();

    threadContext.fireEndThreadEvent();
    verify(threadLifeCycleListener).endThread();

    verifyNoMoreInteractions(threadLifeCycleListener);

    threadContext.fireEndThreadEvent();
    verify(threadLifeCycleListener, times(2)).endThread();
    verifyNoMoreInteractions(threadLifeCycleListener);

    threadContext.removeThreadLifeCycleListener(threadLifeCycleListener);

    threadContext.fireEndThreadEvent();
    verifyNoMoreInteractions(threadLifeCycleListener);
  }

  @Test public void testDelayReports() throws Exception {
    final ThreadContext threadContext =
      new ThreadContextImplementation(m_properties,
                                      m_statisticsServices,
                                      1,
                                      null);

    when(m_dispatchContext.getTest()).thenReturn(new StubTest(14, "test"));
    when(m_dispatchContext.getStatisticsForTest())
    .thenReturn(m_statisticsForTest);

    threadContext.pushDispatchContext(m_dispatchContext);
    verify(m_dispatchContext).getLogMarker();

    threadContext.setDelayReports(false);
    threadContext.setDelayReports(true);
    verifyNoMoreInteractions(m_dispatchContext);

    assertSame(m_statisticsForTest,
               threadContext.getStatisticsForCurrentTest());
    assertNull(threadContext.getStatisticsForLastTest());

    verify(m_dispatchContext).getStatisticsForTest();

    threadContext.popDispatchContext();
    verify(m_dispatchContext, times(2)).getStatisticsForTest();

    assertNotNull(threadContext.getStatisticsForLastTest());
    assertNull(threadContext.getStatisticsForCurrentTest());

    // Now have a pending context.

    threadContext.reportPendingDispatchContext();
    verify(m_dispatchContext).report();
    threadContext.reportPendingDispatchContext();
    verifyNoMoreInteractions(m_dispatchContext);

    // Test flush at beginning of next test (same test)
    threadContext.pushDispatchContext(m_dispatchContext);
    verify(m_dispatchContext, times(2)).getLogMarker();
    threadContext.popDispatchContext();
    verify(m_dispatchContext, times(3)).getStatisticsForTest();
    threadContext.pushDispatchContext(m_dispatchContext);
    verify(m_dispatchContext, times(2)).report();
    verify(m_dispatchContext, times(3)).getLogMarker();
    threadContext.popDispatchContext();
    verify(m_dispatchContext, times(4)).getStatisticsForTest();
    threadContext.reportPendingDispatchContext();
    verify(m_dispatchContext, times(3)).report();

    // Test flush at beginning of next test (different test).
    threadContext.pushDispatchContext(m_dispatchContext);
    verify(m_dispatchContext, times(4)).getLogMarker();
    threadContext.popDispatchContext();

    when(m_dispatchContext.getTest()).thenReturn(new StubTest(16, "abc"));

    verify(m_dispatchContext, times(5)).getStatisticsForTest();
    threadContext.pushDispatchContext(m_dispatchContext);
    verify(m_dispatchContext, times(4)).report();
    verify(m_dispatchContext, times(5)).getLogMarker();
    threadContext.popDispatchContext();
    verify(m_dispatchContext, times(6)).getStatisticsForTest();
    threadContext.reportPendingDispatchContext();
    verify(m_dispatchContext, times(5)).report();

    // Test flushed at end of run.
    threadContext.pushDispatchContext(m_dispatchContext);
    verify(m_dispatchContext, times(6)).getLogMarker();
    threadContext.popDispatchContext();
    verify(m_dispatchContext, times(7)).getStatisticsForTest();
    threadContext.fireBeginRunEvent();
    verifyNoMoreInteractions(m_dispatchContext);
    threadContext.fireEndRunEvent();
    verify(m_dispatchContext, times(6)).report();

    // Test flushed if delay reports is turned off.
    threadContext.pushDispatchContext(m_dispatchContext);
    verify(m_dispatchContext, times(7)).getLogMarker();
    threadContext.popDispatchContext();
    verify(m_dispatchContext, times(8)).getStatisticsForTest();
    verifyNoMoreInteractions(m_dispatchContext);
    threadContext.setDelayReports(false);
    verify(m_dispatchContext, times(7)).report();
    verifyNoMoreInteractions(m_dispatchContext);
  }

  @Test public void testDispatchContextWhenShuttingDown() throws Exception {
    final ThreadContext threadContext =
      new ThreadContextImplementation(m_properties,
                                      m_statisticsServices,
                                      1,
                                      null);
    threadContext.shutdown();

    for (int i=0; i<2; ++i) {
      try {
        threadContext.pushDispatchContext(m_dispatchContext);
        fail("Expected ShutdownException");
      }
      catch (final ShutdownException e) {
      }

      threadContext.popDispatchContext(); // No-op.

      verifyNoMoreInteractions(m_dispatchContext);
    }
  }

  @Test public void testWithBadDispatchContext() throws Exception {
    final ThreadContext threadContext =
      new ThreadContextImplementation(m_properties,
                                      m_statisticsServices,
                                      1,
                                      null);

    when(m_dispatchContext.getTest()).thenReturn(new StubTest(14, "test"));

    final Throwable t = new DispatchContext.DispatchStateException("foo");
    doThrow(t).when(m_dispatchContext).report();

    threadContext.pushDispatchContext(m_dispatchContext);

    try {
      threadContext.popDispatchContext();
      fail("Expected AssertionError");
    }
    catch (final AssertionError e) {
      assertSame(t, e.getCause());
    }
  }

  @Test public void testWithBadPendingDispatchContext() throws Exception {
    final ThreadContext threadContext =
      new ThreadContextImplementation(m_properties,
                                      m_statisticsServices,
                                      1,
                                      null);

    when(m_dispatchContext.getTest()).thenReturn(new StubTest(14, "test"));

    threadContext.setDelayReports(true);

    final Throwable t = new DispatchContext.DispatchStateException("foo");
    doThrow(t).when(m_dispatchContext).report();

    threadContext.pushDispatchContext(m_dispatchContext);
    threadContext.popDispatchContext();

    try {
      threadContext.reportPendingDispatchContext();
      fail("Expected AssertionError");
    }
    catch (final AssertionError e) {
      assertSame(t, e.getCause());
    }
  }

  @Test public void testGetLogMarker() throws Exception {
    final ThreadContext threadContext =
      new ThreadContextImplementation(m_properties,
                                      m_statisticsServices,
                                      2,
                                      null);

    final Marker marker = threadContext.getLogMarker();
    assertEquals("thread-2", marker.getName());
    assertFalse(marker.hasReferences());
  }

  @Test public void testGetLogMarkerWithRun() throws Exception {
    final ThreadContext threadContext =
      new ThreadContextImplementation(m_properties,
                                      m_statisticsServices,
                                      2,
                                      null);

    threadContext.setCurrentRunNumber(3);

    final Marker marker = threadContext.getLogMarker();
    assertEquals("thread-2", marker.getName());
    assertTrue(marker.contains("run-3"));

    threadContext.setCurrentRunNumber(4);
    assertTrue(marker.contains("run-4"));

    threadContext.setCurrentRunNumber(-1);
    assertFalse(marker.hasReferences());
  }

  @Test public void testGetLogMarkerWithTest() throws Exception {
    final ThreadContextImplementation threadContext =
      new ThreadContextImplementation(m_properties,
                                      m_statisticsServices,
                                      2,
                                      null);

    threadContext.setTestLogMarker(MarkerFactory.getMarker("test-3"));

    final Marker marker = threadContext.getLogMarker();
    assertEquals("thread-2", marker.getName());
    assertTrue(marker.contains("test-3"));

    threadContext.setTestLogMarker(MarkerFactory.getMarker("test-4"));
    assertTrue(marker.contains("test-4"));

    threadContext.setTestLogMarker(null);
    assertFalse(marker.hasReferences());
  }
}
