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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import net.grinder.statistics.StatisticsIndexMap.DoubleIndex;
import net.grinder.statistics.StatisticsIndexMap.DoubleSampleIndex;
import net.grinder.statistics.StatisticsIndexMap.LongIndex;
import net.grinder.statistics.StatisticsIndexMap.LongSampleIndex;
import net.grinder.statistics.StatisticsIndexMap.SampleIndex;
import net.grinder.util.Serialiser;


/**
 * Store an array of raw statistics as unsigned long or double values. Clients
 * can access individual values using an index obtained from a {@link
 * StatisticsIndexMap}.
 *
 * @author Philip Aston
 */
final class StatisticsSetImplementation implements StatisticsSet {

  private final StatisticsIndexMap m_statisticsIndexMap;
  private final long[] m_longData;
  private final double[] m_doubleData;

  // Transient fields are context specific. They are not serialised, nor are
  // they added to other statistics sets. E.g. the "period" field.
  private transient long[] m_transientLongData;

  // true => all statistics are zero; false => they might be.
  private boolean m_zero = true;

  private boolean m_composite;

  /**
   * Creates a new <code>StatisticsSetImplementation</code> instance.
   *
   * @param statisticsIndexMap The {@link StatisticsIndexMap} to use.
   */
  StatisticsSetImplementation(StatisticsIndexMap statisticsIndexMap) {
    m_statisticsIndexMap = statisticsIndexMap;
    m_longData = new long[m_statisticsIndexMap.getNumberOfLongs()];
    m_doubleData = new double[m_statisticsIndexMap.getNumberOfDoubles()];
    m_transientLongData =
      new long[m_statisticsIndexMap.getNumberOfTransientLongs()];
  }

  /**
   * Reset this StatisticsSet to default values. Allows instance to
   * be reused.
   */
  public synchronized void reset() {
    if (!m_zero) {
      Arrays.fill(m_longData, 0);
      Arrays.fill(m_doubleData, 0);
      Arrays.fill(m_transientLongData, 0);
      m_zero = true;
      m_composite = false;
    }
  }

  /**
   * Clone this object.
   *
   * We don't use {@link Object#clone} as that's such a hog's arse of a
   * mechanism. It prevents us from using final variables, requires a lot of
   * casting, and that we catch CloneNotSupported exceptions - even if we know
   * they won't be thrown.
   *
   * @return A copy of this StatisticsSetImplementation.
   */
  public synchronized StatisticsSet snapshot() {
    final StatisticsSetImplementation result =
      new StatisticsSetImplementation(m_statisticsIndexMap);

    if (!m_zero) {
      synchronized (result) {
        System.arraycopy(m_longData,
                         0,
                         result.m_longData,
                         0,
                         result.m_longData.length);

        System.arraycopy(m_doubleData,
                         0,
                         result.m_doubleData,
                         0,
                         result.m_doubleData.length);

        System.arraycopy(m_transientLongData,
                         0,
                         result.m_transientLongData,
                         0, result.m_transientLongData.length);

        result.m_zero = false;
        result.m_composite = m_composite;
      }
    }

    return result;
  }

  /**
   * Return the value specified by <code>index</code>.
   *
   * <p>Unfortunately the Java language specification does not make
   * long access atomic, so we must synchronise.</p>
   *
   * @param index The process specific index.
   * @return The value.
   */
  public synchronized long getValue(LongIndex index) {
    if (index.isTransient()) {
      return m_transientLongData[index.getValue()];
    }

    return m_longData[index.getValue()];
  }

  /**
   * Return the value specified by <code>index</code>.
   *
   * <p>Unfortunately the Java language specification does not make
   * double access atomic, so we must synchronise.</p>
   *
   * @param index The process specific index.
   * @return The value.
   */
  public synchronized double getValue(DoubleIndex index) {
    return m_doubleData[index.getValue()];
  }

  /**
   * Set the value specified by <code>index</code>.
   *
   * <p>Unfortunately the Java language specification does not make
   * long access atomic, so we must synchronise.</p>
   *
   * @param index The process specific index.
   * @param value The value.
   */
  public synchronized void setValue(LongIndex index, long value) {
    if (index.isTransient()) {
      m_transientLongData[index.getValue()] = value;
    }
    else {
      m_longData[index.getValue()] = value;
    }

    m_zero &= value == 0;
  }

