// Copyright (C) 2008 - 2009 Philip Aston
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

import junit.framework.TestCase;
import net.grinder.util.AllocateLowestNumber.IteratorCallback;


/**
 * Unit tests for {@link AllocateLowestNumberImplementation}.
 *
 * @author Philip Aston
 */
public class TestAllocateLowestNumberImplementation extends TestCase {

  public void testAllocateLowestNumber() throws Exception {
    final AllocateLowestNumber set = new AllocateLowestNumberImplementation();

    final Integer object1 = new Integer(121);
    final Integer object2 = new Integer(1);
    final Integer object3 = new Integer(3);
    final Integer object4 = new Integer(31);

    assertEquals(0, set.add(object1));
    assertEquals(0, set.add(object1));
    assertEquals(1, set.add(object2));
    assertEquals(2, set.add(object3));

    set.remove(object2);
    assertEquals(1, set.add(object4));

    final Map<Object, Integer> expectation = new HashMap<Object, Integer>() {{
      put(object1, 0);
      put(object4, 1);
      put(object3, 2);
    }};

    set.forEach(new IteratorCallback() {
      public void objectAndNumber(Object object, int number) {
        assertEquals(expectation.remove(object), new Integer(number));
      }
    });

    assertEquals(0, expectation.size());

    set.remove(object1);
    set.remove(object4);
    set.remove(object4);
    assertEquals(0, set.add(object1));
  }
}
