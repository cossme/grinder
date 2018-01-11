// Copyright (C) 2005 - 2008 Philip Aston
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

package net.grinder.util.thread;

import java.util.Timer;
import java.util.TimerTask;

import junit.framework.TestCase;


/**
 * Unit tests for {@link BooleanCondition}.
 *
 * @author Philip Aston
 */
public class TestBooleanCondition extends TestCase {

  public void testWithSingleThread() throws Exception {
    final BooleanCondition booleanCondition = new BooleanCondition();

    // State is initially false, so we shouldn't block here.
    assertFalse(booleanCondition.await(false));
    assertFalse(booleanCondition.await(false));
    assertFalse(booleanCondition.get());

    booleanCondition.set(true);
    assertTrue(booleanCondition.get());
    assertTrue(booleanCondition.await(true));

    booleanCondition.set(false);
    assertFalse(booleanCondition.get());
    assertFalse(booleanCondition.await(false));
  }

  public void testWithMultipleThreads() throws Exception {
    final Timer timer = new Timer();

    final BooleanCondition booleanCondition = new BooleanCondition();

    final TimerTask setTrueTask = new TimerTask() {
      public void run() { booleanCondition.set(true); }
    };

    timer.schedule(setTrueTask, 0, 1);

    assertTrue(booleanCondition.await(true));
    assertTrue(booleanCondition.await(true));
    assertTrue(booleanCondition.get());

    setTrueTask.cancel();

    final TimerTask wakeUpAllWaitersTask = new TimerTask() {
      public void run() { booleanCondition.wakeUpAllWaiters(); }
    };

    timer.schedule(wakeUpAllWaitersTask, 0, 1);

    // Will return true, because state is false but interrupted.
    assertTrue(booleanCondition.await(false));

    // State should still be true.
    assertTrue(booleanCondition.await(true));

    wakeUpAllWaitersTask.cancel();

    timer.cancel();
  }
}
