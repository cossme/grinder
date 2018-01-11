// Copyright (C) 2002 - 2013 Philip Aston
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

import net.grinder.common.GrinderException;
import HTTPClient.ParseException;
import HTTPClient.ProtocolNotSuppException;
import HTTPClient.URI;


/**
 * Facade through which the script can control the behaviour of the
 * HTTP plug-in.
 *
 * @author Philip Aston
 **/
public final class HTTPPluginControl {

  ///CLOVER:OFF
  private HTTPPluginControl() {
  }
  ///CLOVER:ON

  /**
   * Returns a {@link HTTPPluginConnection} that can be used to set
   * the default behaviour of new connections.
   * @return An {@code HTTPPluginConnection}.
   */
  public static HTTPPluginConnection getConnectionDefaults() {
    return HTTPPluginConnectionDefaults.getConnectionDefaults();
  }

  /**
   * Returns a {@link HTTPPluginConnection} for a particular URL.
   *
   * <p>The resulting {@code HTTPPluginConnection} is valid for
   * the current thread and the current run. It can be used to set
   * specific authentication details, default headers, cookies, proxy
   * servers, and so on for the current thread/run on a per-URL
   * basis.</p>
   *
   * <p>This method will throw a {@link GrinderException} if not
   * called from a worker thread.</p>
   *
   * @param url An absolute URL that specifies the connection.
   * @return a {@code HTTPPluginConnection} value
   * @throws GrinderException If an error occurs.
   * @throws ParseException If {@code url} can not be parsed.
   * @throws ProtocolNotSuppException If {@code url}
   * specifies an unsupported protocol.
   */
  public static HTTPPluginConnection getThreadConnection(final String url)
    throws GrinderException, ParseException, ProtocolNotSuppException {

    return getThreadState().getConnectionWrapper(new URI(url));
  }

  /**
   * Returns the HTTPClient context object for the calling worker
   * thread. This is useful when calling HTTPClient methods directly,
   * e.g. {@link HTTPClient.CookieModule#listAllCookies(Object)}.
   *
   * <p>This method will throw a {@link GrinderException} if not
   * called from a worker thread.</p>
   *
   * @return The context object used for
   * {@code HTTPClient.HTTPConnections} created by this thread.
   * @throws GrinderException If an error occurs.
   */
  public static Object getThreadHTTPClientContext() throws GrinderException {
    return getThreadState();
  }

  /**
   * Provides access to an {@link HTTPUtilities} instance.
   *
   * @return The utilities instance.
   * @throws GrinderException If an error occurs.
   */
  public static HTTPUtilities getHTTPUtilities() throws GrinderException {
    return new HTTPUtilitiesImplementation(HTTPPlugin.getPlugin());
  }

  private static HTTPPluginThreadState getThreadState()
      throws GrinderException {
    return HTTPPlugin.getPlugin().getThreadState();
  }
}
