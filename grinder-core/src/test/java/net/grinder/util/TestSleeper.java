// Copyright (C) 2001 - 2013 Philip Aston
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

package net.grinder.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import net.grinder.common.TimeAuthority;
import net.grinder.testutility.Time;

import org.junit.Test;
import org.slf4j.Logger;


/**
 * Unit tests for {@link SleeperImplementation}.
 *
 * @author Philip Aston
 */
public class TestSleeper {

  private final TimeAuthority m_timeAuthority = new StandardTimeAuthority();

  @Test public void testConstruction() throws Exception {
    try {
      new SleeperImplementation(m_timeAuthority, null, -1, 1);
      fail("IllegalArgumentException expected");
    }
    catch (final IllegalArgumentException e) {
    }

    try {
      new SleeperImplementation(m_timeAuthority, null, 1, -1);
      fail("IllegalArgumentException expected");
    }
    catch (final IllegalArgumentException e) {
    }

    new SleeperImplementation(m_timeAuthority, null, 1, 1);
  }

  @Test public void testSleepNormal() throws Exception {
    // Warm up Hot Spot.
    final Sleeper sleep0 =
      new SleeperImplementation(m_timeAuthority, null, 1, 0);

    final long t1 = sleep0.getTimeInMilliseconds();

    final Time time0 = new Time(0, 1000) {
        @Override
        public void doIt() throws Exception { sleep0.sleepNormal(10); }
      };

    for (int i=0; i<10; i++) { time0.run(); }

    // Now do the tests.
    final Sleeper sleep1 =
      new SleeperImplementation(m_timeAuthority, null, 1, 0);

    assertTrue(
      new Time(50, 70) {
        @Override
        public void doIt() throws Exception  { sleep1.sleepNormal(50); }
      }.run());

    assertTrue(
      new Time(0, 10) {
        @Override
        public void doIt() throws Exception  { sleep1.sleepNormal(0); }
      }.run());

    final Sleeper sleep2 =
      new SleeperImplementation(m_timeAuthority, null, 2, 0);

    assertTrue(
      new Time(100, 120) {
        @Override
        public void doIt() throws Exception  { sleep2.sleepNormal(50); }
      }.run());

    final Sleeper sleep3 =
      new SleeperImplementation(m_timeAuthority, null, 1, 0.1);

    final Time time = new Time(40, 60) {
        @Override
        public void doIt() throws Exception { sleep3.sleepNormal(50);}
      };

    int in = 0;
    for (int i=0; i<30; i++) {
      if (time.run()) {
        ++in;
      }
    }

    assertTrue(in > 20);

    final Sleeper sleep4 =
      new SleeperImplementation(m_timeAuthority, null, 0, 0);

    assertTrue(
      new Time(0, 10) {
        @Override
        public void doIt() throws Exception  { sleep4.sleepNormal(50); }
      }.run());

    assertTrue(sleep0.getTimeInMilliseconds() > t1);
  }

  @Test public void testSleepFlat() throws Exception {
    // Warm up Hot Spot.
    final Sleeper sleep0 =
      new SleeperImplementation(m_timeAuthority, null, 1, 0);

    final Time time0 = new Time(0, 1000) {
        @Override
        public void doIt() throws Exception { sleep0.sleepFlat(10); }
      };

    for (int i=0; i<10; i++) { time0.run(); }

    // Now do the tests.
    final Sleeper sleep1 =
      new SleeperImplementation(m_timeAuthority, null, 1, 0);

    assertTrue(
      new Time(0, 70) {
        @Override
        public void doIt() throws Exception  { sleep1.sleepFlat(50); }
      }.run());

    assertTrue(
      new Time(0, 10) {
        @Override
        public void doIt() throws Exception  { sleep1.sleepFlat(0); }
      }.run());

    final Sleeper sleep2 =
      new SleeperImplementation(m_timeAuthority, null, 2, 0);

    assertTrue(
      new Time(0, 120) {
        @Override
        public void doIt() throws Exception  { sleep2.sleepFlat(50); }
      }.run());

    final Logger logger = mock(Logger.class);

    final Sleeper sleep3 =
      new SleeperImplementation(m_timeAuthority, logger, 1, 0);
    sleep3.sleepFlat(10);

    verify(logger).info(contains("sleeping"), isA(Long.class));
    verifyNoMoreInteractions(logger);
  }

  @Test public void testShutdown() throws Exception {
    final TakeFifty t1 = new TakeFifty();

    assertTrue(
      new Time(500, 1000) {
        @Override
        public void doIt() throws Exception {
          t1.start();
          Thread.sleep(500);
          t1.getSleeper().shutdown();
          t1.join();
        }
      }.run());

    try {
      t1.getSleeper().sleepFlat(10);
      fail("Expected ShutdownException");
    }
    catch (final SleeperImplementation.ShutdownException e) {
    }
  }

  @Test public void testShutdownAllCurrentSleepers() throws Exception {
    final Thread t1 = new TakeFifty();
    final Thread t2 = new TakeFifty();

    assertTrue(
      new Time(500, 1000) {
        @Override
        public void doIt() throws Exception {
          t1.start();
          t2.start();
          Thread.sleep(500);
          SleeperImplementation.shutdownAllCurrentSleepers();
          t1.join();
          t2.join();
        }
      }.run());

    // No-op.
    SleeperImplementation.shutdownAllCurrentSleepers();
  }

  private final class TakeFifty extends Thread {
    private final Sleeper m_sleeper;

    public TakeFifty() throws Sleeper.ShutdownException {
      m_sleeper = new SleeperImplementation(m_timeAuthority, null, 1, 0);
    }

    @Override
    public final void run() {
      try {
        m_sleeper.sleepNormal(50000);
      }
      catch (final SleeperImplementation.ShutdownException e) {
      }
    }

    public final Sleeper getSleeper() {
      return m_sleeper;
    }
  }
}
