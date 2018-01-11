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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileFilter;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.common.ResourcesImplementation;
import net.grinder.console.distribution.AgentCacheState;
import net.grinder.console.distribution.FileChangeWatcher;
import net.grinder.console.editor.Buffer;
import net.grinder.console.editor.EditorModel;
import net.grinder.console.editor.StringTextSource;
import net.grinder.console.editor.StringTextSource.Factory;
import net.grinder.console.editor.TextSource;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.DelegatingStubFactory;
import net.grinder.translation.Translations;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Unit tests for {@link FileTree}.
 *
 * @author Philip Aston
 */
public class TestFileTree extends AbstractJUnit4FileTestCase {
  private static final Resources s_resources =
    new ResourcesImplementation(
      "net.grinder.console.common.resources.Console");

  @Mock
  private ErrorHandler m_errorHandler;

  private final StringTextSource.Factory m_stringTextSourceFactory =
    new StringTextSource.Factory();

  private final DelegatingStubFactory<Factory>
    m_textSourceFactoryStubFactory =
      DelegatingStubFactory.create(m_stringTextSourceFactory);

  private final TextSource.Factory m_textSourceFactory =
    m_textSourceFactoryStubFactory.getStub();

  @Mock
  private AgentCacheState m_agentCacheState;

  @Mock
  private FileChangeWatcher m_fileChangeWatcher;

  @Mock
  private Translations m_translations;

  private final FileFilter m_nullFileFilter = new FileFilter() {
      public boolean accept(File pathname) {
        return true;
      }
    };

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test public void testConstruction() throws Exception {
    final EditorModel editorModel = new EditorModel(m_translations,
                                                    m_textSourceFactory,
                                                    m_agentCacheState,
                                                    m_fileChangeWatcher);

    final BufferTreeModel bufferTreeModel = new BufferTreeModel(editorModel);
    final FileTreeModel fileTreeModel =
      new FileTreeModel(editorModel, m_nullFileFilter, new File("c:"));

    final FileTree fileTree = new FileTree(s_resources, m_translations,
      m_errorHandler, editorModel, bufferTreeModel, fileTreeModel,
      new JLabel().getFont(), new JPopupMenu(), null);

    // Simulate L&F change.
    SwingUtilities.updateComponentTreeUI(fileTree.getComponent());

    assertNotNull(fileTree.getActions());
  }

  @Test public void testEditorModelListener() throws Exception {
    final EditorModel editorModel = new EditorModel(m_translations,
                                                    m_textSourceFactory,
                                                    m_agentCacheState,
                                                    m_fileChangeWatcher);

    final BufferTreeModel bufferTreeModel = new BufferTreeModel(editorModel);
    final FileTreeModel fileTreeModel =
      new FileTreeModel(editorModel, m_nullFileFilter, new File("c:"));

    new FileTree(s_resources, m_translations, m_errorHandler, editorModel,
                 bufferTreeModel, fileTreeModel, new JLabel().getFont(),
                 new JPopupMenu(), null);

    // Exercise the EditorModel listeners.
    editorModel.selectNewBuffer();
    editorModel.getSelectedBuffer().getTextSource().setText("Foo");
    editorModel.closeBuffer(editorModel.getSelectedBuffer());

    // Tests with files outside of the root directory.
    final File f1 = new File(getDirectory(), "file1");
    assertTrue(f1.createNewFile());
    final File f2 = new File(getDirectory(), "file2");
    assertTrue(f2.createNewFile());
    final Buffer buffer = editorModel.selectBufferForFile(f1);
    buffer.save(f2);
    editorModel.closeBuffer(buffer);

    final Buffer buffer2 = editorModel.selectBufferForFile(f1);
    fileTreeModel.setRootDirectory(getDirectory());
    fileTreeModel.refresh();

    editorModel.selectBufferForFile(f1);
    buffer2.save(f2);
    editorModel.selectBuffer(buffer2);

    buffer2.save(f1);
    fileTreeModel.refresh();     // Create new FileNodes.
    editorModel.selectBuffer(buffer2);

    editorModel.selectBufferForFile(f2);
    editorModel.closeBuffer(buffer2);
  }

  @Test public void testDisplay() throws Exception {
    final EditorModel editorModel = new EditorModel(m_translations,
                                                    m_textSourceFactory,
                                                    m_agentCacheState,
                                                    m_fileChangeWatcher);

    final BufferTreeModel bufferTreeModel = new BufferTreeModel(editorModel);
    final FileTreeModel fileTreeModel =
      new FileTreeModel(editorModel, m_nullFileFilter, getDirectory());

    final FileTree fileTree =
      new FileTree(s_resources, m_translations, m_errorHandler, editorModel,
                   bufferTreeModel, fileTreeModel, new JLabel().getFont(),
                   new JPopupMenu(), null);

    final JFrame frame = new JFrame();

    frame.getContentPane().add(fileTree.getComponent(), BorderLayout.CENTER);
    frame.pack();
    frame.setVisible(true);
    frame.dispose();
  }
}
