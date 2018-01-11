// Copyright (C) 2005 - 2013 Philip Aston
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

package net.grinder.plugin.http.tcpproxyfilter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;
import java.io.IOException;

import net.grinder.plugin.http.xml.BasicAuthorizationHeaderType;
import net.grinder.plugin.http.xml.CommonHeadersType;
import net.grinder.plugin.http.xml.HTTPRecordingType;
import net.grinder.plugin.http.xml.HTTPRecordingType.Metadata;
import net.grinder.plugin.http.xml.HeaderType;
import net.grinder.plugin.http.xml.HeadersType;
import net.grinder.plugin.http.xml.HttpRecordingDocument;
import net.grinder.plugin.http.xml.PageType;
import net.grinder.plugin.http.xml.RequestType;
import net.grinder.plugin.http.xml.TokenReferenceType;
import net.grinder.testutility.XMLBeansUtilities;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.EndPoint;
import net.grinder.util.http.URIParser;
import net.grinder.util.http.URIParserImplementation;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import HTTPClient.NVPair;


/**
 * Unit tests for {@link HTTPRecordingImplementation}.
 *
 * @author Philip Aston
 */
public class TestHTTPRecordingImplementation {

  private static final EndPoint endPoint1 = new EndPoint("hostA", 80);
  private static final EndPoint endPoint2 = new EndPoint("hostB", 80);

  @Mock private HTTPRecordingResultProcessor m_resultProcessor;
  @Mock private Logger m_logger;
  @Captor private ArgumentCaptor<HttpRecordingDocument> m_recordingCaptor;

  private final RegularExpressions m_regularExpressions =
    new RegularExpressionsImplementation();

  private final URIParser m_uriParser = new URIParserImplementation();

  private final ConnectionDetails m_connectionDetails1 =
      new ConnectionDetails(endPoint1, endPoint2, false);

