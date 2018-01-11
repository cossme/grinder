// Copyright (C) 2006 Philip Aston
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
 * Read-only view of a statistics set, see {@link StatisticsSet}.
 *
 * @author Philip Aston
 */
public interface ImmutableStatisticsSet {

  /**
   * Clone this object.
   *
   * @return A copy of this StatisticsSet.
   */
  StatisticsSet snapshot();

  /**
   * Return the value specified by <code>index</code>.
   *
   * @param index The index.
   * @return The value.
   */
  long getValue(StatisticsIndexMap.LongIndex index);

  /**
   * Return the value specified by <code>index</code>.
   *
   * @param index The index.
   * @return The value.
   */
  double getValue(StatisticsIndexMap.DoubleIndex index);

  /**
   * Get the total sample value for the sample statistic specified by
   * <code>index</code>.
   *
   * @param index The index.
   * @return The sum.
   */
  long getSum(StatisticsIndexMap.LongSampleIndex index);

  /**
   * Get the total sample value for the sample statistic specified by
   * <code>index</code>.
   *
   * @param index The index.
   * @return The sum.
   */
  double getSum(StatisticsIndexMap.DoubleSampleIndex index);

  /**
   * Get the number of samples for the sample statistic specified by
   * <code>index</code>.
   *
   * @param index The index.
   * @return The count.
   */
  long getCount(StatisticsIndexMap.SampleIndex index);

  /**
   * Get the sample variance for the sample statistic specified by
   * <code>index</code>.
   *
   * @param index The index.
   * @return The count.
   */
  double getVariance(StatisticsIndexMap.SampleIndex index);

  /**
   * Return whether all the statistics are zero. This allows us to optimise
   * cases where there's no information to be processed.
   *
   * <p>
   * This method can return <code>false</code>, even if all of the statistics
   * are zero; but if it returns <code>true</code> they are guaranteed to be
   * zero.
   * </p>
   *
   * @return <code>true</code> => all values are zero.
   */
  boolean isZero();

  /**
   * Return whether this statistics set has been marked as containing
   * composite statistics.
   *
   * @return <code>true</code> => this statistics set contains composite
   * statistics.
   */
  boolean isComposite();
}
