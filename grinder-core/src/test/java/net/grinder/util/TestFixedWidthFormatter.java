// Copyright (C) 2000 - 2011 Philip Aston
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;


/**
 * Unit tests for {@link FixedWidthFormatter}.
 *
 * @author Philip Aston
 */
public class TestFixedWidthFormatter {

  @Test public void testTruncate() throws Exception {
    final String text = "They walked in a line, they wallked in a line";
    final String text2 = "Be on my side and I'll be on your side";

    for (int width=1; width<20; ++width) {
      final MultiLineFormatter leftFormatter =
        new FixedWidthFormatter(FixedWidthFormatter.Align.LEFT,
                                FixedWidthFormatter.Flow.TRUNCATE,
                                width);

      for (int i=0; i<text.length(); ++i) {
        final StringBuilder buffer =
          new StringBuilder(text.substring(0, i));
        final StringBuilder remainder = new StringBuilder(text2);

        leftFormatter.transform(buffer, remainder);

        final String result = buffer.toString();

        assertEquals(width, result.length());

        if (i<width) {
          assertEquals(text.substring(0, i), result.substring(0, i));

          for (int j=i; j<width; ++j) {
            assertEquals(' ', result.charAt(j));
          }
        }
        else {
          assertEquals(text.substring(0, width),
                       result.substring(0, width));
        }

        assertEquals(text2, remainder.toString());
      }

      final MultiLineFormatter rightFormatter =
        new FixedWidthFormatter(FixedWidthFormatter.Align.RIGHT,
                                FixedWidthFormatter.Flow.TRUNCATE,
                                width);

      for (int i=1; i<text.length(); ++i) {
        final StringBuilder buffer =
          new StringBuilder(text.substring(0, i));
        final StringBuilder remainder = new StringBuilder(text2);

        rightFormatter.transform(buffer, remainder);

        final String result = buffer.toString();

        assertEquals(width, result.length());

        if (i<width) {
          assertEquals(text.substring(0, i),
                       result.substring(width-i));

          for (int j=0; j<width-i; ++j) {
            assertEquals(' ', result.charAt(j));
          }
        }
        else {
          assertEquals(text.substring(0, width),
                       result.substring(0, width));
        }

        assertEquals(text2, remainder.toString());
      }
    }
  }

  @Test public void testFlowWrap() throws Exception {
    final String text1 =
      "Harness your hopes to the folks with the liquor with the ropes. " +
      "Red, red ropes, periscopes; they've got everything will ever need " +
      "stored under the chair.";

    final String answerText1 =
      "Harness you\n" +
      "r hopes to \n" +
      "the folks w\n" +
      "ith the liq\n" +
      "uor with th\n" +
      "e ropes. Re\n" +
      "d, red rope\n" +
      "s, periscop\n" +
      "es; they've\n" +
      " got everyt\n" +
      "hing will e\n" +
      "ver need st\n" +
      "ored under \n" +
      "the chair. ";

    final MultiLineFormatter formatter1 =
      new FixedWidthFormatter(FixedWidthFormatter.Align.LEFT,
                              FixedWidthFormatter.Flow.WRAP,
                              11);

    final String answer1 = formatter1.format(text1);
    assertEquals(answerText1, answer1);

    final MultiLineFormatter formatter2 =
      new FixedWidthFormatter(FixedWidthFormatter.Align.CENTRE,
                              FixedWidthFormatter.Flow.WRAP,
                              8);

    final String text2 =
      "Simmer, simmer, simmer down. Simmer, simmer, simmer down. " +
      "Don't waste your precious breath explaining that you are worthwhile.";

    final String answerText2 =
      "Simmer, \n" +
      "simmer, \n" +
      "simmer d\n" +
      "own. Sim\n" +
      "mer, sim\n" +
      "mer, sim\n" +
      "mer down\n" +
      ". Don't \n" +
      "waste yo\n" +
      "ur preci\n" +
      "ous brea\n" +
      "th expla\n" +
      "ining th\n" +
      "at you a\n" +
      "re worth\n" +
      " while. ";

    final String answer2 = formatter2.format(text2);
    assertEquals(answerText2, answer2);
    assertEquals(answerText2, formatter2.format(text2));

    final String text3 = "Embrace the senile genius.";

    final String answerText3 =
      "E\nm\nb\nr\na\nc\ne\n \nt\nh\ne\n \ns\ne\nn\ni\nl\ne\n \n" +
      "g\ne\nn\ni\nu\ns\n.";

    final MultiLineFormatter formatter3 =
      new FixedWidthFormatter(FixedWidthFormatter.Align.RIGHT,
                              FixedWidthFormatter.Flow.WRAP,
                              1);

    final String answer3 = formatter3.format(text3);
    assertEquals(answerText3, answer3);
  }

