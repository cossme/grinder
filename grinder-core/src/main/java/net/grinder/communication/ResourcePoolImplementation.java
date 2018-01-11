// Copyright (C) 2003 - 2012 Philip Aston
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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import net.grinder.common.UncheckedInterruptedException;
import net.grinder.util.ListenerSupport;


/**
 * Implementation of {@link ResourcePool}.
 *
 * @author Philip Aston
 */
final class ResourcePoolImplementation implements ResourcePool {
  private static final int PURGE_FREQUENCY = 1000;

  // Used to signal when a Reservable has been freed.
  private final Object m_reservableFreedMutex = new Object();

  // Guards reserveAll().
  private final Object m_reserveAllMutex = new Object();

  // Guards m_reservables.
  private final Object m_reservablesMutex = new Object();

  // Guarded by m_reservablesMutex.
  private List<Reservable> m_reservables = new ArrayList<Reservable>();
  private int m_lastReservable = 0;
  private int m_nextPurge = 0;

  private final ListenerSupport<Listener> m_listeners =
    new ListenerSupport<Listener>();

  /**
   * Constructor.
   */
  public ResourcePoolImplementation() {
    m_reservables.add(new Sentinel());
  }

  /**
   * Adds a resource to the pool.
   *
   * @param resource The resource to add.
   * @return Allows the client to notify the resource pool if the
   * resource has been closed.
   */
  @Override public Closeable add(final Resource resource) {
    final ResourceWrapper resourceWrapper = new ResourceWrapper(resource);

    synchronized (m_reservablesMutex) {
      m_reservables.add(resourceWrapper);
    }

    m_listeners.apply(
      new ListenerSupport.Informer<Listener>() {
        public void inform(Listener l) { l.resourceAdded(resource); }
      });

    return resourceWrapper;
  }

  /**
   * Returns a resource, reserved for exclusive use by the caller.
   *
   * <p>Resources are handed out to callers in order. A Sentinel is
   * returned once every cycle; if no resources are free the Sentinel
   * is always returned.</p>
   *
   * @return The resource. It is up to the caller to free or close the
   * resource.
   */
  public Reservation reserveNext() {
    synchronized (m_reservablesMutex) {
      purgeZombieResources();

      while (true) {
        if (++m_lastReservable >= m_reservables.size()) {
          m_lastReservable = 0;
        }

        final Reservable reservable = m_reservables.get(m_lastReservable);

        if (reservable.reserve()) {
          return reservable;
        }
      }
    }
  }

  /**
   * Returns a list of all the current resources. Blocks until all
   * Reservations can be reserved. The Sentinel is not included in the
   * list.
   *
   * @return The resources. It is up to the caller to free or close
   * each resource.
   */
  public List<Reservable> reserveAll() {

    // Only one thread gets to reserveAll at a time. Otherwise two threads
    // calling this can deadlock each other. See bug #1199086.
    synchronized (m_reserveAllMutex) {

      final List<Reservable> result;
      final List<Reservable> reserveList;

      synchronized (m_reservablesMutex) {
        purgeZombieResources();

        result = new ArrayList<Reservable>(m_reservables.size());
        reserveList = new ArrayList<Reservable>(m_reservables);
      }

      while (reserveList.size() > 0) {
        // Iterate backwards so remove is cheap.
        final ListIterator<Reservable> iterator =
          reserveList.listIterator(reserveList.size());

        while (iterator.hasPrevious()) {
          final Reservable reservable = iterator.previous();

          if (reservable.isSentinel()) {
            iterator.remove();
          }
          else if (reservable.reserve()) {
            result.add(reservable);
            iterator.remove();
          }
          else if (reservable.isClosed()) {
            iterator.remove();
          }
        }

        if (reserveList.size() > 0) {
          // Block until more resources are freed.
          synchronized (m_reservableFreedMutex) {
            try {
              // Don't block for ever because the outstanding
              // resources might have already been freed.
              m_reservableFreedMutex.wait(1000);
            }
            catch (InterruptedException e) {
              throw new UncheckedInterruptedException(e);
            }
          }
        }
      }

      return result;
    }
  }

