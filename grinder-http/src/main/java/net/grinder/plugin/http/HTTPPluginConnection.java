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

import HTTPClient.NVPair;


/**
 * Interface that script can use to control HTTP connections.
 *
 * <p><em>Most of the documentation for this class has been copied
 * verbatim from the HTTPClient documentation.</em></p>
 *
 * @author Philip Aston
 * @author Richard Perks
 * @author Bertrand Ave
 * @see HTTPPluginControl
 */
public interface HTTPPluginConnection {

  /**
   * Set whether redirects should be automatically followed.
   *
   * <p>This enables the HTTPClient Redirection module.</p>
   *
   * <p>Default: {@code false}</p>
   *
   * @param followRedirects {@code true} => follow redirects.
   */
  void setFollowRedirects(boolean followRedirects);

  /**
   * Set whether cookies will be used.
   *
   * <p>This enables the HTTPClient Cookie module.</p>
   *
   * <p>Default: {@code true}</p>
   *
   * @param useCookies {@code true} => use cookies.
   */
  void setUseCookies(boolean useCookies);

  /**
   * Set whether content encoding will be used.
   *
   * <p>This enables the HTTPClient Content Encoding module.</p>
   *
   * <p>Default: {@code false}</p>
   *
   * @param useContentEncoding {@code true} => use content encoding.
   */
  void setUseContentEncoding(boolean useContentEncoding);

  /**
   * Set whether transfer encoding will be used.
   *
   * <p>This enables the HTTPClient Transfer Encoding module.</p>
   *
   * <p>Default: {@code false}</p>
   *
   * @param useTransferEncoding {@code true} => use transfer encoding.
   */
  void setUseTransferEncoding(boolean useTransferEncoding);

  /**
   * Set whether the HTTPClient Authorization Module is enabled.
   *
   * <p>
   * Default: {@code false}
   * </p>
   *
   * @param useAuthorizationModule
   *          {@code true} => use the HTTPClient Authorization module.
   */
  void setUseAuthorizationModule(boolean useAuthorizationModule);

  /**
   * Sets the default HTTP headers to be sent with each request.
   *
   * <p>
   * The actual headers sent are determined as follows: for each header
   * specified in multiple places a value given as part of the request takes
   * priority over any default values set by this method, which in turn takes
   * priority over any built-in default values. A different way of looking at it
   * is that we start off with a list of all headers specified with the request,
   * then add any default headers set by this method which aren't already in our
   * list, and finally add any built-in headers which aren't yet in the list.
   * There is one exception to this rule: {@code Content-Length} header is
   * always ignored; and when posting form-data any {@code Content-Type} is
   * ignored in favor of the built-in {@code application/x-www-form-urlencoded}
   * (however it will be overridden by any content-type header specified as part
   * of the request).
   * </p>
   *
   * <p>
   * Typical headers you might want to set here are {@code Accept} and its
   * {@code Accept-*} relatives, {@code Connection}, {@code From}, {@code
   * User-Agent}, etc.
   * </p>
   *
   * @param defaultHeaders
   *          an array of header-name/value pairs (do not give the separating
   *          ':').
   */
  void setDefaultHeaders(NVPair[] defaultHeaders);

  /**
   * Sets the timeout to be used for creating connections and
   * reading responses.
   *
   * <p>Setting the timeout to anything other than {@code 0}
   * will cause additional threads to be spawned for each HTTP
   * request made.</p>
   *
   * <p>When a timeout expires the operation will throw a
   * {@link TimeoutException}.</p>
   *
   * <P>When creating new sockets the timeout will limit the time spent
   * doing the host name translation and establishing the connection with
   * the server.</p>
   *
   * <P>The timeout also influences the reading of the response
   * headers. However, it does not specify a how long, for example,
   * {@link HTTPClient.HTTPResponse#getStatusCode} may take, as
   * might be assumed. Instead it specifies how long a read on the
   * socket may take. If the response dribbles in slowly with
   * packets arriving quicker than the timeout then the method will
   * complete normally. I.e. the exception is only thrown if nothing
   * arrives on the socket for the specified time. Furthermore, the
   * timeout only influences the reading of the headers, not the
   * reading of the body.</p>
   *
   * <P>Read timeouts are associated with responses, so that you may
   * change this value before each request and it won't affect the
   * reading of responses to previous requests.</p>
   *
   * @param timeout the time in milliseconds. A time of 0 means wait
   *             indefinitely.
   */
  void setTimeout(int timeout);

  /**
   * Set whether an exception should be thrown if the subject
   * distinguished name of the server's certificate doesn't match
   * the host name when establishing an HTTPS connection.
   *
   * @param b a {@code boolean} value
   */
  void setVerifyServerDistinguishedName(boolean b);

  /**
   * Set the proxy server to use. A null or empty string
   * {@code host} parameter disables the proxy.
   *
   * <P>Note that if you set a proxy for the connection using this
   * method, and a request made over this connection is redirected
   * to a different server, then the connection used for new server
   * will <em>not</em> pick this proxy setting, but instead will use
   * the default proxy settings. The default proxy setting can be
   * set using
   * {@code HTTPPluginControl.getConnectionDefaults().setProxyServer()}.
   *
   * @param  host    The host on which the proxy server resides.
   * @param  port    The port the proxy server is listening on.
   */
  void setProxyServer(String host, int port);

  /**
   * Set the client IP address to use for outbound connections.
   *
   * <p>The default client IP address, and hence the network
   * interface, used for outbound HTTP requests is the first returned
   * to the Java VM by the operating system. This method allows a
   * different network interface to be specified that will be used for
   * connections that are subsequently created. It does not affect
   * existing socket connections that may have already been created
   * for this {@code HTTPPluginConnection}.</p>
   *
   * <p>{@code localAddress} should correspond to a local network
   * interface.If it doesn't a {@code java.net.BindException}
   * will be thrown when the connection is first used.</p>
   *
   * @param localAddress The local host name or IP address to bind to.
   * Pass {@code null} to set the default local interface.
   * @exception URLException If {@code localAddress} could not be
   * resolved.
   */
  void setLocalAddress(String localAddress) throws URLException;

  /**
   * Artificially limit the bandwidth used by this connection.
   *
   * <p>
   * Only bytes in the HTTP message bodies are taken into account when
   * interpreting {@code targetBPS}. No account is taken of the network
   * efficiency (e.g. it might take 10 bits on the wire to transmit one byte of
   * application data), or of the HTTP headers.
   * </p>
   *
   * <p>The limiting is also applied to the bodies of POST requests uploaded to
   * the server.</p>
   *
   * <p>
   * When bandwidth limiting is applied, the time taken by each HTTP request
   * will be correspondingly longer.</p>.
   *
   * @param targetBPS
   *          Target bandwidth in bits per second. Set to {@code 0} to
   *          disable bandwidth limiting.
   */
  void setBandwidthLimit(int targetBPS);

  /**
   * Explicitly closes physical connection to the server. A new connection will
   * be created if this {@link HTTPPluginConnection} is used again. You
   * shouldn't normally need to call this.
   */
  void close();
}
