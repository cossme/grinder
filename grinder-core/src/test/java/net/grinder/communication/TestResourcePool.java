// Copyright (C) 2003 - 2011 Philip Aston
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

package net.grinder.communication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import java.util.concurrent.CyclicBarrier;

import net.grinder.common.UncheckedInterruptedException;
import net.grinder.communication.ResourcePool.Listener;

import org.junit.Test;


/**
 *  Unit test case for {@code ResourcePool}.
 *
 * @author Philip Aston
 */
public class TestResourcePool {

  @Test public void testConstructorAndSentinel() throws Exception {

    final ResourcePool resourcePool = new ResourcePoolImplementation();

    final ResourcePool.Reservation reservation = resourcePool.reserveNext();
    assertTrue(reservation.isSentinel());

    // Reserving the Sentinel is a no-op, so we can reserve it as
    // often as we like.
    final ResourcePool.Reservation reservation2 = resourcePool.reserveNext();
    assertTrue(reservation2.isSentinel());

    // free() and close() are also no-ops.
    reservation2.free();
    reservation2.close();

    assertTrue(!reservation2.isClosed());

    final ResourcePool.Reservation reservation3 = resourcePool.reserveNext();
    assertTrue(reservation3.isSentinel());

    // Sentinel doesn't have a resource.
    assertNull(reservation3.getResource());
  }

  @Test public void testAddAndReserveNext() throws Exception {

    final ResourcePool resourcePool = new ResourcePoolImplementation();

    final Listener listener1 = mock(ResourcePool.Listener.class);
    final Listener listener2 = mock(ResourcePool.Listener.class);

    resourcePool.addListener(listener1);
    resourcePool.addListener(listener2);

    final MyResource resource1 = new MyResource();
    final MyResource resource2 = new MyResource();

    resourcePool.add(resource1);
    resourcePool.add(resource2);

    verify(listener1).resourceAdded(resource1);
    verify(listener1).resourceAdded(resource2);

    verify(listener2).resourceAdded(resource1);
    verify(listener2).resourceAdded(resource2);

    final ResourcePool.Reservation reservation1 = resourcePool.reserveNext();
    final ResourcePool.Reservation reservation2 = resourcePool.reserveNext();
    final ResourcePool.Reservation sentinel = resourcePool.reserveNext();

    assertTrue(!reservation1.isSentinel());
    assertTrue(!reservation2.isSentinel());
    assertTrue(sentinel.isSentinel());
    assertTrue(resourcePool.reserveNext().isSentinel());

    if (resource1.equals(reservation1.getResource())) {
      assertEquals(resource1, reservation1.getResource());
      assertEquals(resource2, reservation2.getResource());
    }
    else {
      assertEquals(resource1, reservation2.getResource());
      assertEquals(resource2, reservation1.getResource());
    }

    reservation2.free();
    assertSame(reservation2, resourcePool.reserveNext());

    assertSame(sentinel, resourcePool.reserveNext());

    reservation1.free();
    assertSame(reservation1, resourcePool.reserveNext());

    reservation1.free();
    reservation2.free();

    // Exercise reserveNext many times to trigger zombie managment -
    // this time round, nothing should be purged.
    for (int i = 0; i < 1100; ++i) {
      resourcePool.reserveNext().free();
    }

    assertEquals(2, resourcePool.countActive());

    reservation2.close();
    assertTrue(reservation2.isClosed());
    assertSame(sentinel, resourcePool.reserveNext());

    verify(listener1).resourceClosed(resource2);
    verify(listener2).resourceClosed(resource2);

    assertTrue(!resource1.isClosed());
    assertTrue(resource2.isClosed());

    for (int i = 0; i < 1100; ++i) {
      resourcePool.reserveNext().free();
    }

    assertEquals(1, resourcePool.countActive());

    verifyNoMoreInteractions(listener1, listener2);
  }

