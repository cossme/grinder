// Copyright (C) 2000 - 2013 Philip Aston
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

import static net.grinder.console.swingui.MnemonicHeuristics.removeMnemonicMarkers;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import net.grinder.common.Closer;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.UncheckedInterruptedException;
import net.grinder.console.ConsoleFoundation;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.distribution.AgentCacheState;
import net.grinder.console.distribution.FileChangeWatcher;
import net.grinder.console.distribution.FileDistribution;
import net.grinder.console.distribution.FileDistributionHandler;
import net.grinder.console.editor.Buffer;
import net.grinder.console.editor.EditorModel;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.SampleListener;
import net.grinder.console.model.SampleModel;
import net.grinder.console.model.SampleModelViews;
import net.grinder.statistics.StatisticsSet;
import net.grinder.translation.Translations;
import net.grinder.util.Directory;
import net.grinder.util.FileContents;
import net.grinder.util.thread.Condition;

import org.slf4j.Logger;

/**
 * Swing UI for console.
 *
 * Thinking about hacking this? See
 * http://madbean.com/blog/2004/17/totallygridbag.html first.
 *
 * @author Philip Aston
 */
public final class ConsoleUI implements ConsoleFoundation.UI {

  // Do not initialise any Swing components before the constructor has
  // set the Look and Feel to avoid loading the default Look and Feel
  // unnecessarily.
  private final LookAndFeel m_lookAndFeel;

  private final ActionTable m_actionTable = new ActionTable();
  private final CloseFileAction m_closeFileAction;
  private final StartAction m_startAction;
  private final ExitAction m_exitAction;
  private final StopAction m_stopAction;
  private final SaveFileAction m_saveFileAction;
  private final SaveFileAsAction m_saveFileAsAction;
  private final DistributeFilesAction m_distributeFilesAction;

  private final Resources m_resources;
  private final Translations m_translations;
  private final ConsoleProperties m_properties;
  private final SampleModel m_model;
  private final SampleModelViews m_sampleModelViews;
  private final ProcessControl m_processControl;
  private final FileDistribution m_fileDistribution;
  private final EditorModel m_editorModel;

  private final JFrame m_frame;
  private final FrameBounds m_frameBounds;
  private final JLabel m_stateLabel;
  private final SamplingControlPanel m_samplingControlPanel;
  private final ErrorHandler m_errorHandler;
  private final OptionalConfirmDialog m_optionalConfirmDialog;
  private final Font m_titleLabelFont;

  private final CumulativeStatisticsTableModel m_cumulativeTableModel;

