// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2011 Philip Aston
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
import static net.grinder.testutility.AssertUtilities.assertNotEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

import net.grinder.statistics.StatisticsIndexMap.DoubleIndex;
import net.grinder.statistics.StatisticsIndexMap.LongIndex;
import net.grinder.util.Serialiser;

import org.junit.Before;
import org.junit.Test;


/**
 * Unit tests for {@link StatisticsSetImplementation}/
 *
 * @author Philip Aston
 * @see StatisticsSetImplementation
 */
public class TestStatisticsSetImplementation {

  private StatisticsIndexMap m_indexMap;

  private LongIndex m_longIndex0;
  private LongIndex m_longIndex1;
  private LongIndex m_longIndex2;
  private LongIndex m_transientLongIndex;

  private DoubleIndex m_doubleIndex0;
  private DoubleIndex m_doubleIndex1;
  private DoubleIndex m_doubleIndex2;

  @Before public void setUp() throws Exception {
    m_indexMap = StatisticsServicesImplementation.getInstance()
        .getStatisticsIndexMap();

    m_longIndex0 = m_indexMap.getLongIndex("userLong0");
    m_longIndex1 = m_indexMap.getLongIndex("userLong1");
    m_longIndex2 = m_indexMap.getLongIndex("userLong2");
    m_transientLongIndex = m_indexMap.getLongIndex("period");
    m_doubleIndex0 = m_indexMap.getDoubleIndex("userDouble0");
    m_doubleIndex1 = m_indexMap.getDoubleIndex("userDouble1");
    m_doubleIndex2 = m_indexMap.getDoubleIndex("userDouble2");
  }

  @Test public void testCreation() {
    final StatisticsSetImplementation statistics =
      new StatisticsSetImplementation(m_indexMap);

    assertEquals(0, statistics.getValue(m_longIndex1));
    assertEquals(0, statistics.getValue(m_transientLongIndex));
    assertDoublesEqual(0d, statistics.getValue(m_doubleIndex2));
    assertFalse(statistics.isComposite());
  }

  @Test public void testReset() throws Exception {
    final StatisticsSetImplementation statistics0 =
      new StatisticsSetImplementation(m_indexMap);

    assertTrue(statistics0.isZero());

    statistics0.reset();
    assertTrue(statistics0.isZero());

    statistics0.setValue(m_longIndex2, 700);
    statistics0.setValue(m_transientLongIndex, 123);
    statistics0.setValue(m_doubleIndex2, -0.9999);
    statistics0.setIsComposite();
    assertEquals(700, statistics0.getValue(m_longIndex2));
    assertEquals(123, statistics0.getValue(m_transientLongIndex));
    assertDoublesEqual(-0.9999d, statistics0.getValue(m_doubleIndex2));
    assertFalse(statistics0.isZero());
    assertTrue(statistics0.isComposite());

    statistics0.reset();
    assertEquals(0, statistics0.getValue(m_longIndex2));
    assertEquals(0, statistics0.getValue(m_transientLongIndex));
    assertDoublesEqual(0d, statistics0.getValue(m_doubleIndex2));
    assertTrue(statistics0.isZero());
    assertFalse(statistics0.isComposite());
  }

