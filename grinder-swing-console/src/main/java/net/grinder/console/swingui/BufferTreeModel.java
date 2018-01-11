// Copyright (C) 2004 - 2009 Philip Aston
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

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import net.grinder.console.editor.Buffer;
import net.grinder.console.editor.EditorModel;
import net.grinder.util.WeakValueHashMap;


/**
 * Simple {@link TreeModel} that lists buffers.
 *
 * @author Philip Aston
 */
final class BufferTreeModel implements TreeModel {

  private final EditorModel m_editorModel;

  private final Object m_rootNode = new Object();
  private final EventListenerList m_listeners = new EventListenerList();
  private final WeakValueHashMap<Buffer, BufferNode> m_buffersToNodes =
    new WeakValueHashMap<Buffer, BufferNode>();

  private BufferNode[] m_bufferNodes = new BufferNode[0];

  BufferTreeModel(EditorModel editorModel) {

    m_editorModel = editorModel;

    m_editorModel.addListener(new EditorModel.AbstractListener() {

        public void bufferAdded(Buffer buffer) {
          bufferListChanged();
        }

        public void bufferRemoved(Buffer buffer) {
          bufferListChanged();
        }
      });

    bufferListChanged();
  }

  private void bufferListChanged() {
    final Buffer[] buffers = m_editorModel.getBuffers();
    m_bufferNodes = new BufferNode[buffers.length];

    m_buffersToNodes.clear();

    for (int i = 0; i < buffers.length; ++i) {
      m_bufferNodes[i] = new BufferNode(buffers[i]);
      m_buffersToNodes.put(buffers[i], m_bufferNodes[i]);
    }

    fireTreeStructureChanged();
  }

  public Object getRoot() {
    return m_rootNode;
  }

  public Object getChild(Object parent, int index) {

    if (parent.equals(getRoot())) {
      if (index >= 0 && index < m_bufferNodes.length) {
        return m_bufferNodes[index];
      }
    }

    return null;
  }

  public int getChildCount(Object parent) {

    if (parent.equals(getRoot())) {
      return m_bufferNodes.length;
    }

    return 0;
  }

  public int getIndexOfChild(Object parent, Object child) {

    if (parent == null || child == null) {
      // The TreeModel Javadoc says we should do this.
      return -1;
    }

    if (parent.equals(getRoot())) {
      for (int i = 0; i < m_bufferNodes.length; ++i) {
        if (child.equals(m_bufferNodes[i])) {
          return i;
        }
      }
    }

    return -1;
  }

  public boolean isLeaf(Object node) {
    if (node instanceof BufferNode) {
      final BufferNode bufferNode = (BufferNode)node;

      if (bufferNode.belongsToModel(this)) {
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

  private void fireTreeStructureChanged() {
    final Object[] listeners = m_listeners.getListenerList();

    final TreeModelEvent event =
      new TreeModelEvent(this, new Object[] { getRoot() });

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

  public void bufferChanged(Buffer buffer) {

    final BufferTreeModel.BufferNode node = m_buffersToNodes.get(buffer);

    if (node != null) {
      valueForPathChanged(node.getPath(), node);
    }
  }

  /**
   * Node for buffers.
   */
  public final class BufferNode implements FileTree.Node {

    private final Buffer m_buffer;
    private final TreePath m_path;

    protected BufferNode(Buffer buffer) {
      m_buffer = buffer;
      m_path = new TreePath(new Object[] { getRoot(), this });
    }

    public String toString() {
      return m_buffer.getDisplayName();
    }

    public Buffer getBuffer() {
      return m_buffer;
    }

    public TreePath getPath() {
      return m_path;
    }

    public File getFile() {
      return getBuffer().getFile();
    }

    public boolean canOpen() {
      return true;
    }

    boolean belongsToModel(BufferTreeModel model) {
      return BufferTreeModel.this == model;
    }
  }
}
