// Copyright (C)  2003 - 2013 Philip Aston
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.common.ErrorHandler;
import net.grinder.translation.Translations;

import org.slf4j.Logger;


/**
 * Wrap up all the machinery used to show an error dialog.
 *
 * @author Philip Aston
 */
final class ErrorDialogHandler implements ErrorHandler {

  private final JOptionPane m_optionPane =
    new JOptionPane(null, JOptionPane.ERROR_MESSAGE, 0, null, null);
  private JOptionPaneDialog m_dialog;
  private final String m_errorTitle;
  private final String m_unexpectedErrorTitle;
  private final String m_errorDetailsTitle;
  private final Object[] m_okOptions;
  private final Object[] m_okDetailsOptions;
  private final Object[] m_detailsOptions;

  private Throwable m_throwable;
  private final Logger m_logger;

  /**
   * Constructor.
   *
   * @param frame Parent frame.
   * @param translations Translation service.
   * @param logger Logger to use for information messages.
   */
  public ErrorDialogHandler(JFrame frame,
                            Translations translations,
                            Logger logger) {
    this(translations, logger);

    m_dialog = new JOptionPaneDialog(frame, null, true, m_optionPane) {
        @Override
        protected boolean shouldClose() {
          return ErrorDialogHandler.this.shouldClose();
        }
      };
  }

  /**
   * Constructor.
   *
   * @param dialog Parent frame.
   * @param translations Translation service.
   * @param logger Logger to use for information messages.XS
   */
  public ErrorDialogHandler(JDialog dialog,
                            Translations translations,
                            Logger logger) {
    this(translations, logger);

    m_dialog = new JOptionPaneDialog(dialog, null, true, m_optionPane) {
        @Override
        protected boolean shouldClose() {
          return ErrorDialogHandler.this.shouldClose();
        }
      };
  }

  public void registerWithLookAndFeel(LookAndFeel lookAndFeel) {
    lookAndFeel.addListener(new LookAndFeel.ComponentListener(m_dialog));
  }

  private ErrorDialogHandler(Translations translations, Logger logger) {
    m_logger = logger;

    m_errorTitle = translations.translate("console.dialog/error");
    m_unexpectedErrorTitle =
        translations.translate("console.phrase/unexpected-error");
    m_errorDetailsTitle =
        translations.translate("console.dialog/error-details");

    final String okText = translations.translate("console.action/ok");
    final String detailsClipboardText =
      translations.translate("console.action/copy-to-clipboard");

    m_okOptions = new Object[] { okText, };
    m_okDetailsOptions = new Object[] { okText, m_errorDetailsTitle, };
    m_detailsOptions = new Object[] { okText, detailsClipboardText, };
  }

  private boolean shouldClose() {
    final Object value = m_optionPane.getValue();

    if (m_throwable != null &&
        value == m_okDetailsOptions[1]) {

      // Details dialog.

      // Sometimes the standard Java library both sucks _and_ blows.
      // We need the line separators in stack traces in a standard
      // format so we can parse them to HTML, and also copy them to
      // the clipboard as unicode. PrintWriter has no such interface.
      // We could extend PrintWriter and override println, but that
      // doesn't help if the exception printStackTrace method uses
      // PrintWriters internally (as GrinderException does). Time for
      // a judicious hack.
      final String oldLineSeparator = System.getProperty("line.separator");

      try {
        System.setProperty("line.separator", "\n");

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new StackTraceHTMLPrintWriter(stringWriter);
        m_throwable.printStackTrace(writer);
        writer.close();

        final JLabel label = new JLabel();
        label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        label.setText(stringWriter.toString());

        final Component scrollPane =
          new JScrollPane(label,
                          JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                          JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
            @Override
            public Dimension getPreferredSize() {
              final Dimension d = super.getPreferredSize();
              d.width = 600;
              d.height = 250;
              return d;
            }
          };

        final JOptionPane detailsOptionPane =
          new JOptionPane(scrollPane, JOptionPane.INFORMATION_MESSAGE);

        detailsOptionPane.setOptions(m_detailsOptions);

        final JOptionPaneDialog detailsDialog =
          new JOptionPaneDialog(m_dialog, m_errorDetailsTitle, true,
                                detailsOptionPane) {

            @Override
            protected boolean shouldClose() {
              if (detailsOptionPane.getValue() == m_detailsOptions[1]) {

                // Copy to clipboard.
                final StringWriter plainStackStringWriter = new StringWriter();
                final PrintWriter printWriter =
                  new PrintWriter(plainStackStringWriter);
                m_throwable.printStackTrace(printWriter);
                printWriter.close();

                final StringSelection stringSelection =
                  new StringSelection(plainStackStringWriter.toString());

                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                  stringSelection, stringSelection);

                return false;
              }

              return true;
            }
          };

        detailsDialog.pack();
        detailsDialog.setLocationRelativeTo(detailsDialog.getOwner());
        detailsDialog.setVisible(true);

        return false;
      }
      finally {
        System.setProperty("line.separator", oldLineSeparator);
      }
    }