  /**
   * Constructor.
   *
   * @param resources Resources.
   * @param translations Translation service.
   * @param consoleProperties Console properties.
   * @param model The console model.
   * @param sampleModelViews Console sample model views.
   * @param processControl ProcessReport control.
   * @param fileDistribution File distribution.
   * @param logger Logger to use.
   * @exception ConsoleException if an error occurs
   */
  // CHECKSTYLE.OFF: MethodLengthCheck
  public ConsoleUI(Resources resources,
                   Translations translations,
                   ConsoleProperties consoleProperties,
                   SampleModel model,
                   SampleModelViews sampleModelViews,
                   ProcessControl processControl,
                   FileDistribution fileDistribution,
                   Logger logger)
    throws ConsoleException {

    m_resources = resources;
    m_translations = translations;
    m_properties = consoleProperties;
    m_model = model;
    m_sampleModelViews = sampleModelViews;
    m_processControl = processControl;
    m_fileDistribution = fileDistribution;

    // Create the frame to contain the a menu and the top level pane.
    // Do before actions are constructed as we use the frame to create dialogs.
    m_frame = new JFrame(m_translations.translate("console/title"));

    final ErrorDialogHandler errorDialogHandler =
      new ErrorDialogHandler(m_frame, m_translations, logger);

    m_errorHandler =
      new SwingDispatcherFactoryImplementation(null).create(ErrorHandler.class,
                                                            errorDialogHandler);

    final SwingDispatcherFactory swingDispatcherFactory =
      new SwingDispatcherFactoryImplementation(m_errorHandler);

    // LookAndFeel constructor will set initial Look and Feel from properties.
    m_lookAndFeel = new LookAndFeel(m_properties, swingDispatcherFactory);

    errorDialogHandler.registerWithLookAndFeel(m_lookAndFeel);

    m_editorModel = new EditorModel(m_translations,
                                    new Editor.TextSourceFactory(),
                                    m_fileDistribution.getAgentCacheState(),
                                    m_fileDistribution);

    m_editorModel.setExternalEditor(m_properties.getExternalEditorCommand(),
                                    m_properties.getExternalEditorArguments());
    m_editorModel.setSelectedPropertiesFile(m_properties.getPropertiesFile());

    m_properties.addPropertyChangeListener(
      new PropertyChangeListener()  {
        public void propertyChange(PropertyChangeEvent e) {
          if (e.getPropertyName().equals(
                ConsoleProperties.EXTERNAL_EDITOR_COMMAND_PROPERTY) ||
              e.getPropertyName().equals(
                ConsoleProperties.EXTERNAL_EDITOR_ARGUMENTS_PROPERTY)) {
            m_editorModel.setExternalEditor(
              m_properties.getExternalEditorCommand(),
              m_properties.getExternalEditorArguments());
          }
          else if (e.getPropertyName().equals(
                ConsoleProperties.PROPERTIES_FILE_PROPERTY)) {
            m_editorModel.setSelectedPropertiesFile(
              m_properties.getPropertiesFile());
          }
        }
      });

    m_optionalConfirmDialog =
      new OptionalConfirmDialog(m_frame, m_translations, m_properties);

    m_closeFileAction = new CloseFileAction();
    m_exitAction = new ExitAction();
    m_startAction = new StartAction();
    m_stopAction = new StopAction();
    m_saveFileAction = new SaveFileAction();
    m_saveFileAsAction = new SaveFileAsAction();
    m_distributeFilesAction = new DistributeFilesAction();

    m_actionTable.add(m_closeFileAction);
    m_actionTable.add(m_exitAction);
    m_actionTable.add(m_startAction);
    m_actionTable.add(m_stopAction);
    m_actionTable.add(m_saveFileAsAction);
    m_actionTable.add(m_distributeFilesAction);
    m_actionTable.add(new AboutAction(m_resources.getImageIcon("logo.image")));
    m_actionTable.add(new ChooseDirectoryAction());
    m_actionTable.add(new StartProcessesAction());
    m_actionTable.add(new NewFileAction());
    m_actionTable.add(new OptionsAction());
    m_actionTable.add(new ResetProcessesAction());
    m_actionTable.add(new SaveFileAction());
    m_actionTable.add(new SaveResultsAction());
    m_actionTable.add(new StopProcessesAction());

    m_stateLabel = new JLabel();
    stateChanged();
    m_samplingControlPanel = new SamplingControlPanel(m_translations);

    final JPanel controlAndTotalPanel = createControlAndTotalPanel();

    // Create the tabbed test display.
    final JTabbedPane tabbedPane = new JTabbedPane();

    final TestGraphPanel graphPanel =
      new TestGraphPanel(tabbedPane,
                         m_model,
                         m_sampleModelViews,
                         m_resources,
                         translations,
                         swingDispatcherFactory);
    graphPanel.resetTests(); // Show logo.

    final JScrollPane graphTabPane =
      new JScrollPane(graphPanel,
                      JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    graphTabPane.setBorder(BorderFactory.createEmptyBorder());

    tabbedPane.addTab(m_translations.translate("console.section/graphs"),
                      m_resources.getImageIcon("graphTab.image"),
                      graphTabPane,
                      m_translations.translate(
                        "console.section/graphs-detail"));

    m_titleLabelFont =
      new JLabel().getFont().deriveFont(Font.PLAIN | Font.ITALIC);

    m_cumulativeTableModel =
      new CumulativeStatisticsTableModel(
        m_model, m_sampleModelViews, m_translations, swingDispatcherFactory);

    final JScrollPane cumulativeTablePane =
      new JScrollPane(new Table(m_cumulativeTableModel));

    cumulativeTablePane.setBorder(
      createTitledBorder("console.section/cumulative-data"));
    cumulativeTablePane.setMinimumSize(new Dimension(100, 60));

    final SampleStatisticsTableModel sampleModel =
      new SampleStatisticsTableModel(
        m_model, m_sampleModelViews, m_translations, swingDispatcherFactory);

    final JScrollPane sampleTablePane = new JScrollPane(new Table(sampleModel));
    sampleTablePane.setBorder(
      createTitledBorder("console.section/sample-data"));
    sampleTablePane.setMinimumSize(new Dimension(100, 60));

    final JSplitPane resultsPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                                  cumulativeTablePane,
                                                  sampleTablePane);

    resultsPane.setOneTouchExpandable(true);
    resultsPane.setResizeWeight(1.0d);
    resultsPane.setBorder(BorderFactory.createEmptyBorder());

    tabbedPane.addTab(m_translations.translate("console.section/results"),
                      m_resources.getImageIcon("resultsTab.image"),
                      resultsPane,
                      m_translations.translate(
                        "console.section/results-detail"));

    final ProcessStatusTableModel processStatusModel =
      new ProcessStatusTableModel(m_translations,
                                  m_processControl,
                                  swingDispatcherFactory);

    final JScrollPane processStatusPane =
      new JScrollPane(new Table(processStatusModel));

    processStatusPane.setBorder(
      createTitledBorder("console.section/processes-detail"));

    tabbedPane.addTab(m_translations.translate("console.section/processes"),
                      m_resources.getImageIcon(
                        "processStatusTableTab.image"),
                      processStatusPane,
                      m_translations.translate(
                        "console.section/processes-detail"));

    final JToolBar editorToolBar = new JToolBar();
    new ToolBarAssembler(editorToolBar, true).populate("editor.toolbar");

    final Font editorSmallFont =
      m_titleLabelFont.deriveFont(Font.PLAIN)
        .deriveFont(m_titleLabelFont.getSize2D() * 0.8f);

    final EditorControls editorControls =
      new EditorControls(
        m_translations, m_editorModel, editorSmallFont, editorToolBar);

    final Editor editor = new Editor(m_editorModel, m_saveFileAction);

    final FileTreeModel fileTreeModel =
      new FileTreeModel(m_editorModel,
                        m_fileDistribution.getDistributionFileFilter(),
                        m_properties.getDistributionDirectory().getFile());

    m_properties.addPropertyChangeListener(
      new PropertyChangeListener()  {
        public void propertyChange(PropertyChangeEvent e) {
          if (e.getPropertyName().equals(
                ConsoleProperties.DISTRIBUTION_DIRECTORY_PROPERTY)) {
            fileTreeModel.setRootDirectory(
              m_properties.getDistributionDirectory().getFile());
          }
        }
      });

    m_fileDistribution.addFileChangedListener(
      swingDispatcherFactory.create(
        FileChangeWatcher.FileChangedListener.class,
        fileTreeModel.new RefreshChangedDirectoriesListener()));

    final JPopupMenu fileTreePopupMenu = new JPopupMenu();

    final FileTree fileTree = new FileTree(m_resources,
                                           m_translations,
                                           getErrorHandler(),
                                           m_editorModel,
                                           new BufferTreeModel(m_editorModel),
                                           fileTreeModel,
                                           editorSmallFont,
                                           fileTreePopupMenu,
                                           m_properties);

    final CustomAction[] fileTreeActions = fileTree.getActions();

    for (int i = 0; i < fileTreeActions.length; ++i) {
      m_actionTable.add(fileTreeActions[i]);
    }

    new PopupMenuAssembler(fileTreePopupMenu).populate(
      "editor.filetree.popupmenu");

    final JPanel editorPanel = new JPanel();
    editorPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
    editorPanel.add(editorControls.getComponent());
    editorPanel.add(editor.getComponent());

    final JToolBar fileTreeToolBar = new JToolBar();
    new ToolBarAssembler(fileTreeToolBar, true).populate("filetree.toolbar");
    fileTreeToolBar.setFloatable(false);
    fileTreeToolBar.setAlignmentX(Component.LEFT_ALIGNMENT);

    final JComponent fileTreeComponent = fileTree.getComponent();
    fileTreeComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
    fileTreeComponent.setPreferredSize(new Dimension(200, 100));

    final JPanel fileTreeControlPanel = new JPanel();
    fileTreeControlPanel.setLayout(
      new BoxLayout(fileTreeControlPanel, BoxLayout.Y_AXIS));
    fileTreeControlPanel.setBorder(BorderFactory.createEmptyBorder());
    fileTreeControlPanel.add(fileTreeToolBar);
    fileTreeControlPanel.add(fileTreeComponent);

    final JSplitPane scriptPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                                 fileTreeControlPanel,
                                                 editorPanel);

