// Copyright (C) 2007 - 2009 Philip Aston
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

import java.beans.PropertyChangeListener;
import java.io.File;

import net.grinder.console.common.Resources;
import net.grinder.console.common.ResourcesImplementation;
import net.grinder.console.distribution.AgentCacheState;
import net.grinder.console.distribution.FileChangeWatcher;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit tests for {@link ExternalEditor}.
 *
 * @author Philip Aston
 */
public class TestExternalEditor extends AbstractFileTestCase {

  private static final String s_testClasspath =
    "\"" + System.getProperty("java.class.path") + "\"";

  private static final Resources s_resources =
    new ResourcesImplementation(
      "net.grinder.console.common.resources.Console");

  private final RandomStubFactory<FileChangeWatcher>
    m_fileChangeWatcherStubFactory =
      RandomStubFactory.create(FileChangeWatcher.class);
  private final FileChangeWatcher m_fileChangeWatcher =
    m_fileChangeWatcherStubFactory.getStub();

  public void testFileToCommandLine() throws Exception {
    final File commandFile = new File("foo");
    final File file = new File("lah");

    final ExternalEditor externalEditor1 =
      new ExternalEditor(null, null, commandFile, "bah dah");
    final String[] result1 = externalEditor1.fileToCommandLine(file);

    AssertUtilities.assertArraysEqual(
      new String[] { commandFile.getAbsolutePath(),
                     "bah",
                     "dah",
                     file.getAbsolutePath(), },
      result1);


    final ExternalEditor externalEditor2 =
      new ExternalEditor(null, null, commandFile, "-f '%f'");
    final String[] result2 = externalEditor2.fileToCommandLine(file);

    AssertUtilities.assertArraysEqual(
      new String[] { commandFile.getAbsolutePath(),
                     "-f", "'" + file.getAbsolutePath() + "'", },
      result2);


    final ExternalEditor externalEditor3 =
      new ExternalEditor(null, null, commandFile, null);
    final String[] result3 = externalEditor3.fileToCommandLine(file);

    AssertUtilities.assertArraysEqual(
      new String[] { commandFile.getAbsolutePath(), file.getAbsolutePath(), },
      result3);
  }

  public void testOpen() throws Exception {
    if (Boolean.getBoolean("build.travis")) {
      final long[] lastInvalidAfter = new long[1];

      final AgentCacheState cacheState =
              new AgentCacheState() {

                public void addListener(PropertyChangeListener listener) {
                }

                public boolean getOutOfDate() {
                  return false;
                }

                public void setNewFileTime(long invalidAfter) {
                  lastInvalidAfter[0] = invalidAfter;
                }
              };


      final StringTextSource.Factory stringTextSourceFactory =
              new StringTextSource.Factory();

      final EditorModel editorModel = new EditorModel(s_resources,
              stringTextSourceFactory,
              cacheState,
              m_fileChangeWatcher);

      String jvmLocation;
      if (System.getProperty("os.name").startsWith("Win")) {
        jvmLocation = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";
      } else {
        jvmLocation = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java";
      }
      final ExternalEditor externalEditor1 =
              new ExternalEditor(cacheState,
                      editorModel,
                      new File(jvmLocation),
                      "-classpath " + s_testClasspath + " " +
                              TouchClass.class.getName() + " " +
                              TouchClass.TOUCH + " %f");

      final File file = new File(getDirectory(), "helloWorld");
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
      ((StringTextSource) buffer.getTextSource()).markDirty();
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
                      new File(jvmLocation),
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
                      new File(jvmLocation),
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
}
