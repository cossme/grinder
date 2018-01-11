// Copyright (C) 2000 - 2008 Philip Aston
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

import java.text.DecimalFormat;
import java.text.FieldPosition;


/**
 * Java doesn't provide a NumberFormatter which understands
 * significant figures, this is a cheap and cheerful one. Not
 * extensively tested.
 *
 * @author Philip Aston
 */
public class SignificantFigureFormat extends DecimalFormat {

  private static final long serialVersionUID = 1;

  private static final int s_decimalPlaces = 40;

  private final int m_significantFigures;

  /**
   * Constructor.
   *
   * @param significantFigures Number of significant figures.
   */
  public SignificantFigureFormat(int significantFigures) {

    // 40 DP, should match value of s_decimalPlaces.
    super("0.0000000000000000000000000000000000000000");

    m_significantFigures = significantFigures;
  }

  private static int boundingPowerOfTen(double number) {

    if (number == 0d ||
        Double.isInfinite(number) ||
        Double.isNaN(number)) {
      return 1;
    }

    final double abs = Math.abs(number);

    int i = 0;
    double x = 1;

    if (abs < 1) {
      while (x > abs) {
        x /= 10;
        --i;
      }

      return i + 1;
    }
    else {
      while (!(x > abs)) {
        x *= 10;
        ++i;
      }

      return i;
    }
  }

  /**
   * Almost certainly doesn't set position correctly.
   *
   * @param number Number to format.
   * @param buffer Buffer to append result to.
   * @param position Field position.
   * @return a <code>StringBuffer</code> value
   */
  public StringBuffer format(double number, StringBuffer buffer,
                             FieldPosition position) {

    if (Double.isInfinite(number) ||
        Double.isNaN(number)) {
      return super.format(number, buffer, position);
    }

    final int shift = boundingPowerOfTen(number) - m_significantFigures;
    final double factor = Math.pow(10, shift);

    super.format(factor * Math.round(number / factor), buffer, position);

    final int truncate =
      shift < 0 ? s_decimalPlaces + shift : s_decimalPlaces + 1;

    buffer.setLength(buffer.length() - truncate);

    return buffer;
  }

  /**
   * Almost certainly doesn't set position correctly.
   *
   * @param number Number to format.
   * @param buffer Buffer to append result to.
   * @param position Field position.
   * @return a <code>StringBuffer</code> value
   */
  public StringBuffer format(long number, StringBuffer buffer,
                             FieldPosition position) {

    return format((double)number, buffer, position);
  }

  /**
   * {@inheritDoc}
   */
  @Override public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o == this) {
      return true;
    }

    return
      super.equals(o) &&
      m_significantFigures == ((SignificantFigureFormat)o).m_significantFigures;
  }

  /**
   * {@inheritDoc}
   */
  @Override public int hashCode() {
    return super.hashCode();
  }
}