  @Test public void testGetValueSetValueAndEquals() throws Exception {
    final StatisticsSetImplementation statistics0 =
      new StatisticsSetImplementation(m_indexMap);
    final StatisticsSetImplementation statistics1 =
      new StatisticsSetImplementation(m_indexMap);

    assertEquals(statistics0, statistics0);
    assertEquals(statistics0, statistics1);
    assertEquals(statistics1, statistics0);

    statistics0.setValue(m_transientLongIndex, 123);
    assertNotEquals(statistics0, statistics1);
    statistics0.setValue(m_transientLongIndex, 0);
    assertEquals(statistics0, statistics1);

    statistics0.setValue(m_longIndex1, 700);
    assertEquals(700, statistics0.getValue(m_longIndex1));
    statistics0.setValue(m_longIndex1, -300);
    assertEquals(-300, statistics0.getValue(m_longIndex1));
    assertNotEquals(statistics0, statistics1);
    assertNotEquals(statistics1, statistics0);

    statistics1.setValue(m_longIndex1, 500);
    assertNotEquals(statistics0, statistics1);
    statistics1.setValue(m_longIndex1, -300);
    assertEquals(statistics0, statistics1);

    statistics0.setValue(m_longIndex0, 1);
    assertNotEquals(statistics0, statistics1);
    statistics1.setValue(m_longIndex0, 1);
    assertEquals(statistics0, statistics1);

    assertEquals(statistics0, statistics0);
    assertEquals(statistics1, statistics1);

    statistics0.setValue(m_longIndex2, 0);
    assertEquals(statistics0, statistics1);

    statistics0.setValue(m_doubleIndex2, 7.00d);
    assertDoublesEqual(7.00d, statistics0.getValue(m_doubleIndex2));
    statistics0.setValue(m_doubleIndex2, 3.00d);
    assertDoublesEqual(3.00d, statistics0.getValue(m_doubleIndex2));
    assertNotEquals(statistics0, statistics1);

    statistics1.setValue(m_doubleIndex2, 5.00d);
    assertNotEquals(statistics0, statistics1);
    statistics1.setValue(m_doubleIndex2, 3.00d);
    assertEquals(statistics0, statistics1);

    statistics0.setValue(m_doubleIndex0, -1.0d);
    assertNotEquals(statistics0, statistics1);
    statistics1.setValue(m_doubleIndex0, -1.0d);
    assertEquals(statistics0, statistics1);

    assertEquals(statistics0, statistics0);
    assertEquals(statistics1, statistics1);

    statistics0.setValue(m_doubleIndex1, 0);
    assertEquals(statistics0, statistics1);

    assertFalse(statistics0.isComposite());
    statistics0.setIsComposite();
    assertTrue(statistics0.isComposite());
    assertNotEquals(statistics0, statistics1);
    statistics1.setIsComposite();
    assertEquals(statistics0, statistics1);
  }

  @Test public void testAddValue() throws Exception {
    final StatisticsSetImplementation statistics0 =
      new StatisticsSetImplementation(m_indexMap);
    final StatisticsSetImplementation statistics1 =
      new StatisticsSetImplementation(m_indexMap);

    statistics0.addValue(m_longIndex1, 700);
    statistics0.addValue(m_longIndex1, 300);
    assertTrue(!statistics0.equals(statistics1));
    statistics1.addValue(m_longIndex1, 500);
    assertTrue(!statistics0.equals(statistics1));
    statistics1.addValue(m_longIndex1, 500);
    assertEquals(statistics0, statistics1);

    statistics0.addValue(m_doubleIndex1, 7.00d);
    statistics0.addValue(m_doubleIndex1, 3.00d);
    assertTrue(!statistics0.equals(statistics1));
    statistics1.addValue(m_doubleIndex1, 5.00d);
    assertTrue(!statistics0.equals(statistics1));
    statistics1.addValue(m_doubleIndex1, 5.00d);
    assertEquals(statistics0, statistics1);
  }

  @Test public void testAddTransientValue() throws Exception {
    final StatisticsSetImplementation statistics0 =
      new StatisticsSetImplementation(m_indexMap);
    final StatisticsSetImplementation statistics1 =
      new StatisticsSetImplementation(m_indexMap);

    statistics0.addValue(m_transientLongIndex, 700);
    statistics1.addValue(m_transientLongIndex, 300);
    assertEquals(statistics0, statistics1);
    assertEquals(0, statistics1.getValue(m_transientLongIndex));
  }

  @Test public void testAdd() throws Exception {
    final StatisticsSetImplementation statistics0 =
      new StatisticsSetImplementation(m_indexMap);
    final StatisticsSetImplementation statistics1 =
      new StatisticsSetImplementation(m_indexMap);

    assertTrue(statistics1.isZero());
    assertFalse(statistics1.isComposite());

    // 0 + 0 = 0
    statistics0.add(statistics1);
    assertEquals(statistics0, statistics1);

    assertFalse(statistics0.isZero());
    assertTrue(statistics1.isZero());
    assertFalse(statistics1.isComposite());

    // 0 + 1 = 1
    statistics0.addValue(m_longIndex0, 100);
    statistics0.addValue(m_doubleIndex2, -5.5);
    statistics0.setIsComposite();
    statistics1.add(statistics0);
    assertEquals(statistics0, statistics1);
    assertTrue(statistics1.isComposite());

    // 1 + 1 != 1
    statistics1.add(statistics0);
    assertTrue(!statistics0.equals(statistics1));

    // 1 + 1 = 2
    statistics0.add(statistics0); // Test add to self.
    assertEquals(statistics0, statistics1);

    assertEquals(200, statistics0.getValue(m_longIndex0));
    assertDoublesEqual(-11d, statistics0.getValue(m_doubleIndex2));
  }

