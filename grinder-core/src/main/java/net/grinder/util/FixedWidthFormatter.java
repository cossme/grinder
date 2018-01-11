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


/**
 * <p>{@link MultiLineFormatter} for fixed width text cells.</p>
 *
 * <p>All white space is converted to plain spaces.</p>
 *
 * <p>When flow policy is {@code WORD_WRAP}, newlines in the
 * source are treated as preferred line breaks.</p>
 *
 * @author Philip Aston
 */
public final class FixedWidthFormatter extends AbstractMultiLineFormatter {

  /**
   * Alignment constants.
   */
  public enum Align {

    /** Left alignment. */
    LEFT {
      void pad(StringBuilder buffer, int remainder) {
        buffer.append(s_space, 0, remainder);
      }
    },

    /** Centre alignment. */
    CENTRE {
      void pad(StringBuilder buffer, int remainder) {
        final int charsLeft = (remainder + 1) / 2;
        final int charsRight = remainder / 2;
        buffer.insert(0, s_space, 0, charsLeft);
        buffer.append(s_space, 0, charsRight);
      }
    },

    /** Right alignment. */
    RIGHT {
      void pad(StringBuilder buffer, int remainder) {
        buffer.insert(0, s_space, 0, remainder);
      }
    };

    abstract void pad(StringBuilder buffer, int space);

    /** Blank space to copy for padding. **/
    private static final char[] s_space = new char[256];

    static {
      for (int i = 0; i < s_space.length; i++) {
        s_space[i] = ' ';
      }
    }
  }

  /**
   * Flow constants.
   */
  public enum Flow {
    /** The flow should be truncated. */
    TRUNCATE {
      void flow(StringBuilder buffer, StringBuilder remainder, int width) {
        if (buffer.length() > width) {
          buffer.setLength(width);
        }
      }
    },

    /** The flow should be wrapped. */
    WRAP {
      void flow(StringBuilder buffer, StringBuilder remainder, int width) {
        if (buffer.length() > width) {
          // We prepend our remainder to the existing one.
          remainder.insert(0, buffer.substring(width));

          // Truncate.
          buffer.setLength(width);
        }
      }
    },

    /** The flow should be word-wrapped. */
    WORD_WRAP {
      /**
       * <p>
       * Search to set splitPosition. Algorithm is first match from:
       * <ol>
       * <li>First new line in first m_width + 1 characters (we replace all new
       * lines with splits)</li>
       * <li>Last white space in first m_width + 1 characters</li>
       *<li>The full width</li>
       * </ol>
       * </p>
       *
       * <p>
       * If the buffer is less than width wide, only new lines are taken into
       * account.
       * </p>
       *
       * @param buffer
       *          The buffer.
       * @return Split position in range [0, m_width]
       */
      private int findWordWrapSplitPosition(StringBuilder buffer, int width) {
        final int length = buffer.length();
        final int right = Math.min(length, width);

        int splitPosition = 0;

        while (splitPosition < right) {
          if (buffer.charAt(splitPosition) == '\n') {
            return splitPosition;
          }

          ++splitPosition;
        }

        if (length > width) {
          splitPosition = width;

          do {
            if (Character.isWhitespace(buffer.charAt(splitPosition))) {
              return splitPosition;
            }
          }
          while (--splitPosition >= 0);

          return width;
        }
        else {
          return length;
        }
      }

      void flow(StringBuilder buffer, StringBuilder remainder, int width) {
        final int length = buffer.length();

        final int splitPosition = findWordWrapSplitPosition(buffer, width);

        // Search forward to ignore white space until the first new
        // line, and set everything from there on as the remainder.
        int nextText = splitPosition;

        while (nextText < length) {
          final char c = buffer.charAt(nextText);

          if  (!Character.isWhitespace(c)) {
            break;
          }

          ++nextText;

          if (c == '\n') {
            // If alignment is Align.LEFT, white space after the new
            // line will become leading space on the next line.
            break;
          }
        }

        if (nextText < length) {
          remainder.insert(0, buffer.substring(nextText));
        }

        buffer.setLength(splitPosition);
      }
    },

    /** The flow should overflow. */
    OVERFLOW {
      void flow(StringBuilder buffer, StringBuilder remainder,  int width) {
      }
    };

    abstract void flow(StringBuilder buffer,
                       StringBuilder remainder,
                       int width);
  }

  private final Align m_alignment;
  private final Flow m_flow;
  private final int m_width;

  /**
   * Constructor.
   *
   * @param alignment
   *          Alignment policy. One of { {@code ALIGN_LEFT}, {@code
   *          ALIGN_CENTRE}, {@code ALIGN_RIGHT} }
   * @param flow
   *          Flow policy. One of { {@code FLOW_TRUNCATE}, {@code FLOW_WRAP},
   *          {@code FLOW_WORD_WRAP}, {@code FLOW_OVERFLOW} }
   * @param width
   *          The cell width.
   *
   */
  public FixedWidthFormatter(Align alignment, Flow flow, int width) {

    if (width <= 0) {
      throw new IllegalArgumentException("Invalid width value");
    }

    m_alignment = alignment;
    m_flow = flow;
    m_width = width;
  }

  /**
   * Alter buffer to contain a single line according to the policy of
   * the formatter. Insert remaining text at the start of
   * {@code remainder}.
   *
   * @param buffer Buffer to transform to a single line.
   * @param remainder Leftovers.
   */
  public void transform(StringBuilder buffer, StringBuilder remainder) {

    m_flow.flow(buffer, remainder, m_width);

    final int length = buffer.length();

    // Canonicalise white space.
    for (int k = 0; k < length; k++) {
      if (Character.isWhitespace(buffer.charAt(k))) {
        buffer.setCharAt(k, ' ');
      }
    }

    if (length < m_width) {
      // Buffer is less than width, pad.

      m_alignment.pad(buffer, m_width - length);
    }
  }
}
