// Copyright (C) 2006 - 2012 Philip Aston
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.slf4j.Logger;

import net.grinder.common.Closer;
import net.grinder.plugin.http.xml.BasicAuthorizationHeaderType;
import net.grinder.plugin.http.xml.BodyType;
import net.grinder.plugin.http.xml.ConflictingTokenReferenceType;
import net.grinder.plugin.http.xml.FormBodyType;
import net.grinder.plugin.http.xml.FormFieldType;
import net.grinder.plugin.http.xml.HeaderType;
import net.grinder.plugin.http.xml.RequestType;
import net.grinder.plugin.http.xml.ResponseType;
import net.grinder.plugin.http.xml.ResponseTokenReferenceType;
import net.grinder.plugin.http.xml.TokenReferenceType;
import net.grinder.plugin.http.xml.TokenResponseLocationType;
import net.grinder.plugin.http.xml.RequestType.Method.Enum;
import net.grinder.tools.tcpproxy.CommentSource;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.util.AttributeStringParser;
import net.grinder.util.StringEscaper;
import net.grinder.util.http.URIParser;
import HTTPClient.Codecs;
import HTTPClient.NVPair;
import HTTPClient.ParseException;

/**
 * Class that handles a particular connection.
 *
 * <p>
 * Multi-threaded calls for a given connection are serialised.
 * </p>
 *
 * @author Philip Aston
 */
final class ConnectionHandlerImplementation implements ConnectionHandler {

  private static final Set<Enum> HTTP_METHODS_WITH_BODY =
    new HashSet<Enum>(Arrays.asList(
        new RequestType.Method.Enum[] {
            RequestType.Method.OPTIONS,
            RequestType.Method.POST,
            RequestType.Method.PUT,
        }
      ));

  private final HTTPRecording m_httpRecording;

  private final Logger m_logger;

  private final RegularExpressions m_regularExpressions;

  private final URIParser m_uriParser;
  private final AttributeStringParser m_attributeStringParser;
  private final StringEscaper m_postBodyStringEscaper;
  private final CommentSource m_commentSource;

  private final ConnectionDetails m_connectionDetails;

  // Buffer should be at least as large as the HTTPProxyTCPProxyEngine buffer.
  // We've introduced buffers at this level to solve a specific issue
  // (bug 3484390). Later, we may push this up the API, and perhaps copy
  // directly from a socket channel into the buffer.
  private final ByteBuffer m_requestBuffer = ByteBuffer.allocate(40960);

  private Request m_request;

  public ConnectionHandlerImplementation(
    HTTPRecording httpRecording,
    Logger logger,
    RegularExpressions regularExpressions,
    URIParser uriParser,
    AttributeStringParser attributeStringParser,
    StringEscaper postBodyStringEscaper,
    CommentSource commentSource,
    ConnectionDetails connectionDetails) {

    m_httpRecording = httpRecording;
    m_logger = logger;
    m_regularExpressions = regularExpressions;
    m_uriParser = uriParser;
    m_attributeStringParser = attributeStringParser;
    m_postBodyStringEscaper = postBodyStringEscaper;
    m_commentSource = commentSource;
    m_connectionDetails = connectionDetails;
  }

  /**
   * {@inheritDoc}
   */
  @Override public synchronized void handleRequest(byte[] buffer, int length) {

    try {
      m_requestBuffer.put(buffer,  0, length);
    }
    catch (BufferOverflowException e) {
      m_logger.error("Filled buffer without matching request line", e);
      m_requestBuffer.clear();
      return;
    }

    m_requestBuffer.flip();

    // String used to parse headers - header names are US-ASCII encoded and
    // anchored to start of line. The correct character set to use for URL's
    // is not well defined by RFC 2616, so we use ISO8859_1. This way we are
    // at least non-lossy (US-ASCII maps characters above 0x7F to '?').
    final String asciiString;
    try {
      asciiString = new String(m_requestBuffer.array(),
                               0,
                               m_requestBuffer.remaining(),
                               "ISO8859_1");
    }
    catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }

