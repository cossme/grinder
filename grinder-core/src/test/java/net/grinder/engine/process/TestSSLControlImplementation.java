// Copyright (C) 2007 - 2011 Philip Aston
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;

import net.grinder.common.SSLContextFactory;
import net.grinder.common.ThreadLifeCycleListener;
import net.grinder.common.SSLContextFactory.SSLContextFactoryException;
import net.grinder.script.InvalidContextException;
import net.grinder.script.SSLControl;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.StreamCopier;


/**
 * Unit tests for {@link SSLControlImplementation}.
 *
 * @author Philip Aston
 */
public class TestSSLControlImplementation extends AbstractFileTestCase {

  private final RandomStubFactory<ThreadContext> m_threadContextStubFactory =
    RandomStubFactory.create(ThreadContext.class);
  private final ThreadContext m_threadContext =
    m_threadContextStubFactory.getStub();

  private final StubThreadContextLocator m_threadContextLocator=
      new StubThreadContextLocator();

  {
    m_threadContextLocator.set(m_threadContext);
    m_threadContextStubFactory.setIgnoreMethod("getThreadSSLContextFactory");
  }

  private final RandomStubFactory<KeyManager> m_keyManagerStubFactory =
    RandomStubFactory.create(KeyManager.class);
  private final KeyManager m_keyManager = m_keyManagerStubFactory.getStub();

  public void testShareContextBetweenRuns() throws Exception {
    final SSLControl sslControl =
      new SSLControlImplementation(m_threadContextLocator);

    assertFalse(sslControl.getShareContextBetweenRuns());
    sslControl.setShareContextBetweenRuns(true);
    assertTrue(sslControl.getShareContextBetweenRuns());
    sslControl.setShareContextBetweenRuns(false);
    assertFalse(sslControl.getShareContextBetweenRuns());

    sslControl.setKeyManagers(new KeyManager[] { m_keyManager });

    final CallData call =
        m_threadContextStubFactory.assertSuccess("setThreadSSLContextFactory",
                                                 SSLContextFactory.class);
    final SSLContextFactory contextFactory =
      (SSLContextFactory)call.getParameters()[0];

    m_threadContextStubFactory.assertSuccess("registerThreadLifeCycleListener",
                                             contextFactory);

    m_threadContextStubFactory.assertNoMoreCalls();

    final ThreadLifeCycleListener lifecycleListener =
      (ThreadLifeCycleListener)contextFactory;

    lifecycleListener.beginThread();
    lifecycleListener.beginRun();

    final SSLContext context1 = contextFactory.getSSLContext();
    assertSame(context1, contextFactory.getSSLContext());

    lifecycleListener.endRun();
    lifecycleListener.beginRun();

    final SSLContext context2 = contextFactory.getSSLContext();
    assertNotSame(context1, contextFactory.getSSLContext());
    assertSame(context2, contextFactory.getSSLContext());

    sslControl.setShareContextBetweenRuns(true);

    lifecycleListener.endRun();
    lifecycleListener.beginRun();

    assertSame(context2, contextFactory.getSSLContext());

    lifecycleListener.endRun();
    lifecycleListener.endThread();
    lifecycleListener.beginShutdown();

    m_threadContextStubFactory.assertNoMoreCalls();
  }

  public void testSetKeyManagersDoesNotLeak() throws Exception {
    final SSLControl sslControl =
      new SSLControlImplementation(m_threadContextLocator);

    sslControl.setKeyManagers(new KeyManager[] { m_keyManager });

    final SSLContextFactory threadSSLContextFactory =
      (SSLContextFactory)
      m_threadContextStubFactory.assertSuccess("setThreadSSLContextFactory",
                                               SSLContextFactory.class)
                                               .getParameters()[0];

    m_threadContextStubFactory.assertSuccess("registerThreadLifeCycleListener",
                                             SSLContextFactory.class);

    m_threadContextStubFactory.assertNoMoreCalls();

    m_threadContextStubFactory.setResult("getThreadSSLContextFactory",
                                         threadSSLContextFactory);

    sslControl.setKeyManagers(new KeyManager[] { m_keyManager });

    m_threadContextStubFactory.assertSuccess("removeThreadLifeCycleListener",
                                             threadSSLContextFactory);

    m_threadContextStubFactory.assertSuccess("setThreadSSLContextFactory",
                                             SSLContextFactory.class);

    m_threadContextStubFactory.assertSuccess("registerThreadLifeCycleListener",
                                             SSLContextFactory.class);

    m_threadContextStubFactory.assertNoMoreCalls();
  }

