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

package net.grinder.console.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import net.grinder.common.GrinderProperties;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.distribution.AgentCacheState;
import net.grinder.console.distribution.FileChangeWatcher;
import net.grinder.console.editor.EditorModel.Listener;
import net.grinder.console.editor.StringTextSource.Factory;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.CallData;
import net.grinder.testutility.DelegatingStubFactory;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.translation.Translations;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;


/**
 * Unit test for {@link EditorModel}.
 *
 * @author Philip Aston
 */
public class TestEditorModel extends AbstractJUnit4FileTestCase {

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

  @Test public void testConstruction() throws Exception {
    final StringTextSource.Factory stringTextSourceFactory =
      new StringTextSource.Factory();

    final DelegatingStubFactory<Factory> textSourceFactoryStubFactory =
      DelegatingStubFactory.create(stringTextSourceFactory);
    final EditorModel editorModel =
      new EditorModel(m_translations,
                      textSourceFactoryStubFactory.getStub(),
                      m_agentCacheState,
                      m_fileChangeWatcher);

    textSourceFactoryStubFactory.assertNoMoreCalls();
    assertNull(editorModel.getSelectedBuffer());
    assertEquals(0, editorModel.getBuffers().length);
  }

  @Test public void testSelectBufferForFile() throws Exception {
    final StringTextSource.Factory stringTextSourceFactory =
      new StringTextSource.Factory();

    final DelegatingStubFactory<Factory> textSourceFactoryStubFactory =
      DelegatingStubFactory.create(stringTextSourceFactory);
    final EditorModel editorModel =
      new EditorModel(m_translations,
                      textSourceFactoryStubFactory.getStub(),
                      m_agentCacheState,
                      m_fileChangeWatcher);

    textSourceFactoryStubFactory.resetCallHistory();

    final File file1 = createFile("myfile.txt", "blah");
    final File file2 = createFile("anotherFile.py", "Some stuff");

    final RandomStubFactory<Listener> listener1StubFactory =
      RandomStubFactory.create(EditorModel.Listener.class);

    final RandomStubFactory<Listener> listener2StubFactory =
      RandomStubFactory.create(EditorModel.Listener.class);

    editorModel.addListener(listener1StubFactory.getStub());
    editorModel.addListener(listener2StubFactory.getStub());

    editorModel.selectBufferForFile(file1);

    assertNotNull(editorModel.getSelectedBuffer());
    assertEquals(file1, editorModel.getSelectedBuffer().getFile());
    textSourceFactoryStubFactory.assertSuccess("create");
    textSourceFactoryStubFactory.assertNoMoreCalls();

    final CallData callData =
      listener1StubFactory.assertSuccess("bufferAdded", Buffer.class);
    final Buffer bufferForFile1 = (Buffer)callData.getParameters()[0];
    assertSame(bufferForFile1, editorModel.getSelectedBuffer());
    listener1StubFactory.assertSuccess("bufferStateChanged", bufferForFile1);
    listener1StubFactory.assertNoMoreCalls();

    listener2StubFactory.assertSuccess("bufferAdded", bufferForFile1);
    listener2StubFactory.assertSuccess("bufferStateChanged", bufferForFile1);
    listener2StubFactory.assertNoMoreCalls();

    // Select same buffer is a no-op.
    editorModel.selectBufferForFile(file1);

    assertSame(bufferForFile1, editorModel.getSelectedBuffer());
    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertNoMoreCalls();

    editorModel.selectBufferForFile(file2);

    assertNotSame(bufferForFile1, editorModel.getSelectedBuffer());
    textSourceFactoryStubFactory.assertSuccess("create");
    textSourceFactoryStubFactory.assertNoMoreCalls();
    listener1StubFactory.assertSuccess("bufferAdded", BufferImplementation.class);
    listener1StubFactory.assertSuccess("bufferStateChanged", bufferForFile1);
    listener1StubFactory.assertSuccess("bufferStateChanged", BufferImplementation.class);
    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertSuccess("bufferAdded", BufferImplementation.class);
    listener2StubFactory.assertSuccess("bufferStateChanged", bufferForFile1);
    listener2StubFactory.assertSuccess("bufferStateChanged", BufferImplementation.class);
    listener2StubFactory.assertNoMoreCalls();

    editorModel.selectBufferForFile(file1);

    textSourceFactoryStubFactory.assertNoMoreCalls();
    assertSame(bufferForFile1, editorModel.getSelectedBuffer());
    listener1StubFactory.assertSuccess("bufferStateChanged", BufferImplementation.class);
    listener1StubFactory.assertSuccess("bufferStateChanged", bufferForFile1);
    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertSuccess("bufferStateChanged", BufferImplementation.class);
    listener2StubFactory.assertSuccess("bufferStateChanged", bufferForFile1);
    listener2StubFactory.assertNoMoreCalls();

    final StringTextSource textSource1 =
      (StringTextSource)bufferForFile1.getTextSource();

    textSource1.markDirty();
    textSource1.markDirty();
    textSource1.markDirty();
    textSource1.markDirty();
    listener1StubFactory.assertSuccess("bufferStateChanged", bufferForFile1);
    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertSuccess("bufferStateChanged", bufferForFile1);
    listener2StubFactory.assertNoMoreCalls();
  }

