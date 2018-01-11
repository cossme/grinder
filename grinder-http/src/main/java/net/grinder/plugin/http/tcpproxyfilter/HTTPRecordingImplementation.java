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
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.grinder.common.GrinderBuild;
import net.grinder.plugin.http.xml.BaseURIType;
import net.grinder.plugin.http.xml.CommonHeadersType;
import net.grinder.plugin.http.xml.HTTPRecordingType;
import net.grinder.plugin.http.xml.HeaderType;
import net.grinder.plugin.http.xml.HeadersType;
import net.grinder.plugin.http.xml.HttpRecordingDocument;
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

import org.apache.xmlbeans.XmlObject;
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

  private final HTTPRecordingParameters m_parameters;
  private final HttpRecordingDocument m_recordingDocument =
    HttpRecordingDocument.Factory.newInstance();
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
   * @param parameters
   *          Recording parameters.
   * @param resultProcessor
   *          Component which handles result.
   * @param logger
   *          A logger.
   * @param regularExpressions
   *          Compiled regular expressions.
   * @param uriParser
   *          A URI parser.
   */
  public HTTPRecordingImplementation(
    HTTPRecordingParameters parameters,
    HTTPRecordingResultProcessor resultProcessor,
    Logger logger,
    RegularExpressions regularExpressions,
    URIParser uriParser) {

    m_parameters = parameters;
    m_resultProcessor = resultProcessor;
    m_logger = logger;
    m_regularExpressions = regularExpressions;
    m_uriParser = uriParser;

    final HTTPRecordingType.Metadata httpRecording =
      m_recordingDocument.addNewHttpRecording().addNewMetadata();

    httpRecording.setVersion("The Grinder " + GrinderBuild.getVersionString());
    httpRecording.setTime(Calendar.getInstance());
    httpRecording.setTestNumberOffset(m_parameters.getTestNumberOffset());
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
    request.setTime(Calendar.getInstance());

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

    request.addNewHeaders();

    request.setMethod(RequestType.Method.Enum.forString(method));

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

    final RelativeURIType uri = request.addNewUri();

    uri.setUnparsed(unescapedURI);

    uri.setExtends(
      m_baseURLMap.getBaseURL(
        connectionDetails.isSecure() ?
          BaseURIType.Scheme.HTTPS : BaseURIType.Scheme.HTTP,
        connectionDetails.getRemoteEndPoint()).getUriId());

    final ParsedURIPartType parsedPath = uri.addNewPath();
    final ParsedURIPartType parsedQueryString = uri.addNewQueryString();
    final String[] fragment = new String[1];

    // Look for tokens in path parameters and query string.
    m_uriParser.parse(relativeURI, new URIParser.AbstractParseListener() {

      public boolean path(String path) {
        parsedPath.addText(path);
        return true;
      }

      public boolean pathParameterNameValue(String name, String value) {
        setTokenReference(
          name, value, parsedPath.addNewTokenReference());
        return true;
      }

      public boolean queryString(String queryString) {
        parsedQueryString.addText(queryString);
        return true;
      }

      public boolean queryStringNameValue(String name, String value) {
        setTokenReference(
          name, value, parsedQueryString.addNewTokenReference());
        return true;
      }

      public boolean fragment(String theFragment) {
        fragment[0] = theFragment;
        return true;
      }
    });

    if (parsedQueryString.getTokenReferenceArray().length == 0 &&
        parsedQueryString.getTextArray().length ==  0) {
      uri.unsetQueryString();
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
    final HttpRecordingDocument result;

    synchronized (m_recordingDocument) {
      result = (HttpRecordingDocument)m_recordingDocument.copy();
    }

    final HTTPRecordingType httpRecording = result.getHttpRecording();
    m_requestList.record(httpRecording);

    final XmlObject[] requestXmlObjects = httpRecording.selectPath(
        "declare namespace ns=" +
        "'http://grinder.sourceforge.net/tcpproxy/http/1.0';" +
        "$this//ns:request");

    final List<RequestType> requests =
        new ArrayList<RequestType>(requestXmlObjects.length);

    for (XmlObject o : requestXmlObjects) {
      requests.add((RequestType) o);
    }

    extractCommonHeaders(requests, httpRecording);

    // Find default headers that are present in all common headers, or
    // request headers that don't extend common headers.

    final CommonHeadersType[] commonHeaders =
        httpRecording.getCommonHeadersArray();

    final List<HeadersType> defaultHeaderSources =
        new ArrayList<HeadersType>(commonHeaders.length);

    for (HeadersType header : commonHeaders) {
      defaultHeaderSources.add(header);
    }

    for (RequestType request : requests) {
      final HeadersType headers = request.getHeaders();

      if (!headers.isSetExtends()) {
        defaultHeaderSources.add(headers);
      }
    }

    final Set<Pair<String, String>> defaultHeaders =
        findSharedHeaders(defaultHeaderSources);

    if (defaultHeaders.size() > 0) {
      final List<CommonHeadersType> newCommonHeaders =
        new ArrayList<CommonHeadersType>(commonHeaders.length + 1);

      final CommonHeadersType defaultHeadersXML =
          CommonHeadersType.Factory.newInstance();
      defaultHeadersXML.setHeadersId("defaultHeaders");
      newCommonHeaders.add(defaultHeadersXML);

      for (Pair<String, String> defaultHeader : defaultHeaders) {
        final HeaderType header = defaultHeadersXML.addNewHeader();
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

        if (headers.sizeOfHeaderArray() == 0) {
          assert emptyCommonHeadersID == null;
          emptyCommonHeadersID = headers.getHeadersId();
        }
        else {
          newCommonHeaders.add(headers);
        }
      }

      httpRecording.setCommonHeadersArray(
        newCommonHeaders.toArray(
          new CommonHeadersType[newCommonHeaders.size()]));

      for (RequestType request : requests) {
        final HeadersType headers = request.getHeaders();

        if (!headers.isSetExtends()) {
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
          CommonHeadersType.Factory.newInstance();

      final HeadersType uncommonHeaders = HeadersType.Factory.newInstance();

      final XmlObject[] children = request.getHeaders().selectPath("./*");

      for (int i = 0; i < children.length; ++i) {
        if (children[i] instanceof HeaderType) {
          final HeaderType header = (HeaderType)children[i];

          if (m_parameters.isCommonHeader(header.getName())) {
            commonHeaders.addNewHeader().set(header);
          }
          else {
            uncommonHeaders.addNewHeader().set(header);
          }
        }
        else {
          uncommonHeaders.addNewAuthorization().set(children[i]);
        }
      }

      // Key that ignores ID.
      final String key =
        Arrays.asList(commonHeaders.getHeaderArray()).toString();

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

      final String key =
          Arrays.asList(commonHeaders.getHeaderArray()).toString();

      if (commonHeaders.sizeOfHeaderArray() > 0 &&
          commonHeadersCountByValue.get(key).intValue() > 1) {
        if (commonHeaders.getHeadersId() == null) {
          commonHeaders.setHeadersId("headers" + idGenerator.next());

          httpRecording.addNewCommonHeaders().set(commonHeaders);
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

    final HeaderType[] oneHeaders =
        headersList.remove(headersList.size() - 1).getHeaderArray();

    final Set<Pair<String, String>> sharedHeaders =
        new HashSet<Pair<String, String>>(oneHeaders.length);

    for (HeaderType header : oneHeaders) {
      sharedHeaders.add(Pair.of(header.getName(), header.getValue()));
    }

    for (HeadersType headers : headersList) {
      final HeaderType[] h = headers.getHeaderArray();

      final Set<Pair<String, String>> sharedHeadersCopy =
          new HashSet<Pair<String, String>>(sharedHeaders);

      for (int j = 0; j < h.length; ++j) {
        final String name = h[j].getName();
        final String value = h[j].getValue();

        sharedHeadersCopy.remove(Pair.of(name, value));
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

    final HeaderType[] headersArray = headers.getHeaderArray();

    final List<Integer> defaultHeaderIndexes =
        new ArrayList<Integer>(defaultHeaders.size());

    for (int i = headersArray.length - 1; i >= 0; --i) {
      if (defaultHeaders.contains(Pair.of(headersArray[i].getName(),
                                          headersArray[i].getValue()))) {
        defaultHeaderIndexes.add(i);
      }
    }

    assert defaultHeaderIndexes.size() == defaultHeaders.size();

    for (int index : defaultHeaderIndexes) {
      headers.removeHeader(index);
    }

    headers.setExtends(defaultHeadersID);
  }

  private final class BaseURLMap {
    private final Map<String, BaseURIType> m_map =
      new HashMap<String, BaseURIType>();

    private final IntGenerator m_idGenerator = new IntGenerator();

    public BaseURIType getBaseURL(
      BaseURIType.Scheme.Enum scheme, EndPoint endPoint) {

      final String key = scheme.toString() + "://" + endPoint;

      synchronized (m_map) {
        final BaseURIType existing = m_map.get(key);

        if (existing != null) {
          return existing;
        }

        final BaseURIType result;

        synchronized (m_recordingDocument) {
          result = m_recordingDocument.getHttpRecording().addNewBaseUri();
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
      final RequestType request = RequestType.Factory.newInstance();

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

          synchronized (m_recordingDocument) {
            // Crude but effective pagination heuristics.
            if (!request.getUri().getExtends().equals(lastBaseURI) ||
                request.isSetBody() ||
                !(m_resourcePathPattern.matcher(request.getUri().getUnparsed())
                     .matches() ||
                  lastResponseWasRedirect) ||
                currentPage == null) {
              currentPage = httpRecording.addNewPage();
            }

            lastBaseURI = request.getUri().getExtends();

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

            currentPage.addNewRequest().set(request);
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

          synchronized (m_recordingDocument) {
            newToken = m_recordingDocument.getHttpRecording().addNewToken();
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
