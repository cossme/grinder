// Copyright (C) 2003 - 2009 Philip Aston
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

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import net.grinder.console.editor.Buffer;
import net.grinder.console.editor.EditorModel;
import net.grinder.console.distribution.FileChangeWatcher;
import net.grinder.util.WeakValueHashMap;


/**
 * {@link TreeModel} that walks file system.
 *
 * @author Philip Aston
 */
final class FileTreeModel implements TreeModel {

  private final EventListenerList m_listeners = new EventListenerList();
  private final EditorModel m_editorModel;
  private final FileFilter m_distributionFileFilter;

  private final FilenameFilter m_directoryFilter =
    new FilenameFilter() {
      public boolean accept(File dir, String name) {
        final File file = new File(dir, name);
        return file.isDirectory() && m_distributionFileFilter.accept(file);
      }
    };

  private final FilenameFilter m_fileFilter =
    new FilenameFilter() {
      public boolean accept(File dir, String name) {
        final File file = new File(dir, name);
        return file.isFile() && m_distributionFileFilter.accept(file);
      }
    };

  /**
   * Map from a File value to the latest Node to be created for the File.
   */
  private final WeakValueHashMap<File, Node> m_filesToNodes =
    new WeakValueHashMap<File, Node>();

  /**
   * Map from a Buffer to the FileNode that is associated with the
   * buffer.
   */
  private final WeakValueHashMap<Buffer, FileNode> m_buffersToFileNodes =
    new WeakValueHashMap<Buffer, FileNode>();

  private RootNode m_rootNode;

  FileTreeModel(EditorModel editorModel,
                FileFilter distributionFileFilter,
                File initialRootDirectory) {
    m_editorModel = editorModel;
    m_distributionFileFilter = distributionFileFilter;
    setRootDirectory(initialRootDirectory);
  }

  public void setRootDirectory(File rootDirectory) {
    m_rootNode = new RootNode(rootDirectory);
    fireTreeStructureChanged(m_rootNode);
  }

  public void refresh() {
    m_rootNode.refresh();
    fireTreeStructureChanged(m_rootNode);
  }

  public Object getRoot() {
    return m_rootNode;
  }

  public Object getChild(Object parent, int index) {

    if (parent instanceof DirectoryNode) {
      final DirectoryNode directoryNode = (DirectoryNode)parent;

      if (directoryNode.belongsToModel(this)) {
        return directoryNode.getChild(index);
      }
    }

    return null;
  }

  public int getChildCount(Object parent) {

    if (parent instanceof DirectoryNode) {
      final DirectoryNode directoryNode = (DirectoryNode)parent;

      if (directoryNode.belongsToModel(this)) {
        return directoryNode.getChildCount();
      }
    }

    return 0;
  }

  public int getIndexOfChild(Object parent, Object child) {

    if (parent == null || child == null) {
      // The TreeModel Javadoc says we should do this.
      return -1;
    }

    if (parent instanceof DirectoryNode) {
      final DirectoryNode directoryNode = (DirectoryNode)parent;

      if (directoryNode.belongsToModel(this)) {
        return directoryNode.getIndexOfChild((Node)child);
      }
    }

    return -1;
  }

  public boolean isLeaf(Object node) {
    if (node instanceof FileNode) {
      final FileNode fileNode = (FileNode)node;

      if (fileNode.belongsToModel(this)) {
        return true;
      }
    }

    return false;
  }

  public void addTreeModelListener(TreeModelListener listener) {
    m_listeners.add(TreeModelListener.class, listener);
  }

  public void removeTreeModelListener(TreeModelListener listener) {
    m_listeners.remove(TreeModelListener.class, listener);
  }

