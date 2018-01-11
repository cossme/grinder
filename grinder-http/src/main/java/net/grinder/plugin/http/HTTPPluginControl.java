// Copyright (C) 2002 - 2012 Philip Aston
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

import HTTPClient.ParseException;
import HTTPClient.ProtocolNotSuppException;
import HTTPClient.URI;

import net.grinder.common.GrinderException;
import net.grinder.plugininterface.PluginProcessContext;


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
   * @return An <code>HTTPPluginConnection</code>.
   */
  public static HTTPPluginConnection getConnectionDefaults() {
    return HTTPPluginConnectionDefaults.getConnectionDefaults();
  }

  /**
   * Returns a {@link HTTPPluginConnection} for a particular URL.
   *
   * <p>The resulting <code>HTTPPluginConnection</code> is valid for
   * the current thread and the current run. It can be used to set
   * specific authentication details, default headers, cookies, proxy
   * servers, and so on for the current thread/run on a per-URL
   * basis.</p>
   *
   * <p>This method will throw a {@link GrinderException} if not
   * called from a worker thread.</p>
   *
   * @param url An absolute URL that specifies the connection.
   * @return a <code>HTTPPluginConnection</code> value
   * @exception GrinderException If an error occurs.
   * @exception ParseException If <code>url</code> can not be parsed.
   * @exception ProtocolNotSuppException If <code>url</code>
   * specifies an unsupported protocol.
   */
  public static HTTPPluginConnection getThreadConnection(String url)
    throws GrinderException, ParseException, ProtocolNotSuppException {

    final HTTPPluginThreadState threadState =
      (HTTPPluginThreadState)getProcessContext().getPluginThreadListener();

    return threadState.getConnectionWrapper(new URI(url));
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
   * <code>HTTPClient.HTTPConnections</code> created by this thread.
   * @exception GrinderException If an error occurs.
   */
  public static Object getThreadHTTPClientContext() throws GrinderException {
    return getProcessContext().getPluginThreadListener();
  }

  /**
   * Provides access to an {@link HTTPUtilities} instance.
   *
   * @return The utilities instance.
   */
  public static HTTPUtilities getHTTPUtilities() {
    return new HTTPUtilitiesImplementation(getProcessContext());
  }

  private static PluginProcessContext getProcessContext() {
    return HTTPPlugin.getPlugin().getPluginProcessContext();
  }
}
