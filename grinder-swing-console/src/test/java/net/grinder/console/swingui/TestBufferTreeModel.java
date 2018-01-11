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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

import net.grinder.console.distribution.AgentCacheState;
import net.grinder.console.distribution.FileChangeWatcher;
import net.grinder.console.editor.Buffer;
import net.grinder.console.editor.EditorModel;
import net.grinder.console.editor.StringTextSource;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.translation.Translations;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;


/**
 * Unit tests for {@link BufferTreeModel}.
 *
 * @author Philip Aston
 */
public class TestBufferTreeModel {

  @Mock private Translations m_translations;

  private final RandomStubFactory<AgentCacheState>
    m_agentCacheStateStubFactory =
      RandomStubFactory.create(AgentCacheState.class);
  private final AgentCacheState m_agentCacheState =
    m_agentCacheStateStubFactory.getStub();

  private final RandomStubFactory<FileChangeWatcher>
    m_fileChangeWatcherStubFactory =
      RandomStubFactory.create(FileChangeWatcher.class);
  private final FileChangeWatcher m_fileChangeWatcher =
    m_fileChangeWatcherStubFactory.getStub();

  @Before public void setUp() {
    initMocks(this);
  }

  @Test public void testConstructionAndGetChildMethods() throws Exception {
    final StringTextSource.Factory stringTextSourceFactory =
      new StringTextSource.Factory();

    final EditorModel editorModel = new EditorModel(m_translations,
                                                    stringTextSourceFactory,
                                                    m_agentCacheState,
                                                    m_fileChangeWatcher);

    editorModel.selectNewBuffer();

    final BufferTreeModel bufferTreeModel = new BufferTreeModel(editorModel);

    final Object rootNode = bufferTreeModel.getRoot();
    assertNotNull(rootNode);
    assertEquals(1, bufferTreeModel.getChildCount(rootNode));
    assertNull(bufferTreeModel.getChild(rootNode, 1));

    final BufferTreeModel.BufferNode bufferNode =
      (BufferTreeModel.BufferNode)bufferTreeModel.getChild(rootNode, 0);
    assertSame(bufferNode.getBuffer(), editorModel.getSelectedBuffer());
    assertTrue(bufferNode.belongsToModel(bufferTreeModel));
    assertEquals(bufferNode.getBuffer().getDisplayName(),
                 bufferNode.toString());
    final Object[] path = bufferNode.getPath().getPath();
    assertEquals(2, path.length);
    assertSame(rootNode, path[0]);
    assertSame(bufferNode, path[1]);
    assertTrue(bufferNode.canOpen());

    final BufferTreeModel.BufferNode anotherBufferNode =
      new BufferTreeModel(editorModel).new BufferNode(null);
    assertFalse(anotherBufferNode.belongsToModel(bufferTreeModel));

    assertEquals(0, bufferTreeModel.getChildCount(bufferNode));
    assertNull(bufferTreeModel.getChild(bufferNode, 0));

    assertEquals(-1, bufferTreeModel.getIndexOfChild(null, bufferNode));
    assertEquals(-1, bufferTreeModel.getIndexOfChild(rootNode, null));
    assertEquals(-1, bufferTreeModel.getIndexOfChild(bufferNode, rootNode));
    assertEquals(-1,
                 bufferTreeModel.getIndexOfChild(rootNode, anotherBufferNode));

    assertEquals(0, bufferTreeModel.getIndexOfChild(rootNode, bufferNode));

    assertTrue(bufferTreeModel.isLeaf(bufferNode));
    assertFalse(bufferTreeModel.isLeaf(rootNode));
    assertFalse(bufferTreeModel.isLeaf(anotherBufferNode));
  }

  @Test public void testSettersAndListeners() throws Exception {
    final StringTextSource.Factory stringTextSourceFactory =
      new StringTextSource.Factory();

    final EditorModel editorModel = new EditorModel(m_translations,
                                                    stringTextSourceFactory,
                                                    m_agentCacheState,
                                                    m_fileChangeWatcher);
    editorModel.selectNewBuffer();
    final Buffer buffer2 = editorModel.getSelectedBuffer();

    final BufferTreeModel bufferTreeModel = new BufferTreeModel(editorModel);

    final RandomStubFactory<TreeModelListener> listener1StubFactory =
      RandomStubFactory.create(TreeModelListener.class);
    final RandomStubFactory<TreeModelListener> listener2StubFactory =
      RandomStubFactory.create(TreeModelListener.class);

    bufferTreeModel.addTreeModelListener(listener1StubFactory.getStub());
    bufferTreeModel.addTreeModelListener(listener2StubFactory.getStub());

    final TreePath treePath = new TreePath(new Object());
    bufferTreeModel.valueForPathChanged(treePath, null);

    final CallData treeNodesChangedCallData =
      listener1StubFactory.assertSuccess("treeNodesChanged",
                                         TreeModelEvent.class);
    final TreeModelEvent event =
      (TreeModelEvent)treeNodesChangedCallData.getParameters()[0];
    assertSame(treePath, event.getTreePath());
    assertSame(bufferTreeModel, event.getSource());
    listener1StubFactory.assertNoMoreCalls();

    listener2StubFactory.assertSuccess("treeNodesChanged",
                                       TreeModelEvent.class);
    listener2StubFactory.assertNoMoreCalls();

    bufferTreeModel.removeTreeModelListener(listener1StubFactory.getStub());

    // removeTreeModelListener() can calls equals() on the listeners.
    listener1StubFactory.resetCallHistory();
    listener2StubFactory.resetCallHistory();

    editorModel.selectBuffer(buffer2);

    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertNoMoreCalls();

    final EditorModel editorModel2 = new EditorModel(m_translations,
                                                    stringTextSourceFactory,
                                                    m_agentCacheState,
                                                    m_fileChangeWatcher);

    editorModel2.selectNewBuffer();
    final Buffer anotherBuffer = editorModel2.getSelectedBuffer();
    bufferTreeModel.bufferChanged(anotherBuffer);

    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertNoMoreCalls();

    final Buffer buffer = editorModel.getSelectedBuffer();
    bufferTreeModel.bufferChanged(buffer);

    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertSuccess("treeNodesChanged",
                                       TreeModelEvent.class);

    editorModel.selectNewBuffer();

    final CallData treeStructureChangedCallData =
      listener2StubFactory.assertSuccess("treeStructureChanged",
                                         TreeModelEvent.class);
    final TreeModelEvent treeStructureChangedEvent =
      (TreeModelEvent)treeStructureChangedCallData.getParameters()[0];
    assertSame(bufferTreeModel, treeStructureChangedEvent.getSource());
    assertEquals(1, treeStructureChangedEvent.getPath().length);
    assertSame(bufferTreeModel.getRoot(),
               treeStructureChangedEvent.getPath()[0]);

    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertNoMoreCalls();

    editorModel.closeBuffer(buffer);

    final CallData treeStructureChangedCallData2 =
      listener2StubFactory.assertSuccess("treeStructureChanged",
                                         TreeModelEvent.class);
    final TreeModelEvent treeStructureChangedEvent2 =
      (TreeModelEvent)treeStructureChangedCallData2.getParameters()[0];
    assertSame(bufferTreeModel, treeStructureChangedEvent2.getSource());
    assertEquals(1, treeStructureChangedEvent2.getPath().length);
    assertSame(bufferTreeModel.getRoot(),
               treeStructureChangedEvent2.getPath()[0]);

    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertNoMoreCalls();
  }
}
