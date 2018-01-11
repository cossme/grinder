// Copyright (C) 2000 Phil Dawes
// Copyright (C) 2000 - 2012 Philip Aston
// Copyright (C) 2001 Paddy Spencer
// Copyright (C) 2003, 2004, 2005 Bertrand Ave
// Copyright (C) 2007 Venelin Mitov
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

package net.grinder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.grinder.plugin.http.tcpproxyfilter.ConnectionCache;
import net.grinder.plugin.http.tcpproxyfilter.ConnectionHandlerFactoryImplementation;
import net.grinder.plugin.http.tcpproxyfilter.HTTPRecordingImplementation;
import net.grinder.plugin.http.tcpproxyfilter.HTTPRequestFilter;
import net.grinder.plugin.http.tcpproxyfilter.HTTPResponseFilter;
import net.grinder.plugin.http.tcpproxyfilter.ParametersFromProperties;
import net.grinder.plugin.http.tcpproxyfilter.ProcessHTTPRecordingWithXSLT;
import net.grinder.plugin.http.tcpproxyfilter.ProcessHTTPRecordingWithXSLT.BuiltInStyleSheet;
import net.grinder.plugin.http.tcpproxyfilter.ProcessHTTPRecordingWithXSLT.StyleSheetFile;
import net.grinder.plugin.http.tcpproxyfilter.RegularExpressionsImplementation;
import net.grinder.tools.tcpproxy.CommentSourceImplementation;
import net.grinder.tools.tcpproxy.CompositeFilter;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.EchoFilter;
import net.grinder.tools.tcpproxy.EndPoint;
import net.grinder.tools.tcpproxy.HTTPProxyTCPProxyEngine;
import net.grinder.tools.tcpproxy.NullFilter;
import net.grinder.tools.tcpproxy.PortForwarderTCPProxyEngine;
import net.grinder.tools.tcpproxy.TCPProxyConsole;
import net.grinder.tools.tcpproxy.TCPProxyEngine;
import net.grinder.tools.tcpproxy.TCPProxyFilter;
import net.grinder.tools.tcpproxy.TCPProxySSLSocketFactory;
import net.grinder.tools.tcpproxy.TCPProxySSLSocketFactoryImplementation;
import net.grinder.tools.tcpproxy.UpdatableCommentSource;
import net.grinder.util.AbstractMainClass;
import net.grinder.util.AttributeStringParserImplementation;
import net.grinder.util.SimpleStringEscaper;
import net.grinder.util.http.URIParserImplementation;

import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.PicoContainer;
import org.picocontainer.behaviors.Caching;
import org.picocontainer.monitors.ConsoleComponentMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is the entry point of The TCPProxy process.
 *
 * @author Phil Dawes
 * @author Philip Aston
 * @author Bertrand Ave
 * @author Venelin Mitov
 */
public final class TCPProxy extends AbstractMainClass {

  private static final String USAGE =
    "  java " + TCPProxy.class.getName() + " <options>" +
    "\n\n" +
    "Commonly used options:" +
    "\n  [-http [jython|clojure|<stylesheet>]]" +
    "\n                               See below." +
    "\n  [-console]                   Display the console." +
    "\n  [-requestfilter <filter>]    Add a request filter." +
    "\n  [-responsefilter <filter>]   Add a response filter." +
    "\n  [-localhost <host name/ip>]  Default is localhost." +
    "\n  [-localport <port>]          Default is 8001." +
    "\n  [-keystore <file>]           Key store details for" +
    "\n  [-keystorepassword <pass>]   SSL certificates." +
    "\n  [-keystoretype <type>]       Default is JSSE dependent." +
    "\n\n" +
    "Other options:" +
    "\n  [-properties <file>]         Properties to pass to the filters." +
    "\n  [-remotehost <host name>]    Default is localhost." +
    "\n  [-remoteport <port>]         Default is 7001." +
    "\n  [-timeout <seconds>]         Proxy engine timeout." +
    "\n  [-httpproxy <host> <port>]   Route via HTTP/HTTPS proxy." +
    "\n  [-httpsproxy <host> <port>]  Override -httpproxy settings for" +
    "\n                               HTTPS." +
    "\n  [-ssl]                       Use SSL when port forwarding." +
    "\n  [-colour]                    Be pretty on ANSI terminals." +
    "\n  [-component <class>]         Register a component class with" +
    "\n                               the filter PicoContainer." +
    "\n  [-debug]                     Make PicoContainer chatty." +
    "\n\n" +
    "<filter> is the name of a class that implements " +
    TCPProxyFilter.class.getName() + " or one of NONE, ECHO. The default " +
    "is ECHO. Multiple filters can be specified for each stream." +
    "\n\n" +
    "By default, the TCPProxy listens as an HTTP/HTTPS Proxy on " +
    "<localhost:localport>." +
    "\n\n" +
    "If either -remotehost or -remoteport is specified, the TCPProxy " +
    "acts a simple port forwarder between <localhost:localport> and " +
    "<remotehost:remoteport>. Specify -ssl for SSL support." +
    "\n\n" +
    "-http sets up request and response filters to produce a test script " +
    "suitable for use with the HTTP plugin. The keywords 'jython', or " +
    "'clojure' can be used to set the script language; or the filename of " +
    "of an alternative XSLT style sheet can be provided. The default is" +
    "'jython' which creates a Jython script." +
    "\n\n" +
    "-timeout is how long the TCPProxy will wait for a request " +
    "before timing out and freeing the local port. The TCPProxy will " +
    "not time out if there are active connections." +
    "\n\n" +
    "-console displays a simple control window that allows the TCPProxy " +
    "to be shutdown cleanly. This is needed because some shells, e.g. " +
    "Cygwin bash, do not allow Java processes to be interrupted cleanly, " +
    "so filters cannot rely on standard shutdown hooks. " +
    "\n\n" +
    "-httpproxy and -httpsproxy allow output to be directed through " +
    "another HTTP/HTTPS proxy; this may help you reach the Internet. " +
    "These options are not supported in port forwarding mode." +
    "\n\n" +
    "Typical usage: " +
    "\n  java " + TCPProxy.class + " -http -console > grinder.py" +
    "\n\n";