  /**
   * Close the resources currently in the pool. Resources can be closed
   * while reserved.
   *
   * <p>
   * This doesn't "shutdown" the pool. I don't want to introduce an extra
   * shutdown state, and the pollute our interface with a shutdown exception.
   * It's up to the owner of the pool to prevent new things from being added to
   * the pool if necessary.
   * </p>
   */
  public void closeCurrentResources() {
    final Reservable[] reservablesClone;

    // We don't reserve so that resources are closed promptly and resource
    // leaks don't cause us a problem. It's up to the resource implementation
    // to allow it to be closed cleanly whilst reserved by another thread.
    synchronized (m_reservablesMutex) {
      reservablesClone =
        m_reservables.toArray(new Reservable[m_reservables.size()]);
    }

    // We don't hold m_reservablesMutex whilst closing the Reservables to
    // remove chance of deadlock with actions taken by listeners.
    for (int i = 0; i < reservablesClone.length; ++i) {
      reservablesClone[i].close();
    }
  }

  /**
   * Count the active resources.
   *
   * @return The number of active resources.
   */
  public int countActive() {
    int result = 0;

    synchronized (m_reservablesMutex) {
      for (Reservable reservable : m_reservables) {
        if (!reservable.isClosed() && !reservable.isSentinel()) {
          ++result;
        }
      }
    }

    return result;
  }

  private void purgeZombieResources() {
    synchronized (m_reservablesMutex) {
      if (++m_nextPurge > PURGE_FREQUENCY) {
        m_nextPurge = 0;

        final List<Reservable> newReservables =
          new ArrayList<Reservable>(m_reservables.size());

        for (Reservable reservable : m_reservables) {
          if (!reservable.isClosed()) {
            newReservables.add(reservable);
          }
        }

        m_reservables = newReservables;
        m_lastReservable = 0;
      }
    }
  }

  /**
   * Add a new listener.
   *
   * @param listener The listener.
   */
  public void addListener(Listener listener) {
    m_listeners.add(listener);
  }

  private interface Reservable extends Reservation {
    boolean reserve();
  }

  private static final class Sentinel implements Reservable {

    public boolean isSentinel() {
      return true;
    }

    public boolean reserve() {
      return true;
    }

    public Resource getResource() {
      return null;
    }

    public void free() {
    }

    public void close() {
    }

    public boolean isClosed() {
      return false;
    }
  }

  private final class ResourceWrapper implements Reservable {

    private final Resource m_resource;
    private boolean m_busy = false;
    private boolean m_closed;

    public ResourceWrapper(Resource resource) {
      m_resource = resource;
    }

    public boolean isSentinel() {
      return false;
    }

    public boolean reserve() {
      // Perhaps assert !m_busy.
      synchronized (this) {
        if (m_busy || m_closed) {
          return false;
        }

        m_busy = true;
      }

      return true;
    }

    public void free() {
      // Perhaps assert m_busy.
      final boolean stateChanged;

      synchronized (this) {
        stateChanged = m_busy;
        m_busy = false;
      }

      if (stateChanged) {
        synchronized (m_reservableFreedMutex) {
          m_reservableFreedMutex.notifyAll();
        }
      }
    }

    public Resource getResource() {
      return m_resource;
    }

    public void close() {

      final boolean stateChanged;

      synchronized (this) {
        stateChanged = !m_closed;

        // If the outside world is closing us, we'll be reserved.
        // If the ResourcePoolImplementation is closing, we might not be.

        if (stateChanged) {
          // Update state before closing resource to prevent possible
          // recursion.
          m_busy = false;
          m_closed = true;

          m_resource.close();
        }
      }

      if (stateChanged) {
        synchronized (m_reservableFreedMutex) {
          m_reservableFreedMutex.notifyAll();
        }

        final ListenerSupport.Informer<Listener> informer;

        try {
          informer = new ListenerSupport.Informer<Listener>() {
              public void inform(Listener l) { l.resourceClosed(m_resource); }
            };
        }
        catch (Exception e) {
          // Hack to fix bug 1592664. When shutting down (so this
          // thread is being validly interrupted), the allocation of
          // the Informer occasionally results in a
          // java.lang.InterruptedException. This is clearly a JRE
          // bug. (Repeatable with J2SE 1.4.2_11-b06 and 1.5.0_04-b05,
          // but not with JRockit). We have to catch Exception, since
          // InterruptedException is a checked exception.

          try {
            throw e;
          }
          catch (RuntimeException runtimeException) {
            throw runtimeException;
          }
          catch (InterruptedException interruptedException) {
            throw new UncheckedInterruptedException(interruptedException);
          }
          catch (Exception exception) {
            throw new AssertionError(exception);
          }
        }

        m_listeners.apply(informer);
      }
    }

    public synchronized boolean isClosed() {
      return m_closed;
    }
  }
}
