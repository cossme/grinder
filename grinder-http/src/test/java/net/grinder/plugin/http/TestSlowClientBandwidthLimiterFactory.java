// Copyright (C) 2008 - 2009 Philip Aston
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

package net.grinder.plugin.http;

import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.Sleeper;
import HTTPClient.HTTPConnection.BandwidthLimiter;
import junit.framework.TestCase;


/**
 * Unit tests for {@link SlowClientBandwidthLimiterFactory}.
 *
 * @author Philip Aston
 */
public class TestSlowClientBandwidthLimiterFactory extends TestCase {

  public void testFactory() throws Exception {
    final SlowClientBandwidthLimiterFactory slowClientBandwidthLimiterFactory =
      new SlowClientBandwidthLimiterFactory(null, 1000);

    final BandwidthLimiter slowClientBandwidthLimiter =
      slowClientBandwidthLimiterFactory.create();

    assertNotNull(slowClientBandwidthLimiter);
    assertNotSame(slowClientBandwidthLimiter,
                  slowClientBandwidthLimiterFactory.create());
  }

  public void testSlowClientBandwidthLimiter() throws Exception {
    final RandomStubFactory<Sleeper> sleeperStubFactory =
      RandomStubFactory.create(Sleeper.class);
    final SlowClientBandwidthLimiterFactory slowClientBandwidthLimiterFactory =
      new SlowClientBandwidthLimiterFactory(sleeperStubFactory.getStub(), 1000);

    final BandwidthLimiter slowClientBandwidthLimiter =
      slowClientBandwidthLimiterFactory.create();

    sleeperStubFactory.setResult("getTimeInMilliseconds", new Long(10000));

    assertEquals(100, slowClientBandwidthLimiter.maximumBytes(0));
    sleeperStubFactory.assertSuccess("getTimeInMilliseconds");
    sleeperStubFactory.assertSuccess("sleepNormal", new Long(0), new Long(0));
    sleeperStubFactory.assertNoMoreCalls();

    sleeperStubFactory.setResult("getTimeInMilliseconds", new Long(10010));
    assertEquals(100, slowClientBandwidthLimiter.maximumBytes(100));
    sleeperStubFactory.assertSuccess("getTimeInMilliseconds");
    sleeperStubFactory.assertSuccess(
      "sleepNormal", new Long(1580), new Long(0));
    sleeperStubFactory.assertNoMoreCalls();

    sleeperStubFactory.setResult("getTimeInMilliseconds", new Long(12000));
    assertEquals(100, slowClientBandwidthLimiter.maximumBytes(200));
    sleeperStubFactory.assertSuccess("getTimeInMilliseconds");
    sleeperStubFactory.assertSuccess(
      "sleepNormal", new Long(1380), new Long(0));
    sleeperStubFactory.assertNoMoreCalls();

    sleeperStubFactory.setResult("getTimeInMilliseconds", new Long(20000));
    assertEquals(100, slowClientBandwidthLimiter.maximumBytes(200));
    sleeperStubFactory.assertSuccess("getTimeInMilliseconds");
    sleeperStubFactory.assertSuccess("sleepNormal", new Long(0), new Long(0));
    sleeperStubFactory.assertNoMoreCalls();
  }
}
