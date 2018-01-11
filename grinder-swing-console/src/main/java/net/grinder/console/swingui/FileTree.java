// Copyright (C) 2004 - 2013 Philip Aston
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.editor.Buffer;
import net.grinder.console.editor.EditorModel;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.swingui.FileTreeModel.FileNode;
import net.grinder.translation.Translations;


/**
 * Panel containing buffer list and file tree.
 *
 * <p>
 * Listens to the Editor Model, and updates a BufferTreeModel and FileTreeModel
 * appropriately.
 * </p>
 *
 * @author Philip Aston
 */
final class FileTree {

  private final Resources m_resources;
  private final Translations m_translations;
  private final ErrorHandler m_errorHandler;
  private final EditorModel m_editorModel;
  private final BufferTreeModel m_bufferTreeModel;
  private final FileTreeModel m_fileTreeModel;
  private final ConsoleProperties m_properties;

  private final JTree m_tree;
  private final OpenAction m_openAction;
  private final OpenExternalAction m_openExternalAction;
  private final SelectPropertiesAction m_selectPropertiesAction;
  private final DeselectPropertiesAction m_deselectPropertiesAction;
  private final JScrollPane m_scrollPane;

  public FileTree(Resources resources,
                  Translations translations,
                  ErrorHandler errorHandler,
                  EditorModel editorModel,
                  BufferTreeModel bufferTreeModel,
                  FileTreeModel fileTreeModel,
                  Font font,
                  JPopupMenu popupMenu,
                  ConsoleProperties properties) {

    m_resources = resources;
    m_translations = translations;
    m_errorHandler = errorHandler;
    m_editorModel = editorModel;
    m_bufferTreeModel = bufferTreeModel;
    m_fileTreeModel = fileTreeModel;
    m_properties = properties;

    final CompositeTreeModel compositeTreeModel = new CompositeTreeModel();

    compositeTreeModel.addTreeModel(m_bufferTreeModel, false);
    compositeTreeModel.addTreeModel(m_fileTreeModel, true);

    m_tree = new JTree(compositeTreeModel) {
        // A new CustomTreeCellRenderer needs to be set whenever the
        // L&F changes because its superclass constructor reads the
        // resources.
        @Override
        public void updateUI() {
          super.updateUI();

          // Unfortunately updateUI is called from the JTree
          // constructor and we can't use the nested
          // CustomTreeCellRenderer until its enclosing class has been
          // fully initialised. We hack to prevent this with the
          // following conditional.
          if (!isRootVisible()) {
            // Changing LAF to metal gets JTree background wrong without this.
            setBackground(new JLabel().getBackground());

            setCellRenderer(
              new CustomTreeCellRenderer(getFont(), getBackground()));
          }
        }
      };

    m_tree.setBackground(new JLabel().getBackground());
    m_tree.setFont(font);

    m_tree.setRootVisible(false);
    m_tree.setShowsRootHandles(true);

    m_tree.setCellRenderer(
      new CustomTreeCellRenderer(m_tree.getFont(), m_tree.getBackground()));
    m_tree.getSelectionModel().setSelectionMode(
      TreeSelectionModel.SINGLE_TREE_SELECTION);

    m_tree.addMouseListener(new MouseListener(popupMenu));

    m_tree.addTreeSelectionListener(new TreeSelectionListener() {
        public void valueChanged(TreeSelectionEvent e) {
          updateActionState();
        }
      });

    m_openAction = new OpenAction();
    m_openExternalAction = new OpenExternalAction();
    m_selectPropertiesAction = new SelectPropertiesAction();
    m_deselectPropertiesAction = new DeselectPropertiesAction();

    // J2SE 1.4 drops the mapping from "ENTER" -> "toggle"
    // (expand/collapse) that J2SE 1.3 has. I like this mapping, so
    // we combine the "toggle" action with our OpenFileAction and let
    // TeeAction figure out which to call based on what's enabled.
    final InputMap inputMap = m_tree.getInputMap();

    inputMap.put(KeyStroke.getKeyStroke("ENTER"), "activateNode");
    inputMap.put(KeyStroke.getKeyStroke("SPACE"), "activateNode");

    final ActionMap actionMap = m_tree.getActionMap();
    actionMap.put("activateNode",
                  new TeeAction(actionMap.get("toggle"), m_openAction));

    m_scrollPane = new JScrollPane(m_tree);
    m_scrollPane.setBorder(BorderFactory.createEtchedBorder());

    m_editorModel.addListener(new EditorModelListener());

    updateActionState();
  }

