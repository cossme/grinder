// Copyright (C) 2001 - 2009 Philip Aston
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;

import net.grinder.common.SSLContextFactory;
import net.grinder.common.SkeletonThreadLifeCycleListener;
import net.grinder.script.InvalidContextException;
import net.grinder.script.SSLControl;
import net.grinder.util.InsecureSSLContextFactory;


/**
 * Implementation of {@link SSLControl}.
 *
 * @author Philip Aston
 */
final class SSLControlImplementation implements SSLControl {

  private final ThreadContextLocator m_threadContextLocator;

  private boolean m_shareContextBetweenRuns = false;

  public SSLControlImplementation(ThreadContextLocator threadContextLocator) {
    m_threadContextLocator = threadContextLocator;
  }

  public void setKeyManagers(final KeyManager[] keyManagers)
    throws InvalidContextException {

    final ThreadContext threadContext = m_threadContextLocator.get();

    if (threadContext == null) {
      throw new InvalidContextException(
        "setKeyManagers is only supported for worker threads.");
    }

    setThreadSSLContextFactory(threadContext,
                               new CachingSSLContextFactory(
                                 new InsecureSSLContextFactory(keyManagers)));
  }

  public void setKeyStore(final InputStream keyStoreInputStream,
                          final String password,
                          String keyStoreType)
    throws GeneralSecurityException, InvalidContextException, IOException {

    final ThreadContext threadContext = m_threadContextLocator.get();

    if (threadContext == null) {
      throw new InvalidContextException(
        "setKeyStore is only supported for worker threads.");
    }

    final char[] passwordChars =
      password != null ? password.toCharArray() : null;

    setThreadSSLContextFactory(
      threadContext,
      new CachingSSLContextFactory(
        new InsecureSSLContextFactory(keyStoreInputStream,
                                      passwordChars,
                                      keyStoreType)));
  }

  public void setKeyStore(InputStream keyStoreInputStream, String password)
    throws GeneralSecurityException, InvalidContextException, IOException {
    setKeyStore(keyStoreInputStream, password, KeyStore.getDefaultType());
  }

  public void setKeyStoreFile(String keyStoreFileName,
                              String password,
                              String keyStoreType)
    throws GeneralSecurityException, InvalidContextException, IOException {

    final ThreadContext threadContext = m_threadContextLocator.get();

    if (threadContext == null) {
      throw new InvalidContextException(
        "setKeyStoreFile is only supported for worker threads.");
    }

    final FileInputStream fileInputStream =
      new FileInputStream(keyStoreFileName);

    try {
      setKeyStore(fileInputStream, password, keyStoreType);
    }
    finally {
      fileInputStream.close();
    }
  }

  public void setKeyStoreFile(String keyStoreFileName, String password)
    throws GeneralSecurityException, InvalidContextException, IOException {
    setKeyStoreFile(keyStoreFileName, password, KeyStore.getDefaultType());
  }

  public SSLContext getSSLContext() throws SSLContextFactoryException {

    final ThreadContext threadContext = m_threadContextLocator.get();

    if (threadContext == null) {
      throw new SSLContextFactoryException(
        "getSSLContext is only supported for worker threads.");
    }

    final SSLContextFactory threadSSLContextFactory =
      threadContext.getThreadSSLContextFactory();

    if (threadSSLContextFactory != null) {
      return threadSSLContextFactory.getSSLContext();
    }
    else {
      final CachingSSLContextFactory defaultContextFactoryForThread =
        new CachingSSLContextFactory(new InsecureSSLContextFactory());

      setThreadSSLContextFactory(threadContext,
                                 defaultContextFactoryForThread);

      return defaultContextFactoryForThread.getSSLContext();
    }
  }

  public boolean getShareContextBetweenRuns() {
    return m_shareContextBetweenRuns;
  }

  public void setShareContextBetweenRuns(boolean b) {
    m_shareContextBetweenRuns = b;
  }

  private void setThreadSSLContextFactory(
    ThreadContext threadContext,
    CachingSSLContextFactory cachingSSLContextFactory) {

    final SSLContextFactory oldSSLContextFactory =
      threadContext.getThreadSSLContextFactory();

    if (oldSSLContextFactory instanceof CachingSSLContextFactory) {
      threadContext.removeThreadLifeCycleListener(
        (CachingSSLContextFactory) oldSSLContextFactory);
    }

    threadContext.setThreadSSLContextFactory(cachingSSLContextFactory);
    threadContext.registerThreadLifeCycleListener(cachingSSLContextFactory);
  }

  private class CachingSSLContextFactory
    extends SkeletonThreadLifeCycleListener implements SSLContextFactory {

    private final SSLContextFactory m_delegateContextFactory;
    private SSLContext m_sslContext;

    public CachingSSLContextFactory(SSLContextFactory delegateContextFactory) {
      m_delegateContextFactory = delegateContextFactory;
    }

    public final SSLContext getSSLContext() throws SSLContextFactoryException {
      if (m_sslContext == null) {
        m_sslContext = m_delegateContextFactory.getSSLContext();
      }

      return m_sslContext;
    }

    public void endRun() {
      if (!m_shareContextBetweenRuns) {
        m_sslContext = null;
      }
    }
  }
}
