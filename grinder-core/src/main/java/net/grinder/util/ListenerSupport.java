// Copyright (C) 2005 - 2009 Philip Aston
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * Generic support for listeners.
 *
 * @param <T> Listener type.
 *
 * @author Philip Aston
 */
public final class ListenerSupport<T> {

  private final List<T> m_listeners = new LinkedList<T>();

  /**
   * Add a listener.
   *
   * @param listener The listener.
   */
  public void add(T listener) {
    synchronized (m_listeners) {
      m_listeners.add(listener);
    }
  }

  /**
   * Remove all instances of the given listener.
   *
   * @param listener The listener.
   */
  public void remove(T listener) {
    synchronized (m_listeners) {
      while (m_listeners.remove(listener)) {
        // Keep checking.
      }
    }
  }

  /**
   * Adapter interface for use with
   * {@link
   * ListenerSupport#apply(net.grinder.util.ListenerSupport.Informer)}.
   */
  public interface Informer<K> {

    /**
     * Should notify the listener appropriately.
     *
     * @param listener The listener.
     */
    void inform(K listener);
  }

  /**
   * Adapter interface for use with
   * {@link
   * ListenerSupport#apply(net.grinder.util.ListenerSupport.HandlingInformer)}.
   */
  public interface HandlingInformer<K> {

    /**
     * Should notify the listener appropriately.
     *
     * @param listener
     *          The listener.
     * @return <code>true</code> => event handled, do not delegate to further
     *          Handlers.
     */
    boolean inform(K listener);
  }

  /**
   * Notify the listeners of an event.
   *
   * @param informer An adapter to be applied to each listener.
   */
  public void apply(Informer<? super T> informer) {
    for (T listener : cloneListenerList()) {
      informer.inform(listener);
    }
  }

  /**
   * Notify the listeners of an event.
   *
   * @param handler An adapter to be applied to each listener.
   * @return <code>true</code> => a listener handled the event.
   */
  public boolean apply(HandlingInformer<? super T> handler) {
    for (T listener : cloneListenerList()) {
      if (handler.inform(listener)) {
        return true;
      }
    }

    return false;
  }

  private List<T> cloneListenerList() {
    synchronized (m_listeners) {
      return new ArrayList<T>(m_listeners);
    }
  }
}
