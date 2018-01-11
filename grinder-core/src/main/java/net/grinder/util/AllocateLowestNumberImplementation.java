// Copyright (C) 2008 - 2011 Philip Aston
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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Implementation of @{link {@link AllocateLowestNumber}.
 *
 * @author Philip Aston
 */
public final class AllocateLowestNumberImplementation
  implements AllocateLowestNumber {

  /** Guarded by self. */
  private Map<Object, Integer> m_map = new HashMap<Object, Integer>();

  /** Guarded by {@link #m_map}. */
  private int m_nextN = 0;

  /**
   * Add a new object. If the object already belongs to the set, the existing
   * associated number is returned.
   *
   * @param o
   *            The object.
   * @return The associated number.
   */
  public int add(Object o) {
    synchronized (m_map) {
      final Integer n = m_map.get(o);

      if (n != null) {
        return n.intValue();
      }

      final int nextN = m_nextN;

      m_map.put(o, nextN);

      ++m_nextN;

      final int mapSize = m_map.size();

      while (m_nextN < mapSize && m_map.containsValue(m_nextN)) {
        ++m_nextN;
      }

      return nextN;
    }
  }

  /**
   * Remove an object from the set. The number previously associated with
   * the object (if any) is freed for re-use.
   *
   * @param o The object.
   */
  public void remove(Object o) {
    synchronized (m_map) {
      final Integer n = m_map.remove(o);

      if (n != null) {
        if (n.intValue() <= m_nextN) {
          m_nextN = n.intValue();
        }
      }
    }
  }

  /**
   * Call <code>iteratorCallback</code> for each member of the set.
   *
   * @param iteratorCallback Called for each member of the set.
   */
  public void forEach(IteratorCallback iteratorCallback) {
    final Map<Object, Integer> clonedMap;
    synchronized (m_map) {
      clonedMap = new HashMap<Object, Integer>(m_map);
    }

    for (Entry<Object, Integer> entry : clonedMap.entrySet()) {
      iteratorCallback.objectAndNumber(entry.getKey(), entry.getValue());
    }
  }
}
