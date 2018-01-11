// Copyright (C) 2001 - 2013 Philip Aston
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import net.grinder.common.Test;
import net.grinder.common.TimeAuthority;
import net.grinder.script.TestRegistry;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.statistics.StatisticsSetFactory;
import net.grinder.statistics.TestStatisticsMap;


/**
 * Registry of Tests.
 *
 * @author Philip Aston
 */
final class TestRegistryImplementation implements TestRegistry {

  private final ThreadContextLocator m_threadContextLocator;
  private final StatisticsSetFactory m_statisticsSetFactory;
  private final TestStatisticsHelper m_testStatisticsHelper;
  private final TimeAuthority m_timeAuthority;

  /**
   * A map of Tests to Statistics for passing elsewhere.
   */
  private final TestStatisticsMap m_testStatisticsMap;

  /**
   * A map of Test to TestData's. (TestData is the class this
   * package uses to store information about Tests). Synchronise on
   * instance when accessing.
   */
  private final Map<Test, TestData> m_testMap = new TreeMap<Test, TestData>();

  /**
   * Tests received since {@link #getNewTests} was last called.
   * Guarded by this.
   */
  private Collection<Test> m_newTests = null;

  private Instrumenter m_instrumenter;

  /**
   * Constructor.
   */
  TestRegistryImplementation(final ThreadContextLocator threadContextLocator,
                             final StatisticsSetFactory statisticsSetFactory,
                             final TestStatisticsHelper testStatisticsHelper,
                             final TimeAuthority timeAuthority) {
    m_threadContextLocator = threadContextLocator;
    m_statisticsSetFactory = statisticsSetFactory;
    m_testStatisticsHelper = testStatisticsHelper;
    m_timeAuthority = timeAuthority;
    m_testStatisticsMap = new TestStatisticsMap(m_statisticsSetFactory);
  }

  /**
   * Register a new test.
   *
   * @param test
   *          The test.
   * @return A ProxyFactory that can be used to create proxies instrumented for
   *         the test.
   */
  @Override
  public RegisteredTest register(final Test test) {

    if (m_instrumenter == null) {
      throw new AssertionError("Instrumenter not set");
    }

    final TestData newTestData;

    synchronized (this) {
      final TestData existing = m_testMap.get(test);

      if (existing != null) {
        return existing;
      }

      newTestData = new TestData(m_threadContextLocator,
                                 m_statisticsSetFactory,
                                 m_testStatisticsHelper,
                                 m_timeAuthority,
                                 m_instrumenter,
                                 test);

      m_testMap.put(test, newTestData);
      m_testStatisticsMap.put(test, newTestData.getTestStatistics());

      if (m_newTests == null) {
        m_newTests = new ArrayList<Test>();
      }

      // To avoid many minor console updates we store a collection of
      // the new tests which is periodically read and sent to the
      // console by the scheduled reporter task.
      m_newTests.add(test);
    }

    return newTestData;
  }

  void setInstrumenter(final Instrumenter instrumenter) {
    m_instrumenter = instrumenter;
  }

  TestStatisticsMap getTestStatisticsMap() {
    return m_testStatisticsMap;
  }

  /**
   * Return any tests registered since the last time
   * <code>getNewTests</code> was called.
   */
  Collection<Test> getNewTests() {
    synchronized (this) {
      try {
        return m_newTests;
      }
      finally {
        m_newTests = null;
      }
    }
  }
}
