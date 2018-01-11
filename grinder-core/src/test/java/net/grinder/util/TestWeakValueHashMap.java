// Copyright (C) 2004 - 2009 Philip Aston
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

import junit.framework.TestCase;


/**
 * Unit test case for {@link WeakValueHashMap}.
 *
 * @author Philip Aston
 */
public class TestWeakValueHashMap extends TestCase {

  public void testWeakValueHashMap() throws Exception {

    final WeakValueHashMap<Object, Object> map =
      new WeakValueHashMap<Object, Object>();

    Object k1 = new Object();
    Object v1 = new Object();
    Object k2 = new Object();
    Object v2 = new Object();
    Object k3 = new Object();
    Object v3 = new Object();

    assertNull(map.get(k1));

    map.put(k1, v1);
    assertEquals(v1, map.get(k1));
    assertNull(map.get(v2));

    map.put(k2, v2);
    assertEquals(v1, map.get(k1));
    assertEquals(v2, map.get(k2));

    map.put(k1, v2);
    assertEquals(v2, map.get(k1));
    assertEquals(v2, map.get(k2));

    map.put(k1, v1);

    v2 = null;
    Runtime.getRuntime().gc();

    assertEquals(v1, map.get(k1));
    assertNull(map.get(k2));

    final Object removedV1 = map.remove(k1);
    assertEquals(v1, removedV1);
    assertNull(map.get(k1));

    final Object removedV2 = map.remove(k2);
    assertNull(removedV2);

    final Object removedV3 = map.remove(k3);
    assertNull(removedV3);

    map.put(k3, v3);
    assertEquals(v3, map.get(k3));

    map.put(k1, v1);
    assertEquals(v1, map.get(k1));

    map.clear();

    assertNull(map.get(k1));
    assertNull(map.get(k2));
    assertNull(map.get(k3));
  }
}
