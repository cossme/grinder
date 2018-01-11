// Copyright (C) 2007 - 2013 Philip Aston
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.beans.PropertyChangeListener;
import java.io.File;

import net.grinder.console.distribution.AgentCacheState;
import net.grinder.console.distribution.FileChangeWatcher;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.translation.Translations;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;


/**
 * Unit tests for {@link ExternalEditor}.
 *
 * @author Philip Aston
 */
public class TestExternalEditor extends AbstractJUnit4FileTestCase {

  private static final String s_testClasspath =
    System.getProperty("java.class.path");

  @Mock private Translations m_translations;

  @Mock
  private FileChangeWatcher m_fileChangeWatcher;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void testFileToCommandLine() throws Exception {
    final File commandFile = new File("foo");
    final File file = new File("lah");

    final ExternalEditor externalEditor1 =
      new ExternalEditor(null, null, commandFile, "bah dah");
    final String[] result1 = externalEditor1.fileToCommandLine(file);

    assertArrayEquals(
      new String[] { commandFile.getAbsolutePath(),
                     "bah",
                     "dah",
                     file.getAbsolutePath(), },
      result1);


    final ExternalEditor externalEditor2 =
      new ExternalEditor(null, null, commandFile, "-f '%f'");
    final String[] result2 = externalEditor2.fileToCommandLine(file);

    assertArrayEquals(
      new String[] { commandFile.getAbsolutePath(),
                     "-f", "'" + file.getAbsolutePath() + "'", },
      result2);


    final ExternalEditor externalEditor3 =
      new ExternalEditor(null, null, commandFile, null);
    final String[] result3 = externalEditor3.fileToCommandLine(file);

    assertArrayEquals(
      new String[] { commandFile.getAbsolutePath(), file.getAbsolutePath(), },
      result3);
  }

  @Test
  public void testOpen() throws Exception {
    final long[] lastInvalidAfter = new long[1];

    final AgentCacheState cacheState =
      new AgentCacheState() {

        @Override
        public void addListener(final PropertyChangeListener listener) {}

        @Override
        public boolean getOutOfDate() { return false; }

        @Override
        public void setNewFileTime(final long invalidAfter) {
          lastInvalidAfter[0] = invalidAfter;
        }};


    final StringTextSource.Factory stringTextSourceFactory =
      new StringTextSource.Factory();

    final EditorModel editorModel = new EditorModel(m_translations,
                                                    stringTextSourceFactory,
                                                    cacheState,
                                                    m_fileChangeWatcher);


    final ExternalEditor externalEditor1 =
      new ExternalEditor(cacheState,
                         editorModel,
                         new File("/usr/bin/java"),
                         "-classpath " + s_testClasspath + " " +
                         TouchClass.class.getName() + " " +
                         TouchClass.TOUCH + " %f");

    final File file = new File(getDirectory(), "hello world");
    assertTrue(file.createNewFile());
    assertTrue(file.setLastModified(0));
    assertEquals(0, file.lastModified());

    externalEditor1.open(file);

    for (int i = 0;
         i < 20 && ExternalEditor.getThreadGroup().activeCount() > 0;
         ++i) {
      Thread.sleep(i * 10);
    }

    final long firstModification = file.lastModified();
    assertTrue(firstModification != 0);
    assertEquals(firstModification, lastInvalidAfter[0]);


    // Clean buffers get reloaded.
    final Buffer buffer = editorModel.selectBufferForFile(file);
    assertTrue(file.setLastModified(0));

    externalEditor1.open(file);

    for (int i = 0;
         i < 20 && ExternalEditor.getThreadGroup().activeCount() > 0;
         ++i) {
      Thread.sleep(i * 10);
    }

    final long secondModification = file.lastModified();
    assertTrue(secondModification != 0);
    assertEquals(secondModification, lastInvalidAfter[0]);
    assertTrue(buffer.isUpToDate());


    // Dirty buffers don't get reloaded.
    assertTrue(file.setLastModified(0));
    buffer.load();
    ((StringTextSource)buffer.getTextSource()).markDirty();
    assertTrue(buffer.isDirty());

    externalEditor1.open(file);

    for (int i = 0;
         i < 20 && ExternalEditor.getThreadGroup().activeCount() > 0;
         ++i) {
      Thread.sleep(i * 10);
    }

    final long thirdModification = file.lastModified();
    assertTrue(thirdModification != 0);
    assertEquals(thirdModification, lastInvalidAfter[0]);
    assertTrue(!buffer.isUpToDate());


    // Try again, this time not editing.
    final ExternalEditor externalEditor2 =
      new ExternalEditor(cacheState,
                         editorModel,
                         new File("/usr/bin/java"),
                         "-classpath " + s_testClasspath + " " +
                         TouchClass.class.getName() + " " +
                         TouchClass.NOOP + " %f");

    assertTrue(file.setLastModified(0));
    assertEquals(0, file.lastModified());

    externalEditor2.open(file);

    for (int i = 0;
         i < 20 && ExternalEditor.getThreadGroup().activeCount() > 0;
         ++i) {
      Thread.sleep(i * 10);
    }

    assertEquals(0, file.lastModified());
    assertEquals(thirdModification, lastInvalidAfter[0]);


    // Once more, this time interrupting the process.

    final ExternalEditor externalEditor3 =
      new ExternalEditor(cacheState,
                         editorModel,
                         new File("/usr/bin/java"),
                         "-classpath " + s_testClasspath + " " +
                         TouchClass.class.getName() + " " +
                         TouchClass.SLEEP + " %f");

    assertTrue(file.setLastModified(0));
    assertEquals(0, file.lastModified());

    externalEditor3.open(file);

    ExternalEditor.getThreadGroup().interrupt();

    assertEquals(0, file.lastModified());
    assertEquals(thirdModification, lastInvalidAfter[0]);
  }
}
