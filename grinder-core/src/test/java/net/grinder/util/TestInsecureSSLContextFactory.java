// Copyright (C) 2004, 2005 Philip Aston
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

package net.grinder.util;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;

import junit.framework.TestCase;


/**
 * Unit test for {@link InsecureSSLContextFactory}.
 *
 * @author Philip Aston
 */
public class TestInsecureSSLContextFactory extends TestCase {

  public TestInsecureSSLContextFactory(String name) {
    super(name);
  }

  public void testWithDefaultKeyManager() throws Exception {
    final InsecureSSLContextFactory sslContextFactory =
      new InsecureSSLContextFactory();

    final SSLContext context = sslContextFactory.getSSLContext();
    assertNotNull(context);

    final SSLContext context2 = sslContextFactory.getSSLContext();

    assertNotNull(context2);
    assertTrue(context != context2);

    // Not a lot more we can test.
  }

  private static final class MyKeyManager implements KeyManager {
  }

  public void testWithMyKeyManagers() throws Exception {
    final KeyManager[] keyManagers = {
      new MyKeyManager(),
      new MyKeyManager(),
    };

    final InsecureSSLContextFactory sslContextFactory =
      new InsecureSSLContextFactory(keyManagers);

    final SSLContext context = sslContextFactory.getSSLContext();
    assertNotNull(context);

    final SSLContext context2 = sslContextFactory.getSSLContext();

    assertNotNull(context2);
    assertTrue(context != context2);
  }
}
