// Copyright (C) 2005 - 2013 Philip Aston
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

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.grinder.common.GrinderException;
import net.grinder.plugin.http.tcpproxyfilter.RegularExpressions;
import net.grinder.plugin.http.tcpproxyfilter.RegularExpressionsImplementation;
import net.grinder.util.AttributeStringParser;
import net.grinder.util.AttributeStringParserImplementation;
import net.grinder.util.http.URIParser;
import net.grinder.util.http.URIParserImplementation;
import HTTPClient.Codecs;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;


/**
 * {@link HTTPUtilities} implementation.
 *
 * @author Philip Aston
 */
class HTTPUtilitiesImplementation implements HTTPUtilities {

  private final URIParser m_uriParser = new URIParserImplementation();
  private final RegularExpressions m_regularExpressions =
    new RegularExpressionsImplementation();
  private final AttributeStringParser m_attributeStringParser =
    new AttributeStringParserImplementation();

  private final ThreadLocal<ParsedBody> m_parsedBodyThreadLocal =
    new ThreadLocal<ParsedBody>();
  private final NameValue[] m_emptyNameValues = new NameValue[0];

  private final HTTPPlugin m_httpPlugin;

  public HTTPUtilitiesImplementation(final HTTPPlugin httpPlugin) {
    m_httpPlugin = httpPlugin;
  }

  @Override
  public NVPair basicAuthorizationHeader(final String userID,
                                         final String password) {
    return new NVPair("Authorization",
                      "Basic " +
                      Codecs.base64Encode(userID + ":" + password));
  }

  @Override
  public HTTPResponse getLastResponse() throws GrinderException {
    return m_httpPlugin.getThreadState().getLastResponse();
  }

  @Override
  public String valueFromLocationURI(final String tokenName)
    throws GrinderException {

    final HTTPResponse response = getLastResponse();

    if (response == null) {
      return "";
    }

    final String location;

    try {
      location = response.getHeader("Location");
    }
    catch (final Exception e) {
      throw new AssertionError("HTTPResponse not initialised (" + e + ")");
    }

    final String[] result = { "" };

    if (location != null) {
      m_uriParser.parse(location, new URIParser.AbstractParseListener() {
        @Override
        public boolean pathParameterNameValue(final String name,
                                              final String value) {
          if (name.equals(tokenName)) {
            result[0] = value;
            return false;
          }

          return true;
        }

        @Override
        public boolean queryStringNameValue(final String name,
                                            final String value) {
          if (name.equals(tokenName)) {
            result[0] = value;
            return false;
          }

          return true;
        }
      });
    }

    return result[0];
  }

  @Override
  public String valueFromBodyInput(final String tokenName)
    throws GrinderException {
    return valueFromBodyInput(tokenName, null);
  }

  @Override
  public String valueFromBodyInput(final String tokenName,
                                   final String afterText)
    throws GrinderException {

    final HTTPResponse response = getLastResponse();

    if (response == null) {
      return "";
    }

    return getParsedBody(response).valueFromBodyInput(tokenName, afterText);
  }

  @Override
  public List<String> valuesFromBodyInput(final String tokenName)
    throws GrinderException {
    return valuesFromBodyInput(tokenName, null);
  }

  @Override
  public List<String> valuesFromBodyInput(final String tokenName,
                                          final String afterText)
    throws GrinderException {

    final HTTPResponse response = getLastResponse();

    if (response == null) {
      return emptyList();
    }

    return getParsedBody(response).valuesFromBodyInput(tokenName, afterText);
  }

  @Override
  public String valueFromHiddenInput(final String tokenName)
    throws GrinderException {
    return valueFromHiddenInput(tokenName, null);
  }

  @Override
  public String valueFromHiddenInput(final String tokenName,
                                     final String afterText)
    throws GrinderException {

    final HTTPResponse response = getLastResponse();

    if (response == null) {
      return "";
    }

    return getParsedBody(response).valueFromHiddenInput(tokenName, afterText);
  }

  @Override
  public List<String> valuesFromHiddenInput(final String tokenName)
    throws GrinderException {
    return valuesFromHiddenInput(tokenName, null);
  }

  @Override
  public List<String> valuesFromHiddenInput(final String tokenName,
                                            final String afterText)
    throws GrinderException {

    final HTTPResponse response = getLastResponse();

    if (response == null) {
      return emptyList();
    }

    return getParsedBody(response).valuesFromHiddenInput(tokenName, afterText);
  }