  @Test public void testSelectNewBuffer() throws Exception {
    final StringTextSource.Factory stringTextSourceFactory =
      new StringTextSource.Factory();

    final DelegatingStubFactory<Factory> textSourceFactoryStubFactory =
      DelegatingStubFactory.create(stringTextSourceFactory);

    final RandomStubFactory<Listener> listener1StubFactory =
      RandomStubFactory.create(EditorModel.Listener.class);

    final EditorModel editorModel =
      new EditorModel(m_translations,
                      textSourceFactoryStubFactory.getStub(),
                      m_agentCacheState,
                      m_fileChangeWatcher);

    final Buffer defaultBuffer = editorModel.getSelectedBuffer();
    assertNull(defaultBuffer);

    editorModel.addListener(listener1StubFactory.getStub());

    textSourceFactoryStubFactory.resetCallHistory();

    editorModel.selectNewBuffer();
    textSourceFactoryStubFactory.assertSuccess("create");
    textSourceFactoryStubFactory.assertNoMoreCalls();

    final Buffer buffer1 = editorModel.getSelectedBuffer();
    assertNotSame(buffer1, defaultBuffer);

    listener1StubFactory.assertSuccess("bufferAdded", buffer1);
    listener1StubFactory.assertSuccess("bufferStateChanged", buffer1);
    listener1StubFactory.assertNoMoreCalls();

    editorModel.selectNewBuffer();

    final Buffer buffer2 = editorModel.getSelectedBuffer();
    assertNotSame(buffer2, buffer1);

    listener1StubFactory.assertSuccess("bufferAdded", buffer2);
    listener1StubFactory.assertSuccess("bufferStateChanged", buffer1);
    listener1StubFactory.assertSuccess("bufferStateChanged", buffer2);
    listener1StubFactory.assertNoMoreCalls();
  }

  private File createFile(final String name, final String text) throws Exception {
    final File file = new File(getDirectory(), name);
    final FileWriter out = new FileWriter(file);
    out.write(text);
    out.close();

    return file;
  }

  @Test public void testIsBoringFile() throws Exception {
    final EditorModel editorModel =
      new EditorModel(m_translations,
                      new StringTextSource.Factory(),
                      m_agentCacheState,
                      m_fileChangeWatcher);

    final File[] boring = {
      new File("some.class"),
      new File("~temporary"),
      new File("#BLAH BLAH"),
      new File("furble.exe"),
      new File("PIC.GIF"),
      new File("dfadhklfda.Jpeg"),
      new File("dfadhklfda.jpg"),
      new File("dfadhklfda.tiff"),
    };

    for (final File element : boring) {
      assertTrue("Is boring: " + element,
                 editorModel.isBoringFile(element));
    }

    final File[] notBoring = {
      null,
      new File("Script.Py"),
      new File("some.java"),
      new File("my.properties"),
      new File("README"),
      new File("info.text"),
    };

    for (int i = 0; i < notBoring.length; ++i) {
      assertTrue("Isn't boring: " + notBoring[i],
                 !editorModel.isBoringFile(notBoring[i]));
    }
  }