  @Test public void testFlowWordWrap() throws Exception {
    final String text1 =
      "Harness your\thopes\tto the folks with the liquor with the ropes.\n" +
      "Red, red ropes, periscopes;\n    they've got everything will ever " +
      "need stored under the chair.";

    final String answerText1 =
      "Harness your    \n" +
      "hopes to the    \n" +
      "folks with the  \n" +
      "liquor with the \n" +
      "ropes.          \n" +
      "Red, red ropes, \n" +
      "periscopes;     \n" +
      "    they've got \n" +
      "everything will \n" +
      "ever need stored\n" +
      "under the chair.";

    final MultiLineFormatter formatter1 =
      new FixedWidthFormatter(FixedWidthFormatter.Align.LEFT,
                              FixedWidthFormatter.Flow.WORD_WRAP,
                              16);

    final String answer1 = formatter1.format(text1);
    assertEquals(answerText1, answer1);

    final String answerText2 =
      "    Harness your\n" +
      "    hopes to the\n" +
      "  folks with the\n" +
      " liquor with the\n" +
      "          ropes.\n" +
      " Red, red ropes,\n" +
      "     periscopes;\n" +
      "     they've got\n" +
      " everything will\n" +
      "ever need stored\n" +
      "under the chair.";

    final MultiLineFormatter formatter2 =
      new FixedWidthFormatter(FixedWidthFormatter.Align.RIGHT,
                              FixedWidthFormatter.Flow.WORD_WRAP,
                              16);

    final String answer2 = formatter2.format(text1);
    assertEquals(answerText2, answer2);

    final String text3 = "Embrace the senile genius.";

    final String answerText3 =
      "Embrac\n" +
      " e the\n" +
      "senile\n" +
      "genius\n" +
      "     .";

    final MultiLineFormatter formatter3 =
      new FixedWidthFormatter(FixedWidthFormatter.Align.RIGHT,
                              FixedWidthFormatter.Flow.WORD_WRAP,
                              6);

    final String answer3 = formatter3.format(text3);
    assertEquals(answerText3, answer3);

    final String text4 = "Space              lah       ";

    final String answerText4 = "Space  \nlah    ";

    final MultiLineFormatter formatter4 =
      new FixedWidthFormatter(FixedWidthFormatter.Align.LEFT,
                              FixedWidthFormatter.Flow.WORD_WRAP,
                              7);

    final String answer4 = formatter4.format(text4);
    assertEquals(answerText4, answer4);
  }

  @Test public void testFlowOverflow() throws Exception {
    final String text1 =
      "Harness your hopes to the folks with the liquor with the ropes.\n" +
      "Red, red ropes, periscopes;\n    they've got everything will ever " +
      "need stored under the chair.";

    final String answerText1 =
      "Harness your hopes to the folks with the liquor with the ropes. " +
      "Red, red ropes, periscopes;     they've got everything will ever " +
      "need stored under the chair.";

    final MultiLineFormatter formatter1 =
      new FixedWidthFormatter(FixedWidthFormatter.Align.LEFT,
                              FixedWidthFormatter.Flow.OVERFLOW,
                              16);

    final String answer1 = formatter1.format(text1);
    assertEquals(answerText1, answer1);
  }

  @Test public void testConstructWithInvalidWidth() throws Exception {
    try {
      new FixedWidthFormatter(FixedWidthFormatter.Align.LEFT,
                              FixedWidthFormatter.Flow.OVERFLOW,
                              0);

      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
    }
  }
}
