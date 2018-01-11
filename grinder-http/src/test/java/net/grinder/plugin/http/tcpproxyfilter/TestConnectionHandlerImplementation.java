// Copyright (C) 2005 - 2012 Philip Aston
// Copyright (C) 2007 Venelin Mitov
// Copyright (C) 2009 Hitoshi Amano
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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.BufferOverflowException;

import net.grinder.plugin.http.xml.FormFieldType;
import net.grinder.plugin.http.xml.RequestType;
import net.grinder.plugin.http.xml.ResponseTokenReferenceType;
import net.grinder.plugin.http.xml.TokenReferenceType;
import net.grinder.plugin.http.xml.TokenResponseLocationType;
import net.grinder.plugin.http.xml.TokenType;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.FileUtilities;
import net.grinder.tools.tcpproxy.CommentSource;
import net.grinder.tools.tcpproxy.CommentSourceImplementation;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.EndPoint;
import net.grinder.util.AttributeStringParser;
import net.grinder.util.AttributeStringParserImplementation;
import net.grinder.util.SimpleStringEscaper;
import net.grinder.util.StringEscaper;
import net.grinder.util.http.URIParser;
import net.grinder.util.http.URIParserImplementation;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;


/**
 * Unit tests for {@link ConnectionHandlerImplementation}.
 *
 * @author Philip Aston
 */
