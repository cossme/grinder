// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2013 Philip Aston
// Copyright (C) 2004 Bertrand Ave
// Copyright (C) 2004 John Stanford White
// Copyright (C) 2004 Calum Fitzgerald
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
import net.grinder.common.TimeAuthority;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginThreadListener;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.script.Statistics;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.util.Sleeper;
import net.grinder.util.SleeperImplementation;
import HTTPClient.CookieModule;
import HTTPClient.DefaultAuthHandler;
import HTTPClient.HTTPConnection;


/**
 * HTTP plug-in.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @author Bertrand Ave
 */
public class HTTPPlugin implements GrinderPlugin {

  private static HTTPPlugin s_singleton;

  /**
   * Static package scope accessor for the initialised instance of the
   * plug-in.
   *
   * @return The plug-in instance.
   * @throws PluginException
   */
  static final HTTPPlugin getPlugin() throws PluginException {
    synchronized (HTTPPlugin.class) {
      if (s_singleton == null) {
        throw new PluginException("Plugin has not been initialised");
      }

      return s_singleton;
    }
  }

  private final PluginProcessContext m_pluginProcessContext;
  private final Sleeper m_slowClientSleeper;
  private final ScriptContext m_scriptContext;
  private final HTTPClient.HTTPConnection.TimeAuthority
    m_httpClientTimeAuthority;
  private boolean m_initialized;

  /**
   * Constructor. Registered with the plugin container to be called at
   * start up.
   *
   * @param processContext The plugin process context.
   * @param scriptContext The script context.
   */
  public HTTPPlugin(final PluginProcessContext processContext,
                    final ScriptContext scriptContext) {

    m_pluginProcessContext = processContext;
    m_scriptContext = scriptContext;

    final TimeAuthority timeAuthority = m_scriptContext.getTimeAuthority();

    m_slowClientSleeper = new SleeperImplementation(timeAuthority, null, 1, 0);

    m_httpClientTimeAuthority = new HTTPClient.HTTPConnection.TimeAuthority() {
        @Override public long getTimeInMilliseconds() {
          return timeAuthority.getTimeInMilliseconds();
        }
      };

    synchronized (HTTPPlugin.class) {
      s_singleton = this;
    }
  }

  final HTTPPluginThreadState getThreadState() throws GrinderException {
    return (HTTPPluginThreadState)
        m_pluginProcessContext.getPluginThreadListener(this);
  }

  final ScriptContext getScriptContext() {
    return m_scriptContext;
  }

  /**
   * Delay initialisation that is costly or has external effects until the
   * plugin is used by the script.
   *
   * @throws PluginException if an error occurs.
   */
  void ensureInitialised() throws PluginException {

    synchronized (this) {
      if (m_initialized) {
        return;
      }

      m_initialized = true;

      // Remove standard HTTPClient modules which we don't want. We load
      // HTTPClient modules dynamically as we don't have public access.
      try {
        // Don't want to retry requests.
        HTTPConnection.removeDefaultModule(
          Class.forName("HTTPClient.RetryModule"));
      }
      catch (final ClassNotFoundException e) {
        throw new PluginException("Could not load HTTPClient modules", e);
      }

      // Turn off cookie permission checks.
      CookieModule.setCookiePolicyHandler(null);

      // Turn off authorisation UI.
      DefaultAuthHandler.setAuthorizationPrompter(null);

      // Register custom statistics.
      try {

        final Statistics statistics = m_scriptContext.getStatistics();

        statistics.registerDataLogExpression(
          "HTTP response code",
          StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_STATUS_KEY);

        statistics.registerDataLogExpression(
          "HTTP response length",
          StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_LENGTH_KEY);

        statistics.registerDataLogExpression(
          "HTTP response errors",
          StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_ERRORS_KEY);

        statistics.registerDataLogExpression(
          "Time to resolve host",
          StatisticsIndexMap.HTTP_PLUGIN_DNS_TIME_KEY);

        statistics.registerDataLogExpression(
          "Time to establish connection",
          StatisticsIndexMap.HTTP_PLUGIN_CONNECT_TIME_KEY);

        statistics.registerDataLogExpression(
          "Time to first byte",
          StatisticsIndexMap.HTTP_PLUGIN_FIRST_BYTE_TIME_KEY);

        statistics.registerDataLogExpression(
          "New connections",
          StatisticsIndexMap.HTTP_PLUGIN_CONNECTIONS_ESTABLISHED);

        statistics.registerSummaryExpression(
          "Mean response length",
          "(/ " + StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_LENGTH_KEY +
          " (+ (count timedTests) untimedTests))");

        statistics.registerSummaryExpression(
          "Response bytes per second",
          "(* 1000 (/ " + StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_LENGTH_KEY +
          " period))");

        statistics.registerSummaryExpression(
          "Response errors",
          StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_ERRORS_KEY);

        statistics.registerSummaryExpression(
          "Mean time to resolve host",
          "(/ " + StatisticsIndexMap.HTTP_PLUGIN_DNS_TIME_KEY +
          " (+ " + StatisticsIndexMap.HTTP_PLUGIN_CONNECTIONS_ESTABLISHED +
          "))");

        statistics.registerSummaryExpression(
          "Mean time to establish connection",
          "(/ " + StatisticsIndexMap.HTTP_PLUGIN_CONNECT_TIME_KEY +
          " (+ " + StatisticsIndexMap.HTTP_PLUGIN_CONNECTIONS_ESTABLISHED +
          "))");

        statistics.registerSummaryExpression(
          "Mean time to first byte",
          "(/ " + StatisticsIndexMap.HTTP_PLUGIN_FIRST_BYTE_TIME_KEY +
          " (+ (count timedTests) untimedTests))");
      }
      catch (final GrinderException e) {
        throw new PluginException(
          "Could not register custom statistics. Try importing HTTPRequest " +
          "from the top level of your script.", e);
      }
    }
  }

  /**
   * Called by the engine to obtain a new PluginThreadListener.
   *
   * @return The new plug-in thread listener.
   * @exception PluginException if an error occurs.
   */
  @Override
  public PluginThreadListener createThreadListener() throws PluginException {

    return new HTTPPluginThreadState(m_scriptContext.getSSLControl(),
                                     m_slowClientSleeper,
                                     m_httpClientTimeAuthority);
  }

  // It may be useful to separate out a null implementation that can
  // be used without the core services. To do this, we should create
  // a separate interface, and default the singleton to use the null
  // implementation. It would be worth registering the underlying
  // script context components with pico: {sslControl, timeAuthority,
  // logger, statistics}, so we reduce the number of null implementations
  // required.
}
