// Copyright (C) 2005 - 2012 Philip Aston
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

import static java.util.Collections.emptySet;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import net.grinder.common.GrinderBuild;
import net.grinder.plugin.http.xml.BaseURIType;
import net.grinder.plugin.http.xml.CommonHeadersType;
import net.grinder.plugin.http.xml.HTTPRecordingType;
import net.grinder.plugin.http.xml.HeaderType;
import net.grinder.plugin.http.xml.HeadersType;
import net.grinder.plugin.http.xml.MethodType;
import net.grinder.plugin.http.xml.PageType;
import net.grinder.plugin.http.xml.ParsedURIPartType;
import net.grinder.plugin.http.xml.RelativeURIType;
import net.grinder.plugin.http.xml.RequestType;
import net.grinder.plugin.http.xml.ResponseType;
import net.grinder.plugin.http.xml.TokenReferenceType;
import net.grinder.plugin.http.xml.TokenType;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.EndPoint;
import net.grinder.util.Pair;
import net.grinder.util.http.URIParser;

import org.picocontainer.Disposable;
import org.slf4j.Logger;

import HTTPClient.ParseException;
import HTTPClient.URI;

/**
 * Contains common state for HTTP recording.
 *
 * @author Philip Aston
 */
public class HTTPRecordingImplementation implements HTTPRecording, Disposable {

  private final HTTPRecordingType m_recording;
  private final HTTPRecordingParameters m_parameters;
  private final Logger m_logger;
  private final HTTPRecordingResultProcessor m_resultProcessor;
  private final RegularExpressions m_regularExpressions;
  private final URIParser m_uriParser;

  private final IntGenerator m_bodyFileIDGenerator = new IntGenerator();
  private final BaseURLMap m_baseURLMap = new BaseURLMap();
  private final RequestList m_requestList = new RequestList();
  private final TokenMap m_tokenMap = new TokenMap();

  private long m_lastResponseTime = 0;

  /**
   * Constructor.
   *
   * @param parameters         Recording parameters.
   * @param resultProcessor    Component which handles result.
   * @param logger             A logger.
   * @param regularExpressions Compiled regular expressions.
   * @param uriParser          A URI parser.
   */
  public HTTPRecordingImplementation(HTTPRecordingParameters parameters, HTTPRecordingResultProcessor resultProcessor,
      Logger logger, RegularExpressions regularExpressions, URIParser uriParser) {

    m_parameters = parameters;
    m_resultProcessor = resultProcessor;
    m_logger = logger;
    m_regularExpressions = regularExpressions;
    m_uriParser = uriParser;

    final HTTPRecordingType.Metadata httpRecording = new HTTPRecordingType.Metadata();
    m_recording = new HTTPRecordingType();
    m_recording.setMetadata(httpRecording);

    httpRecording.setVersion("The Grinder " + GrinderBuild.getVersionString());
    httpRecording.setTime(getTime());
    httpRecording.setTestNumberOffset(m_parameters.getTestNumberOffset());
  }