  private final class MouseListener extends MouseAdapter {
    private final JPopupMenu m_popupMenu;

    private boolean m_handledOnPress;

    private MouseListener(JPopupMenu popupMenu) {
      m_popupMenu = popupMenu;
    }

    @Override
    public void mousePressed(MouseEvent e) {
      m_handledOnPress = false;

      if (!e.isConsumed() && SwingUtilities.isLeftMouseButton(e)) {
        final TreePath path = m_tree.getPathForLocation(e.getX(), e.getY());

        if (path == null) {
          return;
        }

        final Object selectedNode = path.getLastPathComponent();

        if (selectedNode instanceof Node) {
          final Node node = (Node)selectedNode;
          final int clickCount = e.getClickCount();

          final boolean hasBuffer = node.getBuffer() != null;

          if (clickCount == 2 || clickCount == 1 && hasBuffer) {
            m_openAction.invoke(node);
            m_handledOnPress = true;
            e.consume();
          }

          if (clickCount == 2 &&
              hasBuffer &&
              m_selectPropertiesAction.isEnabled()) {
            m_selectPropertiesAction.invoke();
            m_handledOnPress = true;
            e.consume();
          }
        }
      }

      if (e.isPopupTrigger()) {
        m_popupMenu.show(e.getComponent(), e.getX(), e.getY());
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (m_handledOnPress) {
        // Prevent downstream event handlers from overriding our good work.
        e.consume();
      }

      if (e.isPopupTrigger()) {
        m_popupMenu.show(e.getComponent(), e.getX(), e.getY());
      }
    }
  }

  private class EditorModelListener extends EditorModel.AbstractListener {

    @Override
    public void bufferAdded(Buffer buffer) {
      // When a file is opened, the new buffer causes the view to
      // scroll down by one row. This feels wrong, so we compensate.
      final int rowHeight = m_tree.getRowBounds(0).height;
      final JScrollBar verticalScrollBar = m_scrollPane.getVerticalScrollBar();
      verticalScrollBar.setValue(verticalScrollBar.getValue() + rowHeight);
    }

    @Override
    public void bufferStateChanged(Buffer buffer) {
      final File file = buffer.getFile();

      if (file != null) {
        final FileTreeModel.FileNode oldFileNode =
          m_fileTreeModel.findFileNode(buffer);

        // Find a node, if its in our directory structure. This
        // may cause parts of the tree to be refreshed.
        final FileTreeModel.Node node = m_fileTreeModel.findNode(file);

        if (oldFileNode == null || !oldFileNode.equals(node)) {
          // Buffer's associated file has changed.

          if (oldFileNode != null) {
            oldFileNode.setBuffer(null);
          }

          if (node instanceof FileTreeModel.FileNode) {
            final FileTreeModel.FileNode fileNode =
              (FileTreeModel.FileNode)node;

            fileNode.setBuffer(buffer);
            m_tree.scrollPathToVisible(treePathForFileNode(fileNode));
          }
        }
      }

      final FileTreeModel.Node fileNode = m_fileTreeModel.findFileNode(buffer);

      if (fileNode != null) {
        m_fileTreeModel.valueForPathChanged(fileNode.getPath(), fileNode);
      }

      m_bufferTreeModel.bufferChanged(buffer);

      updateActionState();
    }

    @Override
    public void bufferRemoved(Buffer buffer) {
      final FileTreeModel.FileNode fileNode =
        m_fileTreeModel.findFileNode(buffer);

      if (fileNode != null) {
        fileNode.setBuffer(null);
        m_fileTreeModel.valueForPathChanged(fileNode.getPath(), fileNode);
      }
    }
  }