    if (m_request != null && m_request.isComplete()) {
      requestFinished();
    }

    final Matcher matcher =
      m_regularExpressions.getRequestLinePattern().matcher(asciiString);

    if (matcher.find()) {
      // Packet is start of new request message.

      final String method = matcher.group(1);
      final String relativeURI = matcher.group(2);

      if (RequestType.Method.Enum.forString(method) != null) {
        requestFinished();

        m_request = new Request(method,
                                relativeURI,
                                m_commentSource.getComments());
      }
    }

    // Stuff we do whatever.

    if (m_request == null) {
      // We haven't received enough bytes to match the request line.
      m_requestBuffer
        .position(m_requestBuffer.limit())
        .limit(m_requestBuffer.capacity());

      return;
    }
    else if (m_request.getBody() != null) {
      m_request.getBody().write(m_requestBuffer.array(),
                                0,
                                m_requestBuffer.remaining());
      m_requestBuffer.clear();

      return;
    }

    // Still parsing headers.

    String headers = asciiString;
    final int bodyStart;

    final Matcher messageBodyMatcher =
      m_regularExpressions.getMessageBodyPattern().matcher(asciiString);

    if (messageBodyMatcher.find()) {
      bodyStart = messageBodyMatcher.start(1);
      headers = asciiString.substring(0, bodyStart);
    }
    else {
      bodyStart = -1;
    }

    final Matcher headerMatcher =
      m_regularExpressions.getHeaderPattern().matcher(headers);

    while (headerMatcher.find()) {
      final String name = headerMatcher.group(1);
      final String value = headerMatcher.group(2);

      if (m_httpRecording.getParameters().isMirroredHeader(name)) {
        m_request.addHeader(name, value);
      }

      if ("Content-Type".equalsIgnoreCase(name)) {
        m_request.setContentType(value);
      }

      if ("Content-Length".equalsIgnoreCase(name)) {
        m_request.setContentLength(Integer.parseInt(value));
      }
    }

    final Matcher authorizationMatcher =
      m_regularExpressions.getBasicAuthorizationHeaderPattern().matcher(
        headers);

    if (authorizationMatcher.find()) {
      m_request.addBasicAuthorization(authorizationMatcher.group(1));
    }

    // Write out the body after parsing the headers as we need to
    // know the content length.

    if (bodyStart > -1) {
      final int bodyLength = m_requestBuffer.remaining() - bodyStart;

      if (m_request.expectingBody()) {
        m_request.new RequestBody().write(m_requestBuffer.array(),
                                          bodyStart,
                                          bodyLength);
      }
      else {
        if (bodyLength != 0) {
          m_logger.warn("Not expecting body, found {} bytes for {}",
                        bodyLength, m_request);
        }

        m_request.setComplete();
      }
    }