  @Override
  public String valueFromBodyURI(final String tokenName)
    throws GrinderException {
    return valueFromBodyURI(tokenName, null);
  }

  @Override
  public String valueFromBodyURI(final String tokenName, final String afterText)
    throws GrinderException {

    final HTTPResponse response = getLastResponse();

    if (response == null) {
      return "";
    }

    return getParsedBody(response).valueFromBodyURI(tokenName, afterText);
  }

  @Override
  public List<String> valuesFromBodyURI(final String tokenName)
    throws GrinderException {
    return valuesFromBodyURI(tokenName, null);
  }

  @Override
  public List<String> valuesFromBodyURI(final String tokenName,
                                        final String afterText)
    throws GrinderException {

    final HTTPResponse response = getLastResponse();

    if (response == null) {
      return emptyList();
    }

    return getParsedBody(response).valuesFromBodyURI(tokenName, afterText);
  }

  private ParsedBody getParsedBody(final HTTPResponse response) {
    final ParsedBody original = m_parsedBodyThreadLocal.get();

    if (original != null && original.isValidForResponse(response)) {
      return original;
    }

    final ParsedBody newParsedBody = new ParsedBody(response);
    m_parsedBodyThreadLocal.set(newParsedBody);
    return newParsedBody;
  }

  /**
   * Cache parse results from a HTTPResponse.
   *
   * <p>Specific to a thread, so no need to synchronise.</p>
   */
  private final class ParsedBody {

    private final HTTPResponse m_response;
    private final String m_body;
    private final MatchList m_bodyInputMatchList;
    private final MatchList m_hiddenInputMatchList;
    private final MatchList m_bodyURIMatchList;

    public ParsedBody(final HTTPResponse response) {
      m_response = response;

      try {
        // This shouldn't fail as we have already read the complete response.
        m_body = response.getText();
      }
      catch (final Exception e) {
        throw new AssertionError(e);
      }

      m_bodyInputMatchList =
        new InputMatchList(m_regularExpressions.getInputPattern(), m_body);
      m_hiddenInputMatchList =
        new InputMatchList(m_regularExpressions.getHiddenInputPattern(),
                           m_body);
      m_bodyURIMatchList = new BodyURIMatchList(m_body);
    }

    public boolean isValidForResponse(final HTTPResponse response) {
      return m_response.equals(response);
    }

    public String valueFromBodyInput(final String tokenName,
                                     final String afterText) {
      return m_bodyInputMatchList.getMatchValue(tokenName, afterText);
    }

    public List<String> valuesFromBodyInput(final String tokenName,
                                            final String afterText) {
      return m_bodyInputMatchList.getMatchValues(tokenName, afterText);
    }


    public String valueFromHiddenInput(final String tokenName,
                                       final String afterText) {
      return m_hiddenInputMatchList.getMatchValue(tokenName, afterText);
    }

    public List<String> valuesFromHiddenInput(final String tokenName,
                                              final String afterText) {
      return m_hiddenInputMatchList.getMatchValues(tokenName, afterText);
    }

    public String valueFromBodyURI(final String tokenName,
                                   final String afterText) {
      return m_bodyURIMatchList.getMatchValue(tokenName, afterText);
    }

    public List<String> valuesFromBodyURI(final String tokenName,
                                          final String afterText) {
      return m_bodyURIMatchList.getMatchValues(tokenName, afterText);
    }
  }

  private static final class NameValue {
    private final String m_name;
    private final String m_value;

    public NameValue(final String name, final String value) {
      m_name = name;
      m_value = value;
    }

    public String getName() {
      return m_name;
    }

    public String getValue() {
      return m_value;
    }
  }

  private static final class Match {
    private final int m_position;
    private final String m_value;

    public Match(final int position, final String value) {
      m_position = position;
      m_value = value;
    }

    public int getPosition() {
      return m_position;
    }

    public String getValue() {
      return m_value;
    }
  }

  private static class CachedMatchList {
    private final List<Match> m_matchesByPosition = new ArrayList<Match>();
    private int m_lastPosition = -1;

    public void addMatch(final Match match) {
      m_matchesByPosition.add(match);
      m_lastPosition = match.getPosition();
    }

    public Match getMatchFrom(final int startFrom) {
      if (m_lastPosition >= startFrom) {
        for (final Match match : m_matchesByPosition) {
          if (match.getPosition() >= startFrom) {
            return match;
          }
        }
      }

      return null;
    }
  }

