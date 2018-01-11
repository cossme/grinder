// Copyright (C) 2008 Philip Aston
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

import net.grinder.common.StubTest;
import net.grinder.common.Test;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSet;
import junit.framework.TestCase;


/**
 * Unit tests for {@link ModelTestIndex}.
 *
 * @author Philip Aston
 */
public class TestModelTestIndex extends TestCase {

  public void testConstruction() throws Exception {
    final ModelTestIndex nullModelTestIndex = new ModelTestIndex();
    assertEquals(0, nullModelTestIndex.getNumberOfTests());
    assertEquals(0, nullModelTestIndex.getAccumulatorArray().length);

    final StatisticsServices statisticsServices =
      StatisticsServicesImplementation.getInstance();

    final Test[] tests =
      new Test[] {
        new StubTest(100, "first test"),
        new StubTest(101, "second test"),
      };

    final SampleAccumulator[] accumulators =
      new SampleAccumulator[] {
        new SampleAccumulator(
          null, null, statisticsServices.getStatisticsSetFactory()),
        new SampleAccumulator(
          null, null, statisticsServices.getStatisticsSetFactory()),
      };

    final ModelTestIndex modelTestIndex =
      new ModelTestIndex(tests, accumulators);

    assertEquals(tests.length, modelTestIndex.getNumberOfTests());
    assertSame(tests[0], modelTestIndex.getTest(0));
    assertSame(tests[1], modelTestIndex.getTest(1));

    final StatisticsSet cumulativeStatistics0 =
      modelTestIndex.getCumulativeStatistics(0);

    assertNotNull(cumulativeStatistics0);
    assertSame(cumulativeStatistics0, cumulativeStatistics0);
    assertNotSame(cumulativeStatistics0,
                  modelTestIndex.getCumulativeStatistics(1));

    final StatisticsSet lastSampleStatistics0 =
      modelTestIndex.getLastSampleStatistics(0);

    assertNotNull(lastSampleStatistics0);
    assertSame(lastSampleStatistics0, lastSampleStatistics0);
    assertNotSame(lastSampleStatistics0,
                  modelTestIndex.getLastSampleStatistics(1));

    assertNotSame(cumulativeStatistics0, lastSampleStatistics0);
  }
}
