// Copyright (C) 2005 - 2013 Philip Aston
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileFilter;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

import net.grinder.console.distribution.AgentCacheState;
import net.grinder.console.distribution.FileChangeWatcher;
import net.grinder.console.editor.Buffer;
import net.grinder.console.editor.EditorModel;
import net.grinder.console.editor.TextSource;
import net.grinder.console.editor.TextSource.Factory;
import net.grinder.console.swingui.FileTreeModel.FileNode;
import net.grinder.console.swingui.FileTreeModel.Node;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.translation.Translations;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Unit tests for {@link FileTreeModel}.
 *
 * @author Philip Aston
 */
public class TestFileTreeModel extends AbstractJUnit4FileTestCase {

  @Mock private Translations m_translations;

  private final RandomStubFactory<Factory> m_textSourceFactoryStubFactory =
      RandomStubFactory.create(TextSource.Factory.class);

  private final TextSource.Factory m_textSourceFactory =
      m_textSourceFactoryStubFactory.getStub();

  private final RandomStubFactory<AgentCacheState> m_agentCacheStateStubFactory =
      RandomStubFactory.create(AgentCacheState.class);

  private final AgentCacheState m_agentCacheState =
      m_agentCacheStateStubFactory.getStub();

  private final RandomStubFactory<FileChangeWatcher> m_fileChangeWatcherStubFactory =
      RandomStubFactory.create(FileChangeWatcher.class);

  private final FileChangeWatcher m_fileChangeWatcher =
      m_fileChangeWatcherStubFactory.getStub();

  private EditorModel m_editorModel;

  @Before public void setUp() {
    initMocks(this);

    m_editorModel = new EditorModel(m_translations,
                                    m_textSourceFactory,
                                    m_agentCacheState,
                                    m_fileChangeWatcher);
  }

  private final FileFilter m_nullFileFilter = new FileFilter() {
    public boolean accept(File pathname) {
      return true;
    }
  };

  @Test
  public void testWithRootNode() throws Exception {
    final FileTreeModel fileTreeModel =
        new FileTreeModel(m_editorModel, m_nullFileFilter, getDirectory());

    final Node rootNode = (Node) fileTreeModel.getRoot();
    assertFalse(rootNode instanceof FileNode);
    assertFalse(rootNode.canOpen());
    assertNull(rootNode.getBuffer());

    assertEquals(getDirectory(), rootNode.getFile());
    assertEquals(getDirectory().getPath(), rootNode.toString());

    final FileTreeModel fileTreeModel2 =
        new FileTreeModel(m_editorModel, m_nullFileFilter, getDirectory());
    assertTrue(rootNode.belongsToModel(fileTreeModel));
    assertFalse(rootNode.belongsToModel(fileTreeModel2));

    assertNull(fileTreeModel.getChild(rootNode, 0));
    assertNull(fileTreeModel2.getChild(rootNode, 0));
    assertEquals(0, fileTreeModel.getChildCount(rootNode));
    assertEquals(0, fileTreeModel2.getChildCount(rootNode));

    assertFalse(fileTreeModel.isLeaf(rootNode));
    assertFalse(fileTreeModel2.isLeaf(rootNode));

    assertEquals(-1, fileTreeModel.getIndexOfChild(null, null));
    assertEquals(-1, fileTreeModel.getIndexOfChild(rootNode, null));
    assertEquals(-1, fileTreeModel.getIndexOfChild(rootNode, rootNode));
  }