  public JComponent getComponent() {
    return m_scrollPane;
  }

  public CustomAction[] getActions() {
    return new CustomAction[] {
        m_openAction,
        m_openExternalAction,
        m_selectPropertiesAction,
        m_deselectPropertiesAction,
    };
  }

  /**
   * Action for opening the currently selected file in the tree.
   */
  private final class OpenAction extends CustomAction {
    public OpenAction() {
      super(m_resources, m_translations, "open-file");
    }

    public void actionPerformed(ActionEvent event) {
      invoke(m_tree.getLastSelectedPathComponent());
    }

    public void invoke(Object selectedNode) {
      if (selectedNode instanceof BufferTreeModel.BufferNode) {
        m_editorModel.selectBuffer(
          ((BufferTreeModel.BufferNode)selectedNode).getBuffer());
      }
      else if (selectedNode instanceof FileTreeModel.FileNode) {
        final FileNode fileNode = (FileTreeModel.FileNode)selectedNode;

        try {
          fileNode.setBuffer(
            m_editorModel.selectBufferForFile(fileNode.getFile()));

          // The above line can add the buffer to the editor model which
          // causes the BufferTreeModel to fire a top level structure
          // change, which in turn causes the selection to clear. We
          // reselect the original node so our actions are enabled
          // correctly.
          m_tree.setSelectionPath(treePathForFileNode(fileNode));
        }
        catch (ConsoleException e) {
          m_errorHandler.handleException(
            e,
            m_translations.translate("console.phrase/file-error"));
        }
      }
    }
  }


  /**
   * Action for opening the currently selected file in the tree in an external
   * editor.
   */
  private final class OpenExternalAction extends CustomAction {
    public OpenExternalAction() {
      super(m_resources, m_translations, "open-file-external");
    }

    public void actionPerformed(ActionEvent event) {
      final Object selectedNode = m_tree.getLastSelectedPathComponent();

      if (selectedNode instanceof Node) {
        final Node node = (Node)selectedNode;

        final File file = node.getFile();

        if (file != null) {
          final Buffer buffer = node.getBuffer();

          if (buffer != null && buffer.isDirty() &&
                JOptionPane.showConfirmDialog(
                  getComponent(),
                  m_translations.translate(
                    "console.phrase/external-edit-modified-buffer-confirmation"
                    ),
                  file.toString(),
                  JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
              return;
          }

          try {
            m_editorModel.openWithExternalEditor(file);
          }
          catch (ConsoleException e) {
            m_errorHandler.handleException(
              e,
              m_translations.translate("console.phrase/file-error"));
          }
        }
      }
    }
  }

  private final class SelectPropertiesAction extends CustomAction {
    public SelectPropertiesAction() {
      super(m_resources, m_translations, "select-properties");
    }

    public void actionPerformed(ActionEvent event) {
      invoke();
    }

    public void invoke() {
      final Object selectedNode = m_tree.getLastSelectedPathComponent();

      if (selectedNode instanceof Node) {
        final Node node = (Node)selectedNode;

        final File file = node.getFile();

        if (file.isFile()) {
          try {
            // Editor model learns of selection through a properties listener.
            m_properties.setAndSavePropertiesFile(file);
          }
          catch (ConsoleException e) {
            m_errorHandler.handleException(e);
            return;
          }

          m_bufferTreeModel.valueForPathChanged(node.getPath(), node);
          updateActionState();
        }
      }
    }
  }

  private final class DeselectPropertiesAction extends CustomAction {
    public DeselectPropertiesAction() {
      super(m_resources, m_translations, "deselect-properties");
    }

    public void actionPerformed(ActionEvent event) {
      invoke();
    }

    public void invoke() {
      try {
        final File previousProperties = m_properties.getPropertiesFile();

        m_properties.setAndSavePropertiesFile(null);
        updateActionState();

        if (previousProperties != null) {
          final FileTreeModel.Node fileNode =
            m_fileTreeModel.findNode(previousProperties);

          if (fileNode != null) {
            m_fileTreeModel.valueForPathChanged(fileNode.getPath(), fileNode);

            m_bufferTreeModel.bufferChanged(fileNode.getBuffer());
          }
        }
      }
      catch (ConsoleException e) {
        m_errorHandler.handleException(e);
      }
    }
  }

