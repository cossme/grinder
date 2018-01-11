// Copyright (C) 2007 Philip Aston
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

package net.grinder.util;

import junit.framework.TestCase;

public class TestSimpleStringEscaper extends TestCase {

  public void testEncode() throws Exception {

    final String[][] data = new String[][] {
        {"", ""},
        {"\n\\", "\\n\n\\\\"},
        {"\r\n", "\\r\\n\n"},
        {"You should have cottoned on", "You should have cottoned on"},
        {"That Allan\nis a cowboy killer", "That Allan\\n\nis a cowboy killer"},
    };

    final SimpleStringEscaper escaper = new SimpleStringEscaper();

    for (int i = 0; i < data.length; ++i) {
      final String original = data[i][0];
      final String expected = data[i][1];

      assertEquals(expected, escaper.escape(original));
      assertEquals(original, escaper.unescape(escaper.escape(original)));
    }
  }

  public void testDecode() throws Exception {

    final String[][] data = new String[][] {
        {"", ""},
        {"\n\n\r\n\\n", "\n"},
        {"You should \n have cottoned on", "You should  have cottoned on"},
        {"\\blah", "\\blah"},
    };

    final SimpleStringEscaper escaper = new SimpleStringEscaper();

    for (int i = 0; i < data.length; ++i) {
      final String original = data[i][0];
      final String expected = data[i][1];

      assertEquals(expected, escaper.unescape(original));
    }
  }
}