  private HTTPRecordingImplementation m_httpRecording;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);

    m_httpRecording =
        new HTTPRecordingImplementation(new ParametersFromProperties(),
                                        m_resultProcessor,
                                        m_logger,
                                        m_regularExpressions,
                                        m_uriParser);
  }

  @Test public void testConstructorAndDispose() throws Exception {
    verifyNoMoreInteractions(m_resultProcessor);

    assertNotNull(m_httpRecording.getParameters());
    assertSame(m_httpRecording.getParameters(),
               m_httpRecording.getParameters());

    m_httpRecording.dispose();
    m_httpRecording.dispose();

    verify(m_resultProcessor, times(2)).process(m_recordingCaptor.capture());

    final HttpRecordingDocument recording =
        m_recordingCaptor.getAllValues().get(0);
    final HttpRecordingDocument recording2 =
        m_recordingCaptor.getAllValues().get(1);

    XMLBeansUtilities.validate(recording);
    XMLBeansUtilities.validate(recording2);

    assertNotSame("We get a copy", recording, recording2);

    final Metadata metadata = recording.getHttpRecording().getMetadata();
    assertTrue(metadata.getVersion().length() > 0);
    assertNotNull(metadata.getTime());
    assertEquals(0, recording.getHttpRecording().getCommonHeadersArray().length);
    assertEquals(0, recording.getHttpRecording().getBaseUriArray().length);
    assertEquals(0, recording.getHttpRecording().getPageArray().length);
    verifyNoMoreInteractions(m_resultProcessor);

    final IOException exception = new IOException("Eat me");
    doThrow(exception)
    .when(m_resultProcessor).process(isA(HttpRecordingDocument.class));

    m_httpRecording.dispose();

    verify(m_logger).error(exception.getMessage(), exception);
  }

  @Test public void testAddRequest() throws Exception {
    final EndPoint endPoint3 = new EndPoint("hostC", 80);
    final String[] userComments = new String[]{
        "BEGIN ENTER gmail homepage",
        "END ENTER gmail homepage",
        "BEGIN CLICK Sign In",
        "END CLICK Sign In",
    };

    // Request 1
    final RequestType request1 =
      m_httpRecording.addRequest(m_connectionDetails1, "GET", "/");
    for (final String userComment : userComments) {
      request1.addComment(userComment);
    }
    assertEquals("/", request1.getUri().getUnparsed());
    assertEquals("GET", request1.getMethod().toString());
    assertEquals("GET /", request1.getDescription());
    assertArrayEquals(userComments, request1.getCommentArray());
    assertEquals("END CLICK Sign In", request1.getCommentArray(3));
    assertFalse(request1.isSetSleepTime());
    request1.addNewResponse();
    m_httpRecording.markLastResponseTime();

    // Request 2
    final ConnectionDetails connectionDetails2 =
      new ConnectionDetails(endPoint1, endPoint2, false);

    final RequestType request2 =
      m_httpRecording.addRequest(connectionDetails2, "GET", "/foo.gif");
    assertFalse(request2.isSetSleepTime());
    request2.addNewResponse();
    m_httpRecording.markLastResponseTime();
    Thread.sleep(20);

    // Request 3
    final ConnectionDetails connectionDetails3 =
      new ConnectionDetails(endPoint3, endPoint2, true);

    final RequestType request3 =
      m_httpRecording.addRequest(connectionDetails3, "GET", "bah.gif");
    assertEquals("bah.gif", request3.getUri().getUnparsed());
    assertTrue(request3.isSetSleepTime());
    request3.addNewResponse().setStatusCode(302);
    assertFalse(request3.isSetAnnotation());

    final RequestType request4 =
      m_httpRecording.addRequest(connectionDetails3, "GET", "bah.gif");
    request4.addNewResponse().setStatusCode(301);
    assertFalse(request4.isSetAnnotation());

    final RequestType request5 =
      m_httpRecording.addRequest(connectionDetails3, "GET", "bah.gif");
    request5.addNewResponse().setStatusCode(307);
    assertFalse(request5.isSetAnnotation());

    // Ignored because it doesn't have a response.
    m_httpRecording.addRequest(connectionDetails3, "GET", "bah.gif");

    m_httpRecording.dispose();

    verify(m_resultProcessor).process(m_recordingCaptor.capture());

    final HttpRecordingDocument recording = m_recordingCaptor.getValue();

    XMLBeansUtilities.validate(recording);

    verifyNoMoreInteractions(m_resultProcessor);

    final HTTPRecordingType result = recording.getHttpRecording();
    assertEquals(0, result.getCommonHeadersArray().length);

    assertEquals(2, result.getBaseUriArray().length);
    assertEquals("hostb", result.getBaseUriArray(0).getHost());
    assertEquals("https", result.getBaseUriArray(1).getScheme().toString());

    assertEquals(2, result.getPageArray().length);

    final PageType page0 = result.getPageArray(0);
    assertEquals(2, page0.getRequestArray().length);
    assertEquals(result.getBaseUriArray(0).getUriId(),
                 page0.getRequestArray(1).getUri().getExtends());
    assertEquals("/foo.gif",
      page0.getRequestArray(1).getUri().getPath().getTextArray(0));
    assertFalse(page0.getRequestArray(1).isSetAnnotation());

    final PageType page1 = result.getPageArray(1);
    assertEquals(3, page1.getRequestArray().length);
    assertEquals(0, page1.getRequestArray(0).getHeaders().sizeOfHeaderArray());
    assertTrue(page1.getRequestArray(0).isSetAnnotation());
    assertTrue(page1.getRequestArray(1).isSetAnnotation());
    assertTrue(page1.getRequestArray(2).isSetAnnotation());
  }

  @Test public void testAddRequestWithComplexPaths() throws Exception {
    // Request 1
    final RequestType request1 =
      m_httpRecording.addRequest(
        m_connectionDetails1,
        "GET",
        "/path;name=value/blah;dah/foo?foo=y"
      );

    assertEquals("GET", request1.getMethod().toString());
    assertEquals("GET foo", request1.getDescription());
    assertEquals("/path;", request1.getUri().getPath().getTextArray(0));
    assertEquals("token_name", request1.getUri().getPath().getTokenReferenceArray(0).getTokenId());
    assertEquals("/blah;dah/foo", request1.getUri().getPath().getTextArray(1));
    assertEquals(0, request1.getUri().getQueryString().getTextArray().length);
    assertEquals("y", request1.getUri().getQueryString().getTokenReferenceArray(0).getNewValue());
    assertFalse(request1.getUri().isSetFragment());

    final RequestType request2 =
      m_httpRecording.addRequest(m_connectionDetails1, "POST", "/?x=y&fo--o=bah#lah?;blah");

    assertEquals("POST", request2.getMethod().toString());
    assertEquals("POST /", request2.getDescription());
    assertEquals("/", request2.getUri().getPath().getTextArray(0));
    assertEquals(0, request2.getUri().getPath().getTokenReferenceArray().length);
    assertArrayEquals(new String[] {"&"}, request2.getUri().getQueryString().getTextArray());
    assertEquals("token_foo2", request2.getUri().getQueryString().getTokenReferenceArray(1).getTokenId());
    assertEquals("bah", request2.getUri().getQueryString().getTokenReferenceArray(1).getNewValue());
    assertEquals("lah?;blah", request2.getUri().getFragment());

    final RequestType request3 =
      m_httpRecording.addRequest(m_connectionDetails1, "POST", "/?x=y&fo--o=bah#lah?;blah");

    assertArrayEquals(new String[] {"&"}, request3.getUri().getQueryString().getTextArray());
    assertEquals("token_foo2", request3.getUri().getQueryString().getTokenReferenceArray(1).getTokenId());
    assertFalse(request3.getUri().getQueryString().getTokenReferenceArray(1).isSetNewValue());
    assertEquals("lah?;blah", request3.getUri().getFragment());
  }

  @Test public void testAddRequestWithHeaders() throws Exception {

    final RequestType request1 =
      m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request1.setHeaders(createHeaders(new NVPair("foo", "bah"),
                                      new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "x")));
    request1.addNewResponse();

    final RequestType request2 =
      m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request2.setHeaders(createHeaders(new NVPair("fu", "bar"),
                                      new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "y")));
    request2.addNewResponse();

    final RequestType request3 =
      m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request3.setHeaders(createHeaders(new NVPair("fu", "bar"),
                                      new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "y")));
    request3.addNewResponse();

    final RequestType request4 =
      m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request4.setHeaders(createHeaders(new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "z")));

    final BasicAuthorizationHeaderType basicAuthorizationHeaderType =
      request4.getHeaders().addNewAuthorization().addNewBasic();
    basicAuthorizationHeaderType.setUserid("phil");
    basicAuthorizationHeaderType.setPassword("abracaduh");
    request4.addNewResponse();

    // The next two requests trigger the case where there is
    // common header set that matches the default headers.
    final RequestType request5 =
        m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request5.setHeaders(createHeaders(new NVPair("User-Agent", "blah")));
    request5.addNewResponse();

    final RequestType request6 =
        m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request6.setHeaders(createHeaders(new NVPair("User-Agent", "blah")));
    request6.addNewResponse();

    // Request with no response.
    final RequestType request7 =
        m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request7.setHeaders(createHeaders(new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "z")));

    final RequestType request8 =
        m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request8.setHeaders(createHeaders(new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "zz")));
    request8.addNewResponse();

    final RequestType request9 =
        m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request9.setHeaders(createHeaders(new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "zz")));
    request9.addNewResponse();

    m_httpRecording.dispose();

    verify(m_resultProcessor).process(m_recordingCaptor.capture());

    final HTTPRecordingType recording =
      m_recordingCaptor.getValue().getHttpRecording();

    // Default, plus 2 sets.
    assertEquals(3, recording.getCommonHeadersArray().length);

    final CommonHeadersType defaultHeaders = recording.getCommonHeadersArray(0);
    assertEquals(0, defaultHeaders.getAuthorizationArray().length);
    assertEquals(1, defaultHeaders.getHeaderArray().length);
    assertEquals("User-Agent", defaultHeaders.getHeaderArray(0).getName());

    final CommonHeadersType commonHeaders1 = recording.getCommonHeadersArray(1);
    assertEquals(defaultHeaders.getHeadersId(), commonHeaders1.getExtends());
    assertEquals(1, commonHeaders1.getHeaderArray().length);
    assertEquals(0, commonHeaders1.getAuthorizationArray().length);

    assertEquals(
      "defaultHeaders",
      recording.getPageArray(0).getRequestArray(0).getHeaders().getExtends());

    final CommonHeadersType commonHeaders2 = recording.getCommonHeadersArray(2);
    assertEquals(defaultHeaders.getHeadersId(), commonHeaders2.getExtends());
    assertEquals(1, commonHeaders2.getHeaderArray().length);
    assertEquals("zz", commonHeaders2.getHeaderArray(0).getValue());
    assertEquals(0, commonHeaders2.getAuthorizationArray().length);

    final HeadersType headers =
        recording.getPageArray(3).getRequestArray(0).getHeaders();
    assertEquals(1, headers.getHeaderArray().length);
    assertEquals(1, headers.getAuthorizationArray().length);
    assertEquals("phil",
                 headers.getAuthorizationArray(0).getBasic().getUserid());
  }

  private HeadersType createHeaders(final NVPair... nvPairs) {
    final HeadersType result = HeadersType.Factory.newInstance();

    for (final NVPair nvPair : nvPairs) {
      final HeaderType header = result.addNewHeader();
      header.setName(nvPair.getName());
      header.setValue(nvPair.getValue());
    }

    return result;
  }

  @Test public void testExtractHeadersOneRequest() throws Exception {

    final RequestType request1 =
      m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request1.setHeaders(createHeaders(new NVPair("foo", "bah"),
                                      new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "x")));
    request1.addNewResponse();

    final String originalRequestXML = request1.xmlText();

    m_httpRecording.dispose();

    verify(m_resultProcessor).process(m_recordingCaptor.capture());

    final HTTPRecordingType recording =
      m_recordingCaptor.getValue().getHttpRecording();

    assertEquals(0, recording.getCommonHeadersArray().length);

    final RequestType request = recording.getPageArray(0).getRequestArray(0);
    final HeadersType headers = request.getHeaders();
    assertNull(headers.getExtends());
    assertEquals(originalRequestXML, request.xmlText());
  }

  @Test public void testExtractHeadersNoCommonHeaders() throws Exception {

    final RequestType request1 =
      m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request1.setHeaders(createHeaders(new NVPair("foo", "bah"),
                                      new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "x")));
    request1.addNewResponse();

    final RequestType request2 =
        m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request2.setHeaders(createHeaders(new NVPair("fu", "bah"),
                                      new NVPair("User-Agent", "blur"),
                                      new NVPair("Accept", "y")));
    request2.addNewResponse();

    m_httpRecording.dispose();

    verify(m_resultProcessor).process(m_recordingCaptor.capture());

    final HTTPRecordingType recording =
      m_recordingCaptor.getValue().getHttpRecording();

    assertEquals(0, recording.getCommonHeadersArray().length);
  }

  @Test public void testExtractHeadersAllCommonHeaders() throws Exception {

    final RequestType request1 =
      m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request1.setHeaders(createHeaders(new NVPair("foo", "bah"),
                                      new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "x")));
    request1.addNewResponse();

    final RequestType request2 =
        m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request2.setHeaders(createHeaders(new NVPair("foo", "bah"),
                                      new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "x")));
    request2.addNewResponse();

    m_httpRecording.dispose();

    verify(m_resultProcessor).process(m_recordingCaptor.capture());

    final HTTPRecordingType recording =
      m_recordingCaptor.getValue().getHttpRecording();

    assertEquals(1, recording.getCommonHeadersArray().length);

    final RequestType request = recording.getPageArray(0).getRequestArray(0);
    final HeadersType headers = request.getHeaders();
    assertEquals("headers0", headers.getExtends());
    assertEquals(1, headers.sizeOfHeaderArray());
  }

  @Test public void testCreateBodyDataFileName() throws Exception {

    final File file1 = m_httpRecording.createBodyDataFileName();
    final File file2 = m_httpRecording.createBodyDataFileName();

    assertTrue(!file1.equals(file2));
  }

  @Test public void testTokenReferenceMethods() throws Exception {

    assertFalse(m_httpRecording.tokenReferenceExists("foo", null));
    assertFalse(m_httpRecording.tokenReferenceExists("foo", "somewhere"));
    assertNull(m_httpRecording.getLastValueForToken("foo"));

    final TokenReferenceType tokenReference =
        TokenReferenceType.Factory.newInstance();
    tokenReference.setSource("somewhere");
    m_httpRecording.setTokenReference("foo", "bah", tokenReference);

    assertFalse(m_httpRecording.tokenReferenceExists("foo", null));
    assertTrue(m_httpRecording.tokenReferenceExists("foo", "somewhere"));
    assertEquals("bah", m_httpRecording.getLastValueForToken("foo"));

    tokenReference.unsetSource();
    m_httpRecording.setTokenReference("foo", "bah", tokenReference);

    assertTrue(m_httpRecording.tokenReferenceExists("foo", null));
    assertTrue(m_httpRecording.tokenReferenceExists("foo", "somewhere"));
    assertEquals("bah", m_httpRecording.getLastValueForToken("foo"));

    m_httpRecording.setTokenReference("foo", "blah", tokenReference);
    assertEquals("blah", m_httpRecording.getLastValueForToken("foo"));
  }
}
