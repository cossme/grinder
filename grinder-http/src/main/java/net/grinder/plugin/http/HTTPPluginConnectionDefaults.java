// Copyright (C) 2002 - 2012 Philip Aston
// Copyright (C) 2003 Richard Perks
// Copyright (C) 2004 Bertrand Ave
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
import java.net.UnknownHostException;

import HTTPClient.NVPair;


/**
 * A {@link HTTPPluginConnection} that can be used to set the default
 * behaviour of new connections.
 *
 * @author Philip Aston
 * @author Richard Perks
 * @author Bertrand Ave
 */
final class HTTPPluginConnectionDefaults implements HTTPPluginConnection {

  private boolean m_followRedirects = false;
  private boolean m_useCookies = true;
  private boolean m_useContentEncoding = false;
  private boolean m_useTransferEncoding = false;
  private boolean m_useAuthorizationModule = false;
  private NVPair[] m_defaultHeaders = new NVPair[0];
  private int m_timeout = 0;
  private String m_proxyHost;
  private int m_proxyPort;
  private boolean m_verifyServerDistinguishedName = false;
  private InetAddress m_localAddress;
  private int m_slowClientTargetBPS;

  @Override
  public synchronized void setFollowRedirects(final boolean followRedirects) {
    m_followRedirects = followRedirects;
  }

  synchronized boolean getFollowRedirects() {
    return m_followRedirects;
  }

  @Override
  public synchronized void setUseCookies(final boolean useCookies) {
    m_useCookies = useCookies;
  }

  synchronized boolean getUseCookies() {
    return m_useCookies;
  }

  @Override
  public synchronized
    void setUseContentEncoding(final boolean useContentEncoding) {
    m_useContentEncoding = useContentEncoding;
  }

  synchronized boolean getUseContentEncoding() {
    return m_useContentEncoding;
  }

  @Override
  public synchronized
    void setUseTransferEncoding(final boolean useTransferEncoding) {
    m_useTransferEncoding = useTransferEncoding;
  }

  synchronized boolean getUseAuthorizationModule() {
    return m_useAuthorizationModule;
  }

  @Override
  public synchronized void setUseAuthorizationModule(final boolean b) {
    m_useAuthorizationModule = b;
  }

  synchronized boolean getUseTransferEncoding() {
    return m_useTransferEncoding;
  }

  @Override
  public synchronized void setDefaultHeaders(final NVPair[] defaultHeaders) {
    m_defaultHeaders = defaultHeaders;
  }

  synchronized NVPair[] getDefaultHeaders() {
    return m_defaultHeaders;
  }

  @Override
  public synchronized void setTimeout(final int timeout)  {
    m_timeout = timeout;
  }

  synchronized int getTimeout() {
    return m_timeout;
  }

  @Override
  public synchronized void setVerifyServerDistinguishedName(final boolean b) {
    m_verifyServerDistinguishedName = b;
  }

  synchronized boolean getVerifyServerDistinguishedName() {
    return m_verifyServerDistinguishedName;
  }

  @Override
  public synchronized void setProxyServer(final String host, final int port) {
    m_proxyHost = host;
    m_proxyPort = port;
  }

  synchronized String getProxyHost() {
    return m_proxyHost;
  }

  synchronized int getProxyPort() {
    return m_proxyPort;
  }

  @Override
  public synchronized void setLocalAddress(final String localAddress)
    throws URLException {

    try {
      m_localAddress = InetAddress.getByName(localAddress);
    }
    catch (final UnknownHostException e) {
      throw new URLException(e.getMessage(), e);
    }
  }

  synchronized InetAddress getLocalAddress() {
    return m_localAddress;
  }

  @Override
  public synchronized void setBandwidthLimit(final int targetBPS) {
    m_slowClientTargetBPS = targetBPS;
  }

  synchronized int getBandwidthLimit() {
    return m_slowClientTargetBPS;
  }

  private static final HTTPPluginConnectionDefaults
    s_defaultConnectionDefaults = new HTTPPluginConnectionDefaults();

  public static HTTPPluginConnectionDefaults getConnectionDefaults() {
    return s_defaultConnectionDefaults;
  }

  @Override
  public void close() {
    // A no-op.
  }
}