    // OK
    return true;
  }

  /**
   * Method that handles error messages.
   *
   * @param errorMessage The error message.
   */
  public void handleErrorMessage(String errorMessage) {
    handleErrorMessage(errorMessage, m_errorTitle);
  }

  /**
   * Method that handles error messages.
   *
   * @param errorMessage The error message.
   * @param title A title to use.
   */
  public void handleErrorMessage(String errorMessage, String title) {

    m_throwable = null;

    m_optionPane.setMessage(errorMessage);
    m_optionPane.setOptions(m_okOptions);

    showDialog(title);
  }

  /**
   * Method that handles exceptions.
   *
   * @param throwable The exception.
   */
  public void handleException(Throwable throwable) {
    if (throwable instanceof DisplayMessageConsoleException) {
      handleException(throwable, m_errorTitle);
    }
    else {
      handleException(throwable, m_unexpectedErrorTitle);
    }
  }

  /**
   * Method that handles exceptions.
   *
   * @param throwable The exception.
   * @param title A title to use.
   */
  public void handleException(Throwable throwable, String title) {

    m_throwable = throwable;

    m_optionPane.setMessage(m_throwable.getMessage());
    m_optionPane.setOptions(m_okDetailsOptions);

    showDialog(title);
  }

  /**
   * Method that handles information messages.
   *
   * @param informationMessage The information message.
   */
  public void handleInformationMessage(String informationMessage) {
    m_logger.info(informationMessage);
  }

  private void showDialog(String title) {
    m_dialog.setTitle(title);
    m_dialog.pack();
    m_dialog.setLocationRelativeTo(m_dialog.getOwner());
    m_dialog.setVisible(true);
  }

  /**
   * Format stack traces in in pseudo-HTML form for displaying in a
   * JLabel. This is the result of trial and error as JLabel's
   * comprehension of HTML is rudimentary.
   */
  private static final class StackTraceHTMLPrintWriter extends PrintWriter {

    private static final int NO_INDENT = 0;
    private static final int INDENT = 1;
    private static final int CLOSE_INDENT = 2;

    private int m_state = NO_INDENT;

    private static final String OPEN_TOP_LEVEL_LINE_FORMAT =
      "<font size='-1' color='#000000'><strong>";

    private static final String CLOSE_TOP_LEVEL_LINE_FORMAT =
      "</strong></font>";

    private static final String OPEN_INDENT_FORMAT =
      "<blockquote><font color='#202020' size='-2'>";

    private static final String CLOSE_INDENT_FORMAT =
      "</font></blockquote>";

    public StackTraceHTMLPrintWriter(Writer delegate) {
      super(delegate);
      writeDirect("<html><body>" +
                  OPEN_TOP_LEVEL_LINE_FORMAT);
    }

    @Override
    public void write(char[] buffer, int offset, int length) {

      final int last = offset + length;
      int p = offset;

      for (int i = offset; i < last; ++i) {

        final String replacement = replaceCharacter(buffer[i]);

        if (replacement != null) {
          super.write(buffer, p, i - p);
          p = i + 1;
          writeDirect(replacement);
        }
      }

      super.write(buffer, p, last - p);
    }

    @Override
    public void write(String s, int offset, int length) {
      write(s.toCharArray(), offset, length);
    }

    @Override
    public void write(int c) {
      final String replacement = replaceCharacter(c);

      if (replacement != null) {
        writeDirect(replacement);
      }
      else {
        super.write(c);
      }
    }

    private void writeDirect(String s) {
      final char[] chars = s.toCharArray();
      super.write(chars, 0, chars.length);
    }

    private String replaceCharacter(int c) {

      switch (c) {
      case '\t':
        if (m_state == NO_INDENT) {
          m_state = INDENT;
          return CLOSE_TOP_LEVEL_LINE_FORMAT + OPEN_INDENT_FORMAT;
        }
        else {
          m_state = INDENT;
          return "";
        }

      case '\n':
        return endOfLine();

      case '<':
        return "&lt;";

      case '>':
        return "&gt;";

      default:
        if (m_state == CLOSE_INDENT) {
          m_state = NO_INDENT;
          return CLOSE_INDENT_FORMAT + OPEN_TOP_LEVEL_LINE_FORMAT + c;
        }

        return null;
      }
    }

    private String endOfLine() {
      if (m_state == INDENT) {
        m_state = CLOSE_INDENT;
        return "<p>";
      }
      return "";
    }

    @Override
    public void println() {
      writeDirect(endOfLine());
    }

    @Override
    public void close() {
      writeDirect("</body></html>");
      super.close();
    }
  }
}
