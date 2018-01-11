// Copyright (C) 2008 - 2013 Philip Aston
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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import net.grinder.common.GrinderProperties;
import net.grinder.engine.common.EngineException;
import net.grinder.scriptengine.ScriptEngineService.WorkerRunnable;
import net.grinder.scriptengine.ScriptExecutionException;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.util.Sleeper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;


/**
 * Unit tests for {@link GrinderThread}.
 *
 * @author Philip Aston
 */
public class TestGrinderThread extends AbstractJUnit4FileTestCase {

  @Mock private Logger m_logger;
  @Mock private ThreadContext m_threadContext;
  @Mock private WorkerThreadSynchronisation m_workerThreadSynchronisation;
  @Mock private Sleeper m_sleeper;
  @Mock private WorkerRunnableFactory m_workerRunnableFactory;
  @Mock private WorkerRunnable m_workerRunnable;

  private final GrinderProperties m_properties = new GrinderProperties();


  @Before public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(m_workerRunnableFactory.create()).thenReturn(m_workerRunnable);
  }

  @Test public void testConstruction() throws Exception {

    new GrinderThread(m_logger,
                      m_threadContext,
                      m_workerThreadSynchronisation,
                      m_properties,
                      m_sleeper,
                      m_workerRunnableFactory);

    verify(m_workerThreadSynchronisation).threadCreated();

    verifyNoMoreInteractions(m_threadContext);
  }

  @Test public void testRun() throws Exception {

    final GrinderThread grinderThread =
      new GrinderThread(m_logger,
                        m_threadContext,
                        m_workerThreadSynchronisation,
                        m_properties,
                        m_sleeper,
                        m_workerRunnableFactory);

    grinderThread.run();

    verify(m_threadContext).fireBeginThreadEvent();
    verify(m_threadContext).fireBeginRunEvent();
    verify(m_threadContext).fireEndRunEvent();
    verify(m_threadContext).fireBeginShutdownEvent();
    verify(m_threadContext).fireEndThreadEvent();

    verify(m_workerRunnable).run();
    verify(m_workerRunnable).shutdown();

    verify(m_workerThreadSynchronisation).threadCreated();
    verify(m_workerThreadSynchronisation).awaitStart();
    verify(m_workerThreadSynchronisation).threadFinished();

    verify(m_sleeper).sleepFlat(0);
    verifyNoMoreInteractions(m_sleeper);
  }

  @Test public void testMultipleRuns() throws Exception {

    final GrinderThread grinderThread =
      new GrinderThread(m_logger,
                        m_threadContext,
                        m_workerThreadSynchronisation,
                        m_properties,
                        m_sleeper,
                        m_workerRunnableFactory);

    m_properties.setInt("grinder.runs", 2);
    m_properties.setLong("grinder.initialSleepTime", 100);

    grinderThread.run();

    verify(m_threadContext).fireBeginThreadEvent();
    verify(m_threadContext, times(2)).fireBeginRunEvent();
    verify(m_threadContext, times(2)).fireEndRunEvent();
    verify(m_threadContext).fireBeginShutdownEvent();
    verify(m_threadContext).fireEndThreadEvent();

    verify(m_workerRunnable, times(2)).run();
    verify(m_workerRunnable).shutdown();

    verify(m_workerThreadSynchronisation).threadCreated();
    verify(m_workerThreadSynchronisation).awaitStart();
    verify(m_workerThreadSynchronisation).threadFinished();

    verify(m_sleeper).sleepFlat(100);
    verifyNoMoreInteractions(m_sleeper);
  }

  @Test public void testRunForeverShutdownException() throws Exception {

    final GrinderThread grinderThread =
      new GrinderThread(m_logger,
                        m_threadContext,
                        m_workerThreadSynchronisation,
                        m_properties,
                        m_sleeper,
                        m_workerRunnableFactory);

    m_properties.setInt("grinder.runs", 0);

    doThrow(new MyScriptEngineException(new ShutdownException("bye")))
      .when(m_workerRunnable).run();

    grinderThread.run();

    verify(m_threadContext).fireBeginThreadEvent();
    verify(m_threadContext).fireBeginRunEvent();
    verify(m_threadContext, never()).fireEndRunEvent();
    verify(m_threadContext).fireBeginShutdownEvent();
    verify(m_threadContext).fireEndThreadEvent();

    verify(m_workerRunnable).run();
    verify(m_workerRunnable).shutdown();

    verify(m_workerThreadSynchronisation).threadCreated();
    verify(m_workerThreadSynchronisation).awaitStart();
    verify(m_workerThreadSynchronisation).threadFinished();

    verify(m_sleeper).sleepFlat(0);
    verifyNoMoreInteractions(m_sleeper);

    // TODO verify(m_threadLogger).output("shut down");
  }

  @Test public void testRunScriptException() throws Exception {

    final GrinderThread grinderThread =
      new GrinderThread(m_logger,
                        m_threadContext,
                        m_workerThreadSynchronisation,
                        m_properties,
                        m_sleeper,
                        m_workerRunnableFactory);

    doThrow(new MyScriptEngineException("whatever"))
      .when(m_workerRunnable).run();
    doThrow(new MyScriptEngineException("whatever"))
      .when(m_workerRunnable).shutdown();

    grinderThread.run();

    verify(m_threadContext).fireBeginThreadEvent();
    verify(m_threadContext).fireBeginRunEvent();
    verify(m_threadContext).fireEndRunEvent();
    verify(m_threadContext).fireBeginShutdownEvent();
    verify(m_threadContext).fireEndThreadEvent();

    verify(m_workerThreadSynchronisation).threadCreated();
    verify(m_workerThreadSynchronisation).awaitStart();
    verify(m_workerThreadSynchronisation).threadFinished();

    // TODO verify(m_threadLogger).error(and(contains("Aborted run"),
    //                                 contains("short message")));
  }

  @Test public void testRunScriptEngineException() throws Exception {

    final GrinderThread grinderThread =
      new GrinderThread(m_logger,
                        m_threadContext,
                        m_workerThreadSynchronisation,
                        m_properties,
                        m_sleeper,
                        m_workerRunnableFactory);

    when(m_workerRunnableFactory.create())
      .thenThrow(new MyScriptEngineException("blah"));

    grinderThread.run();

    verify(m_threadContext).fireBeginThreadEvent();

    verify(m_workerThreadSynchronisation).threadCreated();
    verify(m_workerThreadSynchronisation).threadFinished();
    verifyNoMoreInteractions(m_workerThreadSynchronisation);

    // TODO verify(m_threadLogger).error(contains("Aborting thread: short message"));
  }

  @Test public void testRunSomeOtherException() throws Exception {

    final GrinderThread grinderThread =
      new GrinderThread(m_logger,
                        m_threadContext,
                        m_workerThreadSynchronisation,
                        m_properties,
                        m_sleeper,
                        m_workerRunnableFactory);

    when(m_workerRunnableFactory.create())
      .thenThrow(new EngineException("blah"));

    grinderThread.run();

    verify(m_threadContext).fireBeginThreadEvent();

    verify(m_workerThreadSynchronisation).threadCreated();
    verify(m_workerThreadSynchronisation).threadFinished();
    verifyNoMoreInteractions(m_workerThreadSynchronisation);

    // TODO verify(m_threadLogger).error(and(contains("Aborting thread:"),
    //                                 contains("blah")));
  }

  private static final class MyScriptEngineException
    extends ScriptExecutionException {
    public MyScriptEngineException(final Throwable t) {
      super("whoops", t);
    }

    public MyScriptEngineException(final String message) {
      super(message);
    }

    @Override
    public String getShortMessage() {
      return "short message";
    }
  }
}