  /**
   * Set the value specified by <code>index</code>.
   *
   * <p>Unfortunately the Java language specification does not make
   * double access atomic, so we must synchronise.</p>
   *
   * @param index The process specific index.
   * @param value The value.
   */
  public synchronized void setValue(DoubleIndex index, double value) {
    m_doubleData[index.getValue()] = value;
    m_zero &= value == 0;
  }

  /**
   * Add <code>value</code> to the value specified by
   * <code>index</code>.
   *
   * <p>Synchronised to ensure we don't lose information.</p>.
   *
   * @param index The process specific index.
   * @param value The value.
   */
  public synchronized void addValue(LongIndex index, long value) {
    if (!index.isTransient()) {
      m_longData[index.getValue()] += value;
      m_zero &= value == 0;
    }
  }

  /**
   * Add <code>value</code> to the value specified by
   * <code>index</code>.
   *
   * <p>Synchronised to ensure we don't lose information.</p>.
   *
   * @param index The process specific index.
   * @param value The value.
   */
  public synchronized void addValue(DoubleIndex index, double value) {

    m_doubleData[index.getValue()] += value;
    m_zero &= value == 0;
  }

  /**
   * Add sample <code>value</code> to the sample statistic specified by
   * <code>index</code>.
   *
   * @param index The index.
   * @param value The value.
   */
  public synchronized void addSample(LongSampleIndex index, long value) {

    setValue(index.getVarianceIndex(),
        calculateVariance(getValue(index.getSumIndex()),
                          getValue(index.getCountIndex()),
                          getValue(index.getVarianceIndex()),
                          value));

    m_longData[index.getSumIndex().getValue()] += value;
    ++m_longData[index.getCountIndex().getValue()];
    m_zero = false;
  }

  /**
   * Add sample <code>value</code> to the sample statistic specified by
   * <code>index</code>.
   *
   * @param index The index.
   * @param value The value.
   */
  public synchronized void addSample(DoubleSampleIndex index, double value) {

    setValue(index.getVarianceIndex(),
        calculateVariance(getValue(index.getSumIndex()),
                          getValue(index.getCountIndex()),
                          getValue(index.getVarianceIndex()),
                          value));

    m_doubleData[index.getSumIndex().getValue()] += value;
    ++m_longData[index.getCountIndex().getValue()];
    m_zero = false;
  }

  /**
   * Reset the sample statistic specified by <code>index</code>.
   *
   * @param index
   */
  public synchronized void reset(LongSampleIndex index) {
    setValue(index.getSumIndex(), 0);
    setValue(index.getCountIndex(), 0);
    setValue(index.getVarianceIndex(), 0);
  }

  /**
   * Reset the sample statistic specified by <code>index</code>.
   *
   * @param index
   */
  public synchronized void reset(DoubleSampleIndex index) {
    setValue(index.getSumIndex(), 0);
    setValue(index.getCountIndex(), 0);
    setValue(index.getVarianceIndex(), 0);
  }

  /**
   * Calculate the variance resulting from adding the sample value
   * <code>newValue</code> to the a population with original attributes (
   * <code>sum</code>, <code>count</code>, <code>variance</code>).
   *
   * @param sum Original total of sample values.
   * @param count Original number of sample values.
   * @param variance Original sample variance.
   * @param newValue New value.
   * @return New sample variance.
   */
  private double calculateVariance(double sum,
                                   long count,
                                   double variance,
                                   double newValue) {
    if (count == 0) {
      return 0;
    }

    final long t1 = count + 1;
    final double t2 = newValue - sum / count;

    return
      (count * variance) / (count + 1) +
      (count / (double)(t1 * t1)) * t2 * t2;
  }

  /**
   * Calculate the variance resulting from combining two sample populations.
   *
   * @param s1 Total of samples in first population.
   * @param n1 Count of samples in first population.
   * @param v1 Variance of samples in first population.
   * @param s2 Total of samples in second population.
   * @param n2 Count of samples in second population.
   * @param v2 Variance of samples in second population.
   * @return Variance of combined population.
   */
  private double calculateVariance(double s1, long n1, double v1,
                                   double s2, long n2, double v2) {
    if (n1 == 0) {
      return v2;
    }
    else if (n2 == 0) {
      return v1;
    }

    final double s = s1 + s2;
    final long n = n1 + n2;

    final double term1 = s1 / n1 - s / n;
    final double term2 = s2 / n2 - s / n;

    return (n1 * (term1 * term1 + v1) + n2 * (term2 * term2 + v2)) / n;
  }