  private void updateActionState() {
    m_deselectPropertiesAction.setEnabled(
      m_editorModel.getSelectedPropertiesFile() != null);

    if (m_tree.isEnabled()) {
      final Object selectedNode = m_tree.getLastSelectedPathComponent();
      if (selectedNode instanceof Node) {
        final Node node = (Node)selectedNode;

        final Buffer buffer = node.getBuffer();
        final File file = node.getFile();

        m_openAction.setEnabled(
          node.canOpen() &&
          (buffer == null ||
           !buffer.equals(m_editorModel.getSelectedBuffer())));
        m_openAction.setRelevantToSelection(node.canOpen());

        m_openExternalAction.setEnabled(file != null && file.isFile());
        m_openExternalAction.setRelevantToSelection(node.canOpen());

        m_selectPropertiesAction.setEnabled(
          m_editorModel.isPropertiesFile(file) &&
          !file.equals(m_editorModel.getSelectedPropertiesFile()));

        m_selectPropertiesAction.setRelevantToSelection(
          m_selectPropertiesAction.isEnabled());

        m_deselectPropertiesAction.setRelevantToSelection(
          m_editorModel.isPropertiesFile(file) &&
          !m_selectPropertiesAction.isEnabled());

        return;
      }
    }

    m_openAction.setEnabled(false);
    m_openAction.setRelevantToSelection(false);
    m_openExternalAction.setEnabled(false);
    m_openExternalAction.setRelevantToSelection(false);
    m_selectPropertiesAction.setEnabled(false);
    m_selectPropertiesAction.setRelevantToSelection(false);
    m_deselectPropertiesAction.setRelevantToSelection(false);
  }

  /**
   * Custom cell renderer.
   */
  private final class CustomTreeCellRenderer extends DefaultTreeCellRenderer {
    private final DefaultTreeCellRenderer m_defaultRenderer =
      new DefaultTreeCellRenderer();

    private final Font m_boldFont;
    private final Font m_boldItalicFont;
    private final ImageIcon m_propertiesIcon =
      m_resources.getImageIcon("file.properties.image");
    private final ImageIcon m_markedPropertiesIcon =
      m_resources.getImageIcon("file.selectedproperties.image");
    private final ImageIcon m_scriptIcon =
      m_resources.getImageIcon("file.script.image");
    private final ImageIcon m_selectedScriptIcon =
      m_resources.getImageIcon("file.selectedscript.image");

    private boolean m_active;

    CustomTreeCellRenderer(Font baseFont, Color background) {
      m_boldFont = baseFont.deriveFont(Font.BOLD);
      m_boldItalicFont = m_boldFont.deriveFont(Font.BOLD | Font.ITALIC);
      m_defaultRenderer.setBackgroundNonSelectionColor(background);
    }

    @Override
    public Component getTreeCellRendererComponent(
      JTree tree, Object value, boolean selected, boolean expanded,
      boolean leaf, int row, boolean hasFocus) {

      if (value instanceof Node) {
        final Node node = (Node)value;

        final File file = node.getFile();

        if (file != null && !file.isFile()) {
          return m_defaultRenderer.getTreeCellRendererComponent(
            tree, value, selected, expanded, leaf, row, hasFocus);
        }

        final Icon icon;

        if (file != null &&
            file.equals(m_editorModel.getSelectedPropertiesFile())) {
          icon = m_markedPropertiesIcon;
        }
        else if (m_editorModel.isSelectedScript(file)) {
          icon = m_selectedScriptIcon;
        }
        else if (m_editorModel.isPropertiesFile(file)) {
          icon = m_propertiesIcon;
        }
        else if (m_editorModel.isScriptFile(file)) {
          icon = m_scriptIcon;
        }
        else {
          icon = m_defaultRenderer.getLeafIcon();
        }

        setLeafIcon(icon);

        final Buffer buffer = node.getBuffer();

        // See note in paint().
        setTextNonSelectionColor(
          buffer == null && m_editorModel.isBoringFile(file) ?
          SystemColor.textInactiveText :
          m_defaultRenderer.getTextNonSelectionColor());

        if (buffer != null) {
          // File has an open buffer.
          setFont(buffer.isDirty() ? m_boldItalicFont : m_boldFont);
          m_active = buffer.equals(m_editorModel.getSelectedBuffer());
        }
        else {
          setFont(m_defaultRenderer.getFont());
          m_active = false;
        }

        return super.getTreeCellRendererComponent(
          tree, value, selected, expanded, leaf, row, hasFocus);
      }
      else {
        return m_defaultRenderer.getTreeCellRendererComponent(
          tree, value, selected, expanded, leaf, row, hasFocus);
      }
    }

