// Copyright (C) 2004 - 2011 Philip Aston
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

package net.grinder.script;

import java.io.InputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;

import net.grinder.common.SSLContextFactory;


/**
 * Script control of SSL behaviour. This can be used to create SSL
 * sockets or to set the appropriate client certificates to use. An
 * implementation of this interface can be obtained using {@link
 * Grinder.ScriptContext#getSSLControl}.
 *
 * <p>The Grinder provides specialised SSL support.
 *
 * <ul> <li>By default, each run has its own SSL context so SSL
 * sessions are not shared across threads or runs. Sharing sessions is
 * not desirable because this would potentially allow a server under
 * test to do less work. See {@link #setShareContextBetweenRuns}.</li>

 * <li>The Grinder trusts every server it talks to. It does not do any
 * validation of server certificates or host names. To do so would
 * make set up more onerous and place an unnecessary burden on The
 * Grinder worker processes.</li>
 *
 * <li>The Grinder uses an insecure source of random information. This
 * makes the SSL communication cryptographically less secure, but does
 * not affect the processing necessary on any server it talks to. One
 * benefit of this is faster start up times.</li>
 * </ul>
 * </p>
 *
 * <p><b>Do not use The Grinder to implement any SSL communication
 * that you want to be secure.</b></p>
 *
 * <p> <a name="set_methods"></a> This interface provides several
 * methods for specifying the appropriate certificate and key pair to
 * use for a thread:</p>
 *
 * <ul>
 * <li>{@link #setKeyManagers}</li>
 * <li>{@link #setKeyStore(InputStream, String)}</li>
 * <li>{@link #setKeyStoreFile(String, String)}</li>
 * </ul>
 *
 * <p>Each of these methods creates a new SSL context when called. (They
 * do not invalidate existing connections that use the previous
 * context). Consequently you will want to call these methods before
 * making connections for a particular run.</p>
 *
 * @author Philip Aston
 */
public interface SSLControl extends SSLContextFactory {

  /**
   * Set the JSSE {@link KeyManager}s to use for the calling worker
   * thread/run.
   *
   * <p>This will create a new SSL context. See {@link <a
   * href="#set_methods">the note above</a>} for details.</p>
   *
   * <p>For compatibility with JSSE 1.0.X running under J2SE 1.3, The
   * Grinder uses the <code>X509KeyManager</code> in the legacy JSSE
   * <code>com.sun.net.ssl</code> package. This is slightly different
   * to the <code>X509KeyManager</code> packaged in
   * <code>javax.net.ssl</code> in J2SE 1.4 and later.</p>
   *
   * @param keyManagers The key managers.
   * @exception InvalidContextException If called from a non-worker
   * thread.
   * @see #setKeyStore(InputStream, String, String)
   * @see #setKeyStoreFile(String, String)
   */
  void setKeyManagers(KeyManager[] keyManagers) throws InvalidContextException;

  /**
   * Set a key store to use for the calling worker thread/run.
   * Convenient alternative to {@link #setKeyManagers}.
   *
   * <p>This will create a new SSL context. See {@link <a
   * href="#set_methods">the note above</a>} for details.</p>
   *
   * @param keyStoreFileName Key store file name.
   * @param password Key store password. Also used as the private key
   * password.
   * @param keyStoreType Key store type.
   * @exception GeneralSecurityException If JSSE could not load the key store.
   * @exception InvalidContextException If called from a non-worker
   * thread.
   * @exception IOException If key store could not be read.
   * @see #setKeyManagers
   * @see #setKeyStoreFile(String, String)
   */
  void setKeyStoreFile(String keyStoreFileName, String password,
                       String keyStoreType)
    throws GeneralSecurityException, InvalidContextException, IOException;

  /**
   * Overloaded version of <code>setKeyStoreFile</code> for key stores of
   * the default type (usually <code>jks</code>).
   *
   * @param keyStoreFileName Key store file name.
   * @param password Key store password. Also used as the private key
   * password.
   * @exception GeneralSecurityException If JSSE could not load the key store.
   * @exception InvalidContextException If called from a non-worker
   * thread.
   * @exception IOException If key store could not be read.
   * @see #setKeyStoreFile(String, String, String)
   */
  void setKeyStoreFile(String keyStoreFileName, String password)
    throws GeneralSecurityException, InvalidContextException, IOException;

  /**
   * Set a key store to use for the calling worker thread/run.
   * Convenient alternative to {@link #setKeyManagers}.
   *
   * <p>This will create a new SSL context. See {@link <a
   * href="#set_methods">the note above</a>} for details.</p>
   *
   * @param keyStoreInputStream Input stream to key store.
   * @param password Key store password. Also used as the private key
   * password.
   * @param keyStoreType Key store type.
   * @exception GeneralSecurityException If JSSE could not load the key store.
   * @exception InvalidContextException If called from a non-worker
   * thread.
   * @exception IOException If key store could not be read.
   * @see #setKeyManagers
   * @see #setKeyStoreFile(String, String)
   */
  void setKeyStore(InputStream keyStoreInputStream,
                   String password,
                   String keyStoreType)
    throws GeneralSecurityException, InvalidContextException, IOException;

  /**
   * Overloaded version of <code>setKeyStore</code> for key stores of
   * the default type (usually <code>jks</code>).
   *
   * @param keyStoreInputStream Input stream to key store.
   * @param password Key store password. Also used as the private key
   * password.
   * @exception GeneralSecurityException If JSSE could not load the key store.
   * @exception InvalidContextException If called from a non-worker
   * thread.
   * @exception IOException If key store could not be read.
   * @see #setKeyStore(InputStream, String, String)
   */
  void setKeyStore(InputStream keyStoreInputStream, String password)
    throws GeneralSecurityException, InvalidContextException, IOException;

  /**
   * {@inheritDoc}
   */
  @Override SSLContext getSSLContext() throws SSLContextFactoryException;

  /**
   * Get whether SSL contexts are shared between runs.
   *
   * @return <code>true</code> => SSL contexts are per thread,
   * <code>false</code> => SSL contexts are per run.
   * @see #setShareContextBetweenRuns
   */
  boolean getShareContextBetweenRuns();

  /**
   * Specify that there should be a single SSL context for a thread.
   * By default, a new SSL context is created per thread. This is a
   * worker process level setting. If you call this, {@link
   * #getSSLContext} will return the same context for every run.
   *
   * <p>If you use this method in conjunction with one of the {@link
   * <a href="#set_methods">setKey...</a>} methods you will want to
   * guard the call to the <code>setKey..</code> method so it is only
   * called once per thread:
   *
   * <pre>
   * grinder.SSLControl.shareContextBetweenRuns = 1
   *
   * class TestRunner:
   *   def __call__(self):
   *     if grinder.runNumber == 0:
   *       # First run.
   *       grinder.SSLControl.setKeyStoreFile("mykeystore.jks", "pass")</pre>
   * </p>
   *
   * <p>Alternatively, set the appropriate key store for the thread in
   * the <code>TestRunner</code> constructor.</p>
   *
   * @param b <code>true</code> => share SSL contexts between runs,
   * <code>false</code> => each run should have a new SSL context.
   */
  void setShareContextBetweenRuns(boolean b);
}