  @Test public void testSnapshot() throws Exception {
    final StatisticsSetImplementation original =
      new StatisticsSetImplementation(m_indexMap);
    original.addValue(m_longIndex0, 10);
    original.setValue(m_doubleIndex2, 3);
    original.setIsComposite();

    final StatisticsSet snapshot = original.snapshot();

    assertDoublesEqual(3d, snapshot.getValue(m_doubleIndex2));
    assertEquals(0, snapshot.getValue(m_longIndex1));
    assertEquals(10, snapshot.getValue(m_longIndex0));
    assertTrue(original.isComposite());

    assertDoublesEqual(3d, original.getValue(m_doubleIndex2));
    assertEquals(0, original.getValue(m_longIndex1));
    assertEquals(10, original.getValue(m_longIndex0));

    original.addValue(m_longIndex0, 5);
    original.addValue(m_doubleIndex0, 20);
    snapshot.addValue(m_longIndex0, 1);

    assertDoublesEqual(3d, snapshot.getValue(m_doubleIndex2));
    assertEquals(0, snapshot.getValue(m_longIndex1));
    assertEquals(11, snapshot.getValue(m_longIndex0));

    assertDoublesEqual(3d, original.getValue(m_doubleIndex2));
    assertDoublesEqual(20d, original.getValue(m_doubleIndex0));
    assertEquals(0, original.getValue(m_longIndex1));
    assertEquals(15, original.getValue(m_longIndex0));

    original.reset();
    final StatisticsSet snapshot2 = original.snapshot();
    assertTrue(snapshot2.isZero());
  }

  @Test public void testLongSampleReadAndWrite() throws Exception {
    final StatisticsSet rawStatistics0 =
      new StatisticsSetImplementation(m_indexMap);

    final StatisticsIndexMap.LongSampleIndex longSampleIndex =
      m_indexMap.getLongSampleIndex("timedTests");

    assertEquals(0, rawStatistics0.getSum(longSampleIndex));
    assertEquals(0, rawStatistics0.getCount(longSampleIndex));
    assertDoublesEqual(0d, rawStatistics0.getVariance(longSampleIndex));

    rawStatistics0.addSample(longSampleIndex, 0);
    assertEquals(0, rawStatistics0.getSum(longSampleIndex));
    assertEquals(1, rawStatistics0.getCount(longSampleIndex));
    assertDoublesEqual(0d, rawStatistics0.getVariance(longSampleIndex));

    rawStatistics0.addSample(longSampleIndex, 5);
    assertEquals(5, rawStatistics0.getSum(longSampleIndex));
    assertEquals(2, rawStatistics0.getCount(longSampleIndex));
    assertDoublesEqual(6.25, rawStatistics0.getVariance(longSampleIndex));

    rawStatistics0.addSample(longSampleIndex, 1);
    assertEquals(6, rawStatistics0.getSum(longSampleIndex));
    assertEquals(3, rawStatistics0.getCount(longSampleIndex));
    assertDoublesEqual(14 / 3d, rawStatistics0.getVariance(longSampleIndex));

    rawStatistics0.reset(longSampleIndex);
    assertEquals(0, rawStatistics0.getSum(longSampleIndex));
    assertEquals(0, rawStatistics0.getCount(longSampleIndex));
    assertDoublesEqual(0d, rawStatistics0.getVariance(longSampleIndex));

    final StatisticsSet rawStatistics1 =
      new StatisticsSetImplementation(m_indexMap);

    rawStatistics1.addSample(longSampleIndex, -1);
    assertEquals(-1, rawStatistics1.getSum(longSampleIndex));
    assertEquals(1, rawStatistics1.getCount(longSampleIndex));
    assertDoublesEqual(0, rawStatistics1.getVariance(longSampleIndex));

    rawStatistics0.add(rawStatistics1);
    assertEquals(-1, rawStatistics0.getSum(longSampleIndex));
    assertEquals(1, rawStatistics0.getCount(longSampleIndex));
    assertDoublesEqual(0, rawStatistics0.getVariance(longSampleIndex));
    assertEquals(-1, rawStatistics1.getSum(longSampleIndex));
    assertEquals(1, rawStatistics1.getCount(longSampleIndex));
    assertDoublesEqual(0, rawStatistics1.getVariance(longSampleIndex));
  }