  /**
   * Entry point.
   *
   * @param args Command line arguments.
   */
  public static void main(String[] args) {
    final Logger logger = LoggerFactory.getLogger("tcpproxy");

    try {
      final TCPProxy tcpProxy = new TCPProxy(args, logger);
      tcpProxy.run();
    }
    catch (LoggedInitialisationException e) {
      System.exit(1);
    }
    catch (Throwable e) {
      logger.error("Could not initialise", e);
      System.exit(2);
    }

    System.exit(0);
  }

  private final DefaultPicoContainer m_filterContainer =
    new DefaultPicoContainer(new Caching());

  private final TCPProxyEngine m_proxyEngine;

  /**
   * Package scope for unit tests.
   */
  TCPProxy(String[] args, Logger logger) throws Exception {
    super(logger, USAGE);

    final PrintWriter output = new PrintWriter(System.out);
    m_filterContainer.addComponent(output);

    m_filterContainer.addComponent(logger);

    final UpdatableCommentSource commentSource =
      new CommentSourceImplementation();
    m_filterContainer.addComponent(commentSource);

    // Default values.
    int localPort = 8001;
    String remoteHost = "localhost";
    String localHost = "localhost";
    int remotePort = 7001;
    boolean useSSLPortForwarding = false;
    File keyStoreFile = null;
    char[] keyStorePassword = null;
    String keyStoreType = null;
    boolean isHTTPProxy = true;
    boolean console = false;
    EndPoint chainedHTTPProxy = null;
    EndPoint chainedHTTPSProxy = null;
    int timeout = 0;
    boolean useColour = false;

    final FilterChain requestFilterChain = new FilterChain("request");
    final FilterChain responseFilterChain = new FilterChain("response");

    try {
      // Parse 1.
      for (int i = 0; i < args.length; i++) {
        if ("-properties".equalsIgnoreCase(args[i])) {
          final Properties properties = new Properties();
          final FileInputStream in = new FileInputStream(new File(args[++i]));
          try {
            properties.load(in);
          }
          finally {
            in.close();
          }
          System.getProperties().putAll(properties);
        }
      }

      // Parse 2.
      for (int i = 0; i < args.length; i++) {
        if ("-requestfilter".equalsIgnoreCase(args[i])) {
          requestFilterChain.add(args[++i]);
        }
        else if ("-responsefilter".equalsIgnoreCase(args[i])) {
          responseFilterChain.add(args[++i]);
        }
        else if ("-component".equalsIgnoreCase(args[i])) {
          final Class<?> componentClass;

          try {
            componentClass = Class.forName(args[++i]);
          }
          catch (ClassNotFoundException e) {
            throw barfError("Class '" + args[i] + "' not found.");
          }
          m_filterContainer.addComponent(componentClass);
        }
        else if ("-http".equalsIgnoreCase(args[i])) {
          requestFilterChain.add(HTTPRequestFilter.class);
          responseFilterChain.add(HTTPResponseFilter.class);
          m_filterContainer.addComponent(
            AttributeStringParserImplementation.class);
          m_filterContainer.addComponent(ConnectionCache.class);
          m_filterContainer.addComponent(
            ConnectionHandlerFactoryImplementation.class);
          m_filterContainer.addComponent(ParametersFromProperties.class);
          m_filterContainer.addComponent(HTTPRecordingImplementation.class);
          m_filterContainer.addComponent(ProcessHTTPRecordingWithXSLT.class);
          m_filterContainer.addComponent(
            RegularExpressionsImplementation.class);
          m_filterContainer.addComponent(URIParserImplementation.class);
          m_filterContainer.addComponent(SimpleStringEscaper.class);

          if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
            final String s = args[++i];

            if ("jython".equals(s)) {
              // Default.
            }
            else if ("oldjython".equals(s)) { // Undocumented option.
              m_filterContainer.addComponent(
                BuiltInStyleSheet.TraditionalJython);
            }
            else if ("clojure".equals(s)) {
              m_filterContainer.addComponent(BuiltInStyleSheet.Clojure);
            }
            else {
              m_filterContainer.addComponent(new StyleSheetFile(new File(s)));
            }
          }
        }
        else if ("-localhost".equalsIgnoreCase(args[i])) {
          localHost = args[++i];
        }
        else if ("-localport".equalsIgnoreCase(args[i])) {
          localPort = Integer.parseInt(args[++i]);
        }
        else if ("-remotehost".equalsIgnoreCase(args[i])) {
          remoteHost = args[++i];
          isHTTPProxy = false;
        }
        else if ("-remoteport".equalsIgnoreCase(args[i])) {
          remotePort = Integer.parseInt(args[++i]);
          isHTTPProxy = false;
        }
        else if ("-ssl".equalsIgnoreCase(args[i])) {
          useSSLPortForwarding = true;
        }
        else if ("-keystore".equalsIgnoreCase(args[i])) {
          keyStoreFile = new File(args[++i]);
        }
        else if ("-keystorepassword".equalsIgnoreCase(args[i]) ||
                 "-storepass".equalsIgnoreCase(args[i])) {
          keyStorePassword = args[++i].toCharArray();
        }
        else if ("-keystoretype".equalsIgnoreCase(args[i]) ||
                 "-storetype".equalsIgnoreCase(args[i])) {
          keyStoreType = args[++i];
        }
        else if ("-timeout".equalsIgnoreCase(args[i])) {
          timeout = Integer.parseInt(args[++i]) * 1000;
        }
        else if ("-console".equalsIgnoreCase(args[i])) {
          console = true;
        }
        else if ("-colour".equalsIgnoreCase(args[i]) ||
                 "-color".equalsIgnoreCase(args[i])) {
          useColour = true;
        }
        else if ("-properties".equalsIgnoreCase(args[i])) {
          /* Already handled */
          ++i;
        }
        else if ("-httpproxy".equalsIgnoreCase(args[i])) {
          chainedHTTPProxy =
            new EndPoint(args[++i], Integer.parseInt(args[++i]));
        }
        else if ("-httpsproxy".equalsIgnoreCase(args[i])) {
          chainedHTTPSProxy =
            new EndPoint(args[++i], Integer.parseInt(args[++i]));
        }
        else if ("-debug".equalsIgnoreCase(args[i])) {
          m_filterContainer.changeMonitor(
            new ConsoleComponentMonitor(System.err));
        }
        else {
          throw barfUsage();
        }
      }
    }
    catch (FileNotFoundException fnfe) {
      throw barfError(fnfe.getMessage());
    }
    catch (IndexOutOfBoundsException e) {
      throw barfUsage();
    }
    catch (NumberFormatException e) {
      throw barfUsage();
    }

