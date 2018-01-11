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

package net.grinder.console.swingui;

import java.awt.Toolkit;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;


/**
 * JTextField that only accepts integers within a particular range.
 *
 * @author Philip Aston
 */
class IntegerField extends JTextField {

  private static final Toolkit s_toolkit = Toolkit.getDefaultToolkit();
  private static final double s_log10 = Math.log(10);
  private final int m_minimumValue;
  private final int m_maximumValue;

  private static int log10(long x) {
    // The 1e-10 is a cosmological constant to account for FP
    // rounding errors that occur, e.g. for x=10.
    return (int)((Math.log(x) / s_log10) + 1e-10);
  }

  private static int maxFieldWidth(int minimumValue, int maximumValue) {
    final long min = minimumValue < 0 ? 10 * -minimumValue : minimumValue;
    final long max = maximumValue < 0 ? 10 * -maximumValue : maximumValue;

    return log10(Math.max(Math.abs(min), Math.abs(max))) + 1;
  }

  public IntegerField(int minimumValue, int maximumValue) {
    super(maxFieldWidth(minimumValue, maximumValue));

    if (minimumValue > maximumValue) {
      throw new IllegalArgumentException(
        "Minimum value exceeds maximum value");
    }

    m_minimumValue = minimumValue;
    m_maximumValue = maximumValue;

    setValue(m_minimumValue);
    setDocument(new FormattedDocument());
  }

  public int getValue() {

    try {
      return Integer.parseInt(getText());
    }
    catch (NumberFormatException e) {
      // Occurs if field is blank or "-".
      return m_minimumValue;
    }
  }

  public void setValue(int value) {

    if (value < m_minimumValue || value > m_maximumValue) {
      throw new IllegalArgumentException("Value out of bounds");
    }

    setText(Integer.toString(value));
  }

  /**
   * Extend {@code PlainDocument} to perform our checks.
   */
  public class FormattedDocument extends PlainDocument {

    public void insertString(int offset, String string,
                             AttributeSet attributeSet)
      throws BadLocationException {

      final String currentText = super.getText(0, getLength());

      final String result =
        currentText.substring(0, offset) + string +
        currentText.substring(offset);

      if (m_minimumValue >= 0 || !"-".equals(result)) {
        try {
          final int x = Integer.parseInt(result);

          if (x < m_minimumValue || x > m_maximumValue) {
            s_toolkit.beep();
            return;
          }
        }
        catch (NumberFormatException e) {
          s_toolkit.beep();
          return;
        }
      }

      super.insertString(offset, string, attributeSet);
    }
  }

  public void addChangeListener(final ChangeListener listener) {

    getDocument().addDocumentListener(new DocumentListener() {

        private void notifyChangeListener() {
          listener.stateChanged(new ChangeEvent(this));
        }

        public void changedUpdate(DocumentEvent e) {
          notifyChangeListener();
        }

        public void insertUpdate(DocumentEvent e) {
          notifyChangeListener();
        }

        public void removeUpdate(DocumentEvent e) {
          notifyChangeListener();
        }
      });
  }
}