  private interface MatchList {

    String getMatchValue(String tokenName, String afterText);

    List<String> getMatchValues(String tokenName, String afterText);
  }

  private static class CachedValueMap {
    private final Map<String, CachedMatchList> m_map =
      new HashMap<String, CachedMatchList>();

    public CachedMatchList get(final String tokenName) {
      final CachedMatchList existing = m_map.get(tokenName);

      if (existing != null) {
        return existing;
      }

      final CachedMatchList newCachedValueList = new CachedMatchList();
      m_map.put(tokenName, newCachedValueList);
      return newCachedValueList;
    }
  }

  private abstract static class AbstractMatchList implements MatchList {
    private final String m_body;
    private final Matcher m_matcher;
    private final CachedValueMap m_cache = new CachedValueMap();

    public AbstractMatchList(final Pattern pattern, final String body) {
      m_body = body;
      m_matcher = pattern.matcher(body);
    }

    @Override
    public String getMatchValue(final String tokenName,
                                final String afterText) {
      final int startFrom = getStartFrom(afterText);

      if (startFrom == -1) {
        return "";
      }

      final CachedMatchList cachedValueList = m_cache.get(tokenName);

      final Match match = getMatch(cachedValueList, tokenName, startFrom);
      return match != null ? match.getValue() : "";
    }

    @Override
    public List<String> getMatchValues(final String tokenName,
                                       final String afterText) {

      int startFrom = getStartFrom(afterText);

      if (startFrom == -1) {
        return emptyList();
      }

      final CachedMatchList cachedValueList = m_cache.get(tokenName);

      final List<String> result = new ArrayList<String>();

      while (true) {
        final Match match = getMatch(cachedValueList, tokenName, startFrom);

        if (match == null) {
          return result;
        }

        result.add(match.getValue());
        startFrom = match.getPosition() + 1;
      }
    }

    private Match getMatch(final CachedMatchList cachedValueList,
                           final String tokenName,
                           final int startFrom) {

      final Match existingMatch = cachedValueList.getMatchFrom(startFrom);

      if (existingMatch != null) {
        return existingMatch;
      }

      Match result = null;

      // Cache miss, parse more of the body.
      while (result == null && m_matcher.find()) {
        final NameValue[] nameValueArray = parseMatch();

        final int matchPosition = m_matcher.start();

        for (final NameValue element : nameValueArray) {
          final String name = element.getName();

          final Match match = new Match(matchPosition,
                                        element.getValue());

          if (name.equals(tokenName)) {
            cachedValueList.addMatch(match);

            if (result == null && matchPosition >= startFrom) {
              result = match;
            }
          }
          else {
            m_cache.get(name).addMatch(match);
          }
        }
      }

      return result;
    }

    private int getStartFrom(final String text) {
      // afterText parameter is infrequently used, so memoizing this
      // method would cost more than it saved.

      return text == null ? 0 : m_body.indexOf(text);
    }

    protected final Matcher getMatcher() {
      return m_matcher;
    }

    protected abstract NameValue[] parseMatch();
  }

  private final class InputMatchList extends AbstractMatchList {

    public InputMatchList(final Pattern pattern, final String body) {
      super(pattern, body);
    }

    @Override
    protected NameValue[] parseMatch() {
      final AttributeStringParser.AttributeMap map =
        m_attributeStringParser.parse(getMatcher().group());

      final String name = map.get("name");
      final String value = map.get("value");

      if (name != null && value != null) {
        return new NameValue[] { new NameValue(name, value) };
      }

      return m_emptyNameValues;
    }
  }

  private final class BodyURIMatchList extends AbstractMatchList {

    public BodyURIMatchList(final String body) {
      super(m_regularExpressions.getHyperlinkURIPattern(), body);
    }

    @Override
    protected NameValue[] parseMatch() {
      final List<NameValue> result = new ArrayList<NameValue>();

      final String uri = getMatcher().group(1);

      m_uriParser.parse(uri, new URIParser.AbstractParseListener() {
        @Override
        public boolean pathParameterNameValue(final String name,
                                              final String value) {
          result.add(new NameValue(name, value));
          return true;
        }

        @Override
        public boolean queryStringNameValue(final String name,
                                            final String value) {
          result.add(new NameValue(name, value));
          return true;
        }
      });

      return result.toArray(new NameValue[result.size()]);
    }
  }
}
