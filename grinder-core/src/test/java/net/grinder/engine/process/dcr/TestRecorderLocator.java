// Copyright (C) 2009 - 2011 Philip Aston
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import net.grinder.common.UncheckedGrinderException;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.dcr.RecorderLocator;
import net.grinder.engine.process.dcr.RecorderRegistry;
import net.grinder.scriptengine.Recorder;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit tests for {@link RecorderLocator}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestRecorderLocator extends TestCase {

  private final RandomStubFactory<Recorder> m_recorderStubFactory =
    RandomStubFactory.create(Recorder.class);
  private final Recorder m_recorder = m_recorderStubFactory.getStub();

  private final RandomStubFactory<Recorder> m_recorderStubFactory2 =
      RandomStubFactory.create(Recorder.class);
  private final Recorder m_recorder2 = m_recorderStubFactory2.getStub();

  private final RecorderRegistry m_recorderRegistry =
    RecorderLocator.getRecorderRegistry();

  @Override protected void tearDown() throws Exception {
    super.tearDown();
    RecorderLocator.clearRecorders();
  }

  public void testNullBehaviour() throws Exception {
    RecorderLocator.enter(this, "foo");
    RecorderLocator.exit(this, "foo", false);
  }

  public void testSingleRegistration() throws Exception {
    final Object target = new Object();

    m_recorderRegistry.register(target, "location", m_recorder);
    m_recorderStubFactory.assertNoMoreCalls();

    RecorderLocator.enter(target, "location");
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertNoMoreCalls();

    RecorderLocator.exit(target, "location", true);
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    RecorderLocator.enter(this, "location");
    RecorderLocator.exit(this, "location", true);
    m_recorderStubFactory.assertNoMoreCalls();

    RecorderLocator.enter(target, "location2");
    RecorderLocator.exit(target, "location2", true);
    m_recorderStubFactory.assertNoMoreCalls();

    RecorderLocator.enter(target, "location");
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertNoMoreCalls();

    RecorderLocator.exit(target, "location", false);
    m_recorderStubFactory.assertSuccess("end", false);
    m_recorderStubFactory.assertNoMoreCalls();

    // Interned strings shouldn't match.
    RecorderLocator.enter(target, new String("location"));
    RecorderLocator.exit(target, new String("location"), true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  public void testBadRegistration() throws Exception {
    final Object target = new Object();

    final EngineException exception = new EngineException("bork");

    m_recorderStubFactory.setThrows("start", exception);
    m_recorderStubFactory.setThrows("end", exception);

    m_recorderRegistry.register(target, "location", m_recorder);
    m_recorderStubFactory.assertNoMoreCalls();

    try {
      RecorderLocator.enter(target, "location");
      fail("Expected UncheckedGrinderException");
    }
    catch (UncheckedGrinderException e) {
      assertSame(exception, e.getCause());
    }
    m_recorderStubFactory.assertException("start", exception);
    m_recorderStubFactory.assertNoMoreCalls();

    try {
      RecorderLocator.exit(target, "location", false);
      fail("Expected UncheckedGrinderException");
    }
    catch (UncheckedGrinderException e) {
      assertSame(exception, e.getCause());
    }
    m_recorderStubFactory.assertException("end", exception, false);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  public void testMultipleRegistrations() throws Exception {
    final Object target = new Object();
    final Object target2 = new Object();

    m_recorderRegistry.register(target, "location", m_recorder);

    RecorderLocator.enter(target2, "location");
    RecorderLocator.exit(target2, "location", false);
    m_recorderStubFactory.assertNoMoreCalls();

    RecorderLocator.enter(target, "location");
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertNoMoreCalls();

    RecorderLocator.exit(target, "location", true);
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_recorderRegistry.register(target2, "location", m_recorder2);
    m_recorderRegistry.register(target2, "location2", m_recorder);

    RecorderLocator.enter(target, "location");
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertNoMoreCalls();

    RecorderLocator.enter(target2, "location");
    m_recorderStubFactory2.assertSuccess("start");
    m_recorderStubFactory2.assertNoMoreCalls();

    RecorderLocator.exit(target, "location", true);
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    RecorderLocator.exit(target2, "location", false);
    m_recorderStubFactory2.assertSuccess("end", false);
    m_recorderStubFactory2.assertNoMoreCalls();
    m_recorderStubFactory.assertNoMoreCalls();

    RecorderLocator.enter(target2, "location2");
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertNoMoreCalls();

    RecorderLocator.exit(target2, "location2", true);
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  public void testNestedRegistrations() throws Exception {
    final Object target = new Object();

    m_recorderRegistry.register(target, "location", m_recorder);

    // Same target, location, recorder => noop.
    m_recorderRegistry.register(target, "location", m_recorder);

    m_recorderRegistry.register(target, "location", m_recorder2);

    RecorderLocator.enter(target, "location");
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory2.assertSuccess("start");
    m_recorderStubFactory.assertNoMoreCalls();
    m_recorderStubFactory2.assertNoMoreCalls();

    RecorderLocator.exit(target, "location", false);
    m_recorderStubFactory2.assertSuccess("end", false);
    m_recorderStubFactory.assertSuccess("end", false);
    m_recorderStubFactory.assertNoMoreCalls();
    m_recorderStubFactory2.assertNoMoreCalls();
  }

  public void testWithNull() throws Exception {
    final Object target = new Object();

    m_recorderRegistry.register(target, "location", m_recorder);
    m_recorderStubFactory.assertNoMoreCalls();

    RecorderLocator.enter(null, "location");
    m_recorderStubFactory.assertNoMoreCalls();

    RecorderLocator.exit(null, "location", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  public void testConcurrency() throws Exception {
    final ExecutorService executor = Executors.newCachedThreadPool();

    final AtomicInteger runs = new AtomicInteger(0);
    final AtomicInteger n = new AtomicInteger(0);

    final Recorder instrumentation = new Recorder() {
      public void start() throws EngineException {
        n.incrementAndGet();
      }

      public void end(boolean success) throws EngineException {
        n.decrementAndGet();
      }
    };

    final Random random = new Random();

    final String[] locations = { "L1", "L2", "L3" };

    class RegisterInstrumentation implements Runnable {
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
        catch (RejectedExecutionException e) {
        }
      }}

    final List<Runnable> runnables = new ArrayList<Runnable>();

    for (int i = 0; i < 10; ++i) {
      runnables.add(new RegisterInstrumentation());
    }

    for (Runnable r : runnables) {
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
