// Copyright (C) 2004 - 2013 Philip Aston
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
import net.grinder.common.TimeAuthority;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSetFactory;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.TimeAuthorityStubFactory;


/**
 * Unit test case for <code>TestRegistryImplementation</code>.
 *
 * @author Philip Aston
 */
public class TestTestRegistry extends TestCase {
  private final RandomStubFactory<TestStatisticsHelper>
    m_testStatisticsHelperStubFactory =
      RandomStubFactory.create(TestStatisticsHelper.class);
  private final TestStatisticsHelper m_testStatisticsHelper =
    m_testStatisticsHelperStubFactory.getStub();

  private final TimeAuthorityStubFactory m_timeAuthorityStubFactory =
    new TimeAuthorityStubFactory();
  private final TimeAuthority m_timeAuthority =
    m_timeAuthorityStubFactory.getStub();

  public TestTestRegistry(final String name) {
    super(name);
  }

  public void testConstructor() throws Exception {
    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();
    final StatisticsSetFactory statisticsSetFactory =
      StatisticsServicesImplementation.getInstance().getStatisticsSetFactory();

    final TestRegistryImplementation testRegistryImplementation =
      new TestRegistryImplementation(
        threadContextLocator, statisticsSetFactory, m_testStatisticsHelper,
        m_timeAuthority);

    assertNotNull(testRegistryImplementation.getTestStatisticsMap());

    m_testStatisticsHelperStubFactory.assertNoMoreCalls();
    m_timeAuthorityStubFactory.assertNoMoreCalls();
  }

  public void testRegister() throws Exception {
    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();
    final StatisticsSetFactory statisticsSetFactory =
      StatisticsServicesImplementation.getInstance().getStatisticsSetFactory();

    final TestRegistryImplementation testRegistryImplementation =
      new TestRegistryImplementation(
        threadContextLocator, statisticsSetFactory, m_testStatisticsHelper,
        m_timeAuthority);

    assertNull(testRegistryImplementation.getNewTests());

    final Test test1 = new StubTest(1, "Test 1");
    final Test test2 = new StubTest(2, "Test 2");

    try {
      testRegistryImplementation.register(test1);
      fail("Expected AssertionError");
    }
    catch (final AssertionError e) {
    }

    final RandomStubFactory<Instrumenter> instrumenterStubFactory =
      RandomStubFactory.create(Instrumenter.class);
    testRegistryImplementation.setInstrumenter(
      instrumenterStubFactory.getStub());

    final TestRegistryImplementation.RegisteredTest registeredTest1a =
      testRegistryImplementation.register(test1);

    final TestRegistryImplementation.RegisteredTest registeredTest1b =
      testRegistryImplementation.register(test1);

    final TestRegistryImplementation.RegisteredTest registeredTest2 =
      testRegistryImplementation.register(test2);

    assertSame(registeredTest1a, registeredTest1b);
    assertNotSame(registeredTest2, registeredTest1a);

    assertTrue(testRegistryImplementation.getNewTests().contains(test1));
    assertNull(testRegistryImplementation.getNewTests());

    m_testStatisticsHelperStubFactory.assertNoMoreCalls();
    m_timeAuthorityStubFactory.assertNoMoreCalls();
  }
}
