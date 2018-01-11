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

import net.grinder.util.Sleeper;
import HTTPClient.CookieModule;
import HTTPClient.HTTPConnection;
import HTTPClient.NVPair;


/**
 * Implementation of {@link HTTPPluginConnection} using {@link
 * HTTPClient.HTTPConnection}.
 *
 * @author Philip Aston
 * @author Richard Perks
  * @author Bertrand Ave
 */
final class HTTPConnectionWrapper implements HTTPPluginConnection {

  private static final Class<?> s_authorizationModule;
  private static final Class<?> s_contentEncodingModule;
  private static final Class<?> s_redirectionModule;
  private static final Class<?> s_transferEncodingModule;

  private final HTTPConnection m_httpConnection;
  private final Sleeper m_slowClientSleeper;

  static {
    // Load HTTPClient modules dynamically as we don't have public
    // access.
    try {
      s_authorizationModule = Class.forName("HTTPClient.AuthorizationModule");
      s_contentEncodingModule = Class.forName(
        "HTTPClient.ContentEncodingModule");
      s_redirectionModule = Class.forName("HTTPClient.RedirectionModule");
      s_transferEncodingModule = Class.forName(
        "HTTPClient.TransferEncodingModule");
    }
    catch (final ClassNotFoundException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public HTTPConnectionWrapper(final HTTPConnection httpConnection,
                               final HTTPPluginConnectionDefaults defaults,
                               final Sleeper slowClientSleeper) {

    m_httpConnection = httpConnection;
    m_slowClientSleeper = slowClientSleeper;
    m_httpConnection.setAllowUserInteraction(false);
    m_httpConnection.setTestConnectionHealthWithBlockingRead(true);

    setFollowRedirects(defaults.getFollowRedirects());
    setUseCookies(defaults.getUseCookies());
    setUseContentEncoding(defaults.getUseContentEncoding());
    setUseTransferEncoding(defaults.getUseTransferEncoding());
    setUseAuthorizationModule(defaults.getUseAuthorizationModule());
    setDefaultHeaders(defaults.getDefaultHeaders());
    setTimeout(defaults.getTimeout());
    setVerifyServerDistinguishedName(
      defaults.getVerifyServerDistinguishedName());
    setProxyServer(defaults.getProxyHost(), defaults.getProxyPort());
    setLocalAddress(defaults.getLocalAddress());
    setBandwidthLimit(defaults.getBandwidthLimit());
  }

  HTTPConnection getConnection() {
    return m_httpConnection;
  }

  @Override
  public void setFollowRedirects(final boolean followRedirects) {

    if (followRedirects) {
      m_httpConnection.addModule(s_redirectionModule, 0);
    }
    else {
      m_httpConnection.removeModule(s_redirectionModule);
    }
  }

  @Override
  public void setUseCookies(final boolean useCookies) {

    if (useCookies) {
      m_httpConnection.addModule(CookieModule.class, 0);
    }
    else {
      m_httpConnection.removeModule(CookieModule.class);
    }
  }

  @Override
  public void setUseContentEncoding(final boolean useContentEncoding) {
    if (useContentEncoding) {
      m_httpConnection.addModule(s_contentEncodingModule, 0);
    }
    else {
      m_httpConnection.removeModule(s_contentEncodingModule);
    }
  }

  @Override
  public void setUseTransferEncoding(final boolean useTransferEncoding) {
    if (useTransferEncoding) {
      m_httpConnection.addModule(s_transferEncodingModule, 0);
    }
    else {
      m_httpConnection.removeModule(s_transferEncodingModule);
    }
  }

  @Override
  public void setUseAuthorizationModule(final boolean useAuthorizationModule) {
    if (useAuthorizationModule) {
      m_httpConnection.addModule(s_authorizationModule, 0);
    }
    else {
      m_httpConnection.removeModule(s_authorizationModule);
    }
  }

  @Override
  public void setDefaultHeaders(final NVPair[] defaultHeaders) {
    m_httpConnection.setDefaultHeaders(defaultHeaders);
  }

  @Override
  public void setTimeout(final int timeout) {
    m_httpConnection.setTimeout(timeout);
  }

  @Override
  public void setVerifyServerDistinguishedName(final boolean b) {
    m_httpConnection.setCheckCertificates(b);
  }

  @Override
  public void setProxyServer(final String host, final int port) {
    m_httpConnection.setCurrentProxy(host, port);
  }

  @Override
  public void setLocalAddress(final String localAddress) throws URLException {

    try {
      setLocalAddress(InetAddress.getByName(localAddress));
    }
    catch (final UnknownHostException e) {
      throw new URLException(e.getMessage(), e);
    }
  }

  private void setLocalAddress(final InetAddress localAddress) {
    m_httpConnection.setLocalAddress(localAddress, 0);
  }

  @Override
  public void setBandwidthLimit(final int targetBPS) {
    if (targetBPS < 1) {
      m_httpConnection.setBufferGrowthStrategyFactory(null);
    }
    else {
      m_httpConnection.setBufferGrowthStrategyFactory(
        new SlowClientBandwidthLimiterFactory(m_slowClientSleeper, targetBPS));
    }
  }

  @Override
  public void close() {
    m_httpConnection.stop();
  }
}
