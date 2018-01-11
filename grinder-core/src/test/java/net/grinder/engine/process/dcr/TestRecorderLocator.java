// Copyright (C) 2009 - 2012 Philip Aston
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

package net.grinder.engine.process.dcr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.grinder.common.UncheckedGrinderException;
import net.grinder.engine.common.EngineException;
import net.grinder.scriptengine.Recorder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@link RecorderLocator}.
 *
 * @author Philip Aston
 */
public class TestRecorderLocator {

  @Mock private Recorder m_recorder;
  @Mock private Recorder m_recorder2;

  private final RecorderRegistry m_recorderRegistry =
    RecorderLocator.getRecorderRegistry();

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @After public void tearDown() throws Exception {
    RecorderLocator.clearRecorders();
  }

  @Test public void testNullBehaviour() throws Exception {
    RecorderLocator.enter(this, "foo");
    RecorderLocator.exit(this, "foo", false);
  }

  @Test public void testSingleRegistration() throws Exception {
    final Object target = new Object();

    m_recorderRegistry.register(target, "location", m_recorder);

    RecorderLocator.enter(target, "location");
    verify(m_recorder).start();

    RecorderLocator.exit(target, "location", true);
    verify(m_recorder).end(true);

    // Wrong target.
    RecorderLocator.enter(this, "location");
    RecorderLocator.exit(this, "location", true);

    // Wrong location.
    RecorderLocator.enter(target, "location2");
    RecorderLocator.exit(target, "location2", true);

    RecorderLocator.enter(target, "location");
    verify(m_recorder, times(2)).start();

    RecorderLocator.exit(target, "location", false);
    verify(m_recorder).end(false);

    // Interned strings shouldn't match.
    RecorderLocator.enter(target, new String("location"));
    RecorderLocator.exit(target, new String("location"), true);

    verifyNoMoreInteractions(m_recorder);
  }

  @Test public void testBadRegistration() throws Exception {
    final Object target = new Object();

    final EngineException exception = new EngineException("bork");

    Mockito.doThrow(exception).when(m_recorder).start();
    Mockito.doThrow(exception).when(m_recorder).end(false);

    m_recorderRegistry.register(target, "location", m_recorder);

    try {
      RecorderLocator.enter(target, "location");
      fail("Expected UncheckedGrinderException");
    }
    catch (final UncheckedGrinderException e) {
      assertSame(exception, e.getCause());
    }

    verify(m_recorder).start();

    try {
      RecorderLocator.exit(target, "location", false);
      fail("Expected UncheckedGrinderException");
    }
    catch (final UncheckedGrinderException e) {
      assertSame(exception, e.getCause());
    }

    verify(m_recorder).end(false);

    verifyNoMoreInteractions(m_recorder);
  }

  @Test public void testBadRegistrationTwoTargets() throws Exception {
    final Object target = new Object();
    final Object target2 = new Object();

    final EngineException exception = new EngineException("bork");

    Mockito.doThrow(exception).when(m_recorder).start();
    Mockito.doThrow(exception).when(m_recorder).end(false);

    m_recorderRegistry.register(target, target2, "location", m_recorder);

    try {
      RecorderLocator.enter(target, target2, "location");
      fail("Expected UncheckedGrinderException");
    }
    catch (final UncheckedGrinderException e) {
      assertSame(exception, e.getCause());
    }

    verify(m_recorder).start();

    try {
      RecorderLocator.exit(target, target2, "location", false);
      fail("Expected UncheckedGrinderException");
    }
    catch (final UncheckedGrinderException e) {
      assertSame(exception, e.getCause());
    }

    verify(m_recorder).end(false);

    verifyNoMoreInteractions(m_recorder);
  }

