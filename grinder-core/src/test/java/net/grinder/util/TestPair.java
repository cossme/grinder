// Copyright (C) 2009 - 2012 Philip Aston
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

import static net.grinder.testutility.AssertUtilities.assertNotEquals;


/**
 * Unit test for {@link Pair}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestPair extends TestCase {

  public void testBasics() throws Exception {
    final Pair<Integer, String> p1 = Pair.of(3, "123");

    assertEquals(new Integer(3), p1.getFirst());
    assertEquals("123", p1.getSecond());

    assertEquals("(3, 123)", p1.toString());

    final Pair<Integer, String> p2 = Pair.of((Integer)null, "123");
    assertNull(p2.getFirst());
    assertEquals("(null, 123)", p2.toString());
  }

  public void testEquality() throws Exception {

    final Pair<Integer, String> p1 = Pair.of(3, "123");

    assertEquals(p1, p1);
    assertEquals(p1.hashCode(), p1.hashCode());
    assertNotEquals(p1, null);
    assertNotEquals(p1, this);

    final Pair<Integer, String> p2 = Pair.of(4, "123");

    assertNotEquals(p1, p2);
    assertEquals(p2, p2);

    final Pair<Integer, String> p3 = Pair.of(3, "123");

    assertEquals(p1, p3);
    assertEquals(p1.hashCode(), p3.hashCode());

    final Pair<Integer, String> p4 = Pair.of((Integer)null, (String)null);
    assertEquals(p4, p4);
    assertEquals(p4.hashCode(), p4.hashCode());
    assertNotEquals(p4, p3);

    final Pair<Integer, String> p5 = Pair.of(3, (String)null);
    assertNotEquals(p3, p5);
    assertNotEquals(p5, p3);
    assertEquals(p5, p5);
    assertEquals(p5.hashCode(), p5.hashCode());
  }
}