  @Test public void testReserveAll() throws Exception {

    final ResourcePool resourcePool = new ResourcePoolImplementation();
    assertEquals(0, resourcePool.reserveAll().size());
    assertEquals(0, resourcePool.reserveAll().size());

    final MyResource resource1 = new MyResource();
    final MyResource resource2 = new MyResource();

    resourcePool.add(resource1);
    resourcePool.add(resource2);

    final List<?> reservations = resourcePool.reserveAll();
    assertEquals(2, reservations.size());

    final ResourcePool.Reservation reservation1 =
      (ResourcePool.Reservation)reservations.get(0);

    final ResourcePool.Reservation reservation2 =
      (ResourcePool.Reservation)reservations.get(1);

    if (resource1.equals(reservation1.getResource())) {
      assertEquals(resource1, reservation1.getResource());
      assertEquals(resource2, reservation2.getResource());
    }
    else {
      assertEquals(resource1, reservation2.getResource());
      assertEquals(resource2, reservation1.getResource());
    }

    assertTrue(!reservation1.isSentinel());
    reservation1.free();

    assertTrue(!reservation2.isSentinel());
    reservation2.free();

    final List<?> reservations2 = resourcePool.reserveAll();
    assertEquals(2, reservations2.size());

    ((ResourcePool.Reservation)reservations2.get(0)).free();

    assertEquals(
      UncheckedInterruptedException.class,
      new BlockingActionThread() {
        protected void blockingAction() throws InterruptedException {
          resourcePool.reserveAll();
        }
      }.getException().getClass());
  }

  @Test public void testReserveAllMultiThreaded() throws Exception {
    final ResourcePool resourcePool = new ResourcePoolImplementation();

    resourcePool.add(new MyResource());
    resourcePool.add(new MyResource());
    resourcePool.add(new MyResource());
    resourcePool.add(new MyResource());
    resourcePool.add(new MyResource());

    final CyclicBarrier barrier = new CyclicBarrier(20);

    class ReserveAll implements Runnable {
      public void run() {
        try {
          barrier.await();
        }
        catch (Exception e) {
        }

        for (int i = 0; i < 50; ++i) {
          final List<? extends ResourcePool.Reservation> list =
            resourcePool.reserveAll();

          assert list.size() == 5;

          for (ResourcePool.Reservation reservation : list) {
            reservation.free();
          }
        }
      }
    }

    final Thread[] threads = new Thread[barrier.getParties()];

    for (int i = 0; i<threads.length; ++i) {
      threads[i] = new Thread(new ReserveAll());
    }

    for (int i = 0; i<threads.length; ++i) {
      threads[i].start();
    }

    for (int i=0; i<threads.length; ++i) {
      threads[i].join();
    }
  }

  @Test public void testClose() throws Exception {

    final ResourcePool resourcePool = new ResourcePoolImplementation();
    assertEquals(0, resourcePool.reserveAll().size());
    assertEquals(0, resourcePool.reserveAll().size());

    final Listener listener = mock(ResourcePool.Listener.class);

    final MyResource resource1 = new MyResource();
    final MyResource resource2 = new MyResource();

    resourcePool.addListener(listener);

    resourcePool.add(resource1);
    resourcePool.add(resource2);

    verify(listener).resourceAdded(resource1);
    verify(listener).resourceAdded(resource2);

    final List<?> reservations = resourcePool.reserveAll();
    assertEquals(2, reservations.size());

    ((ResourcePool.Reservation)reservations.get(1)).free();

    resourcePool.closeCurrentResources();

    verify(listener).resourceClosed(resource1);
    verify(listener).resourceClosed(resource2);

    final List<?> reservations2 = resourcePool.reserveAll();
    assertEquals(0, reservations2.size());

    assertTrue(resource1.isClosed());
    assertTrue(resource2.isClosed());

    verifyNoMoreInteractions(listener);
  }

  @Test public void testCountActive() throws Exception {

    final ResourcePool resourcePool = new ResourcePoolImplementation();
    assertEquals(0, resourcePool.countActive());

    final MyResource resource1 = new MyResource();
    final MyResource resource2 = new MyResource();

    resourcePool.add(resource1);
    assertEquals(1, resourcePool.countActive());

    resourcePool.add(resource2);
    assertEquals(2, resourcePool.countActive());

    final List<?> reservations = resourcePool.reserveAll();
    assertEquals(2, resourcePool.countActive());

    ((ResourcePool.Reservation)reservations.get(1)).close();
    assertEquals(1, resourcePool.countActive());

    ((ResourcePool.Reservation)reservations.get(0)).close();
    assertEquals(0, resourcePool.countActive());
  }

  private static class MyResource implements ResourcePool.Resource {

    private boolean m_closed = false;

    public void close() {
      m_closed = true;
    }

    public boolean isClosed() {
      return m_closed;
    }
  }
}