  @Test
  public void testListener() throws Exception {
    final RandomStubFactory<TreeModelListener> listenerStubFactory1 =
        RandomStubFactory.create(TreeModelListener.class);
    final RandomStubFactory<TreeModelListener> listenerStubFactory2 =
        RandomStubFactory.create(TreeModelListener.class);

    final FileTreeModel fileTreeModel =
        new FileTreeModel(m_editorModel, m_nullFileFilter, getDirectory());
    fileTreeModel.addTreeModelListener(listenerStubFactory1.getStub());
    fileTreeModel.addTreeModelListener(listenerStubFactory2.getStub());

    fileTreeModel.setRootDirectory(getDirectory());
    final Node rootNode = (Node) fileTreeModel.getRoot();

    final CallData callData =
        listenerStubFactory1.assertSuccess("treeStructureChanged",
          TreeModelEvent.class);
    final TreeModelEvent event = (TreeModelEvent) callData.getParameters()[0];
    assertEquals(fileTreeModel, event.getSource());
    assertArrayEquals(rootNode.getPath().getPath(), event.getPath());
    listenerStubFactory1.assertNoMoreCalls();
    listenerStubFactory2.assertSuccess("treeStructureChanged",
      TreeModelEvent.class);
    listenerStubFactory2.assertNoMoreCalls();

    fileTreeModel.refresh();
    listenerStubFactory1.assertSuccess("treeStructureChanged",
      TreeModelEvent.class);
    listenerStubFactory1.assertNoMoreCalls();
    listenerStubFactory2.assertSuccess("treeStructureChanged",
      TreeModelEvent.class);
    listenerStubFactory2.assertNoMoreCalls();

    // For some reason, if we record the "equals" invocations that the
    // remove causes, the next assertNoMoreCalls fails with a
    // ConcurrentModificationException. Seems bogus to me, but its not
    // pertinent to the test.
    listenerStubFactory1.setIgnoreObjectMethods();
    listenerStubFactory2.setIgnoreObjectMethods();
    fileTreeModel.removeTreeModelListener(listenerStubFactory1.getStub());
    fileTreeModel.refresh();

    listenerStubFactory1.assertNoMoreCalls();
    listenerStubFactory2.assertSuccess("treeStructureChanged",
      TreeModelEvent.class);
    listenerStubFactory2.assertNoMoreCalls();

    fileTreeModel.valueForPathChanged(rootNode.getPath(), rootNode);
    final CallData callData2 =
        listenerStubFactory2.assertSuccess("treeNodesChanged",
          TreeModelEvent.class);
    final TreeModelEvent event2 = (TreeModelEvent) callData2.getParameters()[0];
    assertEquals(fileTreeModel, event2.getSource());
    assertArrayEquals(rootNode.getPath().getPath(), event2.getPath());
    listenerStubFactory2.assertNoMoreCalls();
  }

  @Test
  public void testWithFileStructure() throws Exception {
    final File file1 = new File(getDirectory(), "file1");
    assertTrue(file1.createNewFile());
    final File dir1 = new File(getDirectory(), "dir1");
    assertTrue(dir1.mkdir());
    final File file2 = new File(dir1, "file2");
    assertTrue(file2.createNewFile());
    final File file3 = new File(dir1, "file3");
    assertTrue(file3.createNewFile());

    final FileTreeModel fileTreeModel =
        new FileTreeModel(m_editorModel, m_nullFileFilter, getDirectory());
    final FileTreeModel fileTreeModel2 =
        new FileTreeModel(m_editorModel, m_nullFileFilter, getDirectory());
    fileTreeModel.setRootDirectory(getDirectory());
    final Node rootNode = (Node) fileTreeModel.getRoot();
    assertFalse(rootNode.canOpen());
    assertNull(rootNode.getBuffer());

    assertEquals(2, fileTreeModel.getChildCount(rootNode));
    final Node dir1Node = (Node) fileTreeModel.getChild(rootNode, 0);
    final FileNode file1Node = (FileNode) fileTreeModel.getChild(rootNode, 1);
    assertEquals(file1.getName(), file1Node.toString());
    assertTrue(file1Node.canOpen());

    assertNull(fileTreeModel.getChild(file1Node, 0));
    assertEquals(0, fileTreeModel.getChildCount(file1Node));
    assertTrue(fileTreeModel.isLeaf(file1Node));
    assertFalse(fileTreeModel2.isLeaf(file1Node));

    assertEquals(-1, fileTreeModel.getIndexOfChild(file1Node, file1Node));
    assertEquals(0, fileTreeModel.getIndexOfChild(rootNode, dir1Node));
    assertEquals(1, fileTreeModel.getIndexOfChild(rootNode, file1Node));
    assertEquals(-1, fileTreeModel2.getIndexOfChild(rootNode, file1Node));

    final FileNode file3Node = (FileNode) fileTreeModel.getChild(dir1Node, 1);
    assertEquals(1, fileTreeModel.getIndexOfChild(dir1Node, file3Node));
  }