    scriptPane.setOneTouchExpandable(true);
    scriptPane.setBorder(BorderFactory.createEmptyBorder());

    tabbedPane.addTab(m_translations.translate("console.section/editor"),
                      m_resources.getImageIcon("scriptTab.image"),
                      scriptPane,
                      m_translations.translate(
                        "console.section/editor-detail"));

    final JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.add(controlAndTotalPanel, BorderLayout.WEST);
    contentPanel.add(tabbedPane, BorderLayout.CENTER);

    // Create a panel to hold the tool bar and the test pane.
    final JPanel toolBarPanel = new JPanel(new BorderLayout());
    final JToolBar mainToolBar = new JToolBar();
    new ToolBarAssembler(mainToolBar, false).populate("main.toolbar");
    toolBarPanel.add(mainToolBar, BorderLayout.NORTH);
    toolBarPanel.add(contentPanel, BorderLayout.CENTER);

    m_frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    m_frame.addWindowListener(new WindowCloseAdapter());

    final Container topLevelPane = m_frame.getContentPane();
    final JMenuBar menuBar = new JMenuBar();
    new MenuBarAssembler(menuBar).populate("menubar");
    topLevelPane.add(menuBar, BorderLayout.NORTH);
    topLevelPane.add(toolBarPanel, BorderLayout.CENTER);

    final ImageIcon logoIcon = m_resources.getImageIcon("logo.image");

    if (logoIcon != null) {
      final Image logoImage = logoIcon.getImage();

      if (logoImage != null) {
        // We've a high resolution image, but the Ubuntu Unity switcher
        // renders it in low resolution. The launcher looks OK though.
        // I think this is a Unity bug.
        m_frame.setIconImage(logoImage);
      }
    }

    m_model.addModelListener(
      swingDispatcherFactory.create(
        SampleModel.Listener.class,
        new SampleModel.AbstractListener() {
          @Override
          public void stateChanged() { ConsoleUI.this.stateChanged(); }
        }
      ));

    m_lookAndFeel.addListener(new LookAndFeelListener());

    m_frameBounds = new FrameBounds(m_properties, m_frame);
    m_frameBounds.restore();

    resultsPane.setDividerLocation(resultsPane.getMaximumDividerLocation());

