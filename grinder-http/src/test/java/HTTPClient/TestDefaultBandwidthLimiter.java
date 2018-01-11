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

package HTTPClient;

import HTTPClient.HTTPConnection.BandwidthLimiter;
import HTTPClient.HTTPConnection.BandwidthLimiterFactory;
import junit.framework.TestCase;


/**
 * Unit tests for our default {@link HTTPConnection.BandwidthLimiter}
 * implementation.
 * 
 * @author Philip Aston
 */
public class TestDefaultBandwidthLimiter extends TestCase {
  
  public void testBandwithLimiter() throws Exception {
    final HTTPConnection connection = new HTTPConnection("foo");
    final BandwidthLimiterFactory bandwidthLimiterFactory =
      connection.getBandwidthLimiterFactory();
    
    final BandwidthLimiter bandwidthLimiter =
      bandwidthLimiterFactory.create();
    
    assertEquals(Integer.MAX_VALUE, bandwidthLimiter.maximumBytes(0));
    assertEquals(Integer.MAX_VALUE, bandwidthLimiter.maximumBytes(100));
    assertEquals(Integer.MAX_VALUE,
                 bandwidthLimiter.maximumBytes(Integer.MAX_VALUE));
    assertEquals(Integer.MAX_VALUE, bandwidthLimiter.maximumBytes(0));
  }
}