    if (timeout < 0) {
      throw barfError("Timeout must be non-negative.");
    }

    final EndPoint localEndPoint = new EndPoint(localHost, localPort);
    final EndPoint remoteEndPoint = new EndPoint(remoteHost, remotePort);

    if (chainedHTTPSProxy == null && chainedHTTPProxy != null) {
      chainedHTTPSProxy = chainedHTTPProxy;
    }

    if (chainedHTTPSProxy != null && !isHTTPProxy) {
      throw barfError("Routing through a HTTP/HTTPS proxy is not supported " +
                      "in port forwarding mode.");
    }

    final TCPProxyFilter requestFilter = requestFilterChain.resolveFilter();
    final TCPProxyFilter responseFilter = responseFilterChain.resolveFilter();

    final StringBuilder startMessage = new StringBuilder();

    startMessage.append("Initialising as ");

    if (isHTTPProxy) {
      startMessage.append("an HTTP/HTTPS proxy");
    }
    else {
      if (useSSLPortForwarding) {
        startMessage.append("an SSL port forwarder");
      }
      else {
        startMessage.append("a TCP port forwarder");
      }
    }

    startMessage.append(" with the parameters:");
    startMessage.append("\n   Request filters:    ");
    startMessage.append(requestFilter);
    startMessage.append("\n   Response filters:   ");
    startMessage.append(responseFilter);
    startMessage.append("\n   Local address:      " + localEndPoint);

