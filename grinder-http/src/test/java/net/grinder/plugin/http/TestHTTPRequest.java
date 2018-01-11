// Copyright (C) 2000 - 2013 Philip Aston
// Copyright (C) 2015 Marc Holden
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

import static net.grinder.testutility.AssertUtilities.assertContains;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Random;

import net.grinder.common.SSLContextFactory;
import net.grinder.engine.process.dcr.DCRContextImplementation;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.script.InvalidContextException;
import net.grinder.script.Statistics;
import net.grinder.script.Statistics.StatisticsForTest;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.Recorder;
import net.grinder.scriptengine.java.JavaScriptEngineService;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.util.InsecureSSLContextFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import HTTPClient.HTTPConnection.TimeAuthority;
import HTTPClient.HTTPResponse;
import HTTPClient.HttpURLConnection;
import HTTPClient.NVPair;
import HTTPClient.ParseException;


/**
 * Unit tests for {@link HTTPRequest}.
 *
 * @author Philip Aston
 */
public class TestHTTPRequest {
  private static final Random s_random = new Random();

  private final SSLContextFactory m_sslContextFactory =
      new InsecureSSLContextFactory();

  @Mock private PluginProcessContext m_pluginProcessContext;
  @Mock private ScriptContext m_scriptContext;
  @Mock private Statistics m_statistics;
  @Mock private TimeAuthority m_timeAuthority;
  @Mock private StatisticsForTest m_statisticsForTest;

  @Mock private Logger m_logger;
  @Captor private ArgumentCaptor<String> m_stringCaptor;

  private HTTPPlugin m_httpPlugin;
  private HTTPRequestHandler m_handler;

  @Before public void setUp() throws Exception {

    MockitoAnnotations.initMocks(this);

    final HTTPPluginThreadState threadState =
      new HTTPPluginThreadState(m_sslContextFactory,
                                null,
                                m_timeAuthority);

    when(m_scriptContext.getStatistics()).thenReturn(m_statistics);
    when(m_scriptContext.getLogger()).thenReturn(m_logger);

    m_httpPlugin = new HTTPPlugin(m_pluginProcessContext,
                                  m_scriptContext);

    when(m_pluginProcessContext.getPluginThreadListener(m_httpPlugin))
      .thenReturn(threadState);

    m_handler = new HTTPRequestHandler();
    m_handler.start();
  }

  @After public void tearDown() throws Exception {
    m_handler.shutdown();
  }

  @Test public void testTimeout() throws Exception {
    final HTTPPluginConnectionDefaults connectionDefaults =
      HTTPPluginConnectionDefaults.getConnectionDefaults();

    final int originalTimeout = connectionDefaults.getTimeout();
    final String originalProxyHost = connectionDefaults.getProxyHost();
    final int originalProxyPort = connectionDefaults.getProxyPort();

    try {
      connectionDefaults.setTimeout(1);

      try {
        final HTTPRequest request = new HTTPRequest();
        request.GET("http://idontexist.grinder.sf.net");
        fail("Expected TimeoutException");
      }
      catch (final TimeoutException e) {
      }

      try {
        connectionDefaults.setProxyServer("idontexist.grinder.sf.net", 8080);
        final HTTPRequest request2 = new HTTPRequest();
        request2.GET("http://idontexist.grinder.sf.net");
        fail("Expected TimeoutException");
      }
      catch (final TimeoutException e) {
      }
    }
    finally {
      connectionDefaults.setTimeout(originalTimeout);
      connectionDefaults.setProxyServer(originalProxyHost, originalProxyPort);
    }
  }

  @Test public void testSetUrl() throws Exception {
    final HTTPRequest httpRequest = new HTTPRequest();

    assertNull(httpRequest.getUrl());

    try {
      httpRequest.setUrl("foo/bah");
      fail("Expected URLException");
    }
    catch (final URLException e) {
    }

    assertNull(httpRequest.getUrl());

    try {
      httpRequest.setUrl("http://foo:bah/blah");
      fail("Expected ParseException");
    }
    catch (final ParseException e) {
    }

    assertNull(httpRequest.getUrl());

    httpRequest.setUrl("http://foo/bah");

    assertEquals("http://foo/bah", httpRequest.getUrl());
  }

  @Test public void testSetHeaders() {
    final HTTPRequest httpRequest = new HTTPRequest();

    assertEquals(0, httpRequest.getHeaders().length);

    final NVPair[] newHeaders = new NVPair[] {
      new NVPair("name", "value"),
      new NVPair("another name", "another value"),
    };

    httpRequest.setHeaders(newHeaders);
    assertArrayEquals(newHeaders, httpRequest.getHeaders());
  }

  @Test public void testDELETE() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    try {
      request.DELETE();
      fail("Expected URLException");
    }
    catch (final URLException e) {
    }

    try {
      request.DELETE("/partial");
      fail("Expected URLException");
    }
    catch (final URLException e) {
    }

    final HTTPResponse response = request.DELETE(m_handler.getURL());
    assertEquals(200, response.getStatusCode());
    assertEquals("DELETE / HTTP/1.1", m_handler.getRequestFirstHeader());

    request.setUrl(m_handler.getURL());
    final HTTPResponse response2 = request.DELETE("/foo");
    assertEquals(200, response2.getStatusCode());
    assertEquals("DELETE /foo HTTP/1.1", m_handler.getRequestFirstHeader());

