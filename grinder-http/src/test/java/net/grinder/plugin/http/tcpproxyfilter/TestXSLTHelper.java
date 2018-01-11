// Copyright (C) 2005 - 2011 Philip Aston
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.text.ParseException;

import org.junit.After;
import org.junit.Test;

import HTTPClient.Codecs;


/**
 * Unit tests for {@link XSLTHelper}.
 *
 * @author Philip Aston
 */
public class TestXSLTHelper {

  @After public void setUp() throws Exception {
    XSLTHelper.resetIndent();
    XSLTHelper.setIndentString("  ");
  }

  @Test public void testBase64ToPython() throws Exception {
    final byte[] bytes0 = { 0, -42, 1, 22, };

    assertEquals("\"\\x00\\xD6\\x01\\x16\"",
                 XSLTHelper.base64ToPython(
                   new String(Codecs.base64Encode(bytes0))));

    XSLTHelper.changeIndent(2);

    assertEquals("\"\\x00\\xD6\\x01\\x16\"",
      XSLTHelper.base64ToPython(new String(Codecs.base64Encode(bytes0))));

    assertEquals("\"\"", XSLTHelper.base64ToPython(""));

    final byte[] bytes1 = new byte[300];

    for (int i = 0; i < bytes1.length; ++i) {
      bytes1[i] = (byte) i;
    }

    assertEquals(
      "\"\\x00\\x01\\x02\\x03\\x04\\x05\\x06\\x07\\x08\\x09\\x0A\\x0B\\x0C\\x0D\\x0E\\x0F\"\n" +
      "    \"\\x10\\x11\\x12\\x13\\x14\\x15\\x16\\x17\\x18\\x19\\x1A\\x1B\\x1C\\x1D\\x1E\\x1F\"\n" +
      "    \"\\x20\\x21\\x22\\x23\\x24\\x25\\x26\\x27\\x28\\x29\\x2A\\x2B\\x2C\\x2D\\x2E\\x2F\"\n" +
      "    \"\\x30\\x31\\x32\\x33\\x34\\x35\\x36\\x37\\x38\\x39\\x3A\\x3B\\x3C\\x3D\\x3E\\x3F\"\n" +
      "    \"\\x40\\x41\\x42\\x43\\x44\\x45\\x46\\x47\\x48\\x49\\x4A\\x4B\\x4C\\x4D\\x4E\\x4F\"\n" +
      "    \"\\x50\\x51\\x52\\x53\\x54\\x55\\x56\\x57\\x58\\x59\\x5A\\x5B\\x5C\\x5D\\x5E\\x5F\"\n" +
      "    \"\\x60\\x61\\x62\\x63\\x64\\x65\\x66\\x67\\x68\\x69\\x6A\\x6B\\x6C\\x6D\\x6E\\x6F\"\n" +
      "    \"\\x70\\x71\\x72\\x73\\x74\\x75\\x76\\x77\\x78\\x79\\x7A\\x7B\\x7C\\x7D\\x7E\\x7F\"\n" +
      "    \"\\x80\\x81\\x82\\x83\\x84\\x85\\x86\\x87\\x88\\x89\\x8A\\x8B\\x8C\\x8D\\x8E\\x8F\"\n" +
      "    \"\\x90\\x91\\x92\\x93\\x94\\x95\\x96\\x97\\x98\\x99\\x9A\\x9B\\x9C\\x9D\\x9E\\x9F\"\n" +
      "    \"\\xA0\\xA1\\xA2\\xA3\\xA4\\xA5\\xA6\\xA7\\xA8\\xA9\\xAA\\xAB\\xAC\\xAD\\xAE\\xAF\"\n" +
      "    \"\\xB0\\xB1\\xB2\\xB3\\xB4\\xB5\\xB6\\xB7\\xB8\\xB9\\xBA\\xBB\\xBC\\xBD\\xBE\\xBF\"\n" +
      "    \"\\xC0\\xC1\\xC2\\xC3\\xC4\\xC5\\xC6\\xC7\\xC8\\xC9\\xCA\\xCB\\xCC\\xCD\\xCE\\xCF\"\n" +
      "    \"\\xD0\\xD1\\xD2\\xD3\\xD4\\xD5\\xD6\\xD7\\xD8\\xD9\\xDA\\xDB\\xDC\\xDD\\xDE\\xDF\"\n" +
      "    \"\\xE0\\xE1\\xE2\\xE3\\xE4\\xE5\\xE6\\xE7\\xE8\\xE9\\xEA\\xEB\\xEC\\xED\\xEE\\xEF\"\n" +
      "    \"\\xF0\\xF1\\xF2\\xF3\\xF4\\xF5\\xF6\\xF7\\xF8\\xF9\\xFA\\xFB\\xFC\\xFD\\xFE\\xFF\"\n" +
      "    \"\\x00\\x01\\x02\\x03\\x04\\x05\\x06\\x07\\x08\\x09\\x0A\\x0B\\x0C\\x0D\\x0E\\x0F\"\n" +
      "    \"\\x10\\x11\\x12\\x13\\x14\\x15\\x16\\x17\\x18\\x19\\x1A\\x1B\\x1C\\x1D\\x1E\\x1F\"\n" +
      "    \"\\x20\\x21\\x22\\x23\\x24\\x25\\x26\\x27\\x28\\x29\\x2A\\x2B\"",
      XSLTHelper.base64ToPython(new String(Codecs.base64Encode(bytes1))));
  }