  @Test
  public void testRefreshAndFindNode() throws Exception {
    final FileTreeModel fileTreeModel =
        new FileTreeModel(m_editorModel, m_nullFileFilter, getDirectory());
    fileTreeModel.setRootDirectory(getDirectory());
    final Node rootNode = (Node) fileTreeModel.getRoot();

    final File dir1 = new File(getDirectory(), "dir1");

    fileTreeModel.refresh();
    assertNull(fileTreeModel.findNode(dir1));

    assertTrue(dir1.mkdir());
    final File dir2 = new File(getDirectory(), "dir2");
    assertTrue(dir2.mkdir());

    fileTreeModel.refresh();
    assertNotNull(fileTreeModel.findNode(dir1));
    fileTreeModel.refresh();
    assertNotNull(fileTreeModel.findNode(dir1));
    assertTrue(dir1.delete());
    assertNotNull(fileTreeModel.findNode(dir1));
    fileTreeModel.refresh();
    assertNull(fileTreeModel.findNode(dir1));
    assertTrue(dir1.mkdir());
    assertTrue(dir2.delete());

    final File file1 = new File(getDirectory(), "file1");
    assertTrue(file1.createNewFile());
    final File file2 = new File(dir1, "file2");
    assertTrue(file2.createNewFile());
    final File file3 = new File(dir1, "file3");
    assertTrue(file3.createNewFile());

    final Node dir1Node = fileTreeModel.findNode(dir1);
    assertEquals(dir1Node, fileTreeModel.getChild(rootNode, 0));

    final FileNode file3Node = (FileNode) fileTreeModel.findNode(file3);
    assertSame(file3Node, fileTreeModel.getChild(dir1Node, 1));
    assertSame(file3Node, fileTreeModel.findNode(file3));

    final RandomStubFactory<TreeModelListener> listenerStubFactory =
        RandomStubFactory.create(TreeModelListener.class);
    fileTreeModel.addTreeModelListener(listenerStubFactory.getStub());

    final File dir3 = new File(dir1, "dir3");
    final File file4 = new File(dir3, "file4");
    assertNull(fileTreeModel.findNode(file4));

    listenerStubFactory.assertNoMoreCalls();

    assertTrue(dir3.mkdir());
    assertTrue(file4.createNewFile());
    final FileNode file4Node = (FileNode) fileTreeModel.findNode(file4);

    final CallData callData =
        listenerStubFactory.assertSuccess("treeStructureChanged",
          TreeModelEvent.class);
    assertEquals("treeStructureChanged", callData.getMethodName());
    assertArrayEquals(new Class[] { TreeModelEvent.class, },
      callData.getParameterTypes());
    final TreeModelEvent event = (TreeModelEvent) callData.getParameters()[0];
    assertEquals(fileTreeModel, event.getSource());
    assertArrayEquals(
      dir1Node.getPath().getPath(),
      event.getPath());
    listenerStubFactory.assertNoMoreCalls();

    assertEquals(file4, file4Node.getFile());
  }

  @Test
  public void testFindFileNode() throws Exception {
    final File file1 = new File(getDirectory(), "file1");
    assertTrue(file1.createNewFile());

    final FileTreeModel fileTreeModel =
        new FileTreeModel(m_editorModel, m_nullFileFilter, getDirectory());
    fileTreeModel.setRootDirectory(getDirectory());
    final FileNode file1Node = (FileNode) fileTreeModel.findNode(file1);

    final RandomStubFactory<Buffer> bufferStubFactory1 =
        RandomStubFactory.create(Buffer.class);
    final Buffer buffer1 = bufferStubFactory1.getStub();
    final RandomStubFactory<Buffer> bufferStubFactory2 =
        RandomStubFactory.create(Buffer.class);
    final Buffer buffer2 = bufferStubFactory2.getStub();

    file1Node.setBuffer(buffer1);
    assertSame(buffer1, file1Node.getBuffer());
    assertEquals(file1Node, fileTreeModel.findFileNode(buffer1));
    assertNull(fileTreeModel.findFileNode(buffer2));

    file1Node.setBuffer(buffer2);
    assertNull(fileTreeModel.findFileNode(buffer1));
    assertEquals(file1Node, fileTreeModel.findFileNode(buffer2));
  }

  @Test
  public void testRefreshChangedDirectoriesListener() throws Exception {
    final File dir1 = new File(getDirectory(), "dir1");
    assertTrue(dir1.mkdir());
    final File file1 = new File(getDirectory(), "file1");
    assertTrue(file1.createNewFile());
    final File file2 = new File(dir1, "file2");
    assertTrue(file2.createNewFile());
    final File file3 = new File(dir1, "file3");
    assertTrue(file3.createNewFile());

    final FileTreeModel fileTreeModel =
        new FileTreeModel(m_editorModel, m_nullFileFilter, getDirectory());
    fileTreeModel.setRootDirectory(getDirectory());

    final RandomStubFactory<TreeModelListener> listenerStubFactory =
        RandomStubFactory.create(TreeModelListener.class);
    final TreeModelListener listener =
        listenerStubFactory.getStub();
    fileTreeModel.addTreeModelListener(listener);

    final FileChangeWatcher.FileChangedListener filesChangedListener =
        fileTreeModel.new RefreshChangedDirectoriesListener();

    filesChangedListener.filesChanged(new File[0]);
    listenerStubFactory.assertNoMoreCalls();

    filesChangedListener.filesChanged(new File[] { file2, dir1, });
    listenerStubFactory.assertSuccess("treeStructureChanged",
      TreeModelEvent.class);
  }
}
