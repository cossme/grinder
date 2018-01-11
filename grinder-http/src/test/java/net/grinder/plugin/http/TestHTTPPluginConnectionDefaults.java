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

package net.grinder.plugin.http;

import java.net.InetAddress;

import HTTPClient.NVPair;
import junit.framework.TestCase;


/**
 * Unit tests for {@link HTTPPluginConnectionDefaults}.
 *
 * @author Philip Aston
 */
public class TestHTTPPluginConnectionDefaults extends TestCase {

  public void testHTTPPluginConnectionDefaults() throws Exception {
    final HTTPPluginConnectionDefaults defaults =
      new HTTPPluginConnectionDefaults();

    assertFalse(defaults.getFollowRedirects());
    defaults.setFollowRedirects(true);
    assertTrue(defaults.getFollowRedirects());

    assertTrue(defaults.getUseCookies());
    defaults.setUseCookies(false);
    assertFalse(defaults.getUseCookies());

    assertFalse(defaults.getUseContentEncoding());
    defaults.setUseContentEncoding(true);
    assertTrue(defaults.getUseContentEncoding());

    assertFalse(defaults.getUseTransferEncoding());
    defaults.setUseTransferEncoding(true);
    assertTrue(defaults.getUseTransferEncoding());

    assertFalse(defaults.getUseAuthorizationModule());
    defaults.setUseAuthorizationModule(true);
    assertTrue(defaults.getUseAuthorizationModule());

    assertFalse(defaults.getVerifyServerDistinguishedName());
    defaults.setVerifyServerDistinguishedName(true);
    assertTrue(defaults.getVerifyServerDistinguishedName());

    assertEquals(0, defaults.getDefaultHeaders().length);
    final NVPair[] nicePair = { new NVPair("a", "b"), new NVPair("c", "d"), };
    defaults.setDefaultHeaders(nicePair);
    assertSame(nicePair, defaults.getDefaultHeaders());

    assertEquals(0, defaults.getTimeout());
    defaults.setTimeout(123);
    assertEquals(123, defaults.getTimeout());

    assertNull(defaults.getProxyHost());
    assertEquals(0, defaults.getProxyPort());
    defaults.setProxyServer("foo", 7289);
    assertEquals("foo", defaults.getProxyHost());
    assertEquals(7289, defaults.getProxyPort());

    assertEquals(null, defaults.getLocalAddress());

    try {
      defaults.setLocalAddress("unknown host");
      fail("Expected URLException");
    }
    catch (URLException e) {
    }

    assertEquals(null, defaults.getLocalAddress());
    defaults.setLocalAddress(InetAddress.getLocalHost().getHostAddress());
    assertEquals(InetAddress.getLocalHost(), defaults.getLocalAddress());

    assertEquals(0, defaults.getBandwidthLimit());
    defaults.setBandwidthLimit(99);
    assertEquals(99, defaults.getBandwidthLimit());

    // Cover no-op.
    defaults.close();
  }

  public void testSingleton() {
    assertNotNull(HTTPPluginConnectionDefaults.getConnectionDefaults());
    assertSame(HTTPPluginConnectionDefaults.getConnectionDefaults(),
               HTTPPluginConnectionDefaults.getConnectionDefaults());
  }
}
