// Copyright (C) 2004, 2005 Philip Aston
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

package net.grinder.util.html;

import junit.framework.TestCase;


/**
 *  Unit tests for {@link HTMLElement}, {@link HTMLText}.
 *
 * @author Philip Aston
 */
public class TestHTML extends TestCase {

  private static final String[] m_strings = {
    "The difference between me and you",
    "is that I'm not on fire.",
    "",
    "  \n \t",
  };

  public void testHTMLText() throws Exception {
    for (int i = 0; i < m_strings.length; ++i) {
      final HTMLText text = new HTMLText(m_strings[i]);
      assertEquals(m_strings[i], text.toText());
      assertEquals(m_strings[i], text.toHTML());
      assertEquals(m_strings[i], text.toString());
    }
  }

  public void testHTMLElement() throws Exception {
    final HTMLElement parent = new HTMLElement("parent");
    assertEquals("<parent/>", parent.toHTML());
    assertEquals("", parent.toText());

    final HTMLElement child = parent.addElement("child");
    child.addElement("grandChild");
    parent.addElement("child2");
    child.addText("Some text");
    assertEquals(
      "<parent><child><grandChild/>Some text</child><child2/></parent>",
      parent.toHTML());
    assertEquals("Some text", parent.toText());

    final HTMLElement nullElement = new HTMLElement();
    assertEquals("", nullElement.toHTML());
    nullElement.addElement("foo");
    assertEquals("<foo/>", nullElement.toHTML());

    final HTMLElement anotherElement = new HTMLElement("body");
    anotherElement.addElement("P").addText("text");
    anotherElement.addElement("code").addText("text2");
    anotherElement.addElement("br");
    anotherElement.addText("text3");

    assertEquals("<body><P>text</P><code>text2</code><br/>text3</body>",
                 anotherElement.toHTML());
    assertEquals("text\ntext2\ntext3", anotherElement.toText());
  }

  public void testHTMLDocument() throws Exception {
    final HTMLDocument document = new HTMLDocument();
    document.getHead().addElement("title").addText("Test");
    document.getBody().addText("The body");

    assertEquals("<html><head><title>Test</title></head>" +
                 "<body>The body</body></html>",
                 document.toHTML());
  }
}