   XMLGregorianCalendar getTime() {
    try {
      DatatypeFactory dtf = DatatypeFactory.newInstance();
      return dtf.newXMLGregorianCalendar(
              Calendar.getInstance().get(Calendar.YEAR),
              Calendar.getInstance().get(Calendar.MONTH) + 1,
              Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
              Calendar.getInstance().get(Calendar.HOUR),
              Calendar.getInstance().get(Calendar.MINUTE),
              Calendar.getInstance().get(Calendar.SECOND),
              Calendar.getInstance().get(Calendar.MILLISECOND),
              Calendar.getInstance().get(Calendar.ZONE_OFFSET) / (1000 * 60));
  } catch (DatatypeConfigurationException e) {
      m_logger.error(e.getMessage());
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HTTPRecordingParameters getParameters() {
    return m_parameters;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestType addRequest(
    ConnectionDetails connectionDetails, String method, String relativeURI) {

    final RequestType request = m_requestList.add();
    request.setTime(getTime());

    synchronized (this) {
      if (m_lastResponseTime > 0) {
        // We only want to record a sleep time for the first request after
        // a response.
        final long time = System.currentTimeMillis() - m_lastResponseTime;

        if (time > 10) {
          request.setSleepTime(time);
        }
      }

      m_lastResponseTime = 0;
    }


    request.setHeaders(new HeadersType());

    request.setMethod(MethodType.fromValue(method));

    String unescapedURI;

    try {
      unescapedURI = URI.unescape(relativeURI, null);
    }
    catch (ParseException e) {
      unescapedURI = relativeURI;
    }

    final Matcher lastPathElementMatcher =
      m_regularExpressions.getLastPathElementPathPattern().matcher(
        unescapedURI);

    final String description;

    if (lastPathElementMatcher.find()) {
      final String element = lastPathElementMatcher.group(1);

      if (element.trim().length() != 0) {
        description = method + " " + element;
      }
      else {
        description = method + " /";
      }
    }
    else {
      description = method + " " + relativeURI;
    }

    request.setDescription(description);

    final RelativeURIType uri = new RelativeURIType();
    request.setUri(uri);

    uri.setUnparsed(unescapedURI);

    uri.setExtends(
      m_baseURLMap.getBaseURL(
        connectionDetails.isSecure() ? "https" : "http",
        connectionDetails.getRemoteEndPoint()).getUriId());

    final ParsedURIPartType parsedPath = new ParsedURIPartType();
    uri.setPath(parsedPath);
    final ParsedURIPartType parsedQueryString = new ParsedURIPartType();
    uri.setQueryString(parsedQueryString);
    final String[] fragment = new String[1];

    // Look for tokens in path parameters and query string.
    m_uriParser.parse(relativeURI, new URIParser.AbstractParseListener() {

      public boolean path(String path) {
        QName qName = new QName("http://grinder.sourceforge.net/tcpproxy/http/1.0", "text");
        parsedPath.getTextAndTokenReference().add(new JAXBElement<String>(qName, String.class, path));
        return true;
      }

      public boolean pathParameterNameValue(String name, String value) {
        TokenReferenceType newTokenRef = new TokenReferenceType();
        QName qName = new QName("http://grinder.sourceforge.net/tcpproxy/http/1.0", "token-reference");
        parsedPath.getTextAndTokenReference().add(new JAXBElement<TokenReferenceType>(qName, TokenReferenceType.class, newTokenRef));
        setTokenReference(
          name, value, newTokenRef);
        return true;
      }

      public boolean queryString(String queryString) {
        QName qName = new QName("http://grinder.sourceforge.net/tcpproxy/http/1.0", "text");
        parsedQueryString.getTextAndTokenReference().add(new JAXBElement<String>(qName, String.class, queryString));
        return true;
      }

      public boolean queryStringNameValue(String name, String value) {
        TokenReferenceType newTokenRef = new TokenReferenceType();
        QName qName = new QName("http://grinder.sourceforge.net/tcpproxy/http/1.0", "token-reference");
        parsedQueryString.getTextAndTokenReference().add(new JAXBElement<TokenReferenceType>(qName, TokenReferenceType.class, newTokenRef));
        setTokenReference(
          name, value, newTokenRef);
        return true;
      }

      public boolean fragment(String theFragment) {
        fragment[0] = theFragment;
        return true;
      }
    });

    if (parsedQueryString.getTextAndTokenReference().size() ==  0) {
      uri.setQueryString(null);
    }

    if (fragment[0] != null) {
      uri.setFragment(fragment[0]);
    }

    return request;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void markLastResponseTime() {
    synchronized (this) {
      m_lastResponseTime = System.currentTimeMillis();
    }
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void setTokenReference(
    String name, String value, TokenReferenceType tokenReference) {
    m_tokenMap.add(name, value, tokenReference);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getLastValueForToken(String name) {
    return m_tokenMap.getLastValue(name);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public boolean tokenReferenceExists(String name, String source) {
    return m_tokenMap.exists(name, source);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public File createBodyDataFileName() {
    return new File("http-data-" + m_bodyFileIDGenerator.next() + ".dat");
  }

  /**
   * Called after the component has been stopped.
   */
  public void dispose() {
    StringWriter xml = new StringWriter();
    JAXB.marshal(m_recording, xml);
    StringReader reader = new StringReader(xml.toString());
    HTTPRecordingType result = JAXB.unmarshal(reader, HTTPRecordingType.class);
    m_requestList.record(result);

    final List<RequestType> requests = new ArrayList<RequestType>();

    for (PageType page : result.getPage()) {
      requests.addAll(page.getRequest());
    }

    extractCommonHeaders(requests, result);

    // Find default headers that are present in all common headers, or
    // request headers that don't extend common headers.

    final List<CommonHeadersType> commonHeaders =
      result.getCommonHeaders();

    final List<HeadersType> defaultHeaderSources =
        new ArrayList<HeadersType>(commonHeaders.size());

    for (HeadersType header : commonHeaders) {
      defaultHeaderSources.add(header);
    }

    for (RequestType request : requests) {
      final HeadersType headers = request.getHeaders();

      if (headers.getExtends() != null) {
        defaultHeaderSources.add(headers);
      }
    }

    final Set<Pair<String, String>> defaultHeaders =
        findSharedHeaders(defaultHeaderSources);

    if (defaultHeaders.size() > 0) {
      final List<CommonHeadersType> newCommonHeaders =
        new ArrayList<CommonHeadersType>(commonHeaders.size() + 1);

      final CommonHeadersType defaultHeadersXML = new CommonHeadersType();
      defaultHeadersXML.setHeadersId("defaultHeaders");
      newCommonHeaders.add(defaultHeadersXML);

      for (Pair<String, String> defaultHeader : defaultHeaders) {
        final HeaderType header = new HeaderType();
        defaultHeadersXML.getHeaderOrAuthorization().add(header);
        header.setName(defaultHeader.getFirst());
        header.setValue(defaultHeader.getSecond());
      }

      // There can be at most one CommonHeaders that is the same as the
      // default headers. If we find it, we remove it.
      String emptyCommonHeadersID = null;

      for (CommonHeadersType headers : commonHeaders) {
        removeDefaultHeaders(defaultHeaders,
                             defaultHeadersXML.getHeadersId(),
                             headers);

        if (headers.getHeaderOrAuthorization().size() == 0) {
          assert emptyCommonHeadersID == null;
          emptyCommonHeadersID = headers.getHeadersId();
        }
        else {
          newCommonHeaders.add(headers);
        }
      }

      result.getCommonHeaders().addAll(newCommonHeaders);

      for (RequestType request : requests) {
        final HeadersType headers = request.getHeaders();

        if (headers.getExtends() != null) {
          removeDefaultHeaders(defaultHeaders,
                               defaultHeadersXML.getHeadersId(),
                               headers);
        }
        else {
          // Fix up any references to the CommonHeaders that duplicated the
          // defaultHeaders.
          if (headers.getExtends().equals(emptyCommonHeadersID)) {
            headers.setExtends(defaultHeadersXML.getHeadersId());
          }
        }
      }
    }

    try {
      m_resultProcessor.process(result);
    }
    catch (IOException e) {
      m_logger.error(e.getMessage(), e);
    }
  }

  private void extractCommonHeaders(List<RequestType> requests,
                                    HTTPRecordingType httpRecording) {

    // Build identity map of Request to (Common, Uncommon) and map containing
    // the number of times each set of common headers is referenced.
    final Map<RequestType, Pair<CommonHeadersType, HeadersType>>
      parsedRequests =
      new IdentityHashMap<RequestType, Pair<CommonHeadersType, HeadersType>>();

    final Map<String, Integer> commonHeadersCountByValue =
        new HashMap<String, Integer>();

    final Map<String, CommonHeadersType> uniqueCommonHeaders =
        new HashMap<String, CommonHeadersType>();

    for (RequestType request : requests) {
      final CommonHeadersType commonHeaders =
          new CommonHeadersType();

      final HeadersType uncommonHeaders = new HeadersType();

      final List<Object> children = request.getHeaders().getHeaderOrAuthorization();

      for (int i = 0; i < children.size(); ++i) {
        if (children.get(i) instanceof HeaderType) {
          final HeaderType header = (HeaderType)children.get(i);

          if (m_parameters.isCommonHeader(header.getName())) {
            commonHeaders.getHeaderOrAuthorization().add(header);
          }
          else {
            uncommonHeaders.getHeaderOrAuthorization().add(header);
          }
        }
        else {
          uncommonHeaders.getHeaderOrAuthorization().add(children.get(i));
        }
      }

      // Key that ignores ID.
      String key = "";
      for (Object object : commonHeaders.getHeaderOrAuthorization()) {
        if (object instanceof HeaderType) {
          HeaderType header = (HeaderType)object;
          key += header.getName() + "=" + header.getValue() + " ";
        }
      }

      final CommonHeadersType existing = uniqueCommonHeaders.get(key);

      final CommonHeadersType theCommonHeaders;

      if (existing != null) {
        theCommonHeaders = existing;
      }
      else {
        uniqueCommonHeaders.put(key, commonHeaders);
        theCommonHeaders = commonHeaders;
      }

      parsedRequests.put(request, Pair.of(theCommonHeaders, uncommonHeaders));

      final Integer count = commonHeadersCountByValue.get(key);
      final int oldCount = count != null ? count.intValue() : 0;
      commonHeadersCountByValue.put(key, oldCount + 1);
    }

    // Now extract common headers if more than one request refers to them.
    final IntGenerator idGenerator = new IntGenerator();

    for (RequestType request : requests) {
      final Pair<CommonHeadersType, HeadersType> pair =
          parsedRequests.get(request);

      final CommonHeadersType commonHeaders = pair.getFirst();
      final HeadersType uncommonHeaders = pair.getSecond();


      String key = "";
      for (Object object : commonHeaders.getHeaderOrAuthorization()) {
        if (object instanceof HeaderType) {
          HeaderType header = (HeaderType)object;
          key += header.getName() + "=" + header.getValue() + " ";
        }
      }
      
      if (commonHeaders.getHeaderOrAuthorization().size() > 0 &&
          commonHeadersCountByValue.get(key).intValue() > 1) {
        if (commonHeaders.getHeadersId() == null) {
          commonHeaders.setHeadersId("headers" + idGenerator.next());

          httpRecording.getCommonHeaders().add(commonHeaders);
        }

        uncommonHeaders.setExtends(commonHeaders.getHeadersId());

        request.setHeaders(uncommonHeaders);
      }
    }
  }

  /**
   * Find the intersection of (matching name and value) from a set of headers.
   * If there are less than two sets of headers in headersList, the result
   * is empty.
   *
   * @param headersList
   *          Headers to search.
   * @return Shared headers.
   */
  private static Set<Pair<String, String>>
    findSharedHeaders(List<HeadersType> headersList) {

    if (headersList.size() < 2) {
      return emptySet();
    }

    final List<Object> oneHeaders =
        headersList.remove(headersList.size() - 1).getHeaderOrAuthorization();

    final Set<Pair<String, String>> sharedHeaders =
        new HashSet<Pair<String, String>>(oneHeaders.size());

    for (Object object : oneHeaders) {
      if (object instanceof HeaderType) {
        HeaderType header = (HeaderType)object;
        sharedHeaders.add(Pair.of(header.getName(), header.getValue()));
      }
    }

    for (HeadersType headers : headersList) {
      final List<Object> h = headers.getHeaderOrAuthorization();

      final Set<Pair<String, String>> sharedHeadersCopy =
          new HashSet<Pair<String, String>>(sharedHeaders);

      for (int j = 0; j < h.size(); ++j) {
        if (h.get(j) instanceof HeaderType) {
          HeaderType header = (HeaderType)h.get(j);
          final String name = header.getName();
          final String value = header.getValue();
          sharedHeadersCopy.remove(Pair.of(name, value));
        }
      }

      sharedHeaders.removeAll(sharedHeadersCopy);
    }

    return sharedHeaders;
  }

  /**
   * Remove default headers.
   *
   * @param defaultHeaders Headers to remove.
   * @param defaultHeadersID
   * @param headers The headers. Mutated in place.
   */
  private static void removeDefaultHeaders(
        Set<Pair<String, String>> defaultHeaders,
        String defaultHeadersID,
        HeadersType headers) {

    final List<Object> headersArray = headers.getHeaderOrAuthorization();

    final List<Integer> defaultHeaderIndexes =
        new ArrayList<Integer>(defaultHeaders.size());

    for (int i = headersArray.size()- 1; i >= 0; --i) {
      if (headersArray.get(i) instanceof HeaderType) {
        HeaderType header = (HeaderType)headersArray.get(i);
        if (defaultHeaders.contains(Pair.of(header.getName(),
                                            header.getValue()))) {
          defaultHeaderIndexes.add(i);
        }
      }
    }

    assert defaultHeaderIndexes.size() == defaultHeaders.size();

    for (int index : defaultHeaderIndexes) {
      headers.getHeaderOrAuthorization().remove(index);
    }

    headers.setExtends(defaultHeadersID);
  }

  private final class BaseURLMap {
    private final Map<String, BaseURIType> m_map =
      new HashMap<String, BaseURIType>();

    private final IntGenerator m_idGenerator = new IntGenerator();

    public BaseURIType getBaseURL(
      String scheme, EndPoint endPoint) {

      final String key = scheme.toString() + "://" + endPoint;

      synchronized (m_map) {
        final BaseURIType existing = m_map.get(key);

        if (existing != null) {
          return existing;
        }

        final BaseURIType result;

        synchronized (m_recording) {
          result = new BaseURIType();
          m_recording.getBaseUri().add(result);
        }

        result.setUriId("url" + m_idGenerator.next());
        result.setScheme(scheme);
        result.setHost(endPoint.getHost());
        result.setPort(endPoint.getPort());

        m_map.put(key, result);

        return result;
      }
    }
  }

  private final class RequestList {
    private final List<RequestType> m_requests = new ArrayList<RequestType>();
    private final Pattern m_resourcePathPattern = Pattern.compile(
      ".*(?:\\.css|\\.gif|\\.ico|\\.jpe?g|\\.js|\\.png)(?:\\?.*)?$",
      Pattern.CASE_INSENSITIVE);

    public RequestType add() {
      final RequestType request = new RequestType();

      synchronized (m_requests) {
        m_requests.add(request);
      }

      return request;
    }

    public void record(HTTPRecordingType httpRecording) {
      synchronized (m_requests) {
        m_logger.debug("Recording {} requests", m_requests.size());

        String lastBaseURI = null;
        boolean lastResponseWasRedirect = false;

        PageType currentPage = null;

        for (RequestType request : m_requests) {
          final ResponseType response = request.getResponse();

          if (response == null) {
            m_logger.debug("Skipping due to no response: {}", request);
            continue;
          }

          synchronized (m_recording) {
            // Crude but effective pagination heuristics.
            if (!request.getUri().getExtends().equals(lastBaseURI) ||
                request.getBody() != null ||
                !(m_resourcePathPattern.matcher(request.getUri().getUnparsed())
                     .matches() ||
                  lastResponseWasRedirect) ||
                currentPage == null) {
              currentPage = new PageType();
              httpRecording.getPage().add(currentPage);
            }

            lastBaseURI = request.getUri().getExtends();

            if (response.getStatusCode() != null)  {
              switch (response.getStatusCode()) {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case 307:
                  lastResponseWasRedirect = true;

                  request.setAnnotation(
                    "Expecting " + response.getStatusCode() +
                    " '" + response.getReasonPhrase() + "'");
                  break;
                default:
                  lastResponseWasRedirect = false;
              }
            }
            else {
              lastResponseWasRedirect = false;
            }

            currentPage.getRequest().add(request);
          }
        }
      }
    }
  }

  /**
   * Responsible for tokens at the recording level. Generates unique token
   * names. Tracks the last value for a particular token name, allowing the
   * "newValue" attribute of token references to be set appropriately. Token
   * names are deemed to have global (recording) scope; a simple model that
   * might not be right for every use case.
   */
  private final class TokenMap {
    private final Map<String, TokenLastValuePair> m_map =
      new HashMap<String, TokenLastValuePair>();
    private final Map<String, Integer> m_uniqueTokenIDs =
      new HashMap<String, Integer>();

    public void add(
      String name, String value, TokenReferenceType tokenReference) {

      final TokenLastValuePair tokenValuePair;

      synchronized (m_map) {
        final TokenLastValuePair existing = m_map.get(name);

        if (existing == null) {
          final TokenType newToken;

          synchronized (m_recording) {
            newToken = new TokenType();
            m_recording.getToken().add(newToken);
          }

          // Build a tokenID that is also a reasonable identifier.
          final StringBuilder tokenID = new StringBuilder();
          tokenID.append("token_");

          for (int i = 0; i < name.length(); ++i) {
            final char c = name.charAt(i);

            // Python is quite restrictive on what it allows in identifiers.
            if (c >= 'A' && c <= 'Z' ||
                c >= 'a' && c <= 'z' ||
                c >= '0' && c <= '9' ||
                c == '_') {
              tokenID.append(c);
            }
          }

          final String partToken = tokenID.toString();
          final Integer existingValue = m_uniqueTokenIDs.get(partToken);

          if (existingValue != null) {
            tokenID.append(existingValue);
            m_uniqueTokenIDs.put(partToken,
                                 existingValue.intValue() + 1);
          }
          else {
            m_uniqueTokenIDs.put(partToken, 2);
          }

          newToken.setTokenId(tokenID.toString());
          newToken.setName(name);

          tokenValuePair = new TokenLastValuePair(newToken);
          m_map.put(name, tokenValuePair);
        }
        else {
          tokenValuePair = existing;
        }
      }

      tokenReference.setTokenId(tokenValuePair.getToken().getTokenId());

      if (!value.equals(tokenValuePair.getLastValue())) {
        tokenReference.setNewValue(value);
        tokenValuePair.setLastValue(value);
      }

      tokenValuePair.addSource(tokenReference.getSource());
    }

    public String getLastValue(String name) {

      final TokenLastValuePair existing;

      synchronized (m_map) {
        existing = m_map.get(name);
      }

      return existing != null ? existing.getLastValue() : null;
    }

    public boolean exists(String name, String source) {

      final TokenLastValuePair existing;

      synchronized (m_map) {
        existing = m_map.get(name);
      }

      return existing != null && existing.hasAReferenceWithSource(source);
    }
  }

  private static final class TokenLastValuePair {
    private final TokenType m_token;
    private final Set<String> m_sources = new HashSet<String>();
    private String m_lastValue;

    public TokenLastValuePair(TokenType token) {
      m_token = token;
    }

    public TokenType getToken() {
      return m_token;
    }

    public void setLastValue(String lastValue) {
      m_lastValue = lastValue;
    }

    public String getLastValue() {
      return m_lastValue;
    }

    public void addSource(String source) {
      m_sources.add(source);
    }

    public boolean hasAReferenceWithSource(String source) {
      return m_sources.contains(source);
    }
  }
}
