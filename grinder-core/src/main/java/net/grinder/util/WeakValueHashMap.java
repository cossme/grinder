// Copyright (C) 2004 Philip Aston
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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;


/**
 * A map that maintains weak references to its values.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Philip Aston
 */
public final class WeakValueHashMap<K, V>  {
  private final Map<K, WeakReference<V>> m_map =
    new HashMap<K, WeakReference<V>>();

  /**
   * Clear all entries out of the map.
   */
  public void clear() {
    m_map.clear();
  }

  /**
   * Look up a value by key.
   *
   * @param key The key.
   * @return The value, or <code>null</code> if none found.
   */
  public V get(K key) {
    final WeakReference<V> reference = m_map.get(key);
    return reference != null ? reference.get() : null;
  }

  /**
   * Add a value.
   *
   * @param key The key.
   * @param value The value.
   */
  public void put(K key, V value) {
    m_map.put(key, new WeakReference<V>(value));
  }

  /**
   * Remove an entry from the map.
   *
   * @param key The key.
   * @return The removed value, or <code>null</code> if none found.
   */
  public V remove(K key) {
    final WeakReference<V> reference = m_map.remove(key);
    return reference != null ? reference.get() : null;
  }
}