    /**
     * Our parent overrides validate() and revalidate() for speed.
     * This means it never resizes. Go with this, but be a few pixels
     * wider to allow text to be italicised.
     */
    @Override
    public Dimension getPreferredSize() {
      final Dimension result = super.getPreferredSize();

      return result != null ?
        new Dimension(result.width + 3, result.height) : null;
    }

    @Override
    public void paint(Graphics g) {

      final Color backgroundColour;

      // For some reason, setting the text non-selection colour doesn't
      // work here. I've left the logic in anyway. That's why its set
      // in getTreeCellRendererComponent().
      if (m_active) {
        backgroundColour = Colours.FAINT_YELLOW;
        setTextSelectionColor(SystemColor.textText);
        setTextNonSelectionColor(SystemColor.textText);
      }
      else if (selected) {
        backgroundColour = m_defaultRenderer.getBackgroundSelectionColor();
        setTextSelectionColor(m_defaultRenderer.getTextSelectionColor());
      }
      else {
        backgroundColour = m_defaultRenderer.getBackgroundNonSelectionColor();
        setTextNonSelectionColor(m_defaultRenderer.getTextNonSelectionColor());
      }

      if (backgroundColour != null) {
        g.setColor(backgroundColour);
        g.fillRect(0, 0, getWidth() - 1, getHeight());
      }

      // Sigh. The whole reason we override paint is that the
      // DefaultTreeCellRenderer version is crap. We can't call
      // super.super.paint() so we work hard to make the
      // DefaultTreeCellRenderer version ineffectual.

      final boolean oldHasFocus = hasFocus;
      final boolean oldSelected = selected;
      final Color oldBackgroundNonSelectionColour =
        getBackgroundNonSelectionColor();

      try {
        hasFocus = false;
        selected = false;
        setBackgroundNonSelectionColor(backgroundColour);

        super.paint(g);
      }
      finally {
        hasFocus = oldHasFocus;
        selected = oldSelected;
        setBackgroundNonSelectionColor(oldBackgroundNonSelectionColour);
      }

      // Now draw our border.
      final Color borderColour;

      if (m_active) {
        borderColour = getTextNonSelectionColor();
      }
      else if (hasFocus) {
        borderColour = getBorderSelectionColor();
      }
      else {
        borderColour = null;
      }

      if (borderColour != null) {
        g.setColor(borderColour);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
      }
    }
  }

  /**
   * Hard to see how this could be easily incorporated into
   * CompositeTreeModel without having the child models know about the
   * composite model.
   */
  private TreePath treePathForFileNode(FileTreeModel.FileNode fileNode) {
    final Object[] original = fileNode.getPath().getPath();
    final Object[] result = new Object[original.length + 1];
    System.arraycopy(original, 0, result, 1, original.length);

    result[0] = m_tree.getModel().getRoot();

    return new TreePath(result);
  }

  /**
   * Allows us to treat FileNodes and BufferNodes polymorphically.
   */
  interface Node {

    /**
     * @return <code>null</code> if the node has no associated buffer.
     */
    Buffer getBuffer();

    /**
     * @return <code>null</code> if the node has no associated file.
     */
    File getFile();

    TreePath getPath();

    boolean canOpen();
  }
}