public class TestConnectionHandlerImplementation
  extends AbstractJUnit4FileTestCase {

  @Mock private HTTPRecording m_httpRecording;
  @Mock private Logger m_logger;
  @Captor private ArgumentCaptor<ResponseTokenReferenceType>
    m_responseTokenRTCaptor;

  private final RegularExpressions m_regularExpressions =
    new RegularExpressionsImplementation();

  private final URIParser m_uriParser = new URIParserImplementation();

  private final AttributeStringParser m_attributeStringParser =
    new AttributeStringParserImplementation();

  private StringEscaper m_stringEscaper = new SimpleStringEscaper();

  private final ConnectionDetails m_connectionDetails =
    new ConnectionDetails(
      new EndPoint("hostA", 80),
      new EndPoint("hostB", 80),
      false);

  private final CommentSource m_commentSource =
    new CommentSourceImplementation();

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);

    final HTTPRecordingParameters parametersFromProperties =
        new ParametersFromProperties();

    when(m_httpRecording.getParameters()).thenReturn(parametersFromProperties);
  }

  @Test public void testRequestWithGet() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_logger, m_regularExpressions,
        m_uriParser, m_attributeStringParser, null,
        m_commentSource, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("GET"));

    final TokenType token = TokenType.Factory.newInstance();
    token.setName("query");
    token.setTokenId("tokenID");

    when(m_httpRecording.addRequest(m_connectionDetails,
                                    "GET",
                                    "/something?query=whatever"))
      .thenReturn(request);

    final String message = "GET /something?query=whatever HTTP/1.0\r\n\r\n";
    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    final String response = "HTTP/1.0 200 OK\r\n";
    final byte[] responseBuffer = response.getBytes();

    handler.handleResponse(responseBuffer, responseBuffer.length);

    verify(m_httpRecording).markLastResponseTime();

    handler.requestFinished();
  }

  @Test public void testAuthorization() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_logger, m_regularExpressions,
        m_uriParser, m_attributeStringParser, null,
        m_commentSource, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("GET"));

    when(m_httpRecording.addRequest(m_connectionDetails,
                                    "GET",
                                    "/"))
      .thenReturn(request);

    final String message =
      "GET / HTTP/1.1\r\n" +
      "Authorization: Basic aBcD\r\n";
    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    // Bad authorization header.
    verify(m_logger).error(isA(String.class));
    assertEquals(0, request.getHeaders().sizeOfAuthorizationArray());

    final String message2 =
      "GET / HTTP/1.1\r\n" +
      "Authorization: Basic dDpyYXVtc2NobWllcmU=\r\n";
    final byte[] buffer2 = message2.getBytes();
    handler.handleRequest(buffer2, buffer2.length);

    assertEquals("t", request.getHeaders().getAuthorizationArray(0).getBasic().getUserid());
    assertEquals("raumschmiere", request.getHeaders().getAuthorizationArray(0).getBasic().getPassword());
  }

  @Test public void testRequestWithPost() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_logger, m_regularExpressions,
        m_uriParser, m_attributeStringParser, m_stringEscaper, m_commentSource,
        m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("POST"));

    when(m_httpRecording.addRequest(same(m_connectionDetails),
                                    eq("POST"),
                                    isA(String.class)))
      .thenReturn(request);


    final String message =
      "POST /something HTTP/1.0\r\n" +
      "Content-Length: 10\r\n" +
      "Content-Type: bah\r\n" +
      "\r\n" +
      "0123456789";

    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    final String response = "HTTP/1.0 200 OK\r\n";
    final byte[] responseBuffer = response.getBytes();

    handler.handleResponse(responseBuffer, responseBuffer.length);

    verify(m_httpRecording).markLastResponseTime();

    final String message2 =
      "POST /more HTTP/1.0\r\n" +
      "Content-Length: 10\r\n" +
      "\r\n";

    final byte[] buffer2 = message2.getBytes();
    handler.handleRequest(buffer2, buffer2.length);

    final String message3 = "0123456789";
    final byte[] buffer3 = message3.getBytes();
    handler.handleRequest(buffer3, buffer3.length);

    final String message4 =
      "POST /evenmore HTTP/1.0\r\n";

    final byte[] buffer4 = message4.getBytes();
    handler.handleRequest(buffer4, buffer4.length);

    final String message5 =
      "Content-Length: 0\r\n" +
      "\r\n";

    final byte[] buffer5 = message5.getBytes();
    handler.handleRequest(buffer5, buffer5.length);

    verify(m_httpRecording, times(3)).addRequest(same(m_connectionDetails),
                                                 eq("POST"),
                                                 isA(String.class));
  }

  @Test public void testResponseMessage1() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_logger, m_regularExpressions,
        m_uriParser, m_attributeStringParser, null, m_commentSource, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("HEAD"));

    when (m_httpRecording.addRequest(m_connectionDetails,
                                     "HEAD",
                                     "/"))
      .thenReturn(request);

    final String message = "HEAD / HTTP/1.1\r\n";
    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    final String response =
      "HTTP/1.0 302 Redirect\r\n" +
      "Hello: world\r\n" +
      "Location: http://somewhere/;a=b?c=d\r\n";

    final byte[] responseBuffer = response.getBytes();
    handler.handleResponse(responseBuffer, responseBuffer.length);

    verify(m_httpRecording).markLastResponseTime();

    verify(m_httpRecording)
      .setTokenReference(eq("a"),
                         eq("b"),
                         isA(ResponseTokenReferenceType.class));

    verify(m_httpRecording)
    .setTokenReference(eq("c"),
                       eq("d"),
                       isA(ResponseTokenReferenceType.class));
  }

  @Test public void testResponseMessage2() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_logger, m_regularExpressions,
        m_uriParser, m_attributeStringParser, null,
        m_commentSource, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("GET"));

    when(m_httpRecording.addRequest(m_connectionDetails,
                                    "GET",
                                    "/"))
      .thenReturn(request);

    final String message = "GET / HTTP/1.1\r\n";
    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    final String response =
      "HTTP/1.0 200 OK\r\n" +
      "Content-Length:10\r\n\r\n" +
      "0123456789";

    final byte[] responseBuffer = response.getBytes();
    handler.handleResponse(responseBuffer, responseBuffer.length);

    verify(m_httpRecording).markLastResponseTime();
  }

  @Test public void testResponseMessage3() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_logger, m_regularExpressions,
        m_uriParser, m_attributeStringParser, null, m_commentSource, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("GET"));

    when(m_httpRecording.addRequest(m_connectionDetails, "GET", "/"))
      .thenReturn(request);

    final String message = "GET / HTTP/1.1\r\n";
    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    final String response =
      "HTTP/1.0 304 Not Modified\r\n";

    final byte[] responseBuffer = response.getBytes();
    handler.handleResponse(responseBuffer, responseBuffer.length);

    verify(m_httpRecording).markLastResponseTime();
  }

  @Test public void testResponseMessageWithTokensInLinks() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_logger, m_regularExpressions,
        m_uriParser, m_attributeStringParser, null,
        m_commentSource, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("GET"));

    when(m_httpRecording.addRequest(m_connectionDetails, "GET", "/"))
    .thenReturn(request);
    when(m_httpRecording.getLastValueForToken(isA(String.class)))
    .thenReturn("1");

    final String message = "GET / HTTP/1.0\r\n";
    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    final String response =
      "HTTP/1.0 200 OK\r\n" +
      "\r\n" +
      "<html>" +
      "<body><a href='./foo;session=57?token=1'>Hello world</a>" +
      "<a href=\"http://grinder.sourceforge.net/?token=1\">something else</a>";

    final byte[] responseBuffer = response.getBytes();
    handler.handleResponse(responseBuffer, responseBuffer.length);

    verify(m_httpRecording).markLastResponseTime();

    final String response2 =
      "<a href=\"http://grinder.sourceforge.net/?token=2\">something else</a>" +
      "</body>";
    final byte[] responseBuffer2 = response2.getBytes();
    handler.handleResponse(responseBuffer2, responseBuffer2.length);

    // Response is not flushed until the request is over.
    handler.requestFinished();

    verify(m_httpRecording)
      .setTokenReference(eq("session"),
                         eq("57"),
                         m_responseTokenRTCaptor.capture());

    assertEquals(TokenResponseLocationType.RESPONSE_BODY_URI_PATH_PARAMETER.toString(),
                 m_responseTokenRTCaptor.getValue().getSource());

    verify(m_httpRecording)
      .setTokenReference(eq("token"),
                         eq("1"),
                         m_responseTokenRTCaptor.capture());

    final ResponseTokenReferenceType responseTokenReference2 =
      m_responseTokenRTCaptor.getValue();
    assertEquals(1, responseTokenReference2.getConflictingValueArray().length);
    assertEquals("2", responseTokenReference2.getConflictingValueArray()[0].getValue());
    assertEquals(TokenResponseLocationType.RESPONSE_BODY_URI_QUERY_STRING.toString(),
                 responseTokenReference2.getConflictingValueArray()[0].getSource());

    verify(m_httpRecording, times(2)).getLastValueForToken(isA(String.class));
  }

  @Test public void testResponseMessageWithTokensInHiddenParameters() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_logger, m_regularExpressions,
        m_uriParser, m_attributeStringParser, null, m_commentSource, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("GET"));

    when(m_httpRecording.addRequest(m_connectionDetails, "GET", "/"))
    .thenReturn(request);

    final String message = "GET / HTTP/1.0\r\n";
    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    final String response =
      "HTTP/1.0 200 OK\r\n" +
      "\r\n" +
      "<html>" +
      "<body><form>\n\n<input name=\"foo\" value=\"123\" \n" +
      " type=\"HIDDEN\">" +
      "<input type='hidden' name='blah'/>" +
      "</form>" +
      "</body></html>";

    final byte[] responseBuffer = response.getBytes();
    handler.handleResponse(responseBuffer, responseBuffer.length);

    verify(m_httpRecording).markLastResponseTime();

    handler.requestFinished();

    verify(m_httpRecording)
    .setTokenReference(eq("foo"),
                       eq("123"),
                       m_responseTokenRTCaptor.capture());

    assertEquals(TokenResponseLocationType.RESPONSE_BODY_HIDDEN_INPUT.toString(),
      m_responseTokenRTCaptor.getValue().getSource());
  }

  @Test public void testRequestStringBody() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_logger, m_regularExpressions,
        m_uriParser, m_attributeStringParser, m_stringEscaper,
        m_commentSource, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("POST"));

    when(m_httpRecording.addRequest(m_connectionDetails, "POST", "/something"))
    .thenReturn(request);

    final String message =
      "POST /something HTTP/1.0\r\n" +
      "Content-Length: 9\r\n" +
      "Content-Type: bah\r\n" +
      "\r\n" +
      "0123456789";

    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    verify(m_logger).error(contains("content length exceeded"));

    handler.requestFinished(); // Force body to be flushed.

    assertEquals("bah", request.getBody().getContentType());
    assertEquals("012345678", request.getBody().getEscapedString());
    assertFalse(request.getBody().isSetBinary());
    assertFalse(request.getBody().isSetFile());
    assertFalse(request.getBody().isSetForm());
  }

  @Test public void testRequestWithUserComment() throws Exception {
    ConnectionHandler handler = new ConnectionHandlerImplementation(
        m_httpRecording, m_logger, m_regularExpressions,
        m_uriParser, m_attributeStringParser, null,
        m_commentSource, m_connectionDetails);

    //Add some user comments to the comment source. They should be added to the
    //resulting request.
    ((CommentSourceImplementation)m_commentSource).addComment("user comment 1");
    ((CommentSourceImplementation)m_commentSource).addComment("user comment 2");

    final RequestType request = RequestType.Factory.newInstance();

    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("GET"));
    request.setCommentArray(new String[]{"user comment 1", "user comment 2"});

    final TokenType token = TokenType.Factory.newInstance();
    token.setName("query");
    token.setTokenId("tokenID");

    when(m_httpRecording.addRequest(m_connectionDetails,
                                    "GET",
                                    "/something?query=whatever"))
    .thenReturn(request);

    final String message = "GET /something?query=whatever HTTP/1.0\r\n\r\n";
    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    verify(m_httpRecording).addRequest(m_connectionDetails,
                                       "GET",
                                      "/something?query=whatever");
    verifyNoMoreInteractions(m_httpRecording);
  }

  @Test public void testRequestBinaryBody() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_logger, m_regularExpressions,
        m_uriParser, m_attributeStringParser, null,
        m_commentSource, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("POST"));

    when(m_httpRecording.addRequest(m_connectionDetails,
                                    "POST",
                                    "/something"))
    .thenReturn(request);


    final String message =
      "POST /something HTTP/1.0\r\n" +
      "Content-Length: 150\r\n" +
      "Content-Type: bah\r\n" +
      "\r\n";

    final byte[] buffer = message.getBytes();

    final byte[] buffer2 = new byte[50];

    for (int i = 0; i < buffer2.length; i++) {
      buffer2[i] = (byte) i;
    }

    // Deliberately use an oversize buffer to check handleRequest trims
    // correctly.
    final byte[] buffer3 = new byte[buffer.length + buffer2.length + 100];
    System.arraycopy(buffer, 0, buffer3, 0, buffer.length);
    System.arraycopy(buffer2, 0, buffer3, buffer.length, buffer2.length);

    handler.handleRequest(buffer3, buffer.length + buffer2.length);

    handler.handleRequest(buffer2, 50);
    handler.handleRequest(buffer2, 50);

    handler.requestFinished(); // Force body to be flushed.

    assertEquals("bah", request.getBody().getContentType());
    assertEquals(150, request.getBody().getBinary().length);
    assertFalse(request.getBody().isSetEscapedString());
    assertFalse(request.getBody().isSetFile());
    assertFalse(request.getBody().isSetForm());
  }

  @Test public void testRequestFormBody() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_logger, m_regularExpressions,
        m_uriParser, m_attributeStringParser, null,
        m_commentSource, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("POST"));

    when(m_httpRecording.addRequest(m_connectionDetails,
                                    "POST",
                                    "/something"))
    .thenReturn(request);

    when(m_httpRecording.tokenReferenceExists(isA(String.class),
                                              isA(String.class)))
    .thenReturn(Boolean.FALSE);

    final String message =
      "POST /something HTTP/1.0\r\n" +
      "Content-Type: application/x-www-form-urlencoded; blah\r\n" +
      "\r\n" +
      "red=5&blue=10";

    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    handler.requestFinished(); // Force body to be flushed.

    final FormFieldType[] formFieldArray =
      request.getBody().getForm().getFormFieldArray();
    assertEquals(2, formFieldArray.length);
    assertEquals("red", formFieldArray[0].getName());
    assertEquals("10", formFieldArray[1].getValue());
    assertFalse(request.getBody().getForm().getMultipart());
    assertFalse(request.getBody().isSetBinary());
    assertFalse(request.getBody().isSetEscapedString());
    assertFalse(request.getBody().isSetFile());
    assertEquals(0, request.getBody().getForm().getTokenReferenceArray().length);

    final RequestType request2 = RequestType.Factory.newInstance();
    request2.addNewHeaders();
    request2.setMethod(RequestType.Method.Enum.forString("POST"));

    when(m_httpRecording.addRequest(m_connectionDetails,
                                    "POST",
                                    "/something"))
    .thenReturn(request2);
    when(m_httpRecording.tokenReferenceExists(isA(String.class),
                                              isA(String.class)))
    .thenReturn(Boolean.TRUE);

    handler.handleRequest(buffer, buffer.length);

    handler.requestFinished(); // Force body to be flushed.

    assertEquals(0, request2.getBody().getForm().getFormFieldArray().length);
    assertFalse(request2.getBody().getForm().getMultipart());
    assertFalse(request2.getBody().isSetBinary());
    assertFalse(request2.getBody().isSetEscapedString());
    assertFalse(request2.getBody().isSetFile());
    final TokenReferenceType[] tokenReferenceArray =
      request2.getBody().getForm().getTokenReferenceArray();
    assertEquals(2, tokenReferenceArray.length);
    assertFalse(tokenReferenceArray[0].isSetSource());
    assertFalse(tokenReferenceArray[0].isSetNewValue());
  }

  @Test public void testRequestMultipartFormBody() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
            m_httpRecording,
            m_logger,
            m_regularExpressions,
            m_uriParser,
            m_attributeStringParser, null,
            m_commentSource,
            m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("POST"));

    when(m_httpRecording.addRequest(m_connectionDetails,
                                    "POST",
                                    "/something"))
    .thenReturn(request);
    when(m_httpRecording.tokenReferenceExists(isA(String.class),
                                              isA(String.class)))
    .thenReturn(Boolean.FALSE);


    final String message =
        "POST /something HTTP/1.0\r\n" +
        "Content-Type: multipart/form-data; charset=UTF-8; boundary=---------------------------6549821653387387991112192755\r\n" +
        "\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"csrf_ticket\"\r\n" +
        "\r\n" +
        "XXXXXXXXXXXXXXXXXXXX\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"title\"\r\n" +
        "\r\n" +
        "test\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"enable_term\"\r\n" +
        "\r\n" +
        "0\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"sterm_year\"\r\n" +
        "\r\n" +
        "2009\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"sterm_month\"\r\n" +
        "\r\n" +
        "12\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"sterm_day\"\r\n" +
        "\r\n" +
        "3\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"sterm_hour\"\r\n" +
        "\r\n" +
        "\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"sterm_minute\"\r\n" +
        "\r\n" +
        "\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"eterm_year\"\r\n" +
        "\r\n" +
        "2009\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"eterm_month\"\r\n" +
        "\r\n" +
        "12\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"eterm_day\"\r\n" +
        "\r\n" +
        "3\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"eterm_hour\"\r\n" +
        "\r\n" +
        "\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"eterm_minute\"\r\n" +
        "\r\n" +
        "\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"sterm_\"\r\n" +
        "\r\n" +
        "\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"eterm_\"\r\n" +
        "\r\n" +
        "\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"published\"\r\n" +
        "\r\n" +
        "\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"editor\"\r\n" +
        "\r\n" +
        "0\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"data\"\r\n" +
        "\r\n" +
        "testtesttesttesttesttesttesttesttesttesttest\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"file0\"; filename=\"\"\r\n" +
        "Content-Type: application/octet-stream\r\n" +
        "\r\n" +
        "\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"can_follow\"\r\n" +
        "\r\n" +
        "1\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"send\"\r\n" +
        "\r\n" +
        "AAA\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"cid\"\r\n" +
        "\r\n" +
        "2\r\n" +
        "-----------------------------6549821653387387991112192755\r\n" +
        "Content-Disposition: form-data; name=\"aid\"\r\n" +
        "\r\n" +
        "\r\n" +
        "-----------------------------6549821653387387991112192755--\r\n";

    final byte[] buffer = message.getBytes("UTF-8");
    handler.handleRequest(buffer, buffer.length);

    handler.requestFinished(); // Force body to be flushed.

    final FormFieldType[] formFieldArray =
      request.getBody().getForm().getFormFieldArray();
    assertEquals(23, formFieldArray.length);
    assertEquals("csrf_ticket", formFieldArray[0].getName());
    assertEquals("XXXXXXXXXXXXXXXXXXXX", formFieldArray[0].getValue());
    assertEquals("title", formFieldArray[1].getName());
    assertEquals("test", formFieldArray[1].getValue());
    assertEquals("enable_term", formFieldArray[2].getName());
    assertEquals("0", formFieldArray[2].getValue());
    assertEquals("sterm_year", formFieldArray[3].getName());
    assertEquals("2009", formFieldArray[3].getValue());
    assertEquals("sterm_month", formFieldArray[4].getName());
    assertEquals("12", formFieldArray[4].getValue());
    assertEquals("sterm_day", formFieldArray[5].getName());
    assertEquals("3", formFieldArray[5].getValue());
    assertEquals("sterm_hour", formFieldArray[6].getName());
    assertEquals("", formFieldArray[6].getValue());
    assertEquals("sterm_minute", formFieldArray[7].getName());
    assertEquals("", formFieldArray[7].getValue());
    assertEquals("eterm_year", formFieldArray[8].getName());
    assertEquals("2009", formFieldArray[8].getValue());
    assertEquals("eterm_month", formFieldArray[9].getName());
    assertEquals("12", formFieldArray[9].getValue());
    assertEquals("eterm_day", formFieldArray[10].getName());
    assertEquals("3", formFieldArray[10].getValue());
    assertEquals("eterm_hour", formFieldArray[11].getName());
    assertEquals("", formFieldArray[11].getValue());
    assertEquals("eterm_minute", formFieldArray[12].getName());
    assertEquals("", formFieldArray[12].getValue());
    assertEquals("sterm_", formFieldArray[13].getName());
    assertEquals("", formFieldArray[13].getValue());
    assertEquals("eterm_", formFieldArray[14].getName());
    assertEquals("", formFieldArray[14].getValue());
    assertEquals("published", formFieldArray[15].getName());
    assertEquals("", formFieldArray[15].getValue());
    assertEquals("editor", formFieldArray[16].getName());
    assertEquals("0", formFieldArray[16].getValue());
    assertEquals("data", formFieldArray[17].getName());
    assertEquals("testtesttesttesttesttesttesttesttesttesttest", formFieldArray[17].getValue());
    assertEquals("file0", formFieldArray[18].getName());
    assertEquals("", formFieldArray[18].getValue());
    assertEquals("can_follow", formFieldArray[19].getName());
    assertEquals("1", formFieldArray[19].getValue());
    assertEquals("send", formFieldArray[20].getName());
    assertEquals("AAA", formFieldArray[20].getValue());
    assertEquals("cid", formFieldArray[21].getName());
    assertEquals("2", formFieldArray[21].getValue());
    assertEquals("aid", formFieldArray[22].getName());
    assertEquals("", formFieldArray[22].getValue());
    assertEquals(23, formFieldArray.length);
    assertTrue(request.getBody().getForm().getMultipart());
    assertFalse(request.getBody().isSetBinary());
    assertFalse(request.getBody().isSetEscapedString());
    assertFalse(request.getBody().isSetFile());
    assertEquals(0, request.getBody().getForm().getTokenReferenceArray().length);
  }

  @Test public void testRequestFileBody() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_logger, m_regularExpressions,
        m_uriParser, m_attributeStringParser, null,
        m_commentSource, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("POST"));

    final File file = new File(getDirectory(), "formData");


    when(m_httpRecording.addRequest(m_connectionDetails,
                                    "POST",
                                    "/something"))
    .thenReturn(request);

    when(m_httpRecording.createBodyDataFileName()).thenReturn(file);

    final String message =
      "POST /something HTTP/1.0\r\n" +
      "\r\n";

    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    final byte[] buffer2 = new byte[0x8000];

    for (int i = 0; i < buffer2.length; i++) {
      buffer2[i] = (byte) i;
    }

    handler.handleRequest(buffer2, buffer2.length);

    handler.requestFinished(); // Force body to be flushed.

    assertEquals(file.getPath(), request.getBody().getFile());
    assertTrue(file.exists());
    assertTrue(file.canRead());
    assertEquals(0x8000, file.length());
    assertFalse(request.getBody().isSetBinary());
    assertFalse(request.getBody().isSetForm());
    assertFalse(request.getBody().isSetEscapedString());
  }

  @Test public void testRequestFileBody2() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_logger, m_regularExpressions,
        m_uriParser, m_attributeStringParser, null,
        m_commentSource, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("POST"));

    final File file = new File(getDirectory(), "formData");
    file.createNewFile();
    FileUtilities.setCanAccess(file, false);

    when(m_httpRecording.addRequest(m_connectionDetails,
                                    "POST",
                                    "/something"))
    .thenReturn(request);

    when(m_httpRecording.createBodyDataFileName()).thenReturn(file);

    final String message =
      "POST /something HTTP/1.0\r\n" +
      "\r\n";

    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    final byte[] buffer2 = new byte[0x8000];

    for (int i = 0; i < buffer2.length; i++) {
      buffer2[i] = (byte) i;
    }

    handler.handleRequest(buffer2, buffer2.length);

    handler.requestFinished(); // Force body to be flushed.

    assertFalse(request.getBody().isSetFile());
    assertFalse(request.getBody().isSetBinary());
    assertFalse(request.getBody().isSetForm());
    assertFalse(request.getBody().isSetEscapedString());

    verify(m_logger).error(contains("Failed to write body"),
                           isA(FileNotFoundException.class));
  }

  @Test public void testPartitionedRequest() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_logger, m_regularExpressions,
        m_uriParser, m_attributeStringParser, null,
        m_commentSource, m_connectionDetails);

    final byte[] message1Bytes = new String("G").getBytes("US-ASCII");
    handler.handleRequest(message1Bytes, message1Bytes.length);

    final byte[] message2Bytes =
        new String("ET blah HTTP/1.1\n").getBytes("US-ASCII");

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("GET"));

    when(m_httpRecording.addRequest(m_connectionDetails,
                                    "GET",
                                    "blah"))
      .thenReturn(request);

    handler.handleRequest(message2Bytes, message2Bytes.length);

    verify(m_httpRecording).addRequest(m_connectionDetails, "GET", "blah");
  }

  @Test public void testOverflowBuffer() {
    final ConnectionHandler handler =
        new ConnectionHandlerImplementation(
          m_httpRecording, m_logger, m_regularExpressions,
          m_uriParser, m_attributeStringParser, null,
          m_commentSource, m_connectionDetails);

    final byte[] buffer = new byte[0x10000];

    for (int i = 0; i < buffer.length; i++) {
      buffer[i] = (byte) i;
    }

    handler.handleRequest(buffer, buffer.length);

    verify(m_logger).error(contains("Filled buffer without matching"),
                           isA(BufferOverflowException.class));

    // Buffer has been cleared, so we can send more stuff.
    handler.handleRequest(buffer, 10);
  }

  @Test public void testWithBadResponseMessages() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_logger, m_regularExpressions,
        m_uriParser, m_attributeStringParser, null,
        m_commentSource, m_connectionDetails);

    // Response with no request.
    handler.handleResponse(new byte[0], 0);
    verify(m_logger).error(contains("No current request"));

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("GET"));

    when(m_httpRecording.addRequest(m_connectionDetails,
                                    "GET",
                                    "/"))
    .thenReturn(request);

    final String message = "GET / HTTP/1.1\r\n";
    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    // Responses that don't start with a standard response line are logged and
    // ignored.
    handler.handleResponse(new byte[0], 0);

    verify(m_logger).error(contains("No current response"));
  }
}
