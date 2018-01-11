// Copyright (C) 2004 - 2010 Philip Aston
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

import java.io.InputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.grinder.common.SSLContextFactory;


/**
 * Factory which creates SSLContexts. We don't care about
 * cryptographic strength, so can take some shortcuts.
 *
 * <p>I tried using a trivial SecureRandomSpi implementation, but it
 * didn't make SSL measurable faster. Seeding the SecureRandom up
 * front can help on some platforms.</p>
 *
 * @author Philip Aston
 */
public final class InsecureSSLContextFactory implements SSLContextFactory {

  private static final TrustManager[] s_trustManagers = {
    new TrustEveryone(),
  };

  private static final SecureRandom s_insecureRandom;

  static {
    s_insecureRandom = new SecureRandom();

    // We don't care about cryptographic strength. In initial
    // generation of a strongly random seed can be costly, so we short
    // circuit it.
    s_insecureRandom.setSeed(new byte[0]);
  }

  private final KeyManager[] m_keyManagers;

  /**
   * Constructor. Uses a null key manager.
   *
   * <p>
   * The JSSE does not provide an way to access its default SSLContext, and we
   * don't bother trying to replicate it. (The default context is specified by
   * the System properties <code>javax.ssl.net.ssl.keyStore</code>,
   * <code>javax.ssl.net.ssl.keyStorePassword</code>,
   * <code>javax.ssl.net.ssl.keyStoreType</code>. These properties have no
   * effect on The Grinder.)
   */
  public InsecureSSLContextFactory() {
    this(null);
  }

  /**
   * Constructor.
   *
   * @param keyManagers The sources of authentication keys.
   */
  public InsecureSSLContextFactory(KeyManager[] keyManagers) {
    m_keyManagers = keyManagers;
  }

  /**
   * Constructor.
   *
   * @param keyStoreStream Key Store input stream. A key store is read
   * from the stream, but the stream is not closed.
   * @param keyStorePassword Key Store password, or <code>null</code>.
   * @param keyStoreType Key Store type.
   * @exception GeneralSecurityException If the JSSE could not load
   * the key store.
   * @exception IOException If the key store stream could not be read.
   */
  public InsecureSSLContextFactory(InputStream keyStoreStream,
                                   char[] keyStorePassword,
                                   String keyStoreType)
    throws GeneralSecurityException, IOException {

    final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
    keyStore.load(keyStoreStream, keyStorePassword);

    final KeyManagerFactory keyManagerFactory =
      KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, keyStorePassword);

    m_keyManagers = keyManagerFactory.getKeyManagers();
  }

  /**
   * {@inheritDoc}
   */
  @Override public SSLContext getSSLContext()
    throws SSLContextFactoryException {

    try {
      final SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(m_keyManagers, s_trustManagers, s_insecureRandom);
      return sslContext;
    }
    catch (GeneralSecurityException e) {
      throw new SSLContextFactoryException(
        "The JSSE could not create the SSLContext", e);
    }
  }

  private static class TrustEveryone implements X509TrustManager {

    public void checkClientTrusted(X509Certificate[] chain,
                                      String authenticationType) {
    }

    public void checkServerTrusted(X509Certificate[] chain,
                                      String authenticationType) {
    }

    public X509Certificate[] getAcceptedIssuers() { return null; }
  }
}
