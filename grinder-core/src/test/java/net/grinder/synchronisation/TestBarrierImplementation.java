// Copyright (C) 2011 Philip Aston
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

package net.grinder.synchronisation;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.grinder.common.UncheckedInterruptedException;
import net.grinder.script.CancelledBarrierException;
import net.grinder.synchronisation.messages.BarrierIdentity;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


/**
 * Unit tests for {@link BarrierImplementation}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestBarrierImplementation {

  private final static ExecutorService m_executor =
    Executors.newCachedThreadPool();

  private static final BarrierIdentity ID1 = new BarrierIdentity() {};
  private static final BarrierIdentity ID2 = new BarrierIdentity() {};
  private static final BarrierIdentity ID3 = new BarrierIdentity() {};

  private @Mock BarrierGroup m_barrierGroup;
  private @Mock BarrierIdentity.Factory m_identityFactory;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(m_identityFactory.next()).thenReturn(ID1, ID2, ID3);
  }

  @AfterClass public static void tearDownClass() {
    m_executor.shutdown();
  }

  @Test public void testConstruction() throws Exception {
    final BarrierImplementation b =
      new BarrierImplementation(m_barrierGroup, m_identityFactory);

    verify(m_barrierGroup).addListener(b);
    verify(m_barrierGroup).addBarrier();

    verifyNoMoreInteractions(m_barrierGroup);
  }

  @Test public void testGetGroupName() throws Exception {
    final BarrierImplementation b =
      new BarrierImplementation(m_barrierGroup, m_identityFactory);

    when(m_barrierGroup.getName()).thenReturn("mygroup");

    assertEquals("mygroup", b.getName());
  }

  @Test public void testAwaitTimeout() throws Exception {
    final BarrierImplementation b =
      new BarrierImplementation(m_barrierGroup, m_identityFactory);
    reset(m_barrierGroup);

    final boolean result = b.await(1, TimeUnit.MICROSECONDS);

    verify(m_barrierGroup).addWaiter(ID2);
    verify(m_barrierGroup).cancelWaiter(ID2);
    verify(m_barrierGroup).removeBarriers(1);
    verify(m_barrierGroup).removeListener(b);

    verifyNoMoreInteractions(m_barrierGroup);

    assertFalse(result);
  }

  @Test public void testAwaitTimeout2() throws Exception {
    final BarrierImplementation b =
      new BarrierImplementation(m_barrierGroup, m_identityFactory);
    reset(m_barrierGroup);

    final boolean result = b.await(1);

    verify(m_barrierGroup).addWaiter(ID2);

    verify(m_barrierGroup).addWaiter(ID2);
    verify(m_barrierGroup).cancelWaiter(ID2);
    verify(m_barrierGroup).removeBarriers(1);
    verify(m_barrierGroup).removeListener(b);

    verifyNoMoreInteractions(m_barrierGroup);

    assertFalse(result);
  }

  @Test public void testAwaitInterrupted() throws Exception {
    final BarrierImplementation b =
      new BarrierImplementation(m_barrierGroup, m_identityFactory);

    final CyclicBarrier sync = new CyclicBarrier(2);
    final Thread mainThread = Thread.currentThread();

    final Future<Void> future = m_executor.submit(
      new Callable<Void>() {
        public Void call() throws Exception {
          sync.await();
          mainThread.interrupt();
          return null;
        }
      });

    sync.await();

    try {
      b.await();
      fail("Expected UncheckedInterruptedException");
    }
    catch (UncheckedInterruptedException e) {
    }

    future.get();
  }

  @Test public void testAwaitTimeoutInterrupted() throws Exception {
    final BarrierImplementation b =
      new BarrierImplementation(m_barrierGroup, m_identityFactory);

    reset(m_barrierGroup);

    Thread.currentThread().interrupt();

    try {
      b.await(123456);
      fail("Expected UncheckedInterruptedException");
    }
    catch (UncheckedInterruptedException e) {
    }

    verify(m_barrierGroup).addWaiter(ID2);
    verify(m_barrierGroup).cancelWaiter(ID2);
    verify(m_barrierGroup).removeBarriers(1);
    verify(m_barrierGroup).removeListener(b);

    verifyNoMoreInteractions(m_barrierGroup);
  }

  @Test public void testAwaitHappyCase() throws Exception {
    final BarrierImplementation b =
      new BarrierImplementation(m_barrierGroup, m_identityFactory);
    reset(m_barrierGroup);

    final Future<?>[] futureHolder = { null };

    doAnswer(new Answer<Void>() {
      public Void answer(InvocationOnMock invocation) throws Throwable {
        futureHolder[0] = m_executor.submit(new Callable<Void>() {
          public Void call() throws Exception {
            b.awaken(singleton(ID1));
            b.awaken(singleton(ID2));
            return null;
          }});

        return null;
      }}).when(m_barrierGroup).addWaiter(ID2);

    b.await();

    verify(m_barrierGroup).addWaiter(ID2);
    verifyNoMoreInteractions(m_barrierGroup);

    futureHolder[0].get();
  }

  @Test public void testConcurrentAwaitDisalllowedThenCancelWaiter()
    throws Exception {

    final BarrierImplementation b =
      new BarrierImplementation(m_barrierGroup, m_identityFactory);
    reset(m_barrierGroup);

    final Future<?>[] futureHolder = { null };

    doAnswer(new Answer<Void>() {
      public Void answer(InvocationOnMock invocation) throws Throwable {
        futureHolder[0] = m_executor.submit(new Callable<Void>() {
          public Void call() throws Exception {
            try {
              b.await();
              fail("Expected IllegalStateException");
            }
            catch (IllegalStateException e) {
            }
            finally {
              b.cancel();
            }

            return null;
          }});

        return null;
      }}).when(m_barrierGroup).addWaiter(ID2);

    try {
      b.await();
      fail("Expected CancelledBarrierException");
    }
    catch (CancelledBarrierException e) {
    }

    verify(m_barrierGroup).addWaiter(ID2);
    verify(m_barrierGroup).cancelWaiter(ID2);
    verify(m_barrierGroup).removeBarriers(1);
    verify(m_barrierGroup).removeListener(b);
    verifyNoMoreInteractions(m_barrierGroup);

    futureHolder[0].get();
  }

  @Test public void testCancelVirginBarrier() throws Exception {
    final BarrierImplementation b =
      new BarrierImplementation(m_barrierGroup, m_identityFactory);
    reset(m_barrierGroup);

    b.cancel();

    verify(m_barrierGroup).cancelWaiter(ID1);
    verify(m_barrierGroup).removeBarriers(1);
    verify(m_barrierGroup).removeListener(b);

    verifyNoMoreInteractions(m_barrierGroup);

    b.cancel();
    verifyNoMoreInteractions(m_barrierGroup);
  }

  @Test public void testUseCancelledBarrier() throws Exception {
    final BarrierImplementation b =
      new BarrierImplementation(m_barrierGroup, m_identityFactory);
    b.cancel();
    reset(m_barrierGroup);

    try {
      b.await();
      fail("Expected CancelledBarrierException");
    }
    catch(CancelledBarrierException e) {
    }
  }

  @Test public void testCancelWaiterThenAwaken() throws Exception {

    final BarrierImplementation b =
      new BarrierImplementation(m_barrierGroup, m_identityFactory);
    reset(m_barrierGroup);

    final Future<?>[] futureHolder = { null };

    doAnswer(new Answer<Void>() {
      public Void answer(InvocationOnMock invocation) throws Throwable {
        futureHolder[0] = m_executor.submit(new Callable<Void>() {
          public Void call() throws Exception {
            b.cancel();

            b.awaken(singleton(ID2)); // No-op.

            return null;
          }});

        return null;
      }}).when(m_barrierGroup).addWaiter(ID2);

    try {
      b.await();
      fail("Expected CancelledBarrierException");
    }
    catch (CancelledBarrierException e) {
    }

    verify(m_barrierGroup).addWaiter(ID2);
    verify(m_barrierGroup).cancelWaiter(ID2);
    verify(m_barrierGroup).removeBarriers(1);
    verify(m_barrierGroup).removeListener(b);
    verifyNoMoreInteractions(m_barrierGroup);

    futureHolder[0].get();
  }

  @Test public void testCancelTimedWaiter() throws Exception {

    final BarrierImplementation b =
      new BarrierImplementation(m_barrierGroup, m_identityFactory);
    reset(m_barrierGroup);

    final Future<?>[] futureHolder = { null };

    doAnswer(new Answer<Void>() {
      public Void answer(InvocationOnMock invocation) throws Throwable {
        futureHolder[0] = m_executor.submit(new Callable<Void>() {
          public Void call() throws Exception {
            b.cancel();
            return null;
          }});

        return null;
      }}).when(m_barrierGroup).addWaiter(ID2);

    try {
      b.await(2, TimeUnit.DAYS);
      fail("Expected CancelledBarrierException");
    }
    catch (CancelledBarrierException e) {
    }
  }
}
