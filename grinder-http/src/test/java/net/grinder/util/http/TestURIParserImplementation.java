// Copyright (C) 2006 - 2009 Philip Aston
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

package net.grinder.util.http;

import net.grinder.testutility.DelegatingStubFactory;
import net.grinder.util.http.URIParser.ParseListener;

import junit.framework.TestCase;


/**
 * Unit tests for {@link URIParserImplementation}.
 *
 * @author Philip Aston
 */
public class TestURIParserImplementation extends TestCase {

  private ParseListener m_testParseListener =
    new TestParseListener();

  private final DelegatingStubFactory<ParseListener>
    m_parseListenerStubFactory =
      DelegatingStubFactory.create(m_testParseListener);
  private final ParseListener m_parseListener =
    m_parseListenerStubFactory.getStub();

  public void testBasicURIs() throws Exception {
    final URIParser parser = new URIParserImplementation();

    parser.parse("simple", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("path", "simple");
    m_parseListenerStubFactory.assertNoMoreCalls();

    parser.parse("http://foo/simple/", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("scheme", "http");
    m_parseListenerStubFactory.assertSuccess("authority", "foo");
    m_parseListenerStubFactory.assertSuccess("path", "/simple/");
    m_parseListenerStubFactory.assertNoMoreCalls();

    parser.parse("http://foo:1234/?hello#world", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("scheme", "http");
    m_parseListenerStubFactory.assertSuccess("authority", "foo:1234");
    m_parseListenerStubFactory.assertSuccess("path", "/");
    m_parseListenerStubFactory.assertSuccess("queryString", "hello");
    m_parseListenerStubFactory.assertSuccess("fragment", "world");
    m_parseListenerStubFactory.assertNoMoreCalls();

    parser.parse("http://foo:1234/bah%20dah?%42%77", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("scheme", "http");
    m_parseListenerStubFactory.assertSuccess("authority", "foo:1234");
    m_parseListenerStubFactory.assertSuccess("path", "/bah dah");
    m_parseListenerStubFactory.assertSuccess("queryString", "Bw");
    m_parseListenerStubFactory.assertNoMoreCalls();

    // Broken escapes.
    parser.parse("http://foo:1234/bah%20dah?%42%77", m_parseListener);

  }

  public void testParseTermination() {
    final URIParser parser = new URIParserImplementation();

    parser.parse("stop://stop/stop?stop#stop", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("scheme", "stop");
    m_parseListenerStubFactory.assertNoMoreCalls();

    parser.parse("scheme://stop/stop?stop#stop", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("scheme", "scheme");
    m_parseListenerStubFactory.assertSuccess("authority", "stop");
    m_parseListenerStubFactory.assertNoMoreCalls();

    parser.parse("scheme://authority/stop?stop#stop", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("scheme", "scheme");
    m_parseListenerStubFactory.assertSuccess("authority", "authority");
    m_parseListenerStubFactory.assertSuccess("path", "/stop");
    m_parseListenerStubFactory.assertNoMoreCalls();

    parser.parse("scheme://authority/path?stop#stop", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("scheme", "scheme");
    m_parseListenerStubFactory.assertSuccess("authority", "authority");
    m_parseListenerStubFactory.assertSuccess("path", "/path");
    m_parseListenerStubFactory.assertSuccess("queryString", "stop");
    m_parseListenerStubFactory.assertNoMoreCalls();

    parser.parse("scheme://authority/path?queryString#stop", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("scheme", "scheme");
    m_parseListenerStubFactory.assertSuccess("authority", "authority");
    m_parseListenerStubFactory.assertSuccess("path", "/path");
    m_parseListenerStubFactory.assertSuccess("queryString", "queryString");
    m_parseListenerStubFactory.assertSuccess("fragment", "stop");
    m_parseListenerStubFactory.assertNoMoreCalls();

    parser.parse("scheme://authority/path?queryString#fragment", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("scheme", "scheme");
    m_parseListenerStubFactory.assertSuccess("authority", "authority");
    m_parseListenerStubFactory.assertSuccess("path", "/path");
    m_parseListenerStubFactory.assertSuccess("queryString", "queryString");
    m_parseListenerStubFactory.assertSuccess("fragment", "fragment");
    m_parseListenerStubFactory.assertNoMoreCalls();

    parser.parse("scheme://authority/path;stop=value?queryString#fragment", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("scheme", "scheme");
    m_parseListenerStubFactory.assertSuccess("authority", "authority");
    m_parseListenerStubFactory.assertSuccess("path", "/path;");
    m_parseListenerStubFactory.assertSuccess("pathParameterNameValue", "stop", "value");
    m_parseListenerStubFactory.assertNoMoreCalls();

    parser.parse("scheme://authority/path;foo=bah&==name=value?queryString#fragment", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("scheme", "scheme");
    m_parseListenerStubFactory.assertSuccess("authority", "authority");
    m_parseListenerStubFactory.assertSuccess("path", "/path;");
    m_parseListenerStubFactory.assertSuccess("pathParameterNameValue", "foo", "bah");
    m_parseListenerStubFactory.assertSuccess("path", "&==");
    m_parseListenerStubFactory.assertNoMoreCalls();

    parser.parse("scheme://authority/path?name=value&stop=foo", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("scheme", "scheme");
    m_parseListenerStubFactory.assertSuccess("authority", "authority");
    m_parseListenerStubFactory.assertSuccess("path", "/path");
    m_parseListenerStubFactory.assertSuccess("queryStringNameValue", "name", "value");
    m_parseListenerStubFactory.assertSuccess("queryString", "&");
    m_parseListenerStubFactory.assertSuccess("queryStringNameValue", "stop", "foo");
    m_parseListenerStubFactory.assertNoMoreCalls();

    parser.parse("scheme://authority/path?foo=bah&==name=value", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("scheme", "scheme");
    m_parseListenerStubFactory.assertSuccess("authority", "authority");
    m_parseListenerStubFactory.assertSuccess("path", "/path");
    m_parseListenerStubFactory.assertSuccess("queryStringNameValue", "foo", "bah");
    m_parseListenerStubFactory.assertSuccess("queryString", "&==");
    m_parseListenerStubFactory.assertNoMoreCalls();
  }

  public void testPathParameterNameValues() throws Exception {
    final URIParser parser = new URIParserImplementation();

    parser.parse("http:/foo;hello=world/simple/", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("scheme", "http");
    m_parseListenerStubFactory.assertSuccess("path", "/foo;");
    m_parseListenerStubFactory.assertSuccess(
      "pathParameterNameValue", "hello", "world");
    m_parseListenerStubFactory.assertSuccess("path", "/simple/");
    m_parseListenerStubFactory.assertNoMoreCalls();

    parser.parse("http:/foo;hello=world&JSESSIONID=1234", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("scheme", "http");
    m_parseListenerStubFactory.assertSuccess("path", "/foo;");
    m_parseListenerStubFactory.assertSuccess(
      "pathParameterNameValue", "hello", "world");
    m_parseListenerStubFactory.assertSuccess("path", "&");
    m_parseListenerStubFactory.assertSuccess(
      "pathParameterNameValue", "JSESSIONID", "1234");
    m_parseListenerStubFactory.assertNoMoreCalls();

    parser.parse("http:;JSESSIONID=", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("scheme", "http");
    m_parseListenerStubFactory.assertSuccess("path", ";");
    m_parseListenerStubFactory.assertSuccess(
      "pathParameterNameValue", "JSESSIONID", "");
    m_parseListenerStubFactory.assertNoMoreCalls();
  }

  public void testQueryStringNameValues() throws Exception {
    final URIParser parser = new URIParserImplementation();

    parser.parse("http:/foo?hello=world/simple/", m_parseListener);
    m_parseListenerStubFactory.assertSuccess("scheme", "http");
    m_parseListenerStubFactory.assertSuccess("path", "/foo");
    m_parseListenerStubFactory.assertSuccess(
      "queryStringNameValue", "hello", "world/simple/");
    m_parseListenerStubFactory.assertNoMoreCalls();
  }

  public static final class TestParseListener implements ParseListener {

    public boolean scheme(String scheme) {
      return !scheme.equals("stop");
    }

    public boolean authority(String authority) {
      return !authority.equals("stop");
    }

    public boolean path(String path) {
      return !path.equals("/stop") && !path.equals("&==");
    }

    public boolean pathParameterNameValue(String name, String value) {
      return !name.equals("stop");
    }

    public boolean queryString(String queryString) {
      return !queryString.equals("stop") && !queryString.equals("&==");
    }

    public boolean queryStringNameValue(String name, String value) {
      return !name.equals("stop");
    }

    public boolean fragment(String fragment) {
      return !fragment.equals("stop");
    }
  }
}