  @Test public void testIsScriptFile() throws Exception {
    final EditorModel editorModel =
      new EditorModel(m_translations,
                      new StringTextSource.Factory(),
                      m_agentCacheState,
                      m_fileChangeWatcher);

    final File[] script = {
      new File("my file.py"),
      new File(".blah.py"),
      new File("python.PY"),
      new File("~python.py"),
      new File("clojure.clj"),
      new File(".clj"),
    };

    for (final File element : script) {
      assertTrue("Is script: " + element,
                 editorModel.isScriptFile(element));
    }

    final File[] notScript = {
      null,
      new File("script.python"),
      new File("script.py "),
      new File("foo.bah"),
      new File("x.text"),
    };

    for (int i = 0; i < notScript.length; ++i) {
      assertTrue("Isn't script: " + notScript[i],
                 !editorModel.isScriptFile(notScript[i]));
    }
  }

  @Test public void testIsPropertiesFile() throws Exception {
    final EditorModel editorModel =
      new EditorModel(m_translations,
                      new StringTextSource.Factory(),
                      m_agentCacheState,
                      m_fileChangeWatcher);

    final File[] properties = {
      new File("my file.properties"),
      new File(".blah.properties"),
      new File("python.PROPERTIES"),
      new File("~python.properties"),
    };

    for (final File propertie : properties) {
      assertTrue("Is properties: " + propertie,
                 editorModel.isPropertiesFile(propertie));
    }

    final File[] notProperties = {
      null,
      new File("script.props"),
      new File("script.properties "),
      new File("foo.bah"),
      new File("x.text"),
    };

    for (int i = 0; i < notProperties.length; ++i) {
      assertTrue("Isn't properties: " + notProperties[i],
                 !editorModel.isPropertiesFile(notProperties[i]));
    }
  }

  @Test public void testCloseBufferAndIsABufferDirty() throws Exception {
    final EditorModel editorModel =
      new EditorModel(m_translations,
                      new StringTextSource.Factory(),
                      m_agentCacheState,
                      m_fileChangeWatcher);

    final RandomStubFactory<Listener> listenerStubFactory =
      RandomStubFactory.create(EditorModel.Listener.class);

    final File file1 = createFile("myfile.txt", "blah");
    final File file2 = createFile("another.py", "blah");

    editorModel.selectBufferForFile(file1);
    final Buffer buffer1 = editorModel.getSelectedBuffer();

    editorModel.selectBufferForFile(file2);
    final Buffer buffer2 = editorModel.getSelectedBuffer();

    editorModel.selectBufferForFile(createFile("blah", "blah"));
    final Buffer buffer3 = editorModel.getSelectedBuffer();

    assertEquals(3, editorModel.getBuffers().length);

    assertTrue(!editorModel.isABufferDirty());

    editorModel.addListener(listenerStubFactory.getStub());

    editorModel.closeBuffer(buffer3);

    listenerStubFactory.assertSuccess("bufferStateChanged", buffer3);
    listenerStubFactory.assertSuccess("bufferStateChanged", buffer2);
    listenerStubFactory.assertSuccess("bufferRemoved", buffer3);
    listenerStubFactory.assertNoMoreCalls();

    assertEquals(2, editorModel.getBuffers().length);

    assertEquals(buffer2, editorModel.getSelectedBuffer());

    assertTrue(!editorModel.isABufferDirty());

    ((StringTextSource)buffer1.getTextSource()).markDirty();

    assertTrue(editorModel.isABufferDirty());
    listenerStubFactory.assertSuccess("bufferStateChanged", buffer1);
    listenerStubFactory.assertNoMoreCalls();

    editorModel.closeBuffer(buffer1);

    assertTrue(!editorModel.isABufferDirty());
    listenerStubFactory.assertSuccess("bufferRemoved", buffer1);
    listenerStubFactory.assertNoMoreCalls();

    editorModel.closeBuffer(buffer1);
    editorModel.closeBuffer(buffer3);
    listenerStubFactory.assertNoMoreCalls();

    editorModel.closeBuffer(buffer2);
    assertEquals(0, editorModel.getBuffers().length);

    assertTrue(!editorModel.isABufferDirty());

    listenerStubFactory.assertSuccess("bufferStateChanged", buffer2);
    listenerStubFactory.assertSuccess("bufferRemoved", buffer2);
    listenerStubFactory.assertNoMoreCalls();
  }

