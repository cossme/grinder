// Copyright (C) 2000 - 2012 Philip Aston
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

package net.grinder.statistics;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import net.grinder.statistics.StatisticsIndexMap.LongSampleIndex;

/**
 * Unit test case for {@link StatisticsIndexMap}.
 *
 * @author Philip Aston
 * @see StatisticsSet
 */
public class TestStatisticsIndexMap extends TestCase {

  private final StatisticsIndexMap m_indexMap =
    StatisticsServicesImplementation.getInstance().getStatisticsIndexMap();

  public void testLongs() throws Exception {
    final String[] data = {
        "userLong0", "userLong1", "userLong2", "userLong3", };

    final StatisticsIndexMap.LongIndex[] longResults =
      new StatisticsIndexMap.LongIndex[data.length];

    for (int i = 0; i < data.length; i++) {
      longResults[i] = m_indexMap.getLongIndex(data[i]);

      assertNotNull(m_indexMap.getLongIndex(data[i]));
      assertNull(m_indexMap.getDoubleIndex(data[i]));

      for (int j = 0; j < i; ++j) {
        assertTrue(longResults[i].getValue() != longResults[j].getValue());
      }
    }

    for (int i = 0; i < data.length; i++) {
      assertEquals(longResults[i].getValue(), m_indexMap.getLongIndex(
          data[i]).getValue());
    }
  }

  public void testDoubles() throws Exception {
    final String[] data = {
        "userDouble0", "userDouble1", "userDouble2", "userDouble3", };

    final StatisticsIndexMap.DoubleIndex[] doubleResults =
      new StatisticsIndexMap.DoubleIndex[data.length];

    for (int i = 0; i < data.length; i++) {
      doubleResults[i] = m_indexMap.getDoubleIndex(data[i]);

      assertNotNull(m_indexMap.getDoubleIndex(data[i]));
      assertNull(m_indexMap.getLongIndex(data[i]));

      for (int j = 0; j < i; ++j) {
        assertTrue(doubleResults[i].getValue() != doubleResults[j].getValue());
      }
    }

    for (int i = 0; i < data.length; i++) {
      assertEquals(doubleResults[i].getValue(), m_indexMap.getDoubleIndex(
          data[i]).getValue());
    }
  }

  private static class ExpectedIndices  {
    private final Set<Integer> expected;

    public ExpectedIndices(Integer... indices) {
      expected = new HashSet<Integer>(asList(indices));
    }

    public void remove(int index) {
      assertTrue(expected + " contains " + index, expected.remove(index));
    }

    public void assertEmpty() {
      assertTrue(expected + " is empty", expected.size() == 0);
    }
  }

  public void testSlotsAreUnique() throws Exception {
    final StatisticsIndexMap map =
        new StatisticsIndexMap(asList("l1", "l2"),
                               asList("d1", "d2"),
                               asList("t1", "t2"),
                               asList("ls1", "ls2"));

    assertEquals(6, map.getNumberOfLongs());
    assertEquals(4, map.getNumberOfDoubles());
    assertEquals(2, map.getNumberOfTransientLongs());
    assertEquals(0, map.getDoubleSampleIndicies().size());

    final ExpectedIndices expectedLongs =
        new ExpectedIndices(0, 1, 2, 3, 4, 5);
    final ExpectedIndices expectedDoubles = new ExpectedIndices(0, 1, 2, 3);
    final ExpectedIndices expectedTransientLongs = new ExpectedIndices(0, 1);


    expectedLongs.remove(map.getLongIndex("l1").getValue());
    expectedLongs.remove(map.getLongIndex("l2").getValue());

    final LongSampleIndex ls1 = map.getLongSampleIndex("ls1");
    expectedLongs.remove(ls1.getCountIndex().getValue());
    expectedLongs.remove(ls1.getSumIndex().getValue());
    expectedDoubles.remove(ls1.getVarianceIndex().getValue());

    final LongSampleIndex ls2 = map.getLongSampleIndex("ls2");
    expectedLongs.remove(ls2.getCountIndex().getValue());
    expectedLongs.remove(ls2.getSumIndex().getValue());
    expectedDoubles.remove(ls2.getVarianceIndex().getValue());

    expectedTransientLongs.remove(map.getLongIndex("t1").getValue());
    expectedTransientLongs.remove(map.getLongIndex("t2").getValue());
    expectedDoubles.remove(map.getDoubleIndex("d1").getValue());
    expectedDoubles.remove(map.getDoubleIndex("d2").getValue());

    expectedLongs.assertEmpty();
    expectedDoubles.assertEmpty();
    expectedTransientLongs.assertEmpty();
  }
}
