// Copyright (C) 2000 - 2006 Philip Aston
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


/**
 * Access to a set of long and double values. Clients can access
 * individual values using an index object obtained from a {@link
 * StatisticsIndexMap}.
 *
 * @author Philip Aston
 * @see net.grinder.script.Grinder.ScriptContext#getStatistics
 */
public interface StatisticsSet extends ImmutableStatisticsSet {

  /**
   * Reset this StatisticsSet to default values. Allows instance to
   * be reused.
   */
  void reset();

  /**
   * Set the value specified by <code>index</code>.
   *
   * @param index The index.
   * @param value The value.
   */
  void setValue(StatisticsIndexMap.LongIndex index, long value);

  /**
   * Set the value specified by <code>index</code>.
   *
   * @param index The index.
   * @param value The value.
   */
  void setValue(StatisticsIndexMap.DoubleIndex index, double value);

  /**
   * Add <code>value</code> to the value specified by
   * <code>index</code>.
   *
   * @param index The index.
   * @param value The value.
   */
  void addValue(StatisticsIndexMap.LongIndex index, long value);

  /**
   * Add <code>value</code> to the value specified by
   * <code>index</code>.
   *
   * @param index The index.
   * @param value The value.
   */
  void addValue(StatisticsIndexMap.DoubleIndex index, double value);

  /**
   * Add sample <code>value</code> to the sample statistic specified by
   * <code>index</code>.
   *
   * @param index The index.
   * @param value The value.
   */
  void addSample(StatisticsIndexMap.LongSampleIndex index, long value);

  /**
   * Add sample <code>value</code> to the sample statistic specified by
   * <code>index</code>.
   *
   * @param index The index.
   * @param value The value.
   */
  void addSample(StatisticsIndexMap.DoubleSampleIndex index, double value);

  /**
   * Reset the sample statistic specified by <code>index</code>.
   *
   * @param index Index of sample statistic.
   */
  void reset(StatisticsIndexMap.LongSampleIndex index);

  /**
   * Reset the sample statistic specified by <code>index</code>.
   *
   * @param index Index of sample statistic.
   */
  void reset(StatisticsIndexMap.DoubleSampleIndex index);

  /**
   * Add the values of another {@link StatisticsSet} to ours.
   * Assumes we don't need to synchronise access to operand.
   * @param operand The {@link StatisticsSet} value to add.
   */
  void add(ImmutableStatisticsSet operand);

  /**
   * Marked this statistics set as containing composite statistics.
   */
  void setIsComposite();
}
