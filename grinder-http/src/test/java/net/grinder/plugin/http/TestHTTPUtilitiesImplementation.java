// Copyright (C) 2006 - 2013 Philip Aston
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Answers.RETURNS_MOCKS;
import static org.mockito.Mockito.when;
import net.grinder.common.SSLContextFactory;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.script.Statistics;
import net.grinder.util.InsecureSSLContextFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;


/**
 * Unit test case for <code>HTTPUtilitiesImplementation</code>.
 *
 * @author Philip Aston
 */
public class TestHTTPUtilitiesImplementation {

  private final SSLContextFactory m_sslContextFactory =
      new InsecureSSLContextFactory();

  @Mock private PluginProcessContext m_pluginProcessContext;
  @Mock(answer = RETURNS_MOCKS) private ScriptContext m_scriptContext;
  @Mock private Statistics m_statistics;
  @Mock private HTTPClient.HTTPConnection.TimeAuthority m_timeAuthority;

  private HTTPPlugin m_httpPlugin;

  @Before public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    final HTTPPluginThreadState threadState =
      new HTTPPluginThreadState(m_sslContextFactory,
                                null,
                                m_timeAuthority);

    when(m_scriptContext.getStatistics()).thenReturn(m_statistics);

    m_httpPlugin = new HTTPPlugin(m_pluginProcessContext,
                                  m_scriptContext);

