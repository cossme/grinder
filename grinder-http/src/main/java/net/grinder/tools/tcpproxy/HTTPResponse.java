// Copyright (C) 2004 - 2010 Philip Aston
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

package net.grinder.tools.tcpproxy;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.grinder.common.GrinderBuild;
import net.grinder.util.html.HTMLDocument;
import net.grinder.util.html.HTMLElement;


/**
 * Simple HTTP response formatting.
 *
 *  @author Philip Aston
 */
final class HTTPResponse {
  private String m_status = "500 Internal Server Error";
  private final Map<String, String> m_responseHeaders =
    new HashMap<String, String>();
  private final StringBuilder m_responseBody = new StringBuilder();

  public HTTPResponse() {
    setHeader("Proxy-agent", GrinderBuild.getName());
  }

  public void setHeader(String key, String value) {
    m_responseHeaders.put(key, value);
  }

  public void setStatus(String status) {
    m_status = status;
  }

  public void setMessage(String title, HTMLElement text) {
    setHeader("Content-Type", "text/html");
    setHeader("Connection", "close");

    final HTMLDocument document = new HTMLDocument();
    document.getHead().addElement("title").addText(title);
    document.getBody().addElement("h1").addText(title);
    document.getBody().addText(text.toHTML());
    document.getBody().addElement("hr");
    document.getBody().addText(GrinderBuild.getName());
    document.getBody().addElement("br");
    document.getBody().addText(new Date().toString());
    m_responseBody.append(document.toHTML());
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append("HTTP/1.0 ");
    result.append(m_status);
    result.append("\r\n");

    for (Entry<String, String> header :  m_responseHeaders.entrySet()) {
      result.append(header.getKey());
      result.append(": ");
      result.append(header.getValue());
      result.append("\r\n");
    }

    result.append("\r\n");

    result.append(m_responseBody);

    return result.toString();
  }
}
