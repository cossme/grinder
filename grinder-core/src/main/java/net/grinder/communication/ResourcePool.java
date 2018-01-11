// Copyright (C) 2006 - 2009 Philip Aston
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

import java.util.List;


/**
 * A pool of resources.
 *
 * <p>Each resource in the pool is wrapped in a wrapper that keeps
 * track of whether it is currently in use. Clients access resources
 * through {@link Reservation}s.</p>
 *
 * @author Philip Aston
 */
interface ResourcePool {

  /**
   * Adds a resource to the pool.
   *
   * @param resource The resource to add.
   * @return Allows the client to notify the resource pool if the
   * resource has been closed.
   */
  Closeable add(final Resource resource);

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
  Reservation reserveNext();

  /**
   * Returns a list of reservations for all the current resources. Blocks until
   * all Reservations can be reserved. The Sentinel is not included in the list.
   *
   * @return The resources. It is up to the caller to free or close each
   *         resource.
   */
  List<? extends Reservation> reserveAll();

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
  void closeCurrentResources();

  /**
   * Count the active resources.
   *
   * @return The number of active resources.
   */
  int countActive();

  /**
   * Add a new listener.
   *
   * @param listener The listener.
   */
  void addListener(Listener listener);

  /**
   * Public interface to a resource.
   */
  public interface Resource {
    void close();
  }

  /**
   * Listener interface.
   */
  public interface Listener {

    void resourceAdded(Resource resource);

    void resourceClosed(Resource resource);
  }

  /**
   * Something that can be closed.
   */
  public interface Closeable {

    void close();

    boolean isClosed();
  }

  /**
   * Public interface to a resource reservation.
   */
  public interface Reservation extends Closeable {

    boolean isSentinel();

    Resource getResource();

    void free();
  }
}
