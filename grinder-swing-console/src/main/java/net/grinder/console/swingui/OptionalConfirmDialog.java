// Copyright (C)  2005 - 2013 Philip Aston
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

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.translation.Translations;
import net.grinder.util.BooleanProperty;


/**
 * Optional confirmation dialogs.
 *
 * @author Philip Aston
 */
final class OptionalConfirmDialog {

  private final JFrame m_frame;
  private final Translations m_translations;
  private final ConsoleProperties m_properties;

  /**
   * Option value returned if the user has selected not to be asked
   * again.
   */
   public static final int DONT_ASK_OPTION = 999;

  /**
   * Constructor.
   *
   * @param frame Parent frame.
   * @param translations Translation service.
   * @param properties Console properties.
   */
  public OptionalConfirmDialog(JFrame frame,
                               Translations translations,
                               ConsoleProperties properties) {
    m_frame = frame;
    m_translations = translations;
    m_properties = properties;
  }

  /**
   * Show a confirmation dialog.
   *
   * @param message
   *          The question to ask.
   * @param title
   *          The dialog title.
   * @param optionType
   *          The option types to present. See {@link JOptionPane}.
   * @param askPropertyName
   *          The name of a boolean Console property that is set if the user has
   *          chosen not to display the confirmation.
   * @return The chosen option. This is either a value returned from
   *         {@link JOptionPane#showConfirmDialog(Component, Object)} or
   *         {@link #DONT_ASK_OPTION}.
   * @throws BooleanProperty.PropertyException
   *           If the property could not be read or written.
   * @throws DisplayMessageConsoleException
   *           If a problem occurred persisting the property.
   */
  public int show(String message, String title, int optionType,
                  String askPropertyName)
    throws BooleanProperty.PropertyException, DisplayMessageConsoleException {

    final BooleanProperty askProperty =
      new BooleanProperty(m_properties, askPropertyName);

    if (!askProperty.get()) {
      return DONT_ASK_OPTION;
    }

    final JCheckBox dontAskMeAgainCheckBox =
      new JCheckBox(
        m_translations.translate("console.phrase/dont-ask-me-again"));
    dontAskMeAgainCheckBox.setAlignmentX(Component.RIGHT_ALIGNMENT);

    final Object[] messageArray = {
      message,
      new JLabel(), // Pad.
      dontAskMeAgainCheckBox,
    };

    final int chosen =
      JOptionPane.showConfirmDialog(m_frame, messageArray, title, optionType);

    if (chosen != JOptionPane.CANCEL_OPTION &&
        chosen != JOptionPane.CLOSED_OPTION) {
      try {
        askProperty.set(!dontAskMeAgainCheckBox.isSelected());
      }
      catch (BooleanProperty.PropertyException e) {
        final Throwable cause = e.getCause();

        if (cause instanceof DisplayMessageConsoleException) {
          throw (DisplayMessageConsoleException)cause;
        }

        throw e;
      }
    }

    return chosen;
  }
}
