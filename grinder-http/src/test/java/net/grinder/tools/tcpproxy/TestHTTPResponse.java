// Copyright (C) 2005 Philip Aston
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

import net.grinder.testutility.AssertUtilities;
import net.grinder.util.html.HTMLElement;

import junit.framework.TestCase;


/**
 * Unit test case for {@link HTTPResponse}.
 *
 * @author Philip Aston
 */
public class TestHTTPResponse extends TestCase {

  public void testHTTPResponse() throws Exception {
    final HTTPResponse response = new HTTPResponse();

    final String result0 = response.toString();
    AssertUtilities.assertStartsWith(result0,
                                     "HTTP/1.0 500 Internal Server Error\r\n");
    AssertUtilities.assertContainsHeader(result0,
                                         "Proxy-agent", "The Grinder .*");
    AssertUtilities.assertEndsWith(result0, "\r\n\r\n");

    response.setStatus("200 OK");
    final String result1 = response.toString();
    AssertUtilities.assertStartsWith(result1, "HTTP/1.0 200 OK\r\n");

    final HTMLElement message = new HTMLElement();
    message.addElement("p").addText("testing");
    response.setMessage("Test", message);

    final String result2 = response.toString();
    AssertUtilities.assertContainsHeader(result2, "Content-Type", "text/html");
    AssertUtilities.assertContainsHeader(result2, "Connection", "close");

    AssertUtilities.assertContainsPattern(result2,
      "<title>.*Test.*</title>.*<h1>.*Test.*</h1>.*<p>testing</p>");
    AssertUtilities.assertEndsWith(result0, "\r\n\r\n");
  }

}