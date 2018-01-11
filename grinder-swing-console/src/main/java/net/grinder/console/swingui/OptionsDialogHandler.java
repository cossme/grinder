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

package net.grinder.console.swingui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager.LookAndFeelInfo;

import net.grinder.common.GrinderException;
import net.grinder.communication.CommunicationDefaults;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.Resources;
import net.grinder.console.model.ConsoleProperties;


/**
 * Wrap up all the machinery used to show the options dialog.
 *
 * @author Philip Aston
 */
abstract class OptionsDialogHandler {
  private final JFrame m_parentFrame;
  private final LookAndFeel m_lookAndFeel;
  private final Resources m_resources;

  private final LookAndFeelInfo[] m_installedLookAndFeels;

  /** A working copy of console properties. */
  private final ConsoleProperties m_properties;

  private final JTextField m_consoleHost = new JTextField();
  private final IntegerField m_consolePort =
    new IntegerField(CommunicationDefaults.MIN_PORT,
                     CommunicationDefaults.MAX_PORT);
  private final JTextField m_httpHost = new JTextField();
  private final IntegerField m_httpPort =
    new IntegerField(CommunicationDefaults.MIN_PORT,
                     CommunicationDefaults.MAX_PORT);
  private final SamplingControlPanel m_samplingControlPanel;
  private final JSlider m_sfSlider = new JSlider(1, 6, 1);
  private final JCheckBox m_resetConsoleWithProcessesCheckBox;
  private final JComboBox m_lookAndFeelComboBox;
  private final JOptionPaneDialog m_dialog;
  private final JTextField m_externalEditorCommand = new JTextField(20);
  private final JTextField m_externalEditorArguments = new JTextField(20);

  /**
   * Constructor.
   *
   * @param parentFrame Parent frame.
   * @param lookAndFeel The look and feel manager.
   * @param properties A {@link
   * net.grinder.console.model.ConsoleProperties} associated with
   * the properties file to save to.
   * @param resources Resources object to use for strings and things.
   */
  public OptionsDialogHandler(JFrame parentFrame,
                              LookAndFeel lookAndFeel,
                              ConsoleProperties properties,
                              final Resources resources) {

    m_parentFrame = parentFrame;
    m_lookAndFeel = lookAndFeel;
    m_resources = resources;
    m_installedLookAndFeels = lookAndFeel.getInstalledLookAndFeels();
    m_properties = new ConsoleProperties(properties);

    final JPanel addressLabelPanel = new JPanel(new GridLayout(0, 1, 0, 1));
    addressLabelPanel.add(
      new JLabel(m_resources.getString("consoleHost.label")));
    addressLabelPanel.add(
      new JLabel(m_resources.getString("consolePort.label")));
    addressLabelPanel.add(
      new JLabel(m_resources.getString("httpHost.label")));
    addressLabelPanel.add(
      new JLabel(m_resources.getString("httpPort.label")));

    final JPanel addressFieldPanel = new JPanel(new GridLayout(0, 1, 0, 1));
    addressFieldPanel.add(m_consoleHost);
    addressFieldPanel.add(m_consolePort);
    addressFieldPanel.add(m_httpHost);
    addressFieldPanel.add(m_httpPort);

    final JPanel addressPanel = new JPanel();
    addressPanel.setLayout(new BoxLayout(addressPanel, BoxLayout.X_AXIS));
    addressPanel.add(addressLabelPanel);
    addressPanel.add(Box.createHorizontalStrut(5));
    addressPanel.add(addressFieldPanel);

    // Use BorderLayout so the address panel uses its preferred
    // height, and full available width. Sadly I couldn't find a more
    // straightforward way.
    final JPanel communicationTab = new JPanel(new BorderLayout());
    communicationTab.add(addressPanel, BorderLayout.NORTH);
    communicationTab.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    m_samplingControlPanel = new SamplingControlPanel(m_resources);

    final JPanel samplingControlTab = new JPanel(new BorderLayout());
    samplingControlTab.add(m_samplingControlPanel, BorderLayout.NORTH);
    samplingControlTab.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    m_sfSlider.setMajorTickSpacing(1);
    m_sfSlider.setPaintLabels(true);
    m_sfSlider.setSnapToTicks(true);
    final Dimension d = m_sfSlider.getPreferredSize();
    d.width = 0;
    m_sfSlider.setPreferredSize(d);

    final JPanel sfPanel = new JPanel(new GridLayout(0, 2));
    sfPanel.add(new JLabel(m_resources.getString("significantFigures.label")));
    sfPanel.add(m_sfSlider);

    final JPanel editorLabelPanel = new JPanel(new GridLayout(0, 1, 0, 1));
    editorLabelPanel.add(
      new JLabel(m_resources.getString("externalEditorCommand.label")));
    editorLabelPanel.add(
      new JLabel(m_resources.getString("externalEditorArguments.label")));

    final JPanel editorFieldPanel = new JPanel(new GridLayout(0, 1, 0, 1));
    final JPanel commandPanel = new JPanel(new BorderLayout());
    commandPanel.add(m_externalEditorCommand);
    final JButton chooseExternalEditorButton = new JButton();

    chooseExternalEditorButton.setAction(new ChooseCommandAction());

    commandPanel.add(chooseExternalEditorButton, BorderLayout.EAST);
    editorFieldPanel.add(commandPanel);
    editorFieldPanel.add(m_externalEditorArguments);

    final JPanel editorPanel = new JPanel();
    editorPanel.setLayout(new BoxLayout(editorPanel, BoxLayout.X_AXIS));
    editorPanel.add(editorLabelPanel);
    editorPanel.add(Box.createHorizontalStrut(5));
    editorPanel.add(editorFieldPanel);

    final JPanel editorTab = new JPanel(new BorderLayout());
    editorTab.add(editorPanel, BorderLayout.NORTH);
    editorTab.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    m_resetConsoleWithProcessesCheckBox =
      new JCheckBox(m_resources.getString("resetConsoleWithProcesses.label"));
    final JPanel checkBoxPanel = new JPanel();
    checkBoxPanel.add(m_resetConsoleWithProcessesCheckBox);

    final String[] lookAndFeelLabels =
      new String[m_installedLookAndFeels.length];

    for (int i = 0; i < m_installedLookAndFeels.length; ++i) {
      lookAndFeelLabels[i] = m_installedLookAndFeels[i].getName();
    }

    m_lookAndFeelComboBox = new JComboBox(lookAndFeelLabels);

    final JPanel lookAndFeelPanel = new JPanel(new GridLayout(0, 2));
    lookAndFeelPanel.add(
      new JLabel(m_resources.getString("lookAndFeel.label")));
    lookAndFeelPanel.add(m_lookAndFeelComboBox);

    final JPanel miscellaneousPanel = new JPanel();
    miscellaneousPanel.setLayout(
      new BoxLayout(miscellaneousPanel, BoxLayout.Y_AXIS));
    miscellaneousPanel.add(sfPanel);
    miscellaneousPanel.add(checkBoxPanel);
    miscellaneousPanel.add(lookAndFeelPanel);

    final JPanel miscellaneousTab =
      new JPanel(new FlowLayout(FlowLayout.LEFT));
    miscellaneousTab.add(miscellaneousPanel);

    final JTabbedPane tabbedPane = new JTabbedPane();

    tabbedPane.addTab(m_resources.getString("options.communicationTab.title"),
                      null, communicationTab,
                      m_resources.getString("options.communicationTab.tip"));

    tabbedPane.addTab(m_resources.getString("options.samplingTab.title"),
                      null, samplingControlTab,
                      m_resources.getString("options.samplingTab.tip"));

    tabbedPane.addTab(m_resources.getString("options.editorTab.title"),
                      null, editorTab,
                      m_resources.getString("options.editorTab.tip"));

    tabbedPane.addTab(m_resources.getString("options.miscellaneousTab.title"),
                      null, miscellaneousTab,
                      m_resources.getString("options.miscellaneousTab.tip"));

    final Object[] options = {
      m_resources.getString("options.ok.label"),
      m_resources.getString("options.cancel.label"),
      m_resources.getString("options.save.label"),
    };

    final JOptionPane optionPane =
      new JOptionPane(tabbedPane, JOptionPane.PLAIN_MESSAGE, 0, null, options);

    // The SamplingControlPanel will automatically update m_properties.
    m_samplingControlPanel.setProperties(m_properties);

    m_dialog =
      new JOptionPaneDialog(m_parentFrame,
                            m_resources.getString("options.label"),
                            true,
                            optionPane) {

        protected boolean shouldClose() {
          final Object value = optionPane.getValue();

          if (value == options[1]) {
            return true;
          }
          else {
            try {
              setProperties(m_properties);
            }
            catch (ConsoleException e) {
              new ErrorDialogHandler(m_dialog, m_resources, null)
              .handleException(e);
              return false;
            }

            if (value == options[2]) {
              try {
                m_properties.save();
              }
              catch (GrinderException e) {
                final Throwable cause = e.getCause();

                final String messsage =
                  (cause != null ? cause : e).getMessage();

                new ErrorDialogHandler(m_dialog, m_resources, null)
                .handleErrorMessage(messsage,
                                    m_resources.getString("fileError.title"));
                return false;
              }
            }

            // Success.
            setNewOptions(m_properties);
            return true;
          }
        }
      };

    m_lookAndFeel.addListener(
      new LookAndFeel.ComponentListener(m_dialog) {
        public void lookAndFeelChanged() {
          super.lookAndFeelChanged();
          m_dialog.pack();
        }
      });

    m_dialog.pack();
  }

  private void setProperties(ConsoleProperties properties)
    throws ConsoleException {

    properties.setConsoleHost(m_consoleHost.getText());
    properties.setConsolePort(m_consolePort.getValue());
    properties.setHttpHost(m_httpHost.getText());
    properties.setHttpPort(m_httpPort.getValue());
    properties.setExternalEditorCommand(
      new File(m_externalEditorCommand.getText()));
    properties.setExternalEditorArguments(m_externalEditorArguments.getText());
    properties.setSignificantFigures(m_sfSlider.getValue());
    properties.setResetConsoleWithProcesses(
      m_resetConsoleWithProcessesCheckBox.isSelected());

    final int lookAndFeelIndex = m_lookAndFeelComboBox.getSelectedIndex();

    if (lookAndFeelIndex > -1) {
      properties.setLookAndFeel(
        m_installedLookAndFeels[lookAndFeelIndex].getClassName());
    }
  }

  /**
   * Show the dialog.
   *
   * @param initialProperties A set of properties to initialise the
   * options with.
   */
  public void showDialog(ConsoleProperties initialProperties) {
    m_properties.set(initialProperties);

    // Initialise input values.
    m_consoleHost.setText(m_properties.getConsoleHost());
    m_consolePort.setValue(m_properties.getConsolePort());
    m_httpHost.setText(m_properties.getHttpHost());
    m_httpPort.setValue(m_properties.getHttpPort());

    final File externalEditor = m_properties.getExternalEditorCommand();
    m_externalEditorCommand.setText(
      externalEditor != null ? externalEditor.getAbsolutePath() : "");

    m_externalEditorArguments.setText(
      m_properties.getExternalEditorArguments());
    m_sfSlider.setValue(m_properties.getSignificantFigures());
    m_resetConsoleWithProcessesCheckBox.setSelected(
      m_properties.getResetConsoleWithProcesses());

    final String currentLookAndFeel = m_properties.getLookAndFeel();
    int currentLookAndFeelIndex = -1;

    if (currentLookAndFeel != null) {
      for (int i = 0; i < m_installedLookAndFeels.length; ++i) {
        if (currentLookAndFeel.equals(
              m_installedLookAndFeels[i].getClassName()))  {
          currentLookAndFeelIndex = i;
        }
      }
    }

    m_lookAndFeelComboBox.setSelectedIndex(currentLookAndFeelIndex);

    m_samplingControlPanel.refresh();

    m_dialog.setLocationRelativeTo(m_parentFrame);
    SwingUtilities.updateComponentTreeUI(m_dialog);
    m_dialog.setVisible(true);
  }

  /**
   * User should override this to handle new options set by the dialog.
   */
  protected abstract void setNewOptions(ConsoleProperties newOptions);

  private final class ChooseCommandAction extends CustomAction {
    private final JFileChooser m_fileChooser = new JFileChooser(".");

    ChooseCommandAction() {
      super(m_resources, null, true);

      putValue(Action.NAME, "...");

      m_fileChooser.setDialogTitle(
        m_resources.getString("choose-external-editor.label"));

      m_fileChooser.setSelectedFile(
        m_properties.getExternalEditorCommand());

      m_lookAndFeel.addListener(
        new LookAndFeel.ComponentListener(m_fileChooser));
    }

    public void actionPerformed(ActionEvent event) {
      final String buttonText =
        m_resources.getString("choose-external-editor.label");

      if (m_fileChooser.showDialog(m_parentFrame, buttonText) ==
          JFileChooser.APPROVE_OPTION) {

        m_externalEditorCommand.setText(
          m_fileChooser.getSelectedFile().getAbsolutePath());
      }
    }
  }
}