    m_frame.setVisible(true);
  }

  private TitledBorder createTitledBorder(String titleTranslationKey) {
    final TitledBorder border =
      BorderFactory.createTitledBorder(
        BorderFactory.createEmptyBorder(3, 3, 3, 3),
        m_translations.translate(titleTranslationKey));

    border.setTitleFont(m_titleLabelFont);
    border.setTitleColor(SystemColor.textInactiveText);
    border.setTitleJustification(TitledBorder.RIGHT);

    return border;
  }

  private JPanel createControlAndTotalPanel() {
    final LabelledGraph totalGraph =
      new LabelledGraph(m_translations.translate("console.term/total"),
                        m_translations, SystemColor.window,
                        m_model.getTPSExpression(),
                        m_model.getPeakTPSExpression(),
                        m_sampleModelViews.getTestStatisticsQueries());

    final JLabel tpsLabel = new JLabel();
    tpsLabel.setFont(new Font("helvetica", Font.ITALIC | Font.BOLD, 40));
    tpsLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

    m_model.addTotalSampleListener(
      new SampleListener() {
        private final String m_suffix =
          ' ' + m_translations.translate("console.term/tps");

        public void update(StatisticsSet intervalStatistics,
                           StatisticsSet cumulativeStatistics) {
          final NumberFormat format = m_sampleModelViews.getNumberFormat();

          tpsLabel.setText(
            format.format(
              m_model.getTPSExpression().getDoubleValue(intervalStatistics)) +
            m_suffix);

          totalGraph.add(intervalStatistics, cumulativeStatistics, format);
        }
      });

    final JButton stateButton = new CustomJButton();
    stateButton.setBorderPainted(true);
    stateButton.setAction(m_stopAction);
    stateButton.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    m_stopAction.registerButton(stateButton);

    m_stateLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));

    final JPanel statePanel = new JPanel();
    statePanel.setLayout(new BoxLayout(statePanel, BoxLayout.X_AXIS));
    statePanel.add(stateButton);
    statePanel.add(m_stateLabel);

    statePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    m_samplingControlPanel.add(Box.createRigidArea(new Dimension(0, 40)));
    m_samplingControlPanel.add(statePanel);

    m_samplingControlPanel.setBorder(
      BorderFactory.createEmptyBorder(10, 10, 0, 10));
    m_samplingControlPanel.setProperties(m_properties);

    final JPanel controlAndTotalPanel = new JPanel();
    controlAndTotalPanel.setLayout(
      new BoxLayout(controlAndTotalPanel, BoxLayout.Y_AXIS));

    m_samplingControlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    tpsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    totalGraph.setAlignmentX(Component.LEFT_ALIGNMENT);

    controlAndTotalPanel.add(m_samplingControlPanel);
    controlAndTotalPanel.add(Box.createRigidArea(new Dimension(0, 100)));
    controlAndTotalPanel.add(tpsLabel);
    controlAndTotalPanel.add(Box.createRigidArea(new Dimension(0, 20)));
    controlAndTotalPanel.add(totalGraph);

    final JPanel hackToFixLayout = new JPanel();
    hackToFixLayout.add(controlAndTotalPanel);

    return hackToFixLayout;
  }
  // CHECKSTYLE.ON: MethodLengthCheck

  private final class LookAndFeelListener
    extends LookAndFeel.ComponentListener {

    private LookAndFeelListener() {
      super(m_frame);
    }

    @Override
    public void lookAndFeelChanged() {
      m_frame.setVisible(false);

      try {
        m_frameBounds.store();
      }
      catch (ConsoleException e) {
        getErrorHandler().handleException(e);
      }

      super.lookAndFeelChanged();

      m_frameBounds.restore();
      m_frame.setVisible(true);
    }
  }

  private abstract class ListTokeniserTemplate {
    private final JComponent m_component;

    protected ListTokeniserTemplate(JComponent component) {
      m_component = component;
    }

    public void populate(String key) {
      final String tokens = m_resources.getString(key);
      final List<Object> tokenList =
        Collections.list(new StringTokenizer(tokens));

      for (Object itemKey : tokenList) {
        if ("-".equals(itemKey)) {
          dash();
        }
        else if (">".equals(itemKey)) {
          greaterThan();
        }
        else {
          token((String)itemKey);
        }
      }
    }

    protected final JComponent getComponent() {
      return m_component;
    }

    protected void dash() { }
    protected void greaterThan() { }
    protected abstract void token(String key);
  }

  /** Work around polymorphic interface that's missing from Swing. */
  private abstract class AbstractMenuAssembler extends ListTokeniserTemplate {

    protected AbstractMenuAssembler(JComponent component) {
      super(component);
      new MnemonicHeuristics(component);
    }

    @Override
    protected void token(String menuItemKey) {
      final JMenuItem menuItem = new JMenuItem() {

        @Override
        public Dimension getPreferredSize() {
          final Dimension d = super.getPreferredSize();
          d.height = (int) (d.height * 0.9);
          return d;
        }
      };

      m_actionTable.setAction(menuItem, menuItemKey);

      final Icon icon = menuItem.getIcon();

      final Icon rolloverIcon =
        (Icon) menuItem.getAction().getValue(CustomAction.ROLLOVER_ICON);

      menuItem.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          menuItem.setIcon(menuItem.isArmed() ? rolloverIcon : icon);
        }
      });

      getComponent().add(menuItem);
    }
  }

  private final class MenuAssembler extends AbstractMenuAssembler {

    protected MenuAssembler(JMenu component) {
      super(component);
    }

    @Override
    protected void dash() {
      ((JMenu)getComponent()).addSeparator();
    }
  }

  private final class PopupMenuAssembler extends AbstractMenuAssembler {

    protected PopupMenuAssembler(JPopupMenu component) {
      super(component);

      component.addContainerListener(new ContainerAdapter() {
        @Override
        public void componentAdded(ContainerEvent e) {
          if (e.getChild() instanceof JMenuItem) {
            final JMenuItem menuItem = (JMenuItem)e.getChild();

            menuItem.setVisible(
              ((CustomAction)menuItem.getAction()).isRelevantToSelection());

            menuItem.getAction().addPropertyChangeListener(
              new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                  if (evt.getPropertyName().equals(
                    CustomAction.RELEVANT_TO_SELECTION)) {
                    menuItem.setVisible(
                      ((CustomAction)menuItem.getAction())
                      .isRelevantToSelection());
                  }
                }
              }
            );
          }
        }
      });
    }

    @Override
    protected void dash() {
      ((JPopupMenu)getComponent()).addSeparator();
    }
  }

  private final class MenuBarAssembler extends ListTokeniserTemplate {

    protected MenuBarAssembler(JComponent component) {
      super(component);
      new MnemonicHeuristics(component);
    }

    @Override
    protected void greaterThan() {
      getComponent().add(Box.createHorizontalGlue());
    }

    @Override
    protected void token(String key) {
      final JMenu menu =
        new JMenu(m_translations.translate("console.menu/" + key));

      new MenuAssembler(menu).populate(key + ".menu");

      getComponent().add(menu);
    }
  }

  private final class ToolBarAssembler extends ListTokeniserTemplate {

    private final boolean m_small;

    protected ToolBarAssembler(JComponent component, boolean small) {
      super(component);
      m_small = small;
    }

    @Override
    protected void dash() {
      ((JToolBar)getComponent()).addSeparator();
    }

    @Override
    protected void token(String key) {
      final JButton button = new CustomJButton();

      if (m_small) {
        button.setBorder(BorderFactory.createEmptyBorder());
      }

      getComponent().add(button);

      // Must set the action _after_ adding to the tool bar or the
      // rollover image isn't set correctly.
      m_actionTable.setAction(button, key);
    }
  }

  private static class ActionTable {
    private final Map<String, CustomAction> m_map =
      new HashMap<String, CustomAction>();

    public void add(CustomAction action) {
      m_map.put(action.getKey(), action);
    }

    // CHECKSTYLE.OFF: IllegalType - AbstractButton
    public void setAction(AbstractButton button, String actionKey) {
    // CHECKSTYLE.ON: IllegalType
      final CustomAction action = m_map.get(actionKey);

      if (action != null) {
        button.setAction(action);
        action.registerButton(button);
      }
      else {
        // System.err.println("Action '" + actionKey + "' not found");
        button.setEnabled(false);
      }
    }
  }

  private void stateChanged() {
    final SampleModel.State state = m_model.getState();

    m_stateLabel.setText(state.getDescription());

    switch (state.getValue()) {
      case Recording:
        m_stateLabel.setForeground(Colours.DARK_GREEN);
        break;

      case Stopped:
        m_stateLabel.setForeground(Colours.DARK_RED);
        m_stopAction.stopped();
        break;

      default:
        m_stateLabel.setForeground(SystemColor.controlText);
    }
  }

  private final class WindowCloseAdapter extends WindowAdapter {
    @Override
    public void windowClosing(WindowEvent e) {
      m_exitAction.exit();
    }
  }

  private final class SaveResultsAction extends CustomAction {
    private final JFileChooser m_fileChooser = new JFileChooser(".");
    private final JCheckBox m_saveTotalsCheckBox;

    SaveResultsAction() {
      super(m_resources, m_translations, "save-results", true);

      m_fileChooser.setDialogTitle(
        removeMnemonicMarkers(
          m_translations.translate("console.action/save-results")));

      m_fileChooser.setSelectedFile(
        new File(m_translations.translate("console.option/default-filename")));

      m_saveTotalsCheckBox =
        new JCheckBox(
          m_translations.translate("console.option/save-totals-with-results"));
      m_saveTotalsCheckBox.setSelected(
        m_properties.getSaveTotalsWithResults());

      m_fileChooser.setAccessory(m_saveTotalsCheckBox);

      m_lookAndFeel.addListener(
        new LookAndFeel.ComponentListener(m_fileChooser));
    }

    public void actionPerformed(ActionEvent event) {
      if (m_fileChooser.showSaveDialog(m_frame) ==
          JFileChooser.APPROVE_OPTION) {

        final File file = m_fileChooser.getSelectedFile();

        if (file.exists() &&
            JOptionPane.showConfirmDialog(
              m_frame,
              m_translations.translate("console.phrase/overwrite-confirmation"),
              file.toString(),
              JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
          return;
        }

        FileWriter writer = null;
        try {
          writer = new FileWriter(file);

          final String lineSeparator = System.getProperty("line.separator");

          if (m_saveTotalsCheckBox.isSelected()) {
            m_cumulativeTableModel.write(writer,
                                         "\t",
                                         lineSeparator);
          }
          else {
            m_cumulativeTableModel.writeWithoutTotals(writer,
                                                      "\t",
                                                      lineSeparator);
          }
        }
        catch (IOException e) {
          UncheckedInterruptedException.ioException(e);
          getErrorHandler().handleErrorMessage(
            e.getMessage(),
            m_translations.translate("console.phrase/file-error"));
        }
        finally {
          Closer.close(writer);
        }

        try {
          m_properties.setSaveTotalsWithResults(
            m_saveTotalsCheckBox.isSelected());
        }
        catch (ConsoleException e) {
          getErrorHandler().handleException(e);
        }
      }
    }
  }

  private final class OptionsAction extends CustomAction {
    private final OptionsDialogHandler m_optionsDialogHandler;

    OptionsAction() {
      super(m_resources, m_translations, "options", true);

      m_optionsDialogHandler =
        new OptionsDialogHandler(m_frame,
                                 m_lookAndFeel,
                                 m_resources,
                                 m_translations,
                                 m_properties) {
          @Override
          protected void setNewOptions(ConsoleProperties newOptions) {
            m_properties.set(newOptions);
            m_samplingControlPanel.refresh();
          }
        };
    }

    public void actionPerformed(ActionEvent event) {
      m_optionsDialogHandler.showDialog(m_properties);
    }
  }

  private final class AboutAction extends CustomAction {

    private final ImageIcon m_logoIcon;

    AboutAction(ImageIcon logoIcon) {
      super(m_resources, m_translations, "about", true);
      m_logoIcon = logoIcon;
    }

    public void actionPerformed(ActionEvent event) {

      final String title =
        removeMnemonicMarkers(m_translations.translate("console.dialog/about"));

      final String aboutText =
          readResource(
            m_translations.translate("console.dialog/about.text"));

      final JEditorPane htmlPane = new JEditorPane("text/html", aboutText);
      htmlPane.setEditable(false);
      htmlPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      htmlPane.setBackground(new JLabel().getBackground());

      final JScrollPane contents =
        new JScrollPane(htmlPane,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
          @Override
          public Dimension getPreferredSize() {
            final Dimension d = super.getPreferredSize();
            d.width = 500;
            d.height = 400;
            return d;
          }
        };

      htmlPane.setCaretPosition(0);

      JOptionPane.showMessageDialog(m_frame, contents, title,
                                    JOptionPane.PLAIN_MESSAGE,
                                    m_logoIcon);
    }
  }

  private String readResource(String name) {
    final URL resource = getClass().getClassLoader().getResource(name);

    if (resource != null) {
      Reader in = null;

      try {
        in = new InputStreamReader(resource.openStream());

        final StringWriter out = new StringWriter();

        final char[] buffer = new char[1024];

        while (true) {
          final int n = in.read(buffer);

          if (n == -1) {
            break;
          }

          out.write(buffer, 0, n);
        }

        out.close();

        return out.toString();
      }
      catch (final IOException e) {
        UncheckedInterruptedException.ioException(e);
      }
      finally {
        Closer.close(in);
      }
    }

    return "Could not read " + name;
  }

  private final class ExitAction extends CustomAction {

    ExitAction() {
      super(m_resources, m_translations, "exit");
    }

    public void actionPerformed(ActionEvent e) {
      exit();
    }

    void exit() {
      try {
        m_frameBounds.store();
      }
      catch (ConsoleException e) {
        getErrorHandler().handleException(e);
      }

      final Buffer[] buffers = m_editorModel.getBuffers();

      for (int i = 0; i < buffers.length; ++i) {
        if (!m_closeFileAction.closeBuffer(buffers[i])) {
          return;
        }
      }

      System.exit(0);
    }
  }

  private final class StartAction extends CustomAction {
    StartAction() {
      super(m_resources, m_translations, "start");
    }

    public void actionPerformed(ActionEvent e) {
      m_model.start();

      //  putValue() won't work here as the event won't fire if
      //  the value doesn't change.
      firePropertyChange(SET_ACTION_PROPERTY, null, m_stopAction);
    }
  }

  private final class StopAction extends CustomAction {
    StopAction() {
      super(m_resources, m_translations, "stop");
    }

    public void actionPerformed(ActionEvent e) {
      m_model.stop();
      stopped();
    }

    public void stopped() {
      //  putValue() won't work here as the event won't fire if
      //  the value doesn't change.
      firePropertyChange(SET_ACTION_PROPERTY, null, m_startAction);
    }
  }

  private final class NewFileAction extends CustomAction {
    public NewFileAction() {
      super(m_resources, m_translations, "new-file");
    }

    public void actionPerformed(ActionEvent event) {
      m_editorModel.selectNewBuffer();
    }
  }

  private final class SaveFileAction extends CustomAction {
    public SaveFileAction() {
      super(m_resources, m_translations, "save-file");

      m_editorModel.addListener(new EditorModel.AbstractListener() {
          @Override
          public void bufferStateChanged(Buffer ignored) {
            setEnabled(shouldEnable());
          }
        });

      setEnabled(shouldEnable());
    }

    private boolean shouldEnable() {
      final Buffer buffer = m_editorModel.getSelectedBuffer();

      return buffer != null && buffer.isDirty();
    }

    public void actionPerformed(ActionEvent event) {
      try {
        final Buffer buffer = m_editorModel.getSelectedBuffer();

        if (buffer.getFile() != null) {
          if (!buffer.isUpToDate() &&
              JOptionPane.showConfirmDialog(
                m_frame,
                m_translations.translate(
                  "console.phrase/out-of-date-overwrite-confirmation"),
                buffer.getFile().toString(),
                JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
            return;
          }

          buffer.save();
        }
        else {
          m_saveFileAsAction.saveBufferAs(buffer);
        }
      }
      catch (ConsoleException e) {
        getErrorHandler().handleException(e);
      }
    }
  }

  private final class SaveFileAsAction extends CustomAction {

    private final JFileChooser m_fileChooser = new JFileChooser(".");

    public SaveFileAsAction() {
      super(m_resources, m_translations, "save-file-as", true);

      m_editorModel.addListener(new EditorModel.AbstractListener() {
          @Override
          public void bufferStateChanged(Buffer ignored) {
            setEnabled(shouldEnable());
          }
        });

      setEnabled(shouldEnable());

      m_fileChooser.setDialogTitle(
        removeMnemonicMarkers(
          m_translations.translate("console.action/save-file-as")));

      final String pythonFilesText =
          m_translations.translate("console.section/scripts");

      m_fileChooser.addChoosableFileFilter(
        new FileFilter() {
          @Override
          public boolean accept(File file) {
            return m_editorModel.isScriptFile(file) || file.isDirectory();
          }

          @Override
          public String getDescription() {
            return pythonFilesText;
          }
        });

      m_lookAndFeel.addListener(
        new LookAndFeel.ComponentListener(m_fileChooser));
    }

    private boolean shouldEnable() {
      return m_editorModel.getSelectedBuffer() != null;
    }

    public void actionPerformed(ActionEvent event) {
      try {
        saveBufferAs(m_editorModel.getSelectedBuffer());
      }
      catch (ConsoleException e) {
        getErrorHandler().handleException(e);
      }
    }

    void saveBufferAs(Buffer buffer) throws ConsoleException {
      final File currentFile = buffer.getFile();
      final Directory distributionDirectory =
        m_properties.getDistributionDirectory();

      if (currentFile != null) {
        m_fileChooser.setSelectedFile(currentFile);
      }
      else {
        m_fileChooser.setCurrentDirectory(distributionDirectory.getFile());
      }

      if (m_fileChooser.showSaveDialog(m_frame) !=
          JFileChooser.APPROVE_OPTION) {
        return;
      }

      final File file = m_fileChooser.getSelectedFile();

      if (!distributionDirectory.isParentOf(file) &&
        JOptionPane.showConfirmDialog(
          m_frame,
          m_translations.translate(
            "console.phrase/save-outside-of-distribution-confirmation"),
          (String) getValue(NAME),
          JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
        return;
      }

      if (!file.equals(currentFile)) {
        // Save as.
        final Buffer oldBuffer = m_editorModel.getBufferForFile(file);

        if (oldBuffer != null) {
          final List<String> messages = new ArrayList<String>();
          messages.add(
            m_translations.translate(
              "console.phrase/ignore-existing-buffer-confirmation"));

          if (oldBuffer.isDirty()) {
            messages.add(
              m_translations.translate(
                "console.phrase/existing-buffer-has-unsaved-changes"));
          }

          if (!oldBuffer.isUpToDate()) {
            messages.add(
              m_translations.translate(
                "console.phrase/existing-buffer-out-of-date"));
          }

          messages.add(
            m_translations.translate(
              "console.phrase/ignore-existing-buffer-confirmation2"));

          if (JOptionPane.showConfirmDialog(
                m_frame, messages.toArray(), file.toString(),
                JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
            return;
          }

          m_editorModel.closeBuffer(oldBuffer);
        }
        else {
          if (file.exists() &&
              JOptionPane.showConfirmDialog(
                m_frame,
                m_translations.translate(
                  "console.phrase/overwrite-confirmation"),
                file.toString(),
                JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
            return;
          }
        }
      }
      else {
        // We only need to check whether the current buffer is up to date
        // for Save, not Save As.
        if (!buffer.isUpToDate() &&
            JOptionPane.showConfirmDialog(
              m_frame,
              m_translations.translate(
                "console.phrase/out-of-date-overwrite-confirmation"),
              buffer.getFile().toString(),
              JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
          return;
        }
      }

      buffer.save(file);
    }
  }

  private final class CloseFileAction extends CustomAction {
    public CloseFileAction() {
      super(m_resources, m_translations, "close-file");

      m_editorModel.addListener(new EditorModel.AbstractListener() {
          @Override
          public void bufferStateChanged(Buffer ignored) {
            setEnabled(shouldEnable());
          }
        });

      setEnabled(shouldEnable());
    }

    private boolean shouldEnable() {
      return m_editorModel.getSelectedBuffer() != null;
    }

    public void actionPerformed(ActionEvent event) {
      closeBuffer(m_editorModel.getSelectedBuffer());
    }

    boolean closeBuffer(Buffer buffer) {
      if (buffer != null) {
        while (buffer.isDirty()) {
          // Loop until we've saved the buffer successfully or
          // canceled.

          final String confirmationMessage =
              m_translations.translate(
                "console.phrase/save-modified-buffer-confirmation",
                buffer.getDisplayName());

          final int chosen =
            JOptionPane.showConfirmDialog(m_frame,
                                          confirmationMessage,
                                          (String) getValue(NAME),
                                          JOptionPane.YES_NO_CANCEL_OPTION);

          if (chosen == JOptionPane.YES_OPTION) {
            try {
              if (buffer.getFile() != null) {
                buffer.save();
              }
              else {
                m_saveFileAsAction.saveBufferAs(buffer);
              }
            }
            catch (GrinderException e) {
              getErrorHandler().handleException(e);
              return false;
            }
          }
          else if (chosen == JOptionPane.NO_OPTION) {
            break;
          }
          else {
            return false;
          }
        }

        m_editorModel.closeBuffer(buffer);
      }

      return true;
    }
  }

  private class EnableIfAgentsConnected implements ProcessControl.Listener {

    private final Action m_action;

    EnableIfAgentsConnected(Action action) {
      m_action = action;
      enableOrDisable();
    }

    public final void update(ProcessControl.ProcessReports[] processStatuses) {
      enableOrDisable();
    }

    protected final void enableOrDisable() {
      m_action.setEnabled(shouldEnable());
    }

    protected boolean shouldEnable() {
      return m_processControl.getNumberOfLiveAgents() > 0;
    }
  }

  private class StartProcessesAction extends CustomAction {

    StartProcessesAction() {
      super(m_resources, m_translations, "start-processes");
      m_processControl.addProcessStatusListener(
        new EnableIfAgentsConnected(this));
    }

    public void actionPerformed(final ActionEvent event) {
      try {
        final File propertiesFile = m_editorModel.getSelectedPropertiesFile();

        if (propertiesFile == null) {
          final int chosen =
            m_optionalConfirmDialog.show(
              m_translations.translate(
                "console.phrase/properties-not-set-confirmation"),
              (String) getValue(NAME),
              JOptionPane.OK_CANCEL_OPTION,
              "propertiesNotSetAsk");

          if (chosen != JOptionPane.OK_OPTION &&
              chosen != OptionalConfirmDialog.DONT_ASK_OPTION) {
            return;
          }

          m_processControl.startWorkerProcesses(new GrinderProperties());
        }
        else {
          if (m_editorModel.isABufferDirty()) {
            final int chosen =
              m_optionalConfirmDialog.show(
                m_translations.translate(
                    "console.phrase/start-with-unsaved-buffers-confirmation"),
                (String) getValue(NAME),
                JOptionPane.OK_CANCEL_OPTION,
                "startWithUnsavedBuffersAsk");

            if (chosen != JOptionPane.OK_OPTION &&
                chosen != OptionalConfirmDialog.DONT_ASK_OPTION) {
              return;
            }
          }

          if (m_fileDistribution.getAgentCacheState().getOutOfDate()) {
            final int chosen =
              m_optionalConfirmDialog.show(
                m_translations.translate(
                    "console.phrase/caches-out-of-date-confirmation"),
                (String) getValue(NAME),
                JOptionPane.OK_CANCEL_OPTION,
                "distributeOnStartAsk");

            if (chosen != JOptionPane.OK_OPTION &&
                chosen != OptionalConfirmDialog.DONT_ASK_OPTION) {
              return;
            }

            // The distribution is done in a background thread. When it
            // completes, it dispatches our callback in the Swing thread.
            m_distributeFilesAction.distribute(
              new Runnable() {
                public void run() {
                  StartProcessesAction.this.actionPerformed(event);
                }
              });

            return;
          }

          m_processControl.startWorkerProcessesWithDistributedFiles(
            m_properties.getDistributionDirectory(),
            new GrinderProperties(propertiesFile));
        }
      }
      catch (GrinderException e) {
        getErrorHandler().handleException(e);
      }
    }
  }

  private final class ResetProcessesAction extends CustomAction {
    ResetProcessesAction() {
      super(m_resources, m_translations, "reset-processes");
      m_processControl.addProcessStatusListener(
        new EnableIfAgentsConnected(this));
    }

    public void actionPerformed(ActionEvent event) {

      final ConsoleProperties properties = m_properties;

      try {
        final int chosen =
          m_optionalConfirmDialog.show(
            m_translations.translate(
              "console.phrase/reset-console-with-processes-confirmation"),
            (String) getValue(NAME),
            JOptionPane.YES_NO_CANCEL_OPTION,
            "resetConsoleWithProcessesAsk");

        switch (chosen) {
        case JOptionPane.YES_OPTION:
          properties.setResetConsoleWithProcesses(true);
          break;

        case JOptionPane.NO_OPTION:
          properties.setResetConsoleWithProcesses(false);
          break;

        case OptionalConfirmDialog.DONT_ASK_OPTION:
          break;

        default:
          return;
        }
      }
      catch (GrinderException e) {
        getErrorHandler().handleException(e);
        return;
      }

      if (properties.getResetConsoleWithProcesses()) {
        m_model.reset();
        m_sampleModelViews.resetStatisticsViews();
      }

      m_processControl.resetWorkerProcesses();
    }
  }

  private final class StopProcessesAction extends CustomAction {
    StopProcessesAction() {
      super(m_resources, m_translations, "stop-processes");
      m_processControl.addProcessStatusListener(
        new EnableIfAgentsConnected(this));
    }

    public void actionPerformed(ActionEvent event) {

      try {
        final int chosen =
          m_optionalConfirmDialog.show(
            m_translations.translate(
                "console.phrase/stop-proceses-confirmation"),
            (String) getValue(NAME),
            JOptionPane.OK_CANCEL_OPTION,
            "stopProcessesAsk");

        if (chosen != JOptionPane.OK_OPTION &&
            chosen != OptionalConfirmDialog.DONT_ASK_OPTION) {
          return;
        }
      }
      catch (GrinderException e) {
        getErrorHandler().handleException(e);
        return;
      }

      m_processControl.stopAgentAndWorkerProcesses();
    }
  }

  private final class ChooseDirectoryAction extends CustomAction {
    private final JFileChooser m_fileChooser = new JFileChooser(".");

    ChooseDirectoryAction() {
      super(m_resources, m_translations, "choose-directory", true);

      // Sigh - should have a better pattern for dispatching construction
      // activity on the swing thread.
      try {
        SwingUtilities.invokeAndWait(new Runnable() {
          public void run() {
            m_fileChooser.setDialogTitle(
              m_translations.translate(
                "console.action/choose-directory-detail"));

            m_fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            m_fileChooser.setSelectedFile(
              m_properties.getDistributionDirectory().getFile());
          }
          });
      }
      catch (InterruptedException e) {
        throw new UncheckedInterruptedException(e);
      }
      catch (InvocationTargetException e) {
        throw new AssertionError(e);
      }

      m_lookAndFeel.addListener(
        new LookAndFeel.ComponentListener(m_fileChooser));
    }

    public void actionPerformed(ActionEvent event) {
      try {
        final String title =
          MnemonicHeuristics.removeMnemonicMarkers(
            m_translations.translate("console.action/choose-directory"));

        if (m_fileChooser.showDialog(m_frame, title) ==
            JFileChooser.APPROVE_OPTION) {

          final Directory directory =
            new Directory(m_fileChooser.getSelectedFile());
          final File file = directory.getFile();

          if (!file.exists()) {
            if (JOptionPane.showConfirmDialog(
                  m_frame,
                  m_translations.translate("console.prhase/create-directory"),
                  file.toString(),
                  JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
              return;
            }

            directory.create();
          }

          final ConsoleProperties properties = m_properties;
          properties.setAndSaveDistributionDirectory(directory);
        }
      }
      catch (IOException e) {
        UncheckedInterruptedException.ioException(e);
        getErrorHandler().handleException(e);
      }
      catch (GrinderException e) {
        getErrorHandler().handleException(e);
      }
    }
  }

  private final class DistributeFilesAction extends CustomAction {

    private final Condition m_cacheStateCondition = new Condition();

    DistributeFilesAction() {
      super(m_resources, m_translations, "distribute-files");

      final AgentCacheState agentCacheState =
        m_fileDistribution.getAgentCacheState();

      agentCacheState.addListener(new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ignored) {
            setEnabled(shouldEnable());
            synchronized (m_cacheStateCondition) {
              m_cacheStateCondition.notifyAll();
            }
          }
        });

      setEnabled(shouldEnable());
    }

    private boolean shouldEnable() {
      return m_fileDistribution.getAgentCacheState().getOutOfDate();
    }

    public void actionPerformed(ActionEvent event) {
      distribute(null);
    }

    public void distribute(final Runnable onCompletionCallback) {
      final FileDistributionHandler distributionHandler =
        m_fileDistribution.getHandler();

      final ProgressMonitor progressMonitor =
        new ProgressMonitor(m_frame, getValue(NAME), "", 0, 100);
      progressMonitor.setMillisToDecideToPopup(0);
      progressMonitor.setMillisToPopup(0);

      final Runnable distributionRunnable = new Runnable() {
          public void run() {
            while (!progressMonitor.isCanceled()) {
              try {
                final FileDistributionHandler.Result result =
                  distributionHandler.sendNextFile();

                if (result == null) {
                  break;
                }

                progressMonitor.setProgress(result.getProgressInCents());
                progressMonitor.setNote(result.getFileName());
              }
              catch (FileContents.FileContentsException e) {
                // We don't want to put a dialog in the user's face
                // for every problem. Lets just log the the terminal
                // until we have a proper console log.
                e.printStackTrace();
              }
            }

            progressMonitor.close();

            if (onCompletionCallback != null) {
              // The cache status is updated asynchronously by agent reports.
              // If we have a listener, we wait for up to five seconds for all
              // agents to indicate that they are up to date.
              synchronized (m_cacheStateCondition) {
                for (int i = 0; i < 5 && shouldEnable(); ++i) {
                  m_cacheStateCondition.waitNoInterrruptException(1000);
                }
              }

              SwingUtilities.invokeLater(onCompletionCallback);
            }
          }
        };

      new Thread(distributionRunnable).start();
    }
  }

  /**
   * Return an error handler that other classes can use to report
   * problems through the UI.
   *
   * @return The exception handler.
   */
  public ErrorHandler getErrorHandler() {
    return m_errorHandler;
  }
}