  @Test public void testDoubleSampleReadAndWrite() throws Exception {
    try {
      final StatisticsIndexMap.DoubleIndex sumIndex = m_indexMap
          .getDoubleIndex("userDouble0");
      final StatisticsIndexMap.LongIndex countIndex = m_indexMap
          .getLongIndex("userLong0");
      final StatisticsIndexMap.DoubleIndex varianceIndex = m_indexMap
          .getDoubleIndex("userDouble1");

      final StatisticsIndexMap.DoubleSampleIndex doubleSampleIndex = m_indexMap
          .createDoubleSampleIndex("testDoubleSampleStatistic", sumIndex,
            countIndex, varianceIndex);

      final StatisticsSet rawStatistics0 =
        new StatisticsSetImplementation(m_indexMap);

      assertDoublesEqual(0, rawStatistics0.getSum(doubleSampleIndex));
      assertEquals(0, rawStatistics0.getCount(doubleSampleIndex));
      assertDoublesEqual(0d, rawStatistics0.getVariance(doubleSampleIndex));

      rawStatistics0.addSample(doubleSampleIndex, 0);
      assertDoublesEqual(0, rawStatistics0.getSum(doubleSampleIndex));
      assertEquals(1, rawStatistics0.getCount(doubleSampleIndex));
      assertDoublesEqual(0d, rawStatistics0.getVariance(doubleSampleIndex));

      rawStatistics0.addSample(doubleSampleIndex, 5);
      assertDoublesEqual(5, rawStatistics0.getSum(doubleSampleIndex));
      assertEquals(2, rawStatistics0.getCount(doubleSampleIndex));
      assertDoublesEqual(6.25, rawStatistics0.getVariance(doubleSampleIndex));

      rawStatistics0.addSample(doubleSampleIndex, 1);
      assertDoublesEqual(6, rawStatistics0.getSum(doubleSampleIndex));
      assertEquals(3, rawStatistics0.getCount(doubleSampleIndex));
      assertDoublesEqual(14 / 3d, rawStatistics0.getVariance(doubleSampleIndex));

      final StatisticsSet rawStatistics1 =
        new StatisticsSetImplementation(m_indexMap);

      rawStatistics0.add(rawStatistics1);
      assertDoublesEqual(6, rawStatistics0.getSum(doubleSampleIndex));
      assertEquals(3, rawStatistics0.getCount(doubleSampleIndex));
      assertDoublesEqual(14 / 3d, rawStatistics0.getVariance(doubleSampleIndex));
      assertDoublesEqual(0, rawStatistics1.getSum(doubleSampleIndex));
      assertEquals(0, rawStatistics1.getCount(doubleSampleIndex));
      assertDoublesEqual(0d, rawStatistics1.getVariance(doubleSampleIndex));

      rawStatistics1.addSample(doubleSampleIndex, 5);
      rawStatistics1.addSample(doubleSampleIndex, -5);
      assertDoublesEqual(0, rawStatistics1.getSum(doubleSampleIndex));
      assertEquals(2, rawStatistics1.getCount(doubleSampleIndex));
      assertDoublesEqual(25, rawStatistics1.getVariance(doubleSampleIndex));

      rawStatistics0.add(rawStatistics1);
      assertDoublesEqual(6, rawStatistics0.getSum(doubleSampleIndex));
      assertEquals(5, rawStatistics0.getCount(doubleSampleIndex));
      assertDoublesEqual(13.76, rawStatistics0.getVariance(doubleSampleIndex));
      assertDoublesEqual(0, rawStatistics1.getSum(doubleSampleIndex));
      assertEquals(2, rawStatistics1.getCount(doubleSampleIndex));
      assertDoublesEqual(25, rawStatistics1.getVariance(doubleSampleIndex));

      rawStatistics0.reset(doubleSampleIndex);
      assertDoublesEqual(0, rawStatistics0.getSum(doubleSampleIndex));
      assertEquals(0, rawStatistics0.getCount(doubleSampleIndex));
      assertDoublesEqual(0d, rawStatistics0.getVariance(doubleSampleIndex));
    }
    finally {
      m_indexMap.removeDoubleSampleIndex("testDoubleSampleStatistic");
    }
  }

