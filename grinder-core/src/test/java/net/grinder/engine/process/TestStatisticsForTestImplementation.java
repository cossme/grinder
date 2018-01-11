// Copyright (C) 2006 - 2009 Philip Aston
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

package net.grinder.engine.process;

import junit.framework.TestCase;

import net.grinder.common.StubTest;
import net.grinder.common.Test;
import net.grinder.script.InvalidContextException;
import net.grinder.script.NoSuchStatisticException;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSet;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test for {@link TestStatisticsForTestImplementation}.
 *
 * @author Philip Aston
 */
public class TestStatisticsForTestImplementation extends TestCase {

  final RandomStubFactory<DispatchContext> m_dispatchContextStubFactory =
    RandomStubFactory.create(DispatchContext.class);
  final DispatchContext m_dispatchContext =
    m_dispatchContextStubFactory.getStub();

  public void testStatisticsForTestImplementation() throws Exception {
    final StatisticsServices statisticsServices =
      StatisticsServicesImplementation.getInstance();
    final StatisticsIndexMap statisticsIndexMap =
      statisticsServices.getStatisticsIndexMap();
    final TestStatisticsHelper testStatisticsHelper =
      new TestStatisticsHelperImplementation(
        statisticsIndexMap);

    final Test test = new StubTest(1, "Hello");
    m_dispatchContextStubFactory.setResult("getTest", test);

    final StatisticsSet statisticsSet =
      statisticsServices.getStatisticsSetFactory().create();

    final StatisticsForTestImplementation statisticsForTest =
      new StatisticsForTestImplementation(
        m_dispatchContext,
        testStatisticsHelper,
        statisticsSet);

    assertEquals(test, statisticsForTest.getTest());
    assertSame(statisticsSet, statisticsForTest.getStatistics());

    assertTrue(statisticsForTest.getSuccess());
    assertEquals(0, statisticsForTest.getLong("userLong0"));
    assertEquals(0, statisticsForTest.getLong("untimedTests"));
    assertEquals(0.0, statisticsForTest.getDouble("userDouble0"), 0.1);

    statisticsForTest.setLong("userLong0", 10);
    statisticsForTest.setDouble("userDouble0", 1.1);
    statisticsForTest.setSuccess(false);
    assertEquals(10, statisticsForTest.getLong("userLong0"));
    assertEquals(1.1, statisticsForTest.getDouble("userDouble0"), 0.1);
    assertFalse(statisticsForTest.getSuccess());

    statisticsForTest.addLong("untimedTests", 5);
    statisticsForTest.addLong("untimedTests", -2);
    statisticsForTest.addDouble("userDouble0", -5.4);
    assertEquals(3, statisticsForTest.getLong("untimedTests"));
    assertEquals(-4.3, statisticsForTest.getDouble("userDouble0"), 0.1);

    assertEquals(
      -4.3,
      statisticsSet.getValue(
        statisticsIndexMap.getDoubleIndex("userDouble0")),
      0.1);

    try {
      statisticsForTest.getLong("userDouble0");
      fail("Expected NoSuchStatisticException");
    }
    catch (NoSuchStatisticException e) {
    }

    try {
      statisticsForTest.getDouble("foo");
      fail("Expected NoSuchStatisticException");
    }
    catch (NoSuchStatisticException e) {
    }

    m_dispatchContextStubFactory.setResult("getElapsedTime", new Long(123));
    assertEquals(123, statisticsForTest.getTime());

    statisticsForTest.setSuccess(true);
    testStatisticsHelper.recordTest(statisticsSet, 5555);

    statisticsForTest.freeze();

    assertNull(statisticsForTest.getStatistics());
    assertEquals(5555, statisticsForTest.getTime());

    try {
      statisticsForTest.setLong("userLong0", 123);
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
    }

    assertEquals(10, statisticsForTest.getLong("userLong0"));
  }
}