  /**
   * Get the total sample value for the sample statistic specified by
   * <code>index</code>.
   *
   * @param index The index.
   * @return The sum.
   */
  public long getSum(LongSampleIndex index) {
    return getValue(index.getSumIndex());
  }

  /**
   * Get the total sample value for the sample statistic specified by
   * <code>index</code>.
   *
   * @param index The index.
   * @return The sum.
   */
  public double getSum(DoubleSampleIndex index) {
    return getValue(index.getSumIndex());
  }

  /**
   * Get the number of samples for the sample statistic specified by
   * <code>index</code>.
   *
   * @param index The index.
   * @return The count.
   */
  public long getCount(SampleIndex index) {
    return getValue(index.getCountIndex());
  }

  /**
   * Get the sample variance for the sample statistic specified by
   * <code>index</code>.
   *
   * @param index The index.
   * @return The count.
   */
  public double getVariance(SampleIndex index) {
    return getValue(index.getVarianceIndex());
  }

  /**
   * Add the values of another <code>StatisticsSet</code> to ours. Assumes we
   * don't need to synchronise access to operand.
   *
   * <p>
   * <strong>Currently the implementation assumes that the argument is actually
   * a <code>StatisticsSetImplementation</code>.</strong>
   * </p>
   *
   * <p>
   * Synchronised to ensure we don't lose information.
   * </p>.
   *
   * @param operand
   *          The statistics set value to add.
   */
  public synchronized void add(ImmutableStatisticsSet operand) {

    final StatisticsSetImplementation operandImplementation =
      (StatisticsSetImplementation)operand;

    final boolean[] isVarianceIndex = new boolean[m_doubleData.length];

    for (LongSampleIndex index : m_statisticsIndexMap.getLongSampleIndicies()) {
      final LongIndex sumIndex = index.getSumIndex();
      final LongIndex countIndex = index.getCountIndex();
      final DoubleIndex varianceIndex = index.getVarianceIndex();

      setValue(varianceIndex,
        calculateVariance(getValue(sumIndex),
                          getValue(countIndex),
                          getValue(varianceIndex),
                          operand.getValue(sumIndex),
                          operand.getValue(countIndex),
                          operand.getValue(varianceIndex)));

      isVarianceIndex[varianceIndex.getValue()] = true;
    }

    for (DoubleSampleIndex index :
         m_statisticsIndexMap.getDoubleSampleIndicies()) {

      final DoubleIndex sumIndex = index.getSumIndex();
      final LongIndex countIndex = index.getCountIndex();
      final DoubleIndex varianceIndex = index.getVarianceIndex();

      setValue(varianceIndex,
               calculateVariance(getValue(sumIndex),
                                 getValue(countIndex),
                                 getValue(varianceIndex),
                                 operand.getValue(sumIndex),
                                 operand.getValue(countIndex),
                                 operand.getValue(varianceIndex)));

      isVarianceIndex[varianceIndex.getValue()] = true;
    }

    final long[] longData = operandImplementation.m_longData;

    for (int i = 0; i < longData.length; i++) {
      m_longData[i] += longData[i];
    }

    final double[] doubleData = operandImplementation.m_doubleData;

    for (int i = 0; i < doubleData.length; i++) {
      if (!isVarianceIndex[i]) {
        m_doubleData[i] += doubleData[i];
      }
    }

    m_zero = false;

    if (operand.isComposite()) {
      setIsComposite();
    }
  }

  public synchronized boolean isZero() {
    return m_zero;
  }

  public synchronized boolean isComposite() {
    return m_composite;
  }

  public synchronized void setIsComposite() {
    m_composite = true;
  }

  /**
   * Implement value based equality. Mainly used by unit tests.
   *
   * @param o <code>Object</code> to compare to.
   * @return <code>true</code> if and only if the two objects are equal.
   */
  public synchronized boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (o == null || o.getClass() != StatisticsSetImplementation.class) {
      return false;
    }

    final StatisticsSetImplementation otherStatistics =
      (StatisticsSetImplementation)o;

