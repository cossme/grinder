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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import net.grinder.common.AbstractTestSemantics;
import net.grinder.common.Test;


/**
 * A map of test numbers to {@link StatisticsSet}s.
 *
 * <p>Test statistics synchronisation occurs at the granularity of the
 * contained {@link StatisticsSet} instances. The map is synchronised
 * on the <code>TestStatisticsMap</code> itself.</p>
 *
 * @author Philip Aston
 */
public final class TestStatisticsMap implements java.io.Externalizable {

  // The serialVersionUID should be incremented whenever the default
  // statistic indices are changed in StatisticsIndexMap, or
  // when the StatisticsSet externalisation methods are changed.
  private static final long serialVersionUID = 5L;

  private final transient StatisticsSetFactory m_statisticsSetFactory;

  /**
   * Use a TreeMap so we store in test number order. Synchronise on
   * this TestStatisticsMap before accessing.
   */
  private final Map<Test, StatisticsSet> m_data =
    new TreeMap<Test, StatisticsSet>();

  /**
   * Creates a new <code>TestStatisticsMap</code> instance.
   *
   * @param statisticsSetFactory A factory used to build {@link StatisticsSet}s.
   */
  public TestStatisticsMap(StatisticsSetFactory statisticsSetFactory) {
    m_statisticsSetFactory = statisticsSetFactory;
  }

  /**
   * Externalizable classes need a public default constructor.
   */
  public TestStatisticsMap() {
    // No choice but to initialise the StatisticsSetFactory from a singleton.
    // I hate Externalizable.
    this(
      StatisticsServicesImplementation.getInstance().getStatisticsSetFactory());
  }

  /**
   * Put a new {test, statistics} pair in the map.
   *
   * @param test A test.
   * @param statistics The test's statistics.
   */
  public void put(Test test, StatisticsSet statistics) {
    if (!(statistics instanceof StatisticsSetImplementation)) {
      throw new AssertionError(
        "StatisticsSet implementation not supported");
    }

    synchronized (this) {
      m_data.put(test, statistics);
    }
  }

  /**
   * Return the number of entries in the
   * <code>TestStatisticsMap</code>.
   *
   * @return an <code>int</code> value
   */
  public int size() {
    synchronized (this) {
      return m_data.size();
    }
  }

  /**
   * Add the values in another <code>TestStatisticsMap</code> to this
   * <code>TestStatisticsMap</code>.
   *
   * @param other The other <code>TestStatisticsMap</code>.
   */
  public void add(TestStatisticsMap other) {
    synchronized (other) {
      for (Entry<Test, StatisticsSet> othersEntry : other.m_data.entrySet()) {
        final StatisticsSet statistics;

        synchronized (this) {
          final StatisticsSet existingStatistics =
            m_data.get(othersEntry.getKey());

          if (existingStatistics == null) {
            statistics = m_statisticsSetFactory.create();
            put(othersEntry.getKey(), statistics);
          }
          else {
            statistics = existingStatistics;
          }
        }

        statistics.add(othersEntry.getValue());
      }
    }
  }

  /**
   * Reset all our statistics and return a snapshot.
   *
   * @return The snapshot. Only Tests with non-zero statistics are included.
   */
  public TestStatisticsMap reset() {
    final TestStatisticsMap result =
      new TestStatisticsMap(m_statisticsSetFactory);

    new ForEach() {
      public void next(Test test, StatisticsSet statistics) {
        final StatisticsSet snapshot;

        synchronized (statistics) {
          snapshot = statistics.snapshot();
          statistics.reset();
        }

        if (!snapshot.isZero()) {
          result.put(test, snapshot);
        }
      }
    }
    .iterate();

    return result;
  }

  /**
   * Add up all the non-composite statistics.
   *
   * @return The sum of all the non-composite statistics.
   */
  public StatisticsSet nonCompositeStatisticsTotals() {
    final StatisticsSet result = m_statisticsSetFactory.create();

    new ForEach() {
      public void next(Test test, StatisticsSet statistics) {
        if (!statistics.isComposite()) {
          result.add(statistics);
        }
      }
    }
    .iterate();

    return result;
  }

