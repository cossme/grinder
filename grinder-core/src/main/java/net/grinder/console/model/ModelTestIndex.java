// Copyright (C) 2003 - 2012 Philip Aston
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

package net.grinder.console.model;

import net.grinder.common.Test;
import net.grinder.statistics.StatisticsSet;


/**
 * Snapshot of current test structure that is valid at time of notification. The
 * test indicies used are numbered between 0 and getNumberOfTests() - 1 and are
 * not related to the test numbers apart from sharing the same ordering.
 *
 * <p>
 * The test array is immutable, but the statistics may change as new samples are
 * received.
 * </p>
 *
 * @author Philip Aston
 * @see SampleModel.Listener#newTests
 */
public final class ModelTestIndex {

  private final Test[] m_testArray;
  private final SampleAccumulator[] m_accumulatorArray;

  /**
   * Default constructor. Public for clients to use as a null model
   * test index.
   */
  public ModelTestIndex() {
    m_testArray = new Test[0];
    m_accumulatorArray = new SampleAccumulator[0];
  }

  ModelTestIndex(Test[] testArray, SampleAccumulator[] accumulatorArray) {
    m_testArray = testArray;
    m_accumulatorArray = accumulatorArray;
  }

  /**
   * Returns the total number of registered tests.
   *
   * @return The number of tests.
   */
  public int getNumberOfTests() {
    return m_testArray.length;
  }

  /**
   * Return a specific test by index.
   *
   * @param testIndex The test index.
   * @return The test.
   */
  public Test getTest(int testIndex) {
    return m_testArray[testIndex];
  }

  /**
   * Get the cumulative test statistics for a given test.
   *
   * @param testIndex The test index.
   * @return The cumulative statistics.
   */
  public StatisticsSet getCumulativeStatistics(int testIndex) {
    return m_accumulatorArray[testIndex].getCumulativeStatistics();
  }

  /**
   * Get the last sample statistics for a given test.
   *
   * @param testIndex The test index.
   * @return The last sample statistics.
   */
  public StatisticsSet getLastSampleStatistics(int testIndex) {
    return m_accumulatorArray[testIndex].getLastSampleStatistics();
  }

  SampleAccumulator[] getAccumulatorArray() {
    return m_accumulatorArray;
  }
}