    synchronized (otherStatistics) {

      if (m_composite != otherStatistics.m_composite) {
        return false;
      }

      final long[] otherLongData = otherStatistics.m_longData;

      for (int i = 0; i < m_longData.length; i++) {
        if (m_longData[i] != otherLongData[i]) {
          return false;
        }
      }

      for (int i = 0; i < m_doubleData.length; i++) {
        if (m_doubleData[i] != otherStatistics.m_doubleData[i]) {
          return false;
        }
      }

      final long[] otherTransientLongData = otherStatistics.m_transientLongData;

      for (int i = 0; i < m_transientLongData.length; i++) {
        if (m_transientLongData[i] != otherTransientLongData[i]) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Defer to <code>Object.hashCode()</code>.
   *
   * <p>We define <code>hashCode</code> to keep Checkstyle happy, but
   * we don't use it.
   *
   * @return The hash code.
   */
  public int hashCode() {
    long result = 0;

    for (int i = 0; i < m_longData.length; i++) {
      result ^= m_longData[i];
    }

    for (int i = 0; i < m_doubleData.length; i++) {
      result ^= Double.doubleToRawLongBits(m_doubleData[i]);
    }

    for (int i = 0; i < m_transientLongData.length; i++) {
      result ^= m_transientLongData[i];
    }

    return (int)(result ^ (result >> 32));
  }

  /**
   * Return a <code>String</code> representation of this
   * <code>StatisticsSet</code>.
   *
   * @return The <code>String</code>
   */
  public synchronized String toString() {
    final StringBuilder result = new StringBuilder();

    result.append("StatisticsSet = {{");

    for (int i = 0; i < m_longData.length; i++) {
      if (i != 0) {
        result.append(", ");
      }

      result.append(m_longData[i]);
    }

    result.append("}, {");

    for (int i = 0; i < m_doubleData.length; i++) {
      if (i != 0) {
        result.append(", ");
      }

      result.append(m_doubleData[i]);
    }

    result.append("}, {");

    for (int i = 0; i < m_transientLongData.length; i++) {
      if (i != 0) {
        result.append(", ");
      }

      result.append(m_transientLongData[i]);
    }

    result.append("}, composite = ");
    result.append(m_composite ? "true" : "false");
    result.append("}");

    return result.toString();
  }

  /**
   * Efficient externalisation method used by {@link
   * StatisticsSetFactory#writeStatisticsExternal}.
   *
   * <p>Why not implement java.io.Externalizable? Because I consider it
   * morally dubious:
   * <ul>
   * <li>It requires a public default constructor which either has to bypass
   * class constraints, or discover external dependencies through singletons or
   * a service registry</li>
   * <li><em>readExternal</em> should only be called by the serialisation
   * engine, but needs to be public.</li>
   * </ul>
   * </p>
   *
   * <p>Synchronised to ensure a consistent view.</p>
   *
   * <p>If you change this, update TestStatisticsMap serialVersionUID.</p>
   *
   * @param out Handle to the output stream.
   * @param serialiser <code>Serialiser</code> helper object.
   * @exception IOException If an error occurs.
   * @see #StatisticsSetImplementation(StatisticsIndexMap, ObjectInput,
   * Serialiser)
   */
  synchronized void writeExternal(ObjectOutput out, Serialiser serialiser)
    throws IOException {
    for (int i = 0; i < m_longData.length; i++) {
      serialiser.writeLong(out, m_longData[i]);
    }

    for (int i = 0; i < m_doubleData.length; i++) {
      serialiser.writeDouble(out, m_doubleData[i]);
    }

    out.writeBoolean(m_composite);
  }

  /**
   * Efficient externalisation method used by {@link
   * StatisticsSetFactory#readStatisticsExternal}.
   *
   * <p>If you change this, update TestStatisticsMap serialVersionUID.</p>
   *
   * @param statisticsIndexMap The {@link StatisticsIndexMap} to use.
   * @param in Handle to the input stream.
   * @param serialiser <code>Serialiser</code> helper object.
   * @exception IOException If an error occurs.
   * @see #writeExternal(ObjectOutput, Serialiser)
   */
  StatisticsSetImplementation(StatisticsIndexMap statisticsIndexMap,
                              ObjectInput in, Serialiser serialiser)
    throws IOException {
    this(statisticsIndexMap);

    for (int i = 0; i < m_longData.length; i++) {
      m_longData[i] = serialiser.readLong(in);
      m_zero &= m_longData[i] == 0;
    }

    for (int i = 0; i < m_doubleData.length; i++) {
      m_doubleData[i] = serialiser.readDouble(in);
      m_zero &= m_doubleData[i] == 0;
    }

    m_composite = in.readBoolean();
  }
}