  @Test public void testSaveBufferAs() throws Exception {
    final StringTextSource.Factory stringTextSourceFactory =
      new StringTextSource.Factory();

    final EditorModel editorModel = new EditorModel(m_translations,
                                                    stringTextSourceFactory,
                                                    m_agentCacheState,
                                                    m_fileChangeWatcher);

    final RandomStubFactory<Listener> listenerStubFactory =
      RandomStubFactory.create(EditorModel.Listener.class);

    editorModel.selectNewBuffer();
    final Buffer buffer = editorModel.getSelectedBuffer();
    stringTextSourceFactory.getLast().setText("Some text");

    final File file1 = new File(getDirectory(), "a file");
    final File file2 = new File(getDirectory(), "another  file");

    editorModel.addListener(listenerStubFactory.getStub());

    buffer.save(file1);

    // Buffer changed because it is associated with a new file.
    listenerStubFactory.assertSuccess("bufferStateChanged", buffer);
    listenerStubFactory.assertNoMoreCalls();
    m_agentCacheStateStubFactory.assertSuccess("setNewFileTime",
                                               new Long(file1.lastModified()));
    m_agentCacheStateStubFactory.assertNoMoreCalls();

    buffer.save(file1);
    listenerStubFactory.assertNoMoreCalls();
    m_agentCacheStateStubFactory.assertSuccess("setNewFileTime",
                                               new Long(file1.lastModified()));
    m_agentCacheStateStubFactory.assertNoMoreCalls();

    assertEquals(buffer, editorModel.getBufferForFile(file1));

    buffer.save(file2);

    // Buffer changed because it is associated with a new file.
    listenerStubFactory.assertSuccess("bufferStateChanged", buffer);
    listenerStubFactory.assertNoMoreCalls();
    m_agentCacheStateStubFactory.assertSuccess("setNewFileTime",
                                               new Long(file2.lastModified()));
    m_agentCacheStateStubFactory.assertNoMoreCalls();

    assertNull(editorModel.getBufferForFile(file1));
    assertEquals(buffer, editorModel.getBufferForFile(file2));
  }

  @Test public void testGetAndSelectProperties() throws Exception {
    final EditorModel editorModel =
      new EditorModel(m_translations,
                      new StringTextSource.Factory(),
                      m_agentCacheState,
                      m_fileChangeWatcher);

    assertNull(editorModel.getSelectedPropertiesFile());

    final File script = new File(getDirectory(), "lah");

    final File f1 = new File(".");
    editorModel.setSelectedPropertiesFile(f1);
    assertSame(f1, editorModel.getSelectedPropertiesFile());
    editorModel.setSelectedPropertiesFile(null);
    assertFalse(editorModel.isSelectedScript(script));
    assertNull(editorModel.getSelectedPropertiesFile());
    assertFalse(editorModel.isSelectedScript(script));

    final File f2 = new File(getDirectory(), "foo.properties");
    editorModel.setSelectedPropertiesFile(f2);
    assertFalse(editorModel.isSelectedScript(script));
    final File defaultScript =
      new File(getDirectory(), GrinderProperties.DEFAULT_SCRIPT.getPath());
    assertTrue(editorModel.isSelectedScript(defaultScript));

    final GrinderProperties properties = new GrinderProperties(f2);
    properties.setFile("grinder.script", script);
    properties.save();
    assertFalse(editorModel.isSelectedScript(script));
    editorModel.setSelectedPropertiesFile(f2);
    assertTrue(editorModel.isSelectedScript(script));
    assertFalse(editorModel.isSelectedScript(defaultScript));

    // Again with relative path.
    editorModel.setSelectedPropertiesFile(f1);
    assertFalse(editorModel.isSelectedScript(script));
    properties.setFile("grinder.script", new File(script.getName()));
    properties.save();
    assertFalse(editorModel.isSelectedScript(script));
    editorModel.setSelectedPropertiesFile(f2);
    assertTrue(editorModel.isSelectedScript(script));

    // Again with bogus path.
    final char[] manyChars = new char[65536];
    Arrays.fill(manyChars, 'x');
    final File bogusFile = new File(new String(manyChars));
    properties.setFile("grinder.script", bogusFile);
    properties.save();
    assertTrue(editorModel.isSelectedScript(script));
    editorModel.setSelectedPropertiesFile(f2);
    assertFalse(editorModel.isSelectedScript(bogusFile));
    assertFalse(editorModel.isSelectedScript(script));
  }