  @Test public void testBase64ToClojure() throws Exception {
    final byte[] bytes0 = { 0, -42, 1, 22, };

    assertEquals("[0x00 0xD6 0x01 0x16]",
                 XSLTHelper.base64ToClojure(
                   new String(Codecs.base64Encode(bytes0))));

    XSLTHelper.changeIndent(2);

    assertEquals("[0x00 0xD6 0x01 0x16]",
      XSLTHelper.base64ToClojure(new String(Codecs.base64Encode(bytes0))));

    assertEquals("[]", XSLTHelper.base64ToClojure(""));

    final byte[] bytes1 = new byte[300];

    for (int i = 0; i < bytes1.length; ++i) {
      bytes1[i] = (byte) i;
    }

    assertEquals(
     "[0x00 0x01 0x02 0x03 0x04 0x05 0x06 0x07 0x08 0x09 0x0A 0x0B 0x0C 0x0D 0x0E 0x0F\n" +
     "    0x10 0x11 0x12 0x13 0x14 0x15 0x16 0x17 0x18 0x19 0x1A 0x1B 0x1C 0x1D 0x1E 0x1F\n" +
     "    0x20 0x21 0x22 0x23 0x24 0x25 0x26 0x27 0x28 0x29 0x2A 0x2B 0x2C 0x2D 0x2E 0x2F\n" +
     "    0x30 0x31 0x32 0x33 0x34 0x35 0x36 0x37 0x38 0x39 0x3A 0x3B 0x3C 0x3D 0x3E 0x3F\n" +
     "    0x40 0x41 0x42 0x43 0x44 0x45 0x46 0x47 0x48 0x49 0x4A 0x4B 0x4C 0x4D 0x4E 0x4F\n" +
     "    0x50 0x51 0x52 0x53 0x54 0x55 0x56 0x57 0x58 0x59 0x5A 0x5B 0x5C 0x5D 0x5E 0x5F\n" +
     "    0x60 0x61 0x62 0x63 0x64 0x65 0x66 0x67 0x68 0x69 0x6A 0x6B 0x6C 0x6D 0x6E 0x6F\n" +
     "    0x70 0x71 0x72 0x73 0x74 0x75 0x76 0x77 0x78 0x79 0x7A 0x7B 0x7C 0x7D 0x7E 0x7F\n" +
     "    0x80 0x81 0x82 0x83 0x84 0x85 0x86 0x87 0x88 0x89 0x8A 0x8B 0x8C 0x8D 0x8E 0x8F\n" +
     "    0x90 0x91 0x92 0x93 0x94 0x95 0x96 0x97 0x98 0x99 0x9A 0x9B 0x9C 0x9D 0x9E 0x9F\n" +
     "    0xA0 0xA1 0xA2 0xA3 0xA4 0xA5 0xA6 0xA7 0xA8 0xA9 0xAA 0xAB 0xAC 0xAD 0xAE 0xAF\n" +
     "    0xB0 0xB1 0xB2 0xB3 0xB4 0xB5 0xB6 0xB7 0xB8 0xB9 0xBA 0xBB 0xBC 0xBD 0xBE 0xBF\n" +
     "    0xC0 0xC1 0xC2 0xC3 0xC4 0xC5 0xC6 0xC7 0xC8 0xC9 0xCA 0xCB 0xCC 0xCD 0xCE 0xCF\n" +
     "    0xD0 0xD1 0xD2 0xD3 0xD4 0xD5 0xD6 0xD7 0xD8 0xD9 0xDA 0xDB 0xDC 0xDD 0xDE 0xDF\n" +
     "    0xE0 0xE1 0xE2 0xE3 0xE4 0xE5 0xE6 0xE7 0xE8 0xE9 0xEA 0xEB 0xEC 0xED 0xEE 0xEF\n" +
     "    0xF0 0xF1 0xF2 0xF3 0xF4 0xF5 0xF6 0xF7 0xF8 0xF9 0xFA 0xFB 0xFC 0xFD 0xFE 0xFF\n" +
     "    0x00 0x01 0x02 0x03 0x04 0x05 0x06 0x07 0x08 0x09 0x0A 0x0B 0x0C 0x0D 0x0E 0x0F\n" +
     "    0x10 0x11 0x12 0x13 0x14 0x15 0x16 0x17 0x18 0x19 0x1A 0x1B 0x1C 0x1D 0x1E 0x1F\n" +
     "    0x20 0x21 0x22 0x23 0x24 0x25 0x26 0x27 0x28 0x29 0x2A 0x2B]",
     XSLTHelper.base64ToClojure(new String(Codecs.base64Encode(bytes1))));
  }

  @Test public void testFormatTime() throws Exception {
    try {
      XSLTHelper.formatTime("abc");
      fail("Expected ParseException");
    }
    catch (ParseException e) {
    }

    final String s = XSLTHelper.formatTime("2005-01-04T18:30:00");
    assertNotNull(s);
  }