  public void testGetSSLContext() throws Exception {
    final SSLControl sslControl =
      new SSLControlImplementation(m_threadContextLocator);

    // Call 1.
    m_threadContextStubFactory.setResult("getThreadSSLContextFactory", null);

    final SSLContext context = sslControl.getSSLContext();
    assertNotNull(context);

    final CallData call =
      m_threadContextStubFactory.assertSuccess("setThreadSSLContextFactory",
                                               SSLContextFactory.class);
    final SSLContextFactory contextFactory =
      (SSLContextFactory)call.getParameters()[0];

    assertSame(context, contextFactory.getSSLContext());

    m_threadContextStubFactory.assertSuccess("registerThreadLifeCycleListener",
                                             contextFactory);
    m_threadContextStubFactory.assertNoMoreCalls();

    // Call 2.
    m_threadContextStubFactory.setResult("getThreadSSLContextFactory",
                                         contextFactory);

    final SSLContext context2 = sslControl.getSSLContext();
    assertNotNull(context2);

    assertSame(context, context2);
  }

  public void testSetKeyStoreMethods() throws Exception {
    final SSLControl sslControl =
      new SSLControlImplementation(m_threadContextLocator);

    final InputStream keystoreStream =
      getClass().getResourceAsStream("default.keystore");
    sslControl.setKeyStore(
      keystoreStream,
        "passphrase",
        "jks");

    keystoreStream.close();

    final SSLContextFactory threadSSLContextFactory =
      (SSLContextFactory)
      m_threadContextStubFactory.assertSuccess("setThreadSSLContextFactory",
                                               SSLContextFactory.class)
                                               .getParameters()[0];

    m_threadContextStubFactory.assertSuccess("registerThreadLifeCycleListener",
                                             SSLContextFactory.class);

    m_threadContextStubFactory.assertNoMoreCalls();

    m_threadContextStubFactory.setResult("getThreadSSLContextFactory",
                                         threadSSLContextFactory);

    final File myKeyStore = new File(getDirectory(), "my.jks");

    new StreamCopier(4096, true).
    copy(
      getClass().getResourceAsStream("default.keystore"),
      new FileOutputStream(myKeyStore));

    try {
      // Will fail since JKS doesn't support null passwords.
      sslControl.setKeyStoreFile(myKeyStore.getAbsolutePath(), null, "jks");
      fail("Expected IllegalArgumentException or UnrecoverableKeyException");
    }
    catch (Exception e) {
      assertTrue("Unexpected exception type: " + e.getClass(),
                 e instanceof UnrecoverableKeyException ||
                 e instanceof IllegalArgumentException);
    }

    sslControl.setKeyStoreFile(
      myKeyStore.getAbsolutePath(), "passphrase", "jks");

    m_threadContextStubFactory.assertSuccess("removeThreadLifeCycleListener",
                                             threadSSLContextFactory);

    m_threadContextStubFactory.assertSuccess("setThreadSSLContextFactory",
                                             SSLContextFactory.class);

    m_threadContextStubFactory.assertSuccess("registerThreadLifeCycleListener",
                                             SSLContextFactory.class);

    m_threadContextStubFactory.assertNoMoreCalls();
  }

  public void testWithBadContext() throws Exception {
    final SSLControl sslControl =
      new SSLControlImplementation(m_threadContextLocator);

    m_threadContextLocator.set(null);

    try {
      sslControl.setKeyManagers(new KeyManager[0]);
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
    }

    try {
      sslControl.setKeyStore(null, "");
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
    }

    try {
      sslControl.setKeyStoreFile("", "");
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
    }

    try {
      sslControl.getSSLContext();
      fail("Expected SSLContextFactoryException");
    }
    catch (SSLContextFactoryException e) {
    }

    m_threadContextStubFactory.assertNoMoreCalls();
  }
}