  @Test public void testAbstractListener() throws Exception {
    final EditorModel.Listener listener = new EditorModel.AbstractListener() {};

    listener.bufferAdded(null);
    listener.bufferNotUpToDate(null);
    listener.bufferRemoved(null);
    listener.bufferStateChanged(null);
  }

  @Test public void testChangedFilesMonitoring() throws Exception {
    final EditorModel editorModel =
      new EditorModel(m_translations,
                      new StringTextSource.Factory(),
                      m_agentCacheState,
                      m_fileChangeWatcher);

    final CallData addFileChangedListenerCallData =
      m_fileChangeWatcherStubFactory.assertSuccess(
        "addFileChangedListener",
        FileChangeWatcher.FileChangedListener.class);

    final FileChangeWatcher.FileChangedListener fileChangedListener =
      (FileChangeWatcher.FileChangedListener)
      addFileChangedListenerCallData.getParameters()[0];

    final RandomStubFactory<Listener> editorModelListenerStubFactory =
      RandomStubFactory.create(EditorModel.Listener.class);
    editorModel.addListener(editorModelListenerStubFactory.getStub());

    final File f1 = new File(getDirectory(), "test file");
    assertTrue(f1.createNewFile());
    final Buffer buffer = editorModel.selectBufferForFile(f1);
    assertTrue(buffer.isUpToDate());
    editorModelListenerStubFactory.assertSuccess("bufferAdded", buffer);
    editorModelListenerStubFactory.assertSuccess("bufferStateChanged", buffer);
    editorModelListenerStubFactory.assertNoMoreCalls();

    assertTrue(f1.setLastModified(System.currentTimeMillis() + 1000));
    assertFalse(buffer.isUpToDate());

    editorModelListenerStubFactory.assertNoMoreCalls();

    fileChangedListener.filesChanged(new File[] { getDirectory(), f1, });

    editorModelListenerStubFactory.assertSuccess("bufferNotUpToDate", buffer);
    editorModelListenerStubFactory.assertNoMoreCalls();

    // Selecting a modified buffer should also fire bufferNotUpToDate.
    assertTrue(f1.setLastModified(System.currentTimeMillis() + 2000));
    editorModel.selectBufferForFile(f1);
    editorModelListenerStubFactory.assertSuccess("bufferNotUpToDate", buffer);
    editorModelListenerStubFactory.assertNoMoreCalls();
  }

  @Test public void testOpenWithExternalEditor() throws Exception {
    final EditorModel editorModel =
      new EditorModel(m_translations,
                      new StringTextSource.Factory(),
                      m_agentCacheState,
                      m_fileChangeWatcher);

    try {
      editorModel.openWithExternalEditor(null);
      fail("Expected DisplayMessageConsoleException");
    }
    catch (final DisplayMessageConsoleException e) {
      assertNull(e.getCause());
    }

    editorModel.setExternalEditor(new File("not a command"), "bah");

    try {
      editorModel.openWithExternalEditor(new File("foo"));
      fail("Expected DisplayMessageConsoleException");
    }
    catch (final DisplayMessageConsoleException e) {
      assertTrue(e.getCause() instanceof IOException);
    }

    editorModel.setExternalEditor(null, "bah");

    try {
      editorModel.openWithExternalEditor(new File("foo"));
      fail("Expected DisplayMessageConsoleException");
    }
    catch (final DisplayMessageConsoleException e) {
      assertNull(e.getCause());
    }
  }
}