  /**
   * Add up all the composite statistics.
   *
   * @return The sum of all the composite statistics.
   */
  public StatisticsSet compositeStatisticsTotals() {
    final StatisticsSet result = m_statisticsSetFactory.create();

    new ForEach() {
      public void next(Test test, StatisticsSet statistics) {
        if (statistics.isComposite()) {
          result.add(statistics);
        }
      }
    }
    .iterate();

    return result;
  }

  /**
   * Implement value based equality. Used by unit tests, so we don't
   * bother with synchronisation.
   *
   * @param o <code>Object</code> to compare to.
   * @return <code>true</code> if and only if the two objects are equal.
   */
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (o == null || o.getClass() != TestStatisticsMap.class) {
      return false;
    }

    final TestStatisticsMap otherMap = (TestStatisticsMap)o;

    if (m_data.size() != otherMap.m_data.size()) {
      return false;
    }

    final Iterator<Entry<Test, StatisticsSet>> iterator =
      m_data.entrySet().iterator();
    final Iterator<Entry<Test, StatisticsSet>> otherIterator =
      otherMap.m_data.entrySet().iterator();

    while (iterator.hasNext()) {
      final Entry<Test, StatisticsSet> entry = iterator.next();
      final Entry<Test, StatisticsSet> otherEntry = otherIterator.next();

      if (!entry.getKey().equals(otherEntry.getKey()) ||
          !entry.getValue().equals(otherEntry.getValue())) {
        return false;
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
    return super.hashCode();
  }

  /**
   * Return a <code>String</code> representation of this
   * <code>TestStatisticsMap</code>.
   *
   * @return The <code>String</code>
   */
  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append("TestStatisticsMap = {");

    new ForEach() {
      public void next(Test test, StatisticsSet statisticsSet) {
        result.append("(");
        result.append(test);
        result.append(", ");
        result.append(statisticsSet);
        result.append(")");
      }
    }
    .iterate();

    result.append("}");

    return result.toString();
  }

  /**
   * Efficient externalisation method.
   *
   * @param out Handle to the output stream.
   * @exception IOException If an I/O error occurs.
   */
  public void writeExternal(ObjectOutput out) throws IOException {

    synchronized (this) {
      out.writeInt(m_data.size());

      for (Entry<Test, StatisticsSet> entry : m_data.entrySet()) {
        out.writeInt(entry.getKey().getNumber());

        // Its a class invariant that our StatisticsSets are all
        // StatisticsSetImplementations.
        m_statisticsSetFactory.writeStatisticsExternal(
          out, (StatisticsSetImplementation)entry.getValue());
      }
    }
  }

  /**
   * Efficient externalisation method. No synchronisation, assume that
   * we're being read into a new instance.
   *
   * @param in Handle to the input stream.
   * @exception IOException If an I/O error occurs.
   */
  public void readExternal(ObjectInput in) throws IOException {

    final int n = in.readInt();

    m_data.clear();

    for (int i = 0; i < n; i++) {
      m_data.put(new LightweightTest(in.readInt()),
                 m_statisticsSetFactory.readStatisticsExternal(in));
    }
  }

  /**
   * Light weight test implementation that the console uses.
   */
  private static final class LightweightTest extends AbstractTestSemantics {
    private final int m_number;

    public LightweightTest(int number) {
      m_number = number;
    }

    public int getNumber() {
      return m_number;
    }

    public String getDescription() {
      return "";
    }
  }

  /**
   * Convenient visitor-like iteration.
   */
  public abstract class ForEach {
    /**
     * Runs the iteration.
     */
    public void iterate() {
      synchronized (TestStatisticsMap.this) {
        for (Entry<Test, StatisticsSet> entry : m_data.entrySet()) {
          next(entry.getKey(), entry.getValue());
        }
      }
    }

    /**
     * Receives a call for each item in the iteration.
     *
     * @param test The item's Test.
     * @param statistics The item's statistics.
     */
    protected abstract void next(Test test, StatisticsSet statistics);
  }
}