    m_requestBuffer.clear();
  }

  public synchronized void handleResponse(byte[] buffer, int length) {

    if (m_request == null) {
      // We don't support pipelining.
      m_logger.error("UNEXPECTED - No current request");
      return;
    }

    // See notes in handleRequest about why we use this code page.
    final String asciiString;
    try {
      asciiString = new String(buffer, 0, length, "ISO8859_1");
    }
    catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }

    final Matcher matcher =
      m_regularExpressions.getResponseLinePattern().matcher(asciiString);

    if (matcher.find()) {
      // Packet is start of new response message.

      m_httpRecording.markLastResponseTime();

      m_request.addNewResponse(Integer.parseInt(matcher.group(1)),
                               matcher.group(2));
    }

    final Response response = m_request.getResponse();

    if (response == null) {
      m_logger.error("UNEXPECTED - No current response");
    }
    else if (response.getBody() != null) {
      response.getBody().write(buffer, 0, length);
    }
    else {
      // Still parsing headers.

      String headers = asciiString;
      int bodyStart = -1;

      if (m_request.expectingResponseBody()) {
        final Matcher messageBodyMatcher =
          m_regularExpressions.getMessageBodyPattern().matcher(asciiString);

        if (messageBodyMatcher.find()) {
          bodyStart = messageBodyMatcher.start(1);
          headers = asciiString.substring(0, bodyStart);
        }
      }

      final Matcher headerMatcher =
        m_regularExpressions.getHeaderPattern().matcher(headers);

      while (headerMatcher.find()) {
        final String name = headerMatcher.group(1);
        final String value = headerMatcher.group(2);

        if ("Location".equals(name)) {
          m_uriParser.parse(value, new URIParser.AbstractParseListener() {

            public boolean pathParameterNameValue(String name, String value) {
              response.addResponseTokenReference(
                name,
                value,
                TokenResponseLocationType
                .RESPONSE_LOCATION_HEADER_PATH_PARAMETER);

              return true;
            }

            public boolean queryStringNameValue(String name, String value) {
              response.addResponseTokenReference(
                name,
                value,
                TokenResponseLocationType
                .RESPONSE_LOCATION_HEADER_QUERY_STRING);

              return true;
            }
          });
        }
      }

      // Write out the body after parsing the headers for consistency with
      // handleRequest.
      if (bodyStart > -1) {
        response.new ResponseBody().write(
          buffer, bodyStart, length - bodyStart);
      }
    }
  }

  /**
   * Called when a new request message is expected.
   */
  public synchronized void requestFinished() {
    if (m_request != null) {
      m_request.end();
      m_request = null;
    }
  }

  private final class Request {
    private final RequestType m_requestXML;

    private int m_contentLength = -1;
    private String m_contentType = null;
    private RequestBody m_body;
    private boolean m_complete;

    private Response m_response = null;

    public Request(String method, String relativeURI, String[] userComments) {
      m_requestXML =
        m_httpRecording.addRequest(m_connectionDetails, method, relativeURI);

      m_logger.debug("Request started {}", m_requestXML);

      //add the user comments to the request element
      for (int i = 0; i < userComments.length; i++) {
        m_requestXML.addComment(userComments[i]);
      }
    }

    public void addNewResponse(int statusCode, String reasonPhrase) {
      final ResponseType responseXML = m_requestXML.addNewResponse();
      responseXML.setStatusCode(statusCode);
      responseXML.setReasonPhrase(reasonPhrase);

      m_response = new Response(responseXML);
    }

    public Response getResponse() {
      return m_response;
    }

    public void addBasicAuthorization(String base64) {
      final String decoded = Codecs.base64Decode(base64);

      final int colon = decoded.indexOf(":");

      if (colon < 0) {
        m_logger.error("Could not decode Authorization header");
      }
      else {
        final BasicAuthorizationHeaderType basicAuthorization =
          m_requestXML.getHeaders().addNewAuthorization().addNewBasic();

        basicAuthorization.setUserid(decoded.substring(0, colon));
        basicAuthorization.setPassword(decoded.substring(colon + 1));
      }
    }

    public RequestBody getBody() {
      return m_body;
    }

    public boolean expectingBody() {
      return HTTP_METHODS_WITH_BODY.contains(m_requestXML.getMethod());
    }

    public boolean expectingResponseBody() {
      // RFC 2616, 4.3.
      if (m_requestXML.getMethod().equals(RequestType.Method.HEAD)) {
        return false;
      }

      final int status = m_requestXML.getResponse().getStatusCode();

      return status >= 200 &&
             status != HttpURLConnection.HTTP_NO_CONTENT &&
             status != HttpURLConnection.HTTP_NOT_MODIFIED;
    }

    public void setContentType(String contentType) {
      m_contentType = contentType;
    }

    public void setContentLength(int contentLength) {
      m_contentLength = contentLength;
    }

    public void addHeader(String name, String value) {
      final HeaderType header = m_requestXML.getHeaders().addNewHeader();
      header.setName(name);
      header.setValue(value);
    }

    public void end() {
      if (getBody() != null) {
        getBody().end();
      }

      if (getResponse() != null) {
        getResponse().end();
      }

      m_logger.debug("Request finished {}", m_requestXML);
    }

    private class RequestBody extends AbstractBody {
      public RequestBody() {
        assert m_body == null;
        m_body = this;
      }

      public void write(byte[] bytes, int start, int length) {
        // findbugs can't know that we're only called with the
        // ConnectionHandlerImplementation
        // synchronised. Help it out by explicitly locking.
        synchronized (ConnectionHandlerImplementation.this) {
          final int lengthToWrite;

          if (m_contentLength != -1 &&
              length > m_contentLength - getSize()) {

            m_logger.error("Expected content length exceeded, truncating");
            lengthToWrite = m_contentLength - getSize();
          }
          else {
            lengthToWrite = length;
          }

          super.write(bytes, start, lengthToWrite);

          // We mark the request as finished if we've reached the specified
          // Content-Length. We rely on next message or connection close
          // event to flush the data, this allows us to parse the response. We
          // also rely on these events if no Content-Length is specified.
          if (m_contentLength != -1 &&
              getSize() >= m_contentLength) {

            m_request.setComplete();
          }
        }
      }

      public void end() {
        final BodyType body = m_requestXML.addNewBody();

        final boolean isFormData;
        final boolean isMultipart;

        if (m_contentType != null) {
          body.setContentType(m_contentType);
          isMultipart = m_contentType.startsWith("multipart/form-data");
          isFormData =
            isMultipart ||
            m_contentType.startsWith("application/x-www-form-urlencoded");
        }
        else {
          isFormData = false;
          isMultipart = false;
        }

        final byte[] bytes = toByteArray();

        if (bytes.length > 0x4000 && !isFormData ||
            bytes.length > 0x40000) {
          // Large amount of data, use a file.
          final File file = m_httpRecording.createBodyDataFileName();

          FileOutputStream dataStream = null;

          try {
            dataStream = new FileOutputStream(file);
            dataStream.write(bytes, 0, bytes.length);

            body.setFile(file.getPath());
          }
          catch (IOException e) {
            m_logger.error("Failed to write body data to '" + file + "'", e);
          }
          finally {
            Closer.close(dataStream);
          }
        }
        else {
          // Basic handling of strings; should use content character encoding.
          final String iso88591String;

          try {
            iso88591String = new String(bytes, "ISO8859_1");
          }
          catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
          }

          if (isFormData) {
            try {
              final NVPair[] formNameValuePairs =
                isMultipart ?
                    Codecs.mpFormDataDecode(bytes, m_contentType, "/tmp") :
                    Codecs.query2nv(iso88591String);

              final FormBodyType formData = body.addNewForm();
              formData.setMultipart(isMultipart);

              for (int i = 0; i < formNameValuePairs.length; ++i) {
                final String name = formNameValuePairs[i].getName();
                final String value = formNameValuePairs[i].getValue();

                // We only tokenise form attributes that we've seen in a
                // previous hidden input field.
                if (m_httpRecording.tokenReferenceExists(
                      name,
                      TokenResponseLocationType
                      .RESPONSE_BODY_HIDDEN_INPUT.toString())) {

                  final TokenReferenceType tokenReference =
                    formData.addNewTokenReference();
                  m_httpRecording.setTokenReference(
                    name, value, tokenReference);
                }
                else {
                  final FormFieldType formField = formData.addNewFormField();
                  formField.setName(name);
                  formField.setValue(value);
                }
              }
            }
            catch (ParseException e) {
              // Failed to parse form data as name-value pairs, we'll
              // treat it as raw data instead.
            }
            catch (IOException e) {
              // Failed to parse multipart form data, we'll
              // treat it as raw data instead.
            }
          }

          if (body.getForm() == null) {
            boolean looksLikeAnExtendedASCIIString = true;

            for (int i = 0; i < bytes.length; ++i) {
              final char c = iso88591String.charAt(i);

              if (Character.isISOControl(c) && !Character.isWhitespace(c)) {
                looksLikeAnExtendedASCIIString = false;
                break;
              }
            }

            if (looksLikeAnExtendedASCIIString) {
              body.setEscapedString(
                m_postBodyStringEscaper.escape(iso88591String));
            }
            else {
              body.setBinary(bytes);
            }
          }
        }
      }
    }

    public void setComplete() {
      m_complete = true;
    }

    public boolean isComplete() {
      return m_complete;
    }
  }

  private final class Response {

    private final ResponseType m_responseXML;
    private ResponseBody m_body;
    private final Map<String, ResponseTokenReferenceType> m_tokensInResponseMap
     = new HashMap<String, ResponseTokenReferenceType>();

    public Response(ResponseType responseXML) {
      m_responseXML = responseXML;
    }

    public ResponseBody getBody() {
      return m_body;
    }

    public void end() {
      if (getBody() != null) {
        getBody().end();
      }
    }

    public void addResponseTokenReference(String name, String value,
      TokenResponseLocationType.Enum source) {

      final ResponseTokenReferenceType existingTokenReference =
        m_tokensInResponseMap.get(name);

      if (existingTokenReference == null) {
        final ResponseTokenReferenceType newTokenReference =
          m_responseXML.addNewTokenReference();

        newTokenReference.setSource(source.toString());
        m_httpRecording.setTokenReference(name, value, newTokenReference);

        m_tokensInResponseMap.put(name, newTokenReference);
      }
      else {
        if (m_httpRecording.getLastValueForToken(name).equals(value)) {
          // Can't simply check existingTokenReference.getNewValue() as it
          // may not be set.
          return;
        }

        final ConflictingTokenReferenceType conflictingValue =
          existingTokenReference.addNewConflictingValue();

        conflictingValue.setValue(value);
        conflictingValue.setSource(source.toString());
      }
    }

    private class ResponseBody extends AbstractBody {
      public ResponseBody() {
        assert m_body == null;
        m_body = this;
      }

      public void end() {
        // Parse body for href="<url>" patterns containing URL tokens. We could
        // choose to do this only for certain content types, (probably just
        // text/html) but its better to catch too many tokens than too few.

        // This ought to respect content character encoding.
        final String iso85591String;

        try {
          iso85591String = new String(toByteArray(), "ISO8859_1");
        }
        catch (UnsupportedEncodingException e) {
          throw new AssertionError(e);
        }

        final Matcher uriMatcher =
          m_regularExpressions.getHyperlinkURIPattern().matcher(iso85591String);

        while (uriMatcher.find()) {
          m_uriParser.parse(
            uriMatcher.group(1),
            new URIParser.AbstractParseListener() {

              public boolean pathParameterNameValue(String name, String value) {
                addResponseTokenReference(
                  name,
                  value,
                  TokenResponseLocationType.RESPONSE_BODY_URI_PATH_PARAMETER);

                return true;
              }

              public boolean queryStringNameValue(String name, String value) {
                addResponseTokenReference(name, value,
                  TokenResponseLocationType.RESPONSE_BODY_URI_QUERY_STRING);

                return true;
              }
            });
        }

        final Matcher hiddenParameterMatcher = m_regularExpressions
            .getHiddenInputPattern().matcher(iso85591String);

        while (hiddenParameterMatcher.find()) {
          final AttributeStringParser.AttributeMap map =
            m_attributeStringParser.parse(hiddenParameterMatcher.group());

          final String name = map.get("name");
          final String value = map.get("value");

          if (name != null && value != null) {
            addResponseTokenReference(
              name,
              value,
              TokenResponseLocationType.RESPONSE_BODY_HIDDEN_INPUT);
          }
        }
      }
    }
  }

  private abstract static class AbstractBody {
    private final ByteArrayOutputStream m_entityBodyByteStream =
      new ByteArrayOutputStream();

    public void write(byte[] bytes, int start, int length) {
      m_entityBodyByteStream.write(bytes, start, length);
    }

    public abstract void end();

    protected final int getSize() {
      return m_entityBodyByteStream.size();
    }

    protected final byte[] toByteArray() {
      return m_entityBodyByteStream.toByteArray();
    }
  }
}