    if (!isHTTPProxy) {
      startMessage.append("\n   Remote address:     " + remoteEndPoint);
    }

    if (chainedHTTPProxy != null) {
      startMessage.append("\n   HTTP proxy:         " + chainedHTTPProxy);
    }

    if (chainedHTTPSProxy != null) {
      startMessage.append("\n   HTTPS proxy:        " + chainedHTTPSProxy);
    }

    if (keyStoreFile != null) {
      startMessage.append("\n   Key store:          ");
      startMessage.append(keyStoreFile.toString());

      // Key store password is optional.
      if (keyStorePassword != null) {
        startMessage.append("\n   Key store password: ");
        for (int i = 0; i < keyStorePassword.length; ++i) {
          startMessage.append('*');
        }
      }

      // Key store type can be null => use whatever
      // KeyStore.getDefaultType() says (we can't print the default
      // here without loading the JSSE).
      if (keyStoreType != null) {
        startMessage.append("\n   Key store type:     " + keyStoreType);
      }
    }

    logger.info(startMessage.toString());

    final TCPProxySSLSocketFactory sslSocketFactory =
      keyStoreFile != null ?
      new TCPProxySSLSocketFactoryImplementation(keyStoreFile,
                                                 keyStorePassword,
                                                 keyStoreType) :
      new TCPProxySSLSocketFactoryImplementation();

    m_filterContainer.start();

    if (isHTTPProxy) {
      m_proxyEngine =
        new HTTPProxyTCPProxyEngine(
          sslSocketFactory,
          requestFilter, responseFilter,
          output,
          logger,
          localEndPoint,
          useColour,
          timeout,
          chainedHTTPProxy, chainedHTTPSProxy);
    }
    else {
      if (useSSLPortForwarding) {
        m_proxyEngine =
          new PortForwarderTCPProxyEngine(
            sslSocketFactory,
            requestFilter, responseFilter,
            output,
            logger,
            new ConnectionDetails(localEndPoint, remoteEndPoint, true),
            useColour,
            timeout);
      }
      else {
        m_proxyEngine =
          new PortForwarderTCPProxyEngine(
            requestFilter, responseFilter,
            output,
            logger,
            new ConnectionDetails(localEndPoint, remoteEndPoint, false),
            useColour,
            timeout);
      }
    }

    if (console) {
      new TCPProxyConsole(m_proxyEngine, commentSource);
    }

    logger.info("Engine initialised, listening on port " + localPort);
  }

  private void run() {
    final Runnable shutdown = new Runnable() {
      private boolean m_stopped = false;

      public synchronized void run() {
        if (!m_stopped) {
          m_stopped = true;
          m_proxyEngine.stop();
          m_filterContainer.stop();
          m_filterContainer.dispose();
        }
      }
    };

    Runtime.getRuntime().addShutdownHook(new Thread(shutdown));

    m_proxyEngine.run();
    shutdown.run();

    getLogger().info("Engine exited");
  }

  private final class FilterChain {
    private final String m_type;
    private final List<String> m_filterKeys = new ArrayList<String>();
    private int m_value;

    public FilterChain(String type) {
      m_type = type;
    }

    public void add(Class<? extends TCPProxyFilter> theClass) {
      final String key = m_type + ++m_value;
      m_filterContainer.addComponent(key, theClass);
      m_filterKeys.add(key);
    }

    @SuppressWarnings("unchecked")
    public void add(String filterClassName)
      throws LoggedInitialisationException {

      if ("NONE".equals(filterClassName)) {
        add(NullFilter.class);
      }
      else if ("ECHO".equals(filterClassName)) {
        add(EchoFilter.class);
      }
      else {
        final Class<?> filterClass;

        try {
          filterClass = Class.forName(filterClassName);
        }
        catch (ClassNotFoundException e) {
          throw barfError("Class '" + filterClassName + "' not found.");
        }

        if (!TCPProxyFilter.class.isAssignableFrom(filterClass)) {
          throw barfError("The class '" + filterClass.getName() +
                          "' does not implement the interface: '" +
                          TCPProxyFilter.class.getName() + "'.");
        }

        add((Class<? extends TCPProxyFilter>) filterClass);
      }
    }

    public TCPProxyFilter resolveFilter() {
      if (m_filterKeys.size() == 0) {
        add(EchoFilter.class);
      }

      final CompositeFilter result = new CompositeFilter();

      for (String key : m_filterKeys) {
        result.add((TCPProxyFilter) m_filterContainer.getComponent(key));
      }

      return result;
    }
  }

  /**
   *  Accessor for unit tests.
   */
  PicoContainer getFilterContainer() {
    return m_filterContainer;
  }
}