    when(m_pluginProcessContext.getPluginThreadListener(m_httpPlugin))
      .thenReturn(threadState);
  }

  @Test public void testBasicAuthorizationHeader() throws Exception {
    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_httpPlugin);

    final NVPair pair =
      httpUtilities.basicAuthorizationHeader("foo", "secret");
    assertEquals("Authorization", pair.getName());
    assertEquals("Basic Zm9vOnNlY3JldA==", pair.getValue());

    final NVPair pair2 =
      httpUtilities.basicAuthorizationHeader("", "");
    assertEquals("Authorization", pair2.getName());
    assertEquals("Basic Og==", pair2.getValue());
  }

  @Test public void testGetLastResponse() throws Exception {
    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_httpPlugin);

    assertEquals(null, httpUtilities.getLastResponse());

    // HTTPClient isn't hot on interfaces, so we can't stub these.
    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();
    final HTTPRequest request = new HTTPRequest();
    final HTTPResponse httpResponse = request.GET(handler.getURL());

    assertSame(httpResponse, httpUtilities.getLastResponse());

    handler.shutdown();
  }

  @Test public void testValueFromLocationHeader() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_httpPlugin);
    assertEquals("", httpUtilities.valueFromLocationURI("foo"));

    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromLocationURI("foo"));

    handler.addHeader("Location", "http://www.w3.org/pub/WWW/People.html");
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromLocationURI("foo"));

    handler.clearHeaders();
    handler.addHeader(
      "Location", "http://www.w3.org/pub/WWW/People.html?foo=bah&lah=dah");
    request.GET(handler.getURL());
    assertEquals("bah", httpUtilities.valueFromLocationURI("foo"));
    assertEquals("", httpUtilities.valueFromLocationURI("bah"));

    handler.clearHeaders();
    handler.addHeader(
      "Location", "http://www.w3.org/pub/WWW/People.html;foo=?foo=bah&lah=dah");
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromLocationURI("foo"));
    assertEquals("dah", httpUtilities.valueFromLocationURI("lah"));

    handler.clearHeaders();
    handler.addHeader(
      "Location", "http://www.w3.org/pub/WWW/People.html;JSESSIONID=1234");
    request.GET(handler.getURL());
    assertEquals("1234", httpUtilities.valueFromLocationURI("JSESSIONID"));
    assertEquals("", httpUtilities.valueFromLocationURI("foo"));

    handler.shutdown();
  }

  @Test public void testValueFromBodyURI() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_httpPlugin);
    assertEquals("", httpUtilities.valueFromBodyURI("foo"));

    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromBodyURI("foo"));

    handler.setBody(
      "<body><a href='http://www.w3.org/pub/WWW/People.html'>foo</a></body>");
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromBodyURI("foo"));

    handler.setBody(
      "<body><a href='http://www.w3.org/pub/WWW/People.html?foo=bah&lah=dah'>foo</a></body>");
    request.GET(handler.getURL());
    assertEquals("bah", httpUtilities.valueFromBodyURI("foo"));
    assertEquals("", httpUtilities.valueFromBodyURI("bah"));

    handler.setBody(
      "<body><a href='http://www.w3.org/pub/WWW/People.html;foo=?foo=bah&lah=dah'>foo</a></body>");
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromBodyURI("foo"));
    assertEquals("dah", httpUtilities.valueFromBodyURI("lah"));

    handler.setBody(
    "<body><a href='http://www.w3.org/pub/WWW/People.html;JSESSIONID=1234'>foo</a>" +
    "<a href='http://www.w3.org/pub/WWW/People.html;JSESSIONID=5678'>foo</a></body>");
    request.GET(handler.getURL());
    assertEquals("1234", httpUtilities.valueFromBodyURI("JSESSIONID"));
    assertEquals("", httpUtilities.valueFromBodyURI("foo"));

    handler.addHeader("Content-type", "garbage");
    request.GET(handler.getURL());
    assertEquals("1234", httpUtilities.valueFromBodyURI("JSESSIONID"));
    assertEquals("", httpUtilities.valueFromBodyURI("foo"));
    assertEquals("1234", httpUtilities.valueFromBodyURI("JSESSIONID", "<body>"));
    assertEquals("5678", httpUtilities.valueFromBodyURI("JSESSIONID", "</a>"));
    assertEquals("", httpUtilities.valueFromBodyURI("JSESSIONID", "5"));
    assertEquals("", httpUtilities.valueFromBodyURI("JSESSIONID", "999"));

    handler.shutdown();
  }

  @Test public void testValuesFromBodyURI() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_httpPlugin);
    assertEquals(emptyList(), httpUtilities.valuesFromBodyURI("foo"));

    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();
    request.GET(handler.getURL());
    assertEquals(emptyList(), httpUtilities.valuesFromBodyURI("foo"));

    handler.setBody(
      "<body><a href='http://www.w3.org/pub/WWW/People.html'>foo</a></body>");
    request.GET(handler.getURL());
    assertEquals(emptyList(), httpUtilities.valuesFromBodyURI("foo"));

    handler.setBody(
      "<body><a href='http://www.w3.org/pub/WWW/People.html?foo=bah&lah=dah'>foo</a></body>");
    request.GET(handler.getURL());
    assertEquals(singletonList("bah"), httpUtilities.valuesFromBodyURI("foo"));
    assertEquals(emptyList(), httpUtilities.valuesFromBodyURI("bah"));

    handler.setBody(
      "<body><a href='http://www.w3.org/pub/WWW/People.html;foo=?foo=bah&lah=dah'>foo</a></body>");
    request.GET(handler.getURL());
    assertEquals(singletonList(""), httpUtilities.valuesFromBodyURI("foo"));
    assertEquals(singletonList("dah"), httpUtilities.valuesFromBodyURI("lah"));

    handler.setBody(
    "<body><a href='http://www.w3.org/pub/WWW/People.html;JSESSIONID=1234'>foo</a>" +
    "<a href='http://www.w3.org/pub/WWW/People.html;JSESSIONID=5678'>foo</a></body>");
    request.GET(handler.getURL());
    assertEquals(asList("1234", "5678"),
                 httpUtilities.valuesFromBodyURI("JSESSIONID"));
    assertEquals(emptyList(), httpUtilities.valuesFromBodyURI("foo"));

    handler.addHeader("Content-Type", "garbage");
    request.GET(handler.getURL());
    assertEquals(asList("1234", "5678"),
                 httpUtilities.valuesFromBodyURI("JSESSIONID"));
    assertEquals(emptyList(), httpUtilities.valuesFromBodyURI("foo"));
    assertEquals(asList("1234", "5678"),
                 httpUtilities.valuesFromBodyURI("JSESSIONID", "<body>"));
    assertEquals(asList("5678"),
                 httpUtilities.valuesFromBodyURI("JSESSIONID", "</a>"));
    assertEquals(emptyList(),
                 httpUtilities.valuesFromBodyURI("JSESSIONID", "5"));
    assertEquals(emptyList(),
                 httpUtilities.valuesFromBodyURI("JSESSIONID", "999"));

    handler.shutdown();
  }

  @Test public void testValueFromBodyInput() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_httpPlugin);
    assertEquals("", httpUtilities.valueFromBodyInput("foo"));

    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromBodyInput("foo"));

    handler.setBody("<body><input name='foo'>foo</input></body>");
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromBodyInput("foo"));

    // input tags should be empty. The content has no meaning
    handler.setBody("<body><input type='hidden' name='foo' value='bah'>foo</input>" +
                    "<input name='foo' value='blah'>foo</input></body>");
    request.GET(handler.getURL());
    assertEquals("bah", httpUtilities.valueFromBodyInput("foo"));
    assertEquals("", httpUtilities.valueFromBodyInput("bah"));
    assertEquals("bah", httpUtilities.valueFromBodyInput("foo", "<body>"));
    assertEquals("blah", httpUtilities.valueFromBodyInput("foo", "input"));
    assertEquals("", httpUtilities.valueFromBodyInput("foo", "not there"));

    handler.shutdown();
  }

  @Test public void testValuesFromBodyInput() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_httpPlugin);
    assertEquals(emptyList(), httpUtilities.valuesFromBodyInput("foo"));

    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();
    request.GET(handler.getURL());
    assertEquals(emptyList(), httpUtilities.valuesFromBodyInput("foo"));

    handler.setBody("<body><input name='foo'>foo</input></body>");
    request.GET(handler.getURL());
    assertEquals(emptyList(), httpUtilities.valuesFromBodyInput("foo"));

    // input tags should be empty. The content has no meaning
    handler.setBody("<body><input name='foo' value='bah'>foo</input>" +
                    "<input type='hidden' name='foo' value='blah'>foo</input></body>");
    request.GET(handler.getURL());
    assertEquals(asList("bah", "blah"),
                 httpUtilities.valuesFromBodyInput("foo"));
    assertEquals(emptyList(), httpUtilities.valuesFromBodyInput("bah"));
    assertEquals(asList("blah"),
                 httpUtilities.valuesFromBodyInput("foo", "bah"));
    assertEquals(emptyList(),
                 httpUtilities.valuesFromBodyInput("foo", "blah"));
    assertEquals(emptyList(),
                 httpUtilities.valuesFromBodyInput("foo", "not there"));

    handler.shutdown();
  }

  @Test public void testValueFromHiddenInput() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_httpPlugin);
    assertEquals("", httpUtilities.valueFromHiddenInput("foo"));

    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromHiddenInput("foo"));

    handler.setBody("<body><input type='hidden' name='foo'>foo</input></body>");
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromHiddenInput("foo"));

    // input tags should be empty. The content has no meaning
    handler.setBody("<body><input name='foo' value='blah'>foo</input>" +
                    "<input type='hidden' name='foo' value='bah'>foo</input></body>");
    request.GET(handler.getURL());
    assertEquals("bah", httpUtilities.valueFromHiddenInput("foo"));
    assertEquals("", httpUtilities.valueFromHiddenInput("bah"));
    assertEquals("bah", httpUtilities.valueFromHiddenInput("foo", "<body>"));
    assertEquals("", httpUtilities.valueFromHiddenInput("foo", "input"));
    assertEquals("", httpUtilities.valueFromHiddenInput("foo", "not there"));

    handler.shutdown();
  }

  @Test public void testValuesFromHiddenInput() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_httpPlugin);
    assertEquals(emptyList(), httpUtilities.valuesFromHiddenInput("foo"));

    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();
    request.GET(handler.getURL());
    assertEquals(emptyList(), httpUtilities.valuesFromHiddenInput("foo"));

    handler.setBody("<body><input type='hidden' name='foo'>foo</input></body>");
    request.GET(handler.getURL());
    assertEquals(emptyList(), httpUtilities.valuesFromHiddenInput("foo"));

    // input tags should be empty. The content has no meaning
    handler.setBody("<body><input type='hidden' name='foo' value='bah'>foo</input>" +
                    "<input type='hidden' name='foo' value='blah'>foo</input></body>");
    request.GET(handler.getURL());
    assertEquals(asList("bah", "blah"),
                 httpUtilities.valuesFromHiddenInput("foo"));
    assertEquals(emptyList(), httpUtilities.valuesFromHiddenInput("bah"));
    assertEquals(asList("blah"),
                 httpUtilities.valuesFromHiddenInput("foo", "bah"));
    assertEquals(emptyList(),
                 httpUtilities.valuesFromHiddenInput("foo", "blah"));
    assertEquals(emptyList(),
                 httpUtilities.valuesFromHiddenInput("foo", "not there"));

    handler.shutdown();
  }
}