  @Test public void testQuoteForPython() throws Exception {
    assertEquals("None", XSLTHelper.quoteForPython(null));
    assertEquals("''", XSLTHelper.quoteForPython(""));
    assertEquals("\'\\\"\'", XSLTHelper.quoteForPython("\""));
    assertEquals("'foo'", XSLTHelper.quoteForPython("foo"));
    assertEquals("'foo\\''", XSLTHelper.quoteForPython("foo'"));
    assertEquals("' \\\\ '", XSLTHelper.quoteForPython(" \\ "));
    assertEquals("'''foo \n bah'''", XSLTHelper.quoteForPython("foo \n bah"));
    assertEquals("'''foo \\r bah'''", XSLTHelper.quoteForPython("foo \r bah"));
    assertEquals("'foo \\\\n bah'", XSLTHelper.quoteForPython("foo \\n bah"));
  }

  @Test public void testQuoteEOLEscapedStringForPython() throws Exception {
    assertEquals("None", XSLTHelper.quoteEOLEscapedStringForPython(null));
    assertEquals("''", XSLTHelper.quoteEOLEscapedStringForPython(""));
    assertEquals("\'\\\"\'", XSLTHelper.quoteEOLEscapedStringForPython("\""));
    assertEquals("'foo'", XSLTHelper.quoteEOLEscapedStringForPython("foo"));
    assertEquals("'foo\\''", XSLTHelper.quoteEOLEscapedStringForPython("foo'"));
    assertEquals("' \\\\ '", XSLTHelper.quoteEOLEscapedStringForPython(" \\ "));
    assertEquals("'''foo   bah'''",
      XSLTHelper.quoteEOLEscapedStringForPython("foo \n \r bah"));
    assertEquals("'foo \\n bah\\\\'",
      XSLTHelper.quoteEOLEscapedStringForPython("foo \\n bah\\"));
  }

  @Test public void testQuoteForClojure() throws Exception {
    assertEquals("nil", XSLTHelper.quoteForClojure(null));
    assertEquals("\"\"", XSLTHelper.quoteForClojure(""));
    assertEquals("\"\\\"\"", XSLTHelper.quoteForClojure("\""));
    assertEquals("\"\\\"\"", XSLTHelper.quoteForClojure("\""));
    assertEquals("\"foo\"", XSLTHelper.quoteForClojure("foo"));
    assertEquals("\"foo\\'\"", XSLTHelper.quoteForClojure("foo'"));
    assertEquals("\" \\\\ \"", XSLTHelper.quoteForClojure(" \\ "));
    assertEquals("\"foo \n bah\"", XSLTHelper.quoteForClojure("foo \n bah"));
    assertEquals("\"foo \\r bah\"", XSLTHelper.quoteForClojure("foo \r bah"));
    assertEquals("\"foo \\\\n bah\"", XSLTHelper.quoteForClojure("foo \\n bah"));
  }

  @Test public void testQuoteEOLEscapedStringForClojure() throws Exception {
    assertEquals("nil", XSLTHelper.quoteEOLEscapedStringForClojure(null));
    assertEquals("\"\"", XSLTHelper.quoteEOLEscapedStringForClojure(""));
    assertEquals("\"\\\"\"", XSLTHelper.quoteEOLEscapedStringForClojure("\""));
    assertEquals("\"foo\"", XSLTHelper.quoteEOLEscapedStringForClojure("foo"));
    assertEquals("\"foo\\'\"", XSLTHelper.quoteEOLEscapedStringForClojure("foo'"));
  }

  @Test public void testEscape() throws Exception {
    assertEquals("", XSLTHelper.escape(""));
    assertEquals("\\'", XSLTHelper.escape("'"));
    assertEquals("\\\"", XSLTHelper.escape("\""));
    assertEquals("\\\\", XSLTHelper.escape("\\"));
    assertEquals("Hello \\'quoted\\\" \\\\world",
                 XSLTHelper.escape("Hello 'quoted\" \\world"));
  }

  @Test public void testSummariseAsLine() throws Exception {
    assertEquals("blah, blah", XSLTHelper.summariseAsLine("blah, blah", 20));
    assertEquals("blah,...", XSLTHelper.summariseAsLine("blah, blah", 5));
    assertEquals("blah,\\nblah", XSLTHelper.summariseAsLine("blah,\nblah", 20));
    assertEquals("\\r blah,\\t", XSLTHelper.summariseAsLine("\r blah,\t", 20));
    assertEquals("..bla...", XSLTHelper.summariseAsLine("\0\0blah", 5));
  }

  @Test public void testIndent() throws Exception {
    assertEquals("", XSLTHelper.indent());

    XSLTHelper.changeIndent(2);
    assertEquals("    ", XSLTHelper.indent());

    XSLTHelper.setIndentString("\t");
    assertEquals("\t\t", XSLTHelper.indent());
  }

  @Test(expected=UnsupportedOperationException.class)
  public void coverConstructor() throws Exception {
    new XSLTHelper();
  }
}