    final HTTPResponse response3 = request.DELETE();
    assertEquals(200, response3.getStatusCode());
    assertEquals("DELETE / HTTP/1.1", m_handler.getRequestFirstHeader());

    final NVPair[] headers = {
      new NVPair("x", "212"),
      new NVPair("y", "321"),
    };

    final HTTPResponse response4 = request.DELETE("/", headers);
    assertEquals(200, response4.getStatusCode());
    assertEquals("DELETE / HTTP/1.1", m_handler.getRequestFirstHeader());
    m_handler.assertRequestContainsHeader("x: 212");
    m_handler.assertRequestContainsHeader("y: 321");

    final NVPair[] headers2 = {
      new NVPair("x", "1"),
      new NVPair("y", "2"),
      new NVPair("z", "3"),
    };

    request.setHeaders(headers2);

    request.DELETE("/", headers);
    m_handler.assertRequestContainsHeader("x: 212");
    m_handler.assertRequestContainsHeader("y: 321");
    m_handler.assertRequestContainsHeader("z: 3");
    m_handler.assertRequestDoesNotContainHeader("y: 2");
  }

  @Test public void testGET() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    try {
      request.GET();
      fail("Expected URLException");
    }
    catch (final URLException e) {
    }

    try {
      request.GET("#partial");
      fail("Expected URLException");
    }
    catch (final URLException e) {
    }

    final HTTPResponse response = request.GET(m_handler.getURL());
    assertEquals(200, response.getStatusCode());
    assertEquals("GET / HTTP/1.1", m_handler.getRequestFirstHeader());

    request.setUrl(m_handler.getURL());
    final HTTPResponse response2 = request.GET("/foo");
    assertEquals(200,  response2.getStatusCode());
    assertEquals("GET /foo HTTP/1.1", m_handler.getRequestFirstHeader());

    final HTTPResponse response3 = request.GET();
    assertEquals(200, response3.getStatusCode());
    assertEquals("GET / HTTP/1.1", m_handler.getRequestFirstHeader());

    final NVPair[] parameters4 = {
      new NVPair("some", "header"),
      new NVPair("y", "321"),
    };

    final HTTPResponse response4 = request.GET("/lah/de/dah", parameters4);
    assertEquals(200, response4.getStatusCode());
    assertEquals("GET /lah/de/dah?some=header&y=321 HTTP/1.1",
                 m_handler.getRequestFirstHeader());

    final NVPair[] parameters5 = {
      new NVPair("another", "header"),
      new NVPair("y", "331"),
    };

    request.setUrl(m_handler.getURL() + "/lah/");
    final HTTPResponse response5 = request.GET(parameters5);
    assertEquals(200, response5.getStatusCode());
    assertEquals("GET /lah/?another=header&y=331 HTTP/1.1",
                 m_handler.getRequestFirstHeader());

    final NVPair[] headers = {
      new NVPair("key", "value"),
    };

    request.setHeaders(headers);
    final HTTPResponse response6 = request.GET();
    assertEquals(200, response6.getStatusCode());
    assertEquals("GET /lah/ HTTP/1.1", m_handler.getRequestFirstHeader());
    m_handler.assertRequestContainsHeader("key: value");

    final NVPair[] headers2 = {
      new NVPair("key", "anotherValue"),
      new NVPair("x", "1"),
      new NVPair("y", "2"),
    };

    request.GET("/", null, headers2);
    m_handler.assertRequestContainsHeader("x: 1");
    m_handler.assertRequestContainsHeader("y: 2");
    m_handler.assertRequestContainsHeader("key: anotherValue");
    m_handler.assertRequestDoesNotContainHeader("key: value");

    final HTTPResponse response7 = request.GET("//multipleSlashes");
    assertEquals(200, response7.getStatusCode());
    assertEquals("GET //multipleSlashes HTTP/1.1",
                 m_handler.getRequestFirstHeader());

    request.GET("/#a fragment", null, new NVPair[] {});
    assertEquals("GET / HTTP/1.1", m_handler.getRequestFirstHeader());
    m_handler.assertRequestDoesNotContainHeader("key: anotherValue");
    m_handler.assertRequestContainsHeader("key: value");
  }

  @Test public void testHEAD() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    try {
      request.HEAD();
      fail("Expected URLException");
    }
    catch (final URLException e) {
    }

    try {
      request.HEAD("?partial");
      fail("Expected URLException");
    }
    catch (final URLException e) {
    }

    final HTTPResponse response = request.HEAD(m_handler.getURL());
    assertEquals(200, response.getStatusCode());
    assertEquals("HEAD / HTTP/1.1", m_handler.getRequestFirstHeader());

    request.setUrl(m_handler.getURL());
    final HTTPResponse response2 = request.HEAD("/foo");
    assertEquals(200, response2.getStatusCode());
    assertEquals("HEAD /foo HTTP/1.1", m_handler.getRequestFirstHeader());

    final HTTPResponse response3 = request.HEAD();
    assertEquals(200, response3.getStatusCode());
    assertEquals("HEAD / HTTP/1.1", m_handler.getRequestFirstHeader());

    final NVPair[] parameters4 = {
      new NVPair("some", "header"),
      new NVPair("y", "321"),
    };

    final HTTPResponse response4 = request.HEAD("/lah/de/dah", parameters4);
    assertEquals(200, response4.getStatusCode());
    assertEquals("HEAD /lah/de/dah?some=header&y=321 HTTP/1.1",
                 m_handler.getRequestFirstHeader());

    final NVPair[] parameters5 = {
      new NVPair("another", "header"),
      new NVPair("y", "331"),
    };

    request.setUrl(m_handler.getURL() + "/lah/");
    final HTTPResponse response5 = request.HEAD(parameters5);
    assertEquals(200, response5.getStatusCode());
    assertEquals("HEAD /lah/?another=header&y=331 HTTP/1.1",
                 m_handler.getRequestFirstHeader());

    final NVPair[] headers6 = {
      new NVPair("key", "value"),
    };

    request.setHeaders(headers6);
    final HTTPResponse response6 = request.HEAD();
    assertEquals(200, response6.getStatusCode());
    assertEquals("HEAD /lah/ HTTP/1.1", m_handler.getRequestFirstHeader());
    m_handler.assertRequestContainsHeader("key: value");
  }

  @Test public void testOPTIONS() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    try {
      request.OPTIONS();
      fail("Expected URLException");
    }
    catch (final URLException e) {
    }

    try {
      request.OPTIONS("///::partial");
      fail("Expected URLException");
    }
    catch (final URLException e) {
    }

    final HTTPResponse response = request.OPTIONS(m_handler.getURL());
    assertEquals(200, response.getStatusCode());
    assertEquals("OPTIONS / HTTP/1.1", m_handler.getRequestFirstHeader());

    request.setUrl(m_handler.getURL());
    final HTTPResponse response2 = request.OPTIONS("/foo");
    assertEquals(200, response2.getStatusCode());
    assertEquals("OPTIONS /foo HTTP/1.1", m_handler.getRequestFirstHeader());

    final HTTPResponse response3 = request.OPTIONS();
    assertEquals(200, response3.getStatusCode());
    assertEquals("OPTIONS / HTTP/1.1", m_handler.getRequestFirstHeader());

    final byte[] data4 = randomBytes(10);

    final HTTPResponse response4 = request.OPTIONS("/blah", data4);
    assertEquals(200, response4.getStatusCode());
    assertEquals("OPTIONS /blah HTTP/1.1", m_handler.getRequestFirstHeader());
    assertArrayEquals(data4, m_handler.getLastRequestBody());

    final byte[] data5 = randomBytes(100);

    request.setUrl(m_handler.getURL() + "/lah/");
    request.setData(data5);
    final HTTPResponse response5 = request.OPTIONS("/blah");
    assertEquals(200, response5.getStatusCode());
    assertEquals("OPTIONS /blah HTTP/1.1", m_handler.getRequestFirstHeader());
    assertArrayEquals(data5, m_handler.getLastRequestBody());

    final NVPair[] headers6 = {
      new NVPair("key", "value"),
    };

    request.setHeaders(headers6);
    final HTTPResponse response6 = request.OPTIONS();
    assertEquals(200, response6.getStatusCode());
    assertEquals("OPTIONS /lah/ HTTP/1.1", m_handler.getRequestFirstHeader());
    m_handler.assertRequestContainsHeader("key: value");

    final byte[] data6 = randomBytes(10000);

    final HTTPResponse response7 =
      request.OPTIONS("/blah", new ByteArrayInputStream(data6));
    assertEquals(200, response7.getStatusCode());
    assertEquals("OPTIONS /blah HTTP/1.1", m_handler.getRequestFirstHeader());
    assertArrayEquals(data6, m_handler.getLastRequestBody());

    final byte[] data7 = randomBytes(10000);

    final HTTPResponse response8 =
      request.OPTIONS("/blah", new ByteArrayInputStream(data7), headers6);
    assertEquals(200, response8.getStatusCode());
    assertEquals("OPTIONS /blah HTTP/1.1", m_handler.getRequestFirstHeader());
    assertArrayEquals(data7, m_handler.getLastRequestBody());
    m_handler.assertRequestContainsHeader("key: value");
  }

  @Test public void testPOST() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    try {
      request.POST();
      fail("Expected URLException");
    }
    catch (final URLException e) {
    }

    try {
      request.POST("#:/partial");
      fail("Expected URLException");
    }
    catch (final URLException e) {
    }

    final HTTPResponse response = request.POST(m_handler.getURL());
    assertEquals(200, response.getStatusCode());
    assertEquals("POST / HTTP/1.1", m_handler.getRequestFirstHeader());

    request.setUrl(m_handler.getURL());
    final HTTPResponse response2 = request.POST("/foo");
    assertEquals(200, response2.getStatusCode());
    assertEquals("POST /foo HTTP/1.1", m_handler.getRequestFirstHeader());

    final HTTPResponse response3 = request.POST();
    assertEquals(200, response3.getStatusCode());
    assertEquals("POST / HTTP/1.1", m_handler.getRequestFirstHeader());

    final byte[] data4 = randomBytes(10);

    final HTTPResponse response4 = request.POST("/blah", data4);
    assertEquals(200, response4.getStatusCode());
    assertEquals("POST /blah HTTP/1.1", m_handler.getRequestFirstHeader());
    assertArrayEquals(data4, m_handler.getLastRequestBody());

    final byte[] data5 = randomBytes(100);

    request.setUrl(m_handler.getURL() + "/lah/");
    request.setData(data5);
    final HTTPResponse response5 = request.POST("/blah");
    assertEquals(200, response5.getStatusCode());
    assertEquals("POST /blah HTTP/1.1", m_handler.getRequestFirstHeader());
    assertArrayEquals(data5, m_handler.getLastRequestBody());

    final NVPair[] headers6 = {
      new NVPair("key", "value"),
    };

    request.setHeaders(headers6);
    final HTTPResponse response6 = request.POST();
    assertEquals(200, response6.getStatusCode());
    assertEquals("POST /lah/ HTTP/1.1", m_handler.getRequestFirstHeader());
    m_handler.assertRequestContainsHeader("key: value");

    final NVPair[] formData7 = {
      new NVPair("Vessel", "Grace of Lefkas"),
    };

    final HTTPResponse response7 = request.POST("/foo?abc=def", formData7);
    assertEquals(200, response7.getStatusCode());
    assertEquals("POST /foo?abc=def HTTP/1.1",
                 m_handler.getRequestFirstHeader());
    m_handler.assertRequestContainsHeader("key: value");
    final String bodyText7 = new String(m_handler.getLastRequestBody());
    assertTrue(bodyText7.indexOf("Vessel=Grace+of+Lefkas") > -1);

    final NVPair[] formData8 = {
      new NVPair("LOA", "12.3m"),
      new NVPair("Draught", "1.7"),
    };

    request.setFormData(formData8);

    final HTTPResponse response8 = request.POST();
    assertEquals(200, response8.getStatusCode());
    assertEquals("POST /lah/ HTTP/1.1", m_handler.getRequestFirstHeader());
    m_handler.assertRequestContainsHeader("key: value");
    assertArrayEquals(data5, m_handler.getLastRequestBody());

    request.setData(null);

    final HTTPResponse response9 = request.POST();
    assertEquals(200, response9.getStatusCode());
    assertEquals("POST /lah/ HTTP/1.1", m_handler.getRequestFirstHeader());
    m_handler.assertRequestContainsHeader("key: value");
    final String bodyText9 = new String(m_handler.getLastRequestBody());
    assertTrue(bodyText9.indexOf("LOA=12.3m") > -1);

    final HTTPResponse response10 = request.POST(formData7);
    assertEquals(200, response10.getStatusCode());
    assertEquals("POST /lah/ HTTP/1.1", m_handler.getRequestFirstHeader());
    final String bodyText10 = new String(m_handler.getLastRequestBody());
    assertTrue(bodyText10.indexOf("Vessel=Grace+of+Lefkas") > -1);

    final byte[] data6 = randomBytes(10000);

    final HTTPResponse response11 = request.POST("/bhxhh",
                                                 new ByteArrayInputStream(data6));
    assertEquals(200, response11.getStatusCode());
    assertEquals("POST /bhxhh HTTP/1.1", m_handler.getRequestFirstHeader());
    assertArrayEquals(data6, m_handler.getLastRequestBody());

    final byte[] data7 = randomBytes(10000);

    final HTTPResponse response12 =
      request.POST("/bhxhh", new ByteArrayInputStream(data7), headers6);
    assertEquals(200, response12.getStatusCode());
    assertEquals("POST /bhxhh HTTP/1.1", m_handler.getRequestFirstHeader());
    assertArrayEquals(data7, m_handler.getLastRequestBody());
    m_handler.assertRequestContainsHeader("key: value");

    headers6[0] = new NVPair("Content-Length", Integer.toString(data7.length));

    final HTTPResponse response13 =
      request.POST("/bhxhh", new ByteArrayInputStream(data7), headers6);
    assertEquals(200, response13.getStatusCode());
    assertEquals("POST /bhxhh HTTP/1.1", m_handler.getRequestFirstHeader());
    assertArrayEquals(data7, m_handler.getLastRequestBody());
    m_handler.assertRequestContainsHeader("Content-length: " + data7.length);
  }

  @Test public void testPOSTMultiPart() throws Exception {
    final HTTPRequest request = new HTTPRequest();
    request.setUrl(m_handler.getURL());

    final NVPair[] formData = {
        new NVPair("Vessel", "Grace of Lefkas"),
      };

    request.setHeaders(new NVPair[] { new NVPair("key", "value"), });

    final NVPair[] headers2 = {
        new NVPair("Content-Type", "multipart/form-data; boundary=---------------------------7db2b32b130706"),
      };

    final HTTPResponse response =
      request.POST("/foo?abc=def", formData, headers2, true);
    assertEquals(200, response.getStatusCode());
    assertEquals("POST /foo?abc=def HTTP/1.1",
                 m_handler.getRequestFirstHeader());

    m_handler.assertRequestContainsHeader("key: value");

    final String bodyText = new String(m_handler.getLastRequestBody());
    assertTrue(bodyText.indexOf("Grace of Lefkas") > -1);
  }

  @Test public void testPUT() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    try {
      request.PUT();
      fail("Expected URLException");
    }
    catch (final URLException e) {
    }

    try {
      request.PUT("?:/partial");
      fail("Expected URLException");
    }
    catch (final URLException e) {
    }

    final HTTPResponse response = request.PUT(m_handler.getURL());
    assertEquals(200, response.getStatusCode());
    assertEquals("PUT / HTTP/1.1", m_handler.getRequestFirstHeader());

    request.setUrl(m_handler.getURL());
    final HTTPResponse response2 = request.PUT("/foo");
    assertEquals(200, response2.getStatusCode());
    assertEquals("PUT /foo HTTP/1.1", m_handler.getRequestFirstHeader());

    final HTTPResponse response3 = request.PUT();
    assertEquals(200, response3.getStatusCode());
    assertEquals("PUT / HTTP/1.1", m_handler.getRequestFirstHeader());

    final byte[] data4 = randomBytes(10);

    final HTTPResponse response4 = request.PUT("/blah", data4);
    assertEquals(200, response4.getStatusCode());
    assertEquals("PUT /blah HTTP/1.1", m_handler.getRequestFirstHeader());
    assertArrayEquals(data4, m_handler.getLastRequestBody());

    final byte[] data5 = randomBytes(100);

    request.setUrl(m_handler.getURL() + "/lah/");
    request.setData(data5);
    final HTTPResponse response5 = request.PUT("/blah");
    assertEquals(200, response5.getStatusCode());
    assertEquals("PUT /blah HTTP/1.1", m_handler.getRequestFirstHeader());
    assertArrayEquals(data5, m_handler.getLastRequestBody());

    final NVPair[] headers6 = {
      new NVPair("key", "value"),
    };

    request.setHeaders(headers6);
    final HTTPResponse response6 = request.PUT();
    assertEquals(200, response6.getStatusCode());
    assertEquals("PUT /lah/ HTTP/1.1", m_handler.getRequestFirstHeader());
    m_handler.assertRequestContainsHeader("key: value");

    final byte[] data7 = randomBytes(10000);

    final HTTPResponse response7 = request.PUT("/bhhh",
                                               new ByteArrayInputStream(data7));
    assertEquals(200, response7.getStatusCode());
    assertEquals("PUT /bhhh HTTP/1.1", m_handler.getRequestFirstHeader());
    assertArrayEquals(data7, m_handler.getLastRequestBody());
  }

  @Test public void testPATCH() throws Exception {
      final HTTPRequest request = new HTTPRequest();

      try {
        request.PATCH();
        fail("Expected URLException");
      }
      catch (final URLException e) {
      }

      try {
        request.PATCH("?:/partial");
        fail("Expected URLException");
      }
      catch (final URLException e) {
      }

      final HTTPResponse response = request.PATCH(m_handler.getURL());
      assertEquals(200, response.getStatusCode());
      assertEquals("PATCH / HTTP/1.1", m_handler.getRequestFirstHeader());

      request.setUrl(m_handler.getURL());
      final HTTPResponse response2 = request.PATCH("/foo");
      assertEquals(200, response2.getStatusCode());
      assertEquals("PATCH /foo HTTP/1.1", m_handler.getRequestFirstHeader());

      final HTTPResponse response3 = request.PATCH();
      assertEquals(200, response3.getStatusCode());
      assertEquals("PATCH / HTTP/1.1", m_handler.getRequestFirstHeader());

      final byte[] data4 = randomBytes(10);

      final HTTPResponse response4 = request.PATCH("/blah", data4);
      assertEquals(200, response4.getStatusCode());
      assertEquals("PATCH /blah HTTP/1.1", m_handler.getRequestFirstHeader());
      assertArrayEquals(data4, m_handler.getLastRequestBody());

      final byte[] data5 = randomBytes(100);

      request.setUrl(m_handler.getURL() + "/lah/");
      request.setData(data5);
      final HTTPResponse response5 = request.PATCH("/blah");
      assertEquals(200, response5.getStatusCode());
      assertEquals("PATCH /blah HTTP/1.1", m_handler.getRequestFirstHeader());
      assertArrayEquals(data5, m_handler.getLastRequestBody());

      final NVPair[] headers6 = {
        new NVPair("key", "value"),
      };

      request.setHeaders(headers6);
      final HTTPResponse response6 = request.PATCH();
      assertEquals(200, response6.getStatusCode());
      assertEquals("PATCH /lah/ HTTP/1.1", m_handler.getRequestFirstHeader());
      m_handler.assertRequestContainsHeader("key: value");

      final byte[] data7 = randomBytes(10000);

      final HTTPResponse response7 = request.PATCH("/bhhh",
                                                 new ByteArrayInputStream(data7));
      assertEquals(200, response7.getStatusCode());
      assertEquals("PATCH /bhhh HTTP/1.1", m_handler.getRequestFirstHeader());
      assertArrayEquals(data7, m_handler.getLastRequestBody());
    }

  @Test public void testTRACE() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    try {
      request.TRACE();
      fail("Expected URLException");
    }
    catch (final URLException e) {
    }

    try {
      request.TRACE("??partial");
      fail("Expected URLException");
    }
    catch (final URLException e) {
    }

    final HTTPResponse response = request.TRACE(m_handler.getURL());
    assertEquals(200, response.getStatusCode());
    assertEquals("TRACE / HTTP/1.1", m_handler.getRequestFirstHeader());

    request.setUrl(m_handler.getURL());
    final HTTPResponse response2 = request.TRACE("/foo");
    assertEquals(200, response2.getStatusCode());
    assertEquals("TRACE /foo HTTP/1.1", m_handler.getRequestFirstHeader());

    final HTTPResponse response3 = request.TRACE();
    assertEquals(200, response3.getStatusCode());
    assertEquals("TRACE / HTTP/1.1", m_handler.getRequestFirstHeader());

    final NVPair[] headers4 = {
      new NVPair("key", "value"),
    };

    request.setUrl(m_handler.getURL() + "/lah/");
    request.setHeaders(headers4);
    final HTTPResponse response4 = request.TRACE();
    assertEquals(200, response4.getStatusCode());
    assertEquals("TRACE /lah/ HTTP/1.1", m_handler.getRequestFirstHeader());
    m_handler.assertRequestContainsHeader("key: value");
  }

  @Test public void testToString() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    assertEquals("<Undefined URL>\n", request.toString());

    request.setUrl("http://grinder.sf.net/");
    assertEquals("http://grinder.sf.net/\n", request.toString());

    request.setHeaders(new NVPair[] {
                         new NVPair("home", "end"),
                         new NVPair("pause", "insert"),
                       });

    assertEquals("http://grinder.sf.net/\nhome: end\npause: insert\n",
                 request.toString());
  }

  @Test public void testSetDataFromFile() throws Exception {

    final File file = File.createTempFile("testing", "123");
    file.deleteOnExit();

    final OutputStream out = new FileOutputStream(file);

    final byte[] data5 = randomBytes(10);

    out.write(data5);
    out.close();

    final HTTPRequest request = new HTTPRequest();
    request.setDataFromFile(file.getPath());

    assertArrayEquals(data5, request.getData());

  }

  @Test public void testResponseProcessing() throws Exception {
    final HTTPRequest request = new HTTPRequest();
    request.GET(m_handler.getURL());

    verify(m_logger).info(m_stringCaptor.capture());

    final String message = m_stringCaptor.getValue();
    assertTrue(message.indexOf("200") >= 0);
    assertEquals(-1, message.indexOf("Redirect"));

    verify(m_statistics).isTestInProgress();
  }

  @Test public void testRedirectResponseProcessing() throws Exception {

    final int[] redirectCodes = {
        302,
        HttpURLConnection.HTTP_MOVED_PERM,
        HttpURLConnection.HTTP_MOVED_TEMP,
        307,
    };

    for (final int redirectCode : redirectCodes) {
      final HTTPRequestHandler handler = new HTTPRequestHandler() {
        @Override
        protected void writeHeaders(final StringBuffer response) {
          response.append("HTTP/1.0 ");
          response.append(redirectCode);
          response.append(" Moved Temporarily\r\n"); // whatever
        }
      };
      handler.start();

      when(m_statistics.isTestInProgress()).thenReturn(true);
      when(m_statistics.getForCurrentTest()).thenReturn(m_statisticsForTest);

      final HTTPRequest request = new HTTPRequest();
      final HTTPResponse response = request.GET(handler.getURL());
      assertNotNull(response);

      verify(m_logger, atLeastOnce()).info(m_stringCaptor.capture());
      final String message = m_stringCaptor.getValue();

      assertTrue(message.indexOf(Integer.toString(redirectCode)) >= 0);
      assertTrue(message.indexOf("Redirect") >= 0);

      handler.shutdown();
    }
  }

  @Test public void testBadRequestResponseProcessing() throws Exception {
    final HTTPRequestHandler handler = new HTTPRequestHandler() {
      @Override
      protected void writeHeaders(final StringBuffer response) {
        response.append("HTTP/1.0 400 Bad Request\r\n");
      }
    };

    handler.start();

    when(m_statistics.isTestInProgress()).thenReturn(true);
    when(m_statistics.getForCurrentTest()).thenReturn(m_statisticsForTest);

    final HTTPRequest request = new HTTPRequest();
    final HTTPResponse response = request.GET(handler.getURL());
    assertNotNull(response);

    verify(m_logger).info(m_stringCaptor.capture());
    final String message = m_stringCaptor.getValue();
    assertTrue(message.indexOf("400") >= 0);

    handler.shutdown();
  }

  @Test public void testSubclassProcessResponse() throws Exception {
    final Object[] resultHolder = new Object[1];

    final HTTPRequest request = new HTTPRequest() {
        @Override
        public void processResponse(final HTTPResponse response) {
          resultHolder[0] = response;
        }
      };

    final HTTPResponse response = request.GET(m_handler.getURL());

    assertSame(response, resultHolder[0]);
  }

  @Test public void testConnectionTimingsAndStatistics() throws Exception {

    final ListTimeAuthority timeAuthority =
      new ListTimeAuthority(new long[] {
          100, // start time
          101, // start time (internal to HTTPResponse - i.e. post redirect)
          123, // DNS time
          200, // connection time
          219, // time to first byte
      });

    final HTTPPluginThreadState threadState =
      new HTTPPluginThreadState(m_sslContextFactory,
                                null,
                                timeAuthority);

    when(m_pluginProcessContext.getPluginThreadListener(m_httpPlugin))
      .thenReturn(threadState);

    when(m_statistics.isTestInProgress()).thenReturn(true);
    when(m_statistics.getForCurrentTest()).thenReturn(m_statisticsForTest);

    final HTTPRequest request = new HTTPRequest();
    final String bodyText = "Your heart's gone the colour of Coca Cola\n";
    m_handler.setBody(bodyText);
    final HTTPResponse response = request.GET(m_handler.getURL());
    assertEquals(200, response.getStatusCode());
    assertEquals("GET / HTTP/1.1", m_handler.getRequestFirstHeader());

    verify(m_statisticsForTest)
      .addLong(StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_LENGTH_KEY,
               bodyText.length());
    verify(m_statisticsForTest)
      .setLong(StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_STATUS_KEY, 200);
    verify(m_statisticsForTest)
      .addLong(StatisticsIndexMap.HTTP_PLUGIN_DNS_TIME_KEY, 22);
    verify(m_statisticsForTest)
      .addLong(StatisticsIndexMap.HTTP_PLUGIN_CONNECT_TIME_KEY, 99);
    verify(m_statisticsForTest)
      .addLong(StatisticsIndexMap.HTTP_PLUGIN_FIRST_BYTE_TIME_KEY, 119);
    verify(m_statisticsForTest)
      .addLong(StatisticsIndexMap.HTTP_PLUGIN_CONNECTIONS_ESTABLISHED, 1);

    try {
      timeAuthority.getTimeInMilliseconds();
      fail("Not all times used");
    }
    catch (final ArrayIndexOutOfBoundsException e) {
    }

    assertTrue(response.getInputStream() instanceof ByteArrayInputStream);
    assertEquals(bodyText, response.getText());
  }

  @Test public void testConnectionTimingsAndStatisticsInvalidTimes()
      throws Exception {

    final ListTimeAuthority timeAuthority =
        new ListTimeAuthority(new long[] {
            400, // start time
            400, // start time (internal to HTTPResponse - i.e. post redirect)
            0, // DNS time
            0, // connection time
            419, // time to first byte
        });

    final HTTPPluginThreadState threadState =
      new HTTPPluginThreadState(m_sslContextFactory,
                                null,
                                timeAuthority);

    when(m_pluginProcessContext.getPluginThreadListener(m_httpPlugin))
      .thenReturn(threadState);

    when(m_statistics.isTestInProgress()).thenReturn(true);
    when(m_statistics.getForCurrentTest()).thenReturn(m_statisticsForTest);

    m_handler.setBody(null);

    final HTTPRequest request = new HTTPRequest();
    final HTTPResponse response = request.GET(m_handler.getURL());
    assertEquals(200, response.getStatusCode());
    assertEquals("GET / HTTP/1.1", m_handler.getRequestFirstHeader());

    verify(m_statisticsForTest)
      .addLong(StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_LENGTH_KEY, 0);
    verify(m_statisticsForTest)
      .setLong(StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_STATUS_KEY, 200);
    verify(m_statisticsForTest)
      .addLong(StatisticsIndexMap.HTTP_PLUGIN_DNS_TIME_KEY, 0);
    verify(m_statisticsForTest)
      .addLong(StatisticsIndexMap.HTTP_PLUGIN_CONNECT_TIME_KEY, 0);
    verify(m_statisticsForTest)
      .addLong(StatisticsIndexMap.HTTP_PLUGIN_FIRST_BYTE_TIME_KEY, 19);
    verify(m_statisticsForTest)
      .addLong(StatisticsIndexMap.HTTP_PLUGIN_CONNECTIONS_ESTABLISHED, 1);

    try {
      timeAuthority.getTimeInMilliseconds();
      fail("Not all times used");
    }
    catch (final ArrayIndexOutOfBoundsException e) {
    }

    assertTrue(response.getInputStream() instanceof ByteArrayInputStream);
    assertEquals("", response.getText());
  }

  @Test public void testSetReadResponseBody() throws Exception {

    final ListTimeAuthority timeAuthority =
      new ListTimeAuthority(new long[] {
          100, // start time
          110, // start time (internal to HTTPResponse - i.e. post redirect)
          123, // DNS time
          200, // connection time
          219, // time to first byte
      });

    final HTTPPluginThreadState threadState =
      new HTTPPluginThreadState(m_sslContextFactory,
                                null,
                                timeAuthority);

    when(m_pluginProcessContext.getPluginThreadListener(m_httpPlugin))
      .thenReturn(threadState);

    when(m_statistics.isTestInProgress()).thenReturn(true);
    when(m_statistics.getForCurrentTest()).thenReturn(m_statisticsForTest);

    final HTTPRequest request = new HTTPRequest();

    assertTrue(request.getReadResponseBody());
    request.setReadResponseBody(false);
    assertFalse(request.getReadResponseBody());
    request.setReadResponseBody(true);
    assertTrue(request.getReadResponseBody());
    request.setReadResponseBody(false);
    assertFalse(request.getReadResponseBody());

    final String bodyText =
      "Your heart's gone the colour of a dust\nbin\n liner";
    m_handler.setBody(bodyText);

    final HTTPResponse response = request.GET(m_handler.getURL());
    assertEquals(200, response.getStatusCode());
    assertEquals("GET / HTTP/1.1", m_handler.getRequestFirstHeader());

    verify(m_statisticsForTest)
      .addLong(StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_LENGTH_KEY, 0);
    verify(m_statisticsForTest)
      .setLong(StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_STATUS_KEY, 200);
    verify(m_statisticsForTest)
    .addLong(StatisticsIndexMap.HTTP_PLUGIN_DNS_TIME_KEY, 13);
    verify(m_statisticsForTest)
      .addLong(StatisticsIndexMap.HTTP_PLUGIN_CONNECT_TIME_KEY, 90);
    verify(m_statisticsForTest)
      .addLong(StatisticsIndexMap.HTTP_PLUGIN_FIRST_BYTE_TIME_KEY, 119);
    verify(m_statisticsForTest)
      .addLong(StatisticsIndexMap.HTTP_PLUGIN_CONNECTIONS_ESTABLISHED, 1);

    try {
      timeAuthority.getTimeInMilliseconds();
      fail("Not all times used");
    }
    catch (final ArrayIndexOutOfBoundsException e) {
    }
  }

  @Test public void testWithBadStatistics() throws Exception {

    when(m_statistics.isTestInProgress()).thenReturn(true);
    when(m_statistics.getForCurrentTest()).thenReturn(m_statisticsForTest);

    final Exception exception = new InvalidContextException("bah");

    doThrow(exception)
    .when(m_statisticsForTest).addLong(isA(String.class), isA(Long.class));

    final HTTPRequest request = new HTTPRequest();

    try {
      request.GET(m_handler.getURL());
      fail("Expected PluginException");
    }
    catch (final PluginException e) {
      assertSame(exception, e.getCause());
    }
  }

  @Test public void testConnectionClose() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    final HTTPResponse response = request.GET(m_handler.getURL());
    assertEquals(200, response.getStatusCode());
    assertEquals("GET / HTTP/1.1", m_handler.getRequestFirstHeader());

    HTTPPluginControl.getThreadConnection(m_handler.getURL()).close();

    final HTTPResponse response2 = request.GET(m_handler.getURL());
    assertEquals(200, response2.getStatusCode());
    assertEquals("GET / HTTP/1.1", m_handler.getRequestFirstHeader());
  }

  @Test public void testDCRInstrumentation() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    final Instrumenter instrumenter =
      new JavaScriptEngineService(DCRContextImplementation.create(null))
      .createInstrumenters().get(0);

    final Recorder recorder = mock(Recorder.class);

    final net.grinder.common.Test test = mock(net.grinder.common.Test.class);

    instrumenter.instrument(test, recorder, request,
                            HTTPRequest.getHttpMethodFilter());
    verifyNoMoreInteractions(recorder);

    try {
      request.GET();
      fail("Expected URLException");
    }
    catch (final URLException e) {
    }

    // GET() delegates to two more general versions.
    verify(recorder, times(3)).start();
    verify(recorder, times(3)).end(false);
    verifyNoMoreInteractions(recorder);

    try {
      request.GET("#partial");
      fail("Expected URLException");
    }
    catch (final URLException e) {
    }

    // GET(String) delegates to one more general version.
    verify(recorder, times(5)).start();
    verify(recorder, times(5)).end(false);
    verifyNoMoreInteractions(recorder);

    final HTTPResponse response = request.GET(m_handler.getURL());
    assertEquals(200, response.getStatusCode());
    assertEquals("GET / HTTP/1.1", m_handler.getRequestFirstHeader());

    verify(recorder, times(7)).start();
    verify(recorder, times(2)).end(true);
    verifyNoMoreInteractions(recorder);
  }

  @Test public void testReadTimeout() throws Exception {

    final HTTPRequestHandler httpServer = new HTTPRequestHandler();
    httpServer.setResponseDelay(100);
    httpServer.start();

    final HTTPPluginConnectionDefaults connectionDefaults =
      HTTPPluginConnectionDefaults.getConnectionDefaults();

    final int originalTimeout = connectionDefaults.getTimeout();

    try {
      connectionDefaults.setTimeout(1);

      try {
        new HTTPRequest().GET(httpServer.getURL());
        fail("Expected TimeoutException");
      }
      catch (final TimeoutException e) {
      }

      // Need another HTTPRequestHandler - the first will have closed its
      // socket.
      final HTTPRequestHandler httpServer2 = new HTTPRequestHandler();
      httpServer2.setResponseDelay(100);
      httpServer2.start();

      final HTTPPluginConnection connection =
        HTTPPluginControl.getThreadConnection(httpServer2.getURL());

      connection.setTimeout(0);
      final HTTPResponse response = new HTTPRequest().GET(httpServer2.getURL());
      assertEquals("", response.getText());

      connection.setTimeout(1);

      try {
        new HTTPRequest().GET(httpServer2.getURL());
        fail("Expected TimeoutException");
      }
      catch (final TimeoutException e) {
      }

      connection.close();
    }
    finally {
      connectionDefaults.setTimeout(originalTimeout);
    }
  }

  @Test public void testNullHeaders() throws Exception {
    try {
      new HTTPRequest().GET(m_handler.getURL(), null, null);
    }
    catch (final NullPointerException e) {
      assertContains(e.getMessage(), "headers");
    }
  }

  @Test public void testNullHeader() throws Exception {

    final NVPair[] headers = { new NVPair("a", "b"),
                               null };

    try {
      new HTTPRequest().GET(m_handler.getURL(), null, headers);
    }
    catch (final NullPointerException e) {
      assertContains(e.getMessage(), "headers[1]");
    }
  }

  @Test public void testNullHeaderName() throws Exception {

    final NVPair[] headers = { new NVPair("a", "b"),
                               new NVPair(null, "123") };

    try {
      new HTTPRequest().GET(m_handler.getURL(), null, headers);
    }
    catch (final NullPointerException e) {
      assertContains(e.getMessage(), "headers[1].getName()");
    }
  }

  private static byte[] randomBytes(final int max) {
    final byte[] result = new byte[s_random.nextInt(max)];
    s_random.nextBytes(result);
    return result;
  }

  private static class ListTimeAuthority implements TimeAuthority {

    private long[] m_times;
    private int m_last;

    ListTimeAuthority(final long[] times) {
      setTimes(times);
    }

    public void setTimes(final long[] times) {
      m_times = times;
      m_last = -1;
    }

    @Override
    public long getTimeInMilliseconds() {
      return m_times[++m_last];
    }
  }
}