  private void fireTreeStructureChanged(Node node) {
    final Object[] listeners = m_listeners.getListenerList();

    final TreeModelEvent event = new TreeModelEvent(this, node.getPath());

    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      ((TreeModelListener)listeners[i + 1]).treeStructureChanged(event);
    }
  }

  private void fireTreeNodesChanged(TreePath path) {
    final Object[] listeners = m_listeners.getListenerList();

    final TreeModelEvent event = new TreeModelEvent(this, path);

    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      ((TreeModelListener)listeners[i + 1]).treeNodesChanged(event);
    }
  }

  public void valueForPathChanged(TreePath path, Object newValue) {
    fireTreeNodesChanged(path);
  }

  /**
   * Find the {Node} for a file. If a particular part of the file path
   * isn't found in the model, that part of the model is refreshed and
   * checked again.
   *
   * @param file The file to find the corresponding {@link Node} for.
   * @return The node, or <code>null</code> if the file could not be found.
   */
  public Node findNode(File file) {
    final Node existingNode = m_filesToNodes.get(file);

    if (existingNode != null) {
      return existingNode;
    }

    // Maybe its not been expanded. Lets try harder.
    final File[] paths = fileToArrayOfParentPaths(file);

    Node treeStructureChangedNode = null;

    for (int i = 0; i < paths.length - 1; ++i) {
      final Node node = m_filesToNodes.get(paths[i]);

      if (node instanceof DirectoryNode) {
        final DirectoryNode directoryNode = (DirectoryNode)node;

        if (directoryNode.getChildForFile(paths[i + 1]) == null) {
          directoryNode.refresh();
          treeStructureChangedNode = directoryNode;

          if (directoryNode.getChildForFile(paths[i + 1]) == null) {
            return null;
          }
        }
      }
    }

    if (treeStructureChangedNode != null) {
      fireTreeStructureChanged(treeStructureChangedNode);
    }

    return m_filesToNodes.get(file);
  }

  private File[] fileToArrayOfParentPaths(File file) {
    final List<File> list = new ArrayList<File>();

    File f = file;

    while (f != null) {
      list.add(f);
      f = f.getParentFile();
    }

    Collections.reverse(list);

    return list.toArray(new File[list.size()]);
  }

  public FileNode findFileNode(Buffer buffer) {
    return m_buffersToFileNodes.get(buffer);
  }

  /**
   * A {@link
   * net.grinder.console.distribution.FileChangeWatcher.FileChangedListener}
   * that listens for changed file notifications and updates the FileTreeModel
   * appropriately.
   *
   */
  public class RefreshChangedDirectoriesListener
    implements FileChangeWatcher.FileChangedListener {

    public void filesChanged(File[] files) {
      // Refresh the tree path for every file. We could waste time here removing
      // duplicate refreshes, but most times they'll only be a single file.

      for (int i = 0; i < files.length; ++i) {
        // findNode will refresh everything up to the file itself...
        final Node node = findNode(files[i]);

        // ...so if we find a directory node, we'd better refresh that too.
        if (node instanceof DirectoryNode) {
          ((DirectoryNode)node).refresh();
          fireTreeStructureChanged(node);
        }
      }
    }
  }

  /**
   * Node in the tree.
   */
  public abstract class Node implements FileTree.Node {

    private final File m_file;
    private final TreePath m_path;

    protected Node(Node parentNode, File file) {
      m_file = file;

      if (parentNode != null) {
        m_path = parentNode.getPath().pathByAddingChild(this);
      }
      else {
        m_path = new TreePath(this);
      }

      m_filesToNodes.put(file, this);
    }

    public String toString() {
      return m_file.getName();
    }

    public Buffer getBuffer() {
      return null;
    }

    public final File getFile() {
      return m_file;
    }

    public final TreePath getPath() {
      return m_path;
    }

    public boolean canOpen() {
      return false;
    }

    boolean belongsToModel(FileTreeModel model) {
      return FileTreeModel.this == model;
    }
  }

  /**
   * Node that represents a file. Used for the leaves of the tree.
   */
  public final class FileNode extends Node {

    private Buffer m_buffer;

    private FileNode(DirectoryNode parentNode, File file) {
      super(parentNode, file);

      setBuffer(m_editorModel.getBufferForFile(file));
    }

    public void setBuffer(Buffer buffer) {
      if (m_buffer != null) {
        m_buffersToFileNodes.remove(m_buffer);
      }

      m_buffer = buffer;

      if (buffer != null) {
        m_buffersToFileNodes.put(buffer, this);
      }
    }

    public Buffer getBuffer() {
      return m_buffer;
    }

    public boolean canOpen() {
      return true;
    }
  }

  /**
   * Node that represents a directory.
   */
  private class DirectoryNode extends Node {

    private final File[] m_noFiles = new File[0];

    private File[] m_childDirectories = m_noFiles;
    private DirectoryNode[] m_childDirectoryNodes;
    private File[] m_childFiles = m_noFiles;
    private FileNode[] m_childFileNodes;

    DirectoryNode(DirectoryNode parentNode, File file) {
      super(parentNode, file);

      refresh();
    }

    public void refresh() {
      for (int i = 0; i < m_childDirectories.length; ++i) {
        final DirectoryNode oldDirectoryNode =
          (DirectoryNode)m_filesToNodes.remove(m_childDirectories[i]);

        // Can be null if a DirectoryNode has never been created for the
        // directory.
        if (oldDirectoryNode != null) {
          oldDirectoryNode.refresh();
        }
      }

      for (int i = 0; i < m_childFiles.length; ++i) {
        final FileNode oldFileNode =
          (FileNode)m_filesToNodes.remove(m_childFiles[i]);

        if (oldFileNode != null) {
          oldFileNode.setBuffer(null);
        }
      }

      final File[] directories = getFile().listFiles(m_directoryFilter);

      if (directories != null) {
        Arrays.sort(directories);
        m_childDirectories = directories;
      }
      else {
        m_childDirectories = m_noFiles;
      }

      m_childDirectoryNodes = new DirectoryNode[m_childDirectories.length];

      final File[] files = getFile().listFiles(m_fileFilter);

      if (files != null) {
        Arrays.sort(files);
        m_childFiles = files;
      }
      else {
        m_childFiles = m_noFiles;
      }

      m_childFileNodes = new FileNode[m_childFiles.length];
    }

    final Node getChildForFile(File file) {
      if (file.isDirectory()) {
        for (int i = 0; i < m_childDirectories.length; ++i) {
          if (m_childDirectories[i].equals(file)) {
            return getChild(i);
          }
        }
      }
      else {
        for (int i = 0; i < m_childFiles.length; ++i) {
          if (m_childFiles[i].equals(file)) {
            return getChild(i + m_childDirectories.length);
          }
        }
      }

      // Not known here.
      return null;
    }

    public final Node getChild(int index) {
      if (index < m_childDirectories.length) {
        if (m_childDirectoryNodes[index] == null) {
          m_childDirectoryNodes[index] =
            new DirectoryNode(this, m_childDirectories[index]);
        }

        return m_childDirectoryNodes[index];
      }
      else if (index < m_childDirectories.length + m_childFiles.length) {
        final int fileIndex = index - m_childDirectories.length;

        if (m_childFileNodes[fileIndex] == null) {
          m_childFileNodes[fileIndex] =
            new FileNode(this, m_childFiles[fileIndex]);
        }

        return m_childFileNodes[fileIndex];
      }
      else {
        return null;
      }
    }

    public final int getChildCount() {
      return m_childDirectories.length + m_childFiles.length;
    }

    public final int getIndexOfChild(Node child) {
      for (int i = 0; i < m_childDirectories.length; ++i) {
        if (m_childDirectories[i].equals(child.getFile())) {
          return i;
        }
      }

      for (int i = 0; i < m_childFiles.length; ++i) {
        if (m_childFiles[i].equals(child.getFile())) {
          return m_childDirectories.length + i;
        }
      }

      return -1;
    }
  }

  /**
   * Root node of the tree.
   */
  private final class RootNode extends DirectoryNode {

    private RootNode(File file) {
      super(null, file);
    }

    public String toString() {
      return getFile().getPath();
    }
  }
}
