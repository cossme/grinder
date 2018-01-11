// Copyright (C) 2007 - 2012 Philip Aston
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
import java.awt.Font;
import java.awt.SystemColor;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import net.grinder.console.common.ConsoleException;
import net.grinder.console.editor.Buffer;
import net.grinder.console.editor.EditorModel;
import net.grinder.translation.Translations;


/**
 * Builds a panel containing status information and controls for the editor.
 *
 * @author Philip Aston
 */
class EditorControls {

  private final JPanel m_panel;

  /**
   * Constructor.
   *
   * @param translations Translation service.
   * @param editorModel The editor model
   * @param labelFont Base font to use for labels.
   * @param editorToolBar The controls tool bar.
   */
  public EditorControls(final Translations translations,
                        final EditorModel editorModel,
                        Font labelFont,
                        JToolBar editorToolBar)
    throws ConsoleException {

    editorToolBar.setFloatable(false);
    editorToolBar.setAlignmentX(Component.LEFT_ALIGNMENT);

    final Font noFileTitleFont = labelFont;
    final Font unmodifiedFileTitleFont = noFileTitleFont.deriveFont(Font.BOLD);
    final Font modifiedFileTitleFont =
      noFileTitleFont.deriveFont(Font.ITALIC | Font.BOLD);

    final JLabel label = new JLabel();

    label.setForeground(SystemColor.textHighlight);

    editorModel.addListener(new EditorModel.AbstractListener() {
      @Override
      public void bufferStateChanged(Buffer buffer) {
        final Buffer selectedBuffer = editorModel.getSelectedBuffer();

        if (selectedBuffer == null) {
          label.setText(translations.translate("console.section/editor"));
          label.setFont(noFileTitleFont);
        }
        else if (buffer.equals(selectedBuffer)) {
          label.setText(buffer.getDisplayName());
          label.setFont(buffer.isDirty() ?
                        modifiedFileTitleFont : unmodifiedFileTitleFont);
        }
      }
    });

    m_panel = new JPanel() {
      @Override
      public Dimension getPreferredSize() {
        final Dimension mySize = super.getPreferredSize();
        final Dimension parentSize = getParent().getSize();

        mySize.width = parentSize.width - 4; // Fudge factor

        return mySize;
      }
    };

    m_panel.setLayout(new BoxLayout(m_panel, BoxLayout.X_AXIS));

    m_panel.add(editorToolBar);
    m_panel.add(Box.createHorizontalGlue());
    m_panel.add(label);
    m_panel.setBorder(BorderFactory.createEmptyBorder());
  }

  /**
   * Return the UI component.
   *
   * @return The component.
   */
  public JComponent getComponent() {
    return m_panel;
  }
}
