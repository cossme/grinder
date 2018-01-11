// Copyright (C) 2005 - 2013 Philip Aston
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
import net.grinder.common.TimeAuthority;
import net.grinder.engine.process.StopWatch.StopWatchNotRunningException;
import net.grinder.engine.process.StopWatch.StopWatchRunningException;
import net.grinder.util.TimeAuthorityStubFactory;


/**
 * Unit tests for {@link StopWatchImplementation}.
 *
 * @author Philip Aston
 */
public class TestStopWatchImplementation extends TestCase {

  public void testStopWatch() throws Exception {
    final TimeAuthorityStubFactory timeAuthorityStubFactory =
      new TimeAuthorityStubFactory();
    final TimeAuthority timeAuthority =
      timeAuthorityStubFactory.getStub();
    timeAuthorityStubFactory.nextTime(2000);

    final StopWatch stopWatch = new StopWatchImplementation(timeAuthority);

    try {
      stopWatch.stop();
      fail("Expected StopWatchNotRunningException");
    }
    catch (final StopWatchNotRunningException e) {
    }

    timeAuthorityStubFactory.assertNoMoreCalls();

    stopWatch.start();

    timeAuthorityStubFactory.assertSuccess("getTimeInMilliseconds");
    timeAuthorityStubFactory.assertNoMoreCalls();

    try {
      stopWatch.start();
      fail("Expected StopWatchRunningException");
    }
    catch (final StopWatchRunningException e) {
    }

    try {
      stopWatch.reset();
      fail("Expected StopWatchRunningException");
    }
    catch (final StopWatchRunningException e) {
    }

    try {
      stopWatch.getTime();
      fail("Expected StopWatchRunningException");
    }
    catch (final StopWatchRunningException e) {
    }

    timeAuthorityStubFactory.assertNoMoreCalls();

    final StopWatch stopWatch2 = new StopWatchImplementation(timeAuthority);

    try {
      stopWatch2.add(stopWatch);
      fail("Expected StopWatchRunningException");
    }
    catch (final StopWatchRunningException e) {
    }

    timeAuthorityStubFactory.assertNoMoreCalls();
    timeAuthorityStubFactory.nextTime(3000);

    stopWatch.stop();

    assertEquals(1000, stopWatch.getTime());

    timeAuthorityStubFactory.assertSuccess("getTimeInMilliseconds");
    timeAuthorityStubFactory.assertNoMoreCalls();

    stopWatch.reset();

    assertEquals(0, stopWatch.getTime());

    stopWatch.start();
    stopWatch.stop();
    assertEquals(0, stopWatch.getTime());

    stopWatch.start();
    timeAuthorityStubFactory.nextTime(5000);
    stopWatch.stop();
    assertEquals(2000, stopWatch.getTime());
  }
}
