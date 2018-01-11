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

package net.grinder.common;

import junit.framework.TestCase;


/**
 * Unit tests for {@link AbstractTestSemantics}.
 *
 * @author Philip Aston
 */
public class TestAbstractTestSemantics extends TestCase {

  public static class ConcreteTest extends AbstractTestSemantics {
    private final int m_number;
    private final String m_description;

    public ConcreteTest(int number, String description) {
      m_number = number;
      m_description = description;
    }

    public int getNumber() {
      return m_number;
    }

    public String getDescription() {
      return m_description;
    }
  }

  public void testEquals() throws Exception {
    final Test t1 = new ConcreteTest(10, "foo");
    final Test t2 = new ConcreteTest(10, "bah");
    final Test t3 = new ConcreteTest(1, "blah");
    final Test t4 = new Test() {
        public int getNumber() { return 1; }
        public String getDescription() { return null; }
        public final int compareTo(Test o) { return 0; }
      };

    assertEquals(t3, t3);
    assertEquals(t1, t2);
    assertEquals(t2, t1);
    assertTrue(!t1.equals(t3));
    assertTrue(!t1.equals(new Integer(10)));
    assertEquals(t3, t4);
    assertTrue(!t2.equals(t4));
  }

  public void testHashCode() throws Exception {
    final Test t1 = new ConcreteTest(10, null);
    final Test t2 = new ConcreteTest(10, "bah");
    final Test t3 = new ConcreteTest(1, "blah");

    assertEquals(t3.hashCode(), t3.hashCode());
    assertEquals(t1.hashCode(), t2.hashCode());
    assertTrue(t1.hashCode() != t3.hashCode());
  }

  public void testToString() {
    final Test t1 = new ConcreteTest(10, null);
    final Test t2 = new ConcreteTest(99, "bah");

    assertTrue(t1.toString().indexOf("10") >= 0);
    assertTrue(t2.toString().indexOf("99") >= 0);
    assertTrue(t2.toString().indexOf("bah") >= 0);
  }
}