  @Test public void testSerialisation() throws Exception {
    final Random random = new Random();

    final StatisticsSetImplementation original0 =
      new StatisticsSetImplementation(m_indexMap);
    original0.addValue(m_longIndex0, Math.abs(random.nextLong()));
    original0.setValue(m_transientLongIndex, Math.abs(random.nextLong()));
    original0.addValue(m_longIndex2, Math.abs(random.nextLong()));

    final StatisticsSetImplementation original1 =
      new StatisticsSetImplementation(m_indexMap);
    original1.setIsComposite();

    final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

    final ObjectOutputStream objectOutputStream =
      new ObjectOutputStream(byteOutputStream);

    final Serialiser serialiser = new Serialiser();

    original0.writeExternal(objectOutputStream, serialiser);
    original1.writeExternal(objectOutputStream, serialiser);

    objectOutputStream.close();

    final ObjectInputStream objectInputStream = new ObjectInputStream(
      new ByteArrayInputStream(byteOutputStream.toByteArray()));

    final StatisticsSetImplementation received0 =
      new StatisticsSetImplementation(m_indexMap,
                                      objectInputStream,
                                      serialiser);

    final StatisticsSetImplementation received1 =
      new StatisticsSetImplementation(m_indexMap,
                                      objectInputStream,
                                      serialiser);

    // Transient values aren't serialised.
    assertEquals(0, received0.getValue(m_transientLongIndex));
    original0.setValue(m_transientLongIndex, 0);

    assertEquals(original0, received0);
    assertEquals(original1, received1);
  }

  @Test public void testEqualsMiscellanea() throws Exception {
    final StatisticsSet rawStatistics0 =
      new StatisticsSetImplementation(m_indexMap);
    final StatisticsSet rawStatistics1 =
      new StatisticsSetImplementation(m_indexMap);

    assertFalse(rawStatistics0.equals(this));

    final int hashCode0 = rawStatistics0.hashCode();
    assertEquals(hashCode0, rawStatistics1.hashCode());
    assertEquals(hashCode0, rawStatistics0.hashCode());
    rawStatistics0.setValue(m_longIndex0, 99);
    assertFalse(rawStatistics0.hashCode() == hashCode0);
  }

  private static String commaSeparate(int n, String value) {
    final StringBuilder result = new StringBuilder();

    result.append("{");

    for (int i = 0; i < n; ++i) {
      if (i != 0) {
        result.append(", ");
      }

      result.append(value);
    }

    result.append("}");

    return result.toString();
  }

  @Test public void testToString() throws Exception {

    final StatisticsSet rawStatistics =
      new StatisticsSetImplementation(m_indexMap);
    final String s0 = rawStatistics.toString();

    assertTrue(s0.indexOf(commaSeparate(m_indexMap.getNumberOfLongs(), "0"))
               >= 0);

    assertTrue(s0.indexOf(
                 commaSeparate(m_indexMap.getNumberOfTransientLongs(), "0"))
               >= 0);

    assertTrue(s0.indexOf(commaSeparate(m_indexMap.getNumberOfDoubles(), "0.0"))
               >= 0);

    assertTrue(s0.indexOf("composite = false") >= 0);
    rawStatistics.setIsComposite();
    assertTrue(rawStatistics.toString().indexOf("composite = true") >= 0);
  }

  @Test public void testToString2() throws Exception {

    final StatisticsIndexMap statisticsIndexMap2 =
      new StatisticsIndexMap(asList("a"),
                             asList("b"),
                             asList("c", "d"),
                             asList("timedTests"));

    final StatisticsSet rawStatistics =
      new StatisticsSetImplementation(statisticsIndexMap2);

    assertEquals(
      "StatisticsSet = {{0, 0, 0}, {0.0, 0.0}, {0, 0}, composite = false}",
      rawStatistics.toString().toString());
  }

  private void assertDoublesEqual(double a, double b) {
    assertEquals(a, b, 0.000000001d);
  }
}
