// Copyright (C) 2008 - 2011 Philip Aston
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

import static java.util.Collections.singleton;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.BlockingClassLoader;
import net.grinder.util.Sleeper;
import HTTPClient.HTTPConnection;
import HTTPClient.StubHTTPConnection;
import HTTPClient.HTTPConnection.BandwidthLimiterFactory;


/**
 * Unit tests for {@link HTTPConnectionWrapper}.
 *
 * @author Philip Aston
 */
public class TestHTTPConnectionWrapper extends TestCase {

  public void testHTTPConnectionWrapper() throws Exception {

    final RandomStubFactory<Sleeper> sleeperStubFactory =
      RandomStubFactory.create(Sleeper.class);
    final Sleeper sleeper = sleeperStubFactory.getStub();

    final StubHTTPConnection connection = new StubHTTPConnection("foo");

    final BandwidthLimiterFactory defaultBWLimiterFactory =
      connection.getBandwithLimiterFactoryForTest();

    assertModule(connection, "HTTPClient.RedirectionModule", true);
    assertModule(connection, "HTTPClient.CookieModule", true);
    assertModule(connection, "HTTPClient.ContentEncodingModule", true);
    assertModule(connection, "HTTPClient.TransferEncodingModule", true);
    assertModule(connection, "HTTPClient.AuthorizationModule", true);
    assertTrue(connection.getAllowUserInteraction());
    assertFalse(connection.getTestConnectionHealthWithBlockingRead());

    final HTTPPluginConnectionDefaults defaults =
      new HTTPPluginConnectionDefaults();

    final HTTPConnectionWrapper wrapper =
      new HTTPConnectionWrapper(connection, defaults, sleeper);

    assertSame(connection, wrapper.getConnection());
    assertFalse(connection.getAllowUserInteraction());
    assertTrue(connection.getTestConnectionHealthWithBlockingRead());

    assertModule(connection, "HTTPClient.RedirectionModule", false);
    defaults.setFollowRedirects(true);
    new HTTPConnectionWrapper(connection, defaults, sleeper);
    assertModule(connection, "HTTPClient.RedirectionModule", true);
    wrapper.setFollowRedirects(false);
    assertModule(connection, "HTTPClient.RedirectionModule", false);

    assertModule(connection, "HTTPClient.CookieModule", true);
    defaults.setUseCookies(false);
    new HTTPConnectionWrapper(connection, defaults, sleeper);
    assertModule(connection, "HTTPClient.CookieModule", false);
    wrapper.setUseCookies(true);
    assertModule(connection, "HTTPClient.CookieModule", true);

    assertModule(connection, "HTTPClient.ContentEncodingModule", false);
    defaults.setUseContentEncoding(true);
    new HTTPConnectionWrapper(connection, defaults, sleeper);
    assertModule(connection, "HTTPClient.ContentEncodingModule", true);
    wrapper.setUseContentEncoding(false);
    assertModule(connection, "HTTPClient.ContentEncodingModule", false);

    assertModule(connection, "HTTPClient.TransferEncodingModule", false);
    defaults.setUseTransferEncoding(true);
    new HTTPConnectionWrapper(connection, defaults, sleeper);
    assertModule(connection, "HTTPClient.TransferEncodingModule", true);
    wrapper.setUseTransferEncoding(false);
    assertModule(connection, "HTTPClient.TransferEncodingModule", false);

    assertModule(connection, "HTTPClient.AuthorizationModule", false);
    defaults.setUseAuthorizationModule(true);
    new HTTPConnectionWrapper(connection, defaults, sleeper);
    assertModule(connection, "HTTPClient.AuthorizationModule", true);
    wrapper.setUseAuthorizationModule(false);
    assertModule(connection, "HTTPClient.AuthorizationModule", false);

    try {
      wrapper.setLocalAddress("unknown host");
      fail("Expected URLException");
    }
    catch (URLException e) {
    }

    wrapper.setLocalAddress(InetAddress.getLocalHost().getHostName());

    assertEquals(InetAddress.getLocalHost(),
                 connection.getLocalAddressForTest());

    assertSame(defaultBWLimiterFactory,
               connection.getBandwithLimiterFactoryForTest());
    wrapper.setBandwidthLimit(100);
    assertNotSame(defaultBWLimiterFactory,
                  connection.getBandwithLimiterFactoryForTest());
    wrapper.setBandwidthLimit(0);
    assertSame(defaultBWLimiterFactory,
      connection.getBandwithLimiterFactoryForTest());
  }

  private void assertModule(HTTPConnection connection,
                            String className,
                            boolean present) {
    final Class<?>[] modules = connection.getModules();
    final Set<String> classNames = new HashSet<String>();

    for (Class<?> module : modules) {
      classNames.add(module.getName());
    }

    assertEquals(present, classNames.contains(className));
  }

  /** Dumb check to cover static initialisation error handling. */
  public void testClassInitialisation() throws Exception {

    final String wrapperName = HTTPConnectionWrapper.class.getName();

    final ClassLoader blockingLoader =
      new BlockingClassLoader(
         singleton("HTTPClient.AuthorizationModule"),
         singleton(wrapperName),
         Collections.<String>emptySet(),
         false);

    try {
      Class.forName(wrapperName,
                    true,
                    blockingLoader);
      fail("Expected ExceptionInInitializerError");
    }
    catch (ExceptionInInitializerError e) {
      assertTrue(e.getCause() instanceof ClassNotFoundException);
    }
  }
}