  @Test public void testMultipleRegistrations() throws Exception {
    final Object target = new Object();
    final Object target2 = new Object();

    m_recorderRegistry.register(target, "location", m_recorder);

    RecorderLocator.enter(target2, "location");
    RecorderLocator.exit(target2, "location", false);

    RecorderLocator.enter(target, "location");
    verify(m_recorder).start();

    RecorderLocator.exit(target, "location", true);
    verify(m_recorder).end(true);

    m_recorderRegistry.register(target2, "location", m_recorder2);
    m_recorderRegistry.register(target2, "location2", m_recorder);

    RecorderLocator.enter(target, "location");
    verify(m_recorder, times(2)).start();

    RecorderLocator.enter(target2, "location");
    verify(m_recorder2).start();

    RecorderLocator.exit(target, "location", true);
    verify(m_recorder, times(2)).end(true);

    RecorderLocator.exit(target2, "location", false);
    verify(m_recorder2).end(false);

    RecorderLocator.enter(target2, "location2");
    verify(m_recorder, times(3)).start();

    RecorderLocator.exit(target2, "location2", true);
    verify(m_recorder, times(3)).end(true);

    verifyNoMoreInteractions(m_recorder, m_recorder2);
  }

  @Test public void testNestedRegistrations() throws Exception {
    final Object target = new Object();

    m_recorderRegistry.register(target, "location", m_recorder);

    // Same target, location, recorder => noop.
    m_recorderRegistry.register(target, "location", m_recorder);

    m_recorderRegistry.register(target, "location", m_recorder2);

    RecorderLocator.enter(target, "location");
    verify(m_recorder).start();
    verify(m_recorder2).start();

    RecorderLocator.exit(target, "location", false);
    verify(m_recorder2).end(false);
    verify(m_recorder).end(false);

    verifyNoMoreInteractions(m_recorder, m_recorder2);
  }

  @Test public void testNestedRegistrationsTwoTargets() throws Exception {
    final Object target = new Object();
    final Object target2 = new Object();

    m_recorderRegistry.register(target, target2, "location", m_recorder);

    // Same target, location, recorder => noop.
    m_recorderRegistry.register(target, target2, "location", m_recorder);

    m_recorderRegistry.register(target, target2, "location", m_recorder2);

    RecorderLocator.enter(target, target2, "location");
    verify(m_recorder).start();
    verify(m_recorder2).start();

    RecorderLocator.exit(target, target2, "location", false);
    verify(m_recorder2).end(false);
    verify(m_recorder).end(false);

    verifyNoMoreInteractions(m_recorder, m_recorder2);
  }

  @Test public void testWithNull() throws Exception {
    final Object target = new Object();

    m_recorderRegistry.register(target, "location", m_recorder);

    RecorderLocator.enter(null, "location");

    RecorderLocator.exit(null, "location", true);

    verifyNoMoreInteractions(m_recorder);
  }

  @Test public void testWithNullTwoTargets() throws Exception {
    final Object target = new Object();
    final Object target2 = new Object();

    m_recorderRegistry.register(target, target2, "location", m_recorder);

    RecorderLocator.enter(null, "foo", "location");

    RecorderLocator.enter("foo", null, "location");

    RecorderLocator.exit(null, "foo", "location", true);

    RecorderLocator.exit("foo", null, "location", true);

    verifyNoMoreInteractions(m_recorder);
  }

  @Test public void testConcurrency() throws Exception {
    final ExecutorService executor = Executors.newCachedThreadPool();

    final AtomicInteger runs = new AtomicInteger(0);
    final AtomicInteger n = new AtomicInteger(0);

    final Recorder instrumentation = new Recorder() {
      @Override
      public void start() throws EngineException {
        n.incrementAndGet();
      }

      @Override
      public void end(final boolean success) throws EngineException {
        n.decrementAndGet();
      }
    };

    final Random random = new Random();

    final String[] locations = { "L1", "L2", "L3" };

    class RegisterInstrumentation implements Runnable {
      @Override
      public void run() {
        runs.incrementAndGet();

        final String location = locations[random.nextInt(locations.length)];

        if (random.nextInt(10) == 0) {
          m_recorderRegistry.register(this, location, instrumentation);
        }

        RecorderLocator.enter(this, location);
        RecorderLocator.exit(this, location, true);

        try {
          executor.execute(this);
        }
        catch (final RejectedExecutionException e) {
        }
      }}

    final List<Runnable> runnables = new ArrayList<Runnable>();

    for (int i = 0; i < 10; ++i) {
      runnables.add(new RegisterInstrumentation());
    }

    for (final Runnable r : runnables) {
      executor.execute(r);
    }

    while (runs.get() < 10000) {
      Thread.sleep(10);
    }
    executor.shutdown();
    assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));

    assertEquals(0, n.get());
  }
}
