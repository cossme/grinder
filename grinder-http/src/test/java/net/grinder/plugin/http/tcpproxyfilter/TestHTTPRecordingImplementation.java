// Copyright (C) 2005 - 2012 Philip Aston
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

import javax.xml.bind.JAXBElement;

import net.grinder.plugin.http.xml.BasicAuthorizationHeaderType;
import net.grinder.plugin.http.xml.CommonHeadersType;
import net.grinder.plugin.http.xml.HTTPRecordingType;
import net.grinder.plugin.http.xml.HTTPRecordingType.Metadata;
import net.grinder.plugin.http.xml.HeaderType;
import net.grinder.plugin.http.xml.HeadersType;
import net.grinder.plugin.http.xml.PageType;
import net.grinder.plugin.http.xml.RequestType;
import net.grinder.plugin.http.xml.ResponseType;
import net.grinder.plugin.http.xml.TokenReferenceType;
import net.grinder.testutility.AssertUtilities;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.EndPoint;
import net.grinder.util.http.URIParser;
import net.grinder.util.http.URIParserImplementation;

import org.junit.Before;
import org.junit.Ignore;
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
  @Captor private ArgumentCaptor<HTTPRecordingType> m_recordingCaptor;

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

    final HTTPRecordingType recording =
        m_recordingCaptor.getAllValues().get(0);
    final HTTPRecordingType recording2 =
        m_recordingCaptor.getAllValues().get(1);

    // XMLBeansUtilities.validate(recording);
    // XMLBeansUtilities.validate(recording2);

    assertNotSame("We get a copy", recording, recording2);

    final Metadata metadata = recording.getMetadata();
    assertTrue(metadata.getVersion().length() > 0);
    assertNotNull(metadata.getTime());
    assertEquals(0, recording.getCommonHeaders().size());
    assertEquals(0, recording.getBaseUri().size());
    assertEquals(0, recording.getPage().size());
    verifyNoMoreInteractions(m_resultProcessor);

    final IOException exception = new IOException("Eat me");
    doThrow(exception)
    .when(m_resultProcessor).process(isA(HTTPRecordingType.class));

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
    for (int i = 0; i < userComments.length; i++) {
      request1.getComment().add(userComments[i]);
    }
    assertEquals("/", request1.getUri().getUnparsed());
    assertEquals("GET", request1.getMethod().toString());
    assertEquals("GET /", request1.getDescription());
    AssertUtilities.assertArraysEqual(userComments, request1.getComment().toArray());
    assertEquals("END CLICK Sign In", request1.getComment().get(3));
    assertFalse(request1.getSleepTime() != null);
    request1.setResponse(new ResponseType());
    m_httpRecording.markLastResponseTime();

    // Request 2
    final ConnectionDetails connectionDetails2 =
      new ConnectionDetails(endPoint1, endPoint2, false);

    final RequestType request2 =
      m_httpRecording.addRequest(connectionDetails2, "GET", "/foo.gif");
    assertFalse(request2.getSleepTime() != null);
    request2.setResponse(new ResponseType());
    m_httpRecording.markLastResponseTime();
    Thread.sleep(20);

    // Request 3
    final ConnectionDetails connectionDetails3 =
      new ConnectionDetails(endPoint3, endPoint2, true);

    final RequestType request3 =
      m_httpRecording.addRequest(connectionDetails3, "GET", "bah.gif");
    assertEquals("bah.gif", request3.getUri().getUnparsed());
    assertTrue(request3.getSleepTime() != null);
    request3.setResponse(new ResponseType());
    request3.getResponse().setStatusCode(302);
    assertFalse(request3.getAnnotation() != null);

    final RequestType request4 =
      m_httpRecording.addRequest(connectionDetails3, "GET", "bah.gif");
    request4.setResponse(new ResponseType());
    request4.getResponse().setStatusCode(301);
    assertFalse(request4.getAnnotation() != null);

    final RequestType request5 =
      m_httpRecording.addRequest(connectionDetails3, "GET", "bah.gif");
    request5.setResponse(new ResponseType());
    request5.getResponse().setStatusCode(307);
    assertFalse(request5.getAnnotation() != null);

    // Ignored because it doesn't have a response.
    m_httpRecording.addRequest(connectionDetails3, "GET", "bah.gif");

    m_httpRecording.dispose();

    verify(m_resultProcessor).process(m_recordingCaptor.capture());

    final HTTPRecordingType recording = m_recordingCaptor.getValue();

    //XMLBeansUtilities.validate(recording);

    verifyNoMoreInteractions(m_resultProcessor);

    final HTTPRecordingType result = recording;
    assertEquals(0, result.getCommonHeaders().size());

    assertEquals(2, result.getBaseUri().size());
    assertEquals("hostb", result.getBaseUri().get(0).getHost());
    assertEquals("https", result.getBaseUri().get(1).getScheme().toString());

    assertEquals(2, result.getPage().size());

    final PageType page0 = result.getPage().get(0);
    assertEquals(2, page0.getRequest().size());
    assertEquals(result.getBaseUri().get(0).getUriId(),
                 page0.getRequest().get(1).getUri().getExtends());
    assertEquals("/foo.gif", ((JAXBElement<?>) page0.getRequest().get(1).getUri().getPath().getTextAndTokenReference().get(0)).getValue());
    assertFalse(page0.getRequest().get(1).getAnnotation() != null);

    final PageType page1 = result.getPage().get(1);
    assertEquals(3, page1.getRequest().size());
    assertEquals(0, page1.getRequest().get(0).getHeaders().getHeaderOrAuthorization().size());
    assertTrue(page1.getRequest().get(0).getAnnotation() != null);
    assertTrue(page1.getRequest().get(1).getAnnotation() != null);
    assertTrue(page1.getRequest().get(2).getAnnotation() != null);
  }

  @Test 
  public void testAddRequestWithComplexPaths() throws Exception {
    // Request 1
    final RequestType request1 =
      m_httpRecording.addRequest(
        m_connectionDetails1,
        "GET",
        "/path;name=value/blah;dah/foo?foo=y"
      );

    assertEquals("GET", request1.getMethod().toString());
    assertEquals("GET foo", request1.getDescription());
    assertEquals("/path;", ((JAXBElement<?>) request1.getUri().getPath().getTextAndTokenReference().get(0)).getValue());
    assertEquals("token_name",((TokenReferenceType)((JAXBElement<?>) request1.getUri().getPath().getTextAndTokenReference().get(1)).getValue()).getTokenId());
    assertEquals("/blah;dah/foo", ((JAXBElement<?>) request1.getUri().getPath().getTextAndTokenReference().get(2)).getValue());
    assertEquals(1, request1.getUri().getQueryString().getTextAndTokenReference().size());
    assertEquals("y", ((TokenReferenceType)((JAXBElement<?>) request1.getUri().getQueryString().getTextAndTokenReference().get(0)).getValue()).getNewValue());
    assertFalse(request1.getUri().getFragment() != null);

    final RequestType request2 =
      m_httpRecording.addRequest(m_connectionDetails1, "POST", "/?x=y&fo--o=bah#lah?;blah");

    assertEquals("POST", request2.getMethod().toString());
    assertEquals("POST /", request2.getDescription());
    assertEquals("/", ((JAXBElement<?>) request2.getUri().getPath().getTextAndTokenReference().get(0)).getValue());
    assertEquals(1, request2.getUri().getPath().getTextAndTokenReference().size());
    //AssertUtilities.assertArraysEqual(new String[] {"&"}, request2.getUri().getQueryString().getTextAndTokenReference().toArray());
    assertEquals("token_foo2", ((TokenReferenceType)((JAXBElement<?>)request2.getUri().getQueryString().getTextAndTokenReference().get(2)).getValue()).getTokenId());
    assertEquals("bah",  ((TokenReferenceType)((JAXBElement<?>)request2.getUri().getQueryString().getTextAndTokenReference().get(2)).getValue()).getNewValue());
    assertEquals("lah?;blah", request2.getUri().getFragment());

    final RequestType request3 =
      m_httpRecording.addRequest(m_connectionDetails1, "POST", "/?x=y&fo--o=bah#lah?;blah");

    //AssertUtilities.assertArraysEqual(new String[] {"&"}, request3.getUri().getQueryString().getTextAndTokenReference().toArray());
    assertEquals("token_foo2", ((TokenReferenceType)((JAXBElement<?>)request3.getUri().getQueryString().getTextAndTokenReference().get(2)).getValue()).getTokenId());
    assertFalse(((TokenReferenceType)((JAXBElement<?>)request3.getUri().getQueryString().getTextAndTokenReference().get(2)).getValue()).getNewValue() != null);
    assertEquals("lah?;blah", request3.getUri().getFragment());
  }

  @Test
  @Ignore
   public void testAddRequestWithHeaders() throws Exception {

    final RequestType request1 =
      m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request1.setHeaders(createHeaders(new NVPair("foo", "bah"),
                                      new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "x")));
    request1.setResponse(new ResponseType());

    final RequestType request2 =
      m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request2.setHeaders(createHeaders(new NVPair("fu", "bar"),
                                      new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "y")));
    request2.setResponse(new ResponseType());

    final RequestType request3 =
      m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request3.setHeaders(createHeaders(new NVPair("fu", "bar"),
                                      new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "y")));
    request3.setResponse(new ResponseType());

    final RequestType request4 =
      m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request4.setHeaders(createHeaders(new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "z")));

    final BasicAuthorizationHeaderType basicAuthorizationHeaderType = new BasicAuthorizationHeaderType();
    request4.getHeaders().getHeaderOrAuthorization().add(basicAuthorizationHeaderType);
    basicAuthorizationHeaderType.setUserid("phil");
    basicAuthorizationHeaderType.setPassword("abracaduh");
    request4.setResponse(new ResponseType());

    // The next two requests trigger the case where there is
    // common header set that matches the default headers.
    final RequestType request5 =
        m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request5.setHeaders(createHeaders(new NVPair("User-Agent", "blah")));
    request5.setResponse(new ResponseType());

    final RequestType request6 =
        m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request6.setHeaders(createHeaders(new NVPair("User-Agent", "blah")));
    request6.setResponse(new ResponseType());

    // Request with no response.
    final RequestType request7 =
        m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request7.setHeaders(createHeaders(new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "z")));

    final RequestType request8 =
        m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request8.setHeaders(createHeaders(new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "zz")));
    request8.setResponse(new ResponseType());

    final RequestType request9 =
        m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request9.setHeaders(createHeaders(new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "zz")));
    request9.setResponse(new ResponseType());

    m_httpRecording.dispose();

    verify(m_resultProcessor).process(m_recordingCaptor.capture());

    final HTTPRecordingType recording =
      m_recordingCaptor.getValue();

    // Default, plus 2 sets.
    assertEquals(3, recording.getCommonHeaders().size());

    final CommonHeadersType defaultHeaders = recording.getCommonHeaders().get(0);
    assertEquals(0, defaultHeaders.getHeaderOrAuthorization().size());
    assertEquals(1, defaultHeaders.getHeaderOrAuthorization().size());
    //assertEquals("User-Agent", defaultHeaders.getHeaderOrAuthorization().get(0).getName());

    final CommonHeadersType commonHeaders1 = recording.getCommonHeaders().get(1);
    assertEquals(defaultHeaders.getHeadersId(), commonHeaders1.getExtends());
    assertEquals(1, commonHeaders1.getHeaderOrAuthorization().size());
    assertEquals(0, commonHeaders1.getHeaderOrAuthorization().size());

    assertEquals(
      "defaultHeaders",
      recording.getPage().get(0).getRequest().get(0).getHeaders().getExtends());

    final CommonHeadersType commonHeaders2 = recording.getCommonHeaders().get(2);
    assertEquals(defaultHeaders.getHeadersId(), commonHeaders2.getExtends());
    assertEquals(1, commonHeaders2.getHeaderOrAuthorization().size());
    //assertEquals("zz", commonHeaders2.getHeaderOrAuthorization().get(0).getValue());
    assertEquals(0, commonHeaders2.getHeaderOrAuthorization().size());

    final HeadersType headers =
        recording.getPage().get(3).getRequest().get(0).getHeaders();
    assertEquals(1, headers.getHeaderOrAuthorization().size());
    assertEquals(1, headers.getHeaderOrAuthorization().size());
    //assertEquals("phil",
    //             headers.getHeaderOrAuthorization().get(0).getBasic().getUserid());
  }

  private HeadersType createHeaders(NVPair... nvPairs) {
    final HeadersType result = new HeadersType();

    for (int i = 0; i < nvPairs.length; ++i) {
      final HeaderType header = new HeaderType();
      result.getHeaderOrAuthorization().add(header);
      header.setName(nvPairs[i].getName());
      header.setValue(nvPairs[i].getValue());
    }

    return result;
  }

  @Test public void testExtractHeadersOneRequest() throws Exception {

    final RequestType request1 =
      m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request1.setHeaders(createHeaders(new NVPair("foo", "bah"),
                                      new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "x")));
    request1.setResponse(new ResponseType());

    //final String originalRequestXML = request1.xmlText();

    m_httpRecording.dispose();

    verify(m_resultProcessor).process(m_recordingCaptor.capture());

    final HTTPRecordingType recording =
      m_recordingCaptor.getValue();

    assertEquals(0, recording.getCommonHeaders().size());

    final RequestType request = recording.getPage().get(0).getRequest().get(0);
    final HeadersType headers = request.getHeaders();
    assertNull(headers.getExtends());
    //assertEquals(originalRequestXML, request.xmlText());
  }

  @Test public void testExtractHeadersNoCommonHeaders() throws Exception {

    final RequestType request1 =
      m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request1.setHeaders(createHeaders(new NVPair("foo", "bah"),
                                      new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "x")));
    request1.setResponse(new ResponseType());

    final RequestType request2 =
        m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request2.setHeaders(createHeaders(new NVPair("fu", "bah"),
                                      new NVPair("User-Agent", "blur"),
                                      new NVPair("Accept", "y")));
    request2.setResponse(new ResponseType());

    m_httpRecording.dispose();

    verify(m_resultProcessor).process(m_recordingCaptor.capture());

    final HTTPRecordingType recording =
      m_recordingCaptor.getValue();

    assertEquals(0, recording.getCommonHeaders().size());
  }

  @Test 
  @Ignore
  public void testExtractHeadersAllCommonHeaders() throws Exception {

    final RequestType request1 =
      m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request1.setHeaders(createHeaders(new NVPair("foo", "bah"),
                                      new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "x")));
    request1.setResponse(new ResponseType());

    final RequestType request2 =
        m_httpRecording.addRequest(m_connectionDetails1, "GET", "/path");

    request2.setHeaders(createHeaders(new NVPair("foo", "bah"),
                                      new NVPair("User-Agent", "blah"),
                                      new NVPair("Accept", "x")));
    request2.setResponse(new ResponseType());

    m_httpRecording.dispose();

    verify(m_resultProcessor).process(m_recordingCaptor.capture());

    final HTTPRecordingType recording =
      m_recordingCaptor.getValue();

    assertEquals(2, recording.getCommonHeaders().size());

    final RequestType request = recording.getPage().get(0).getRequest().get(0);
    final HeadersType headers = request.getHeaders();
    assertEquals("headers0", headers.getExtends());
    assertEquals(1, headers.getHeaderOrAuthorization().size());
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

    final TokenReferenceType tokenReference = new TokenReferenceType();
    tokenReference.setSource("somewhere");
    m_httpRecording.setTokenReference("foo", "bah", tokenReference);

    assertFalse(m_httpRecording.tokenReferenceExists("foo", null));
    assertTrue(m_httpRecording.tokenReferenceExists("foo", "somewhere"));
    assertEquals("bah", m_httpRecording.getLastValueForToken("foo"));

    tokenReference.setSource(null);
    m_httpRecording.setTokenReference("foo", "bah", tokenReference);

    assertTrue(m_httpRecording.tokenReferenceExists("foo", null));
    assertTrue(m_httpRecording.tokenReferenceExists("foo", "somewhere"));
    assertEquals("bah", m_httpRecording.getLastValueForToken("foo"));

    m_httpRecording.setTokenReference("foo", "blah", tokenReference);
    assertEquals("blah", m_httpRecording.getLastValueForToken("foo"));
  }
}
