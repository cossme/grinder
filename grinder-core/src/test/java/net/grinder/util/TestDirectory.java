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

package net.grinder.util;

import static net.grinder.testutility.FileUtilities.createRandomFile;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.FileUtilities;
import net.grinder.testutility.Serializer;

import org.junit.Test;


/**
 * Unit test case for {@link Directory}.
 *
 * @author Philip Aston
 */
public class TestDirectory extends AbstractJUnit4FileTestCase {

  @Test public void testConstruction() throws Exception {

    final File file = new File(getDirectory(), "x");
    assertTrue(file.createNewFile());

    try {
      new Directory(file);
      fail("Expected DirectoryException");
    }
    catch (final Directory.DirectoryException e) {
    }

    final Directory directory = new Directory(getDirectory());
    assertEquals(0, directory.getWarnings().length);

    assertEquals(getDirectory(), directory.getFile());

    assertEquals(new File("."), new Directory(null).getFile());
  }

  @Test public void testDefaultConstructor() throws Exception {

    final Directory directory = new Directory();
    final File cwd = new File(System.getProperty("user.dir"));
    assertEquals(cwd.getCanonicalPath(),
                 directory.getFile().getCanonicalPath());
  }

  @Test public void testEquality() throws Exception {

    final Directory d1 = new Directory(getDirectory());
    final Directory d2 = new Directory(getDirectory());

    final File f = new File(getDirectory(), "comeonpilgrimyouknowhelovesyou");
    assertTrue(f.mkdir());

    final Directory d3 = new Directory(f);

    assertEquals(d1, d1);
    assertEquals(d1, d2);
    AssertUtilities.assertNotEquals(d2, d3);

    assertEquals(d1.hashCode(), d1.hashCode());
    assertEquals(d1.hashCode(), d2.hashCode());

    AssertUtilities.assertNotEquals(d1, null);
    AssertUtilities.assertNotEquals(d1, f);
  }

  @Test public void testListContents() throws Exception {

    final Directory directory = new Directory(getDirectory());

    final String[] files = {
      "first/three",
      "will-not-be-picked-up",
      "because/they/are/too/old",
      "directory/foo/bah/blah",
      "directory/blah",
      "a/b/c/d/e",
      "a/b/f/g/h",
      "a/b/f/g/i",
      "x",
      "y/z",
      "another",
    };

    final Set<File> expected = new HashSet<File>();

    for (int i=0; i<files.length; ++i) {
      final File file = new File(getDirectory(), files[i]);
      file.getParentFile().mkdirs();
      assertTrue(file.createNewFile());

      if (i < 3) {
        assertTrue(file.setLastModified(10000L * (i + 1)));
      }
      else {
        // Result uses relative paths.
        expected.add(new File(files[i]));
      }
    }

    final File[] badDirectories = {
      new File(getDirectory(), "directory/foo/bah/blah.cantread"),
      new File(getDirectory(), "cantread"),
    };

    for (final File badDirectorie : badDirectories) {
      badDirectorie.getParentFile().mkdirs();
      assertTrue(badDirectorie.mkdir());
      FileUtilities.setCanAccess(badDirectorie, false);
    }

    final File[] filesAfterTimeT = directory.listContents(
      new FileFilter() {
        @Override
        public boolean accept(final File file) {
          return file.isDirectory() || file.lastModified() > 50000L;
        }
      });

    for (final File element : filesAfterTimeT) {
      assertTrue("Contains " + element,
                 expected.contains(element));
    }

    final String[] warnings = directory.getWarnings();
    assertEquals(badDirectories.length, warnings.length);

    final StringBuffer warningsBuffer = new StringBuffer();

    for (final String warning : warnings) {
      warningsBuffer.append(warning);
      warningsBuffer.append("\n");
    }

    final String warningsString = warningsBuffer.toString();

    for (final File badDirectorie : badDirectories) {
      assertTrue(warningsBuffer + " contains " + badDirectorie.getPath(),
                 warningsString.indexOf(badDirectorie.getPath()) > -1);

      FileUtilities.setCanAccess(badDirectorie, true);
    }

    final File[] allFiles =
      directory.listContents(Directory.getMatchAllFilesFilter());
    assertEquals(files.length, allFiles.length);
  }

  @Test public void testDeleteContents() throws Exception {

    final Directory directory = new Directory(getDirectory());

    final String[] files = {
      "directory/foo/bah/blah",
      "directory/blah",
      "a/b/c/d/e",
      "a/b/f/g/h",
      "a/b/f/g/i",
      "x",
      "y/z",
      "another",
    };

    for (final String file2 : files) {
      final File file = new File(getDirectory(), file2);
      file.getParentFile().mkdirs();
      assertTrue(file.createNewFile());
    }

    assertTrue(getDirectory().list().length > 0);

    directory.deleteContents();

    assertEquals(0, getDirectory().list().length);

    // Can't test that deleteContents() throws an exception if
    // contents couldn't be deleted as File.delete() ignores file
    // permissions on W2K.
  }

  @Test public void testCreate() throws Exception {
    final String[] directories = {
      "toplevel",
      "down/a/few",
    };

    for (final String directorie : directories) {
      final Directory directory =
        new Directory(new File(getDirectory(), directorie));
      assertFalse(directory.getFile().exists());
      directory.create();
      assertTrue(directory.getFile().exists());
    }

    final File file = new File(getDirectory(), "readonly");
    assertTrue(file.createNewFile());
    FileUtilities.setCanAccess(file, false);

    try {
      new Directory(new File(getDirectory(), "readonly/foo")).create();
      fail("Expected DirectoryException");
    }
    catch (final Directory.DirectoryException e) {
    }
  }

  @Test public void testDelete() throws Exception {
    final Directory directory1 =
      new Directory(new File(getDirectory(), "a/directory"));
    directory1.create();
    assertTrue(directory1.getFile().exists());
    directory1.delete();
    assertFalse(directory1.getFile().exists());

    final Directory directory2 =
      new Directory(new File(getDirectory(), "another"));
    directory2.create();
    final File file2 = new File(getDirectory(), "another/file");
    assertTrue(file2.createNewFile());

    try {
      directory2.delete();
      fail("Expected DirectoryException");
    }
    catch (final Directory.DirectoryException e) {
    }
  }

  @Test public void testRelativeFileDot() throws Exception {
    final File result = new Directory().relativeFile(new File("."), false);
    assertEquals(new File("."), result);
  }

  @Test public void testRelativeFileWithAbsoluteFile() throws Exception {

    final Directory directory = new Directory(getDirectory());

    final File absoluteFile = new File("blah").getAbsoluteFile();

    assertNull(directory.relativeFile(absoluteFile, true));
  }

  @Test public void relativeFileWithAbsoluteChild1() throws Exception {

    final File absoluteFile =
        new File(getDirectory(), "blah").getAbsoluteFile();

    final Directory directory = new Directory(getDirectory());
    final File result = directory.relativeFile(absoluteFile, false);
    assertTrue(!result.isAbsolute());
    assertEquals("blah", result.getPath());
  }

  @Test public void testRelativeFileWithAbsouteChild2() throws Exception {

    final File absoluteFile =
        new File(getDirectory(), "blah").getAbsoluteFile();

    final Directory directory = new Directory(getDirectory());
    final File result = directory.relativeFile(absoluteFile, true);
    assertTrue(!result.isAbsolute());
    assertEquals("blah", result.getPath());
  }

  @Test public void testRelativeFileWithRelativeChild() throws Exception {

    final File relaiveFile = new File("blah");

    final Directory directory = new Directory(getDirectory());
    final File result = directory.relativeFile(relaiveFile, true);
    assertTrue(!result.isAbsolute());
    assertEquals("blah", result.getPath());
  }

  @Test public void testRelativeFileWithRelativeNonChild1() throws Exception {

    final File relativeFile = new File("../blah");

    final Directory directory = new Directory(getDirectory());
    assertNull(directory.relativeFile(relativeFile, true));
  }

  @Test public void testRelativeFileWithRelativeNonChild2() throws Exception {

    final File relativeFile = new File("../blah");

    final Directory directory = new Directory(getDirectory());
    final File result = directory.relativeFile(relativeFile, false);
    assertSame(relativeFile, result);
  }

  @Test public void testIsParentOf() throws Exception {
    final File f1 = new File("xfoo");
    final File f2 = new File("xfoo/bah");
    final File f3 = new File("xfoo/bah/blah");
    final File f4 = new File("xfoo/bah/dah");

    assertTrue(new Directory(f1).isParentOf(f2));
    assertTrue(new Directory(f1).isParentOf(f3));

    assertFalse(new Directory(f2).isParentOf(f2));
    assertFalse(new Directory(f2).isParentOf(f1));
    assertFalse(new Directory(f3).isParentOf(f1));
    assertFalse(new Directory(f3).isParentOf(f4));
  }

  @Test public void testCopyTo() throws Exception {
    final Set<File> files = new HashSet<File>() {{
      add(new File("a file"));
      add(new File("directory/.afile"));
      add(new File("directory/b/c/d/e"));
    }};

    for (final File relativeFile : files) {
      final File absoluteFile =
        new File(getDirectory(), relativeFile.getPath());

      createRandomFile(absoluteFile);
    }

    final Directory sourceDirectory = new Directory(getDirectory());

    final File output = new File(getDirectory(), "output");
    final Directory outputDirectory = new Directory(output);
    outputDirectory.create();
    final File overwritten = new File(output, "should be deleted");
    createRandomFile(overwritten);

    assertTrue(overwritten.exists());

    sourceDirectory.copyTo(outputDirectory, false);

    assertFalse(overwritten.exists());

    final File[] contents =
      outputDirectory.listContents(Directory.getMatchAllFilesFilter());

    for (final File content : contents) {
      assertTrue("Original contains '" + content + "'",
                 files.contains(content));
    }

    assertEquals(files.size(), contents.length);

    sourceDirectory.copyTo(outputDirectory, true);

    final File[] contents2 =
      outputDirectory.listContents(Directory.getMatchAllFilesFilter());

    for (int i = 0; i < contents2.length; ++i) {
      if (!contents2[i].getPath().startsWith("output")) {
        assertTrue("Original contains '" + contents2[i] + "'",
                   files.contains(contents2[i]));
      }
    }

    final File[] contents3 =
      new Directory(new File("output/output"))
      .listContents(Directory.getMatchAllFilesFilter());

    for (final File element : contents3) {
      assertTrue("Original contains '" + element + "'",
                 files.contains(element));
    }

    assertEquals(files.size() * 2, contents2.length);

    final Directory missingSourceDirectory =
      new Directory(sourceDirectory.getFile(new File("missing")));

    final Directory missingOutputDirectory =
      new Directory(outputDirectory.getFile(new File("notthere")));

    assertFalse(missingSourceDirectory.getFile().exists());
    assertFalse(missingOutputDirectory.getFile().exists());

    try {
      missingSourceDirectory.copyTo(missingOutputDirectory, false);
      fail("Expected DirectoryException");
    }
    catch (final Directory.DirectoryException e) {
    }

    assertFalse(missingSourceDirectory.getFile().exists());
    assertFalse(missingOutputDirectory.getFile().exists());
  }

  @Test public void testSerialization() throws Exception {
    final Directory original = new Directory(getDirectory());

    assertEquals(original, Serializer.serialize(original));
  }

  private static File fromPath(final String... elements) {
    File result = null;

    for (final String e : elements) {
      result = new File(result, e);
    }

    return result;
  }

  private static void assertEqualPaths(
    final String expected,
    final String actual) {

    final String[] expectedElements = expected.split("/");
    final String[] actualElements = actual.split(File.separator);

    assertArrayEquals(actual + " equals " + expected,
                      expectedElements,
                      actualElements);
  }

  @Test public void testRelativePathNotChild1() throws IOException {
    final File f1 = fromPath("a", "b", "c");
    final File f2 = fromPath("a", "b", "x", "y");

    assertEqualPaths("../x/y", Directory.relativePath(f1, f2, false).getPath());
  }

  @Test public void testRelativePathNotChild2() throws IOException {
    final File f1 = fromPath("a", "b", "c");
    final File f2 = fromPath("a", "b", "x", "y");

    assertNull(Directory.relativePath(f1, f2, true));
  }

  @Test public void testRelativePathDifferentFS1() throws IOException {
    final File f1 = fromPath("/", "x", "y", "z");
    final File f2 = fromPath("/", "a", "b");

    assertNull(Directory.relativePath(f1, f2, false));
  }

  @Test public void testRelativePathDifferentFS2() throws IOException {
    final File f1 = fromPath("/", "x", "y", "z");
    final File f2 = fromPath("/", "a", "b");

    assertNull(Directory.relativePath(f1, f2, true));
  }

  @Test public void testRelativePathDot1() throws IOException {
    final File f1 = fromPath(".");
    final File f2 = fromPath(".");

    assertEqualPaths(".", Directory.relativePath(f1, f2, false).getPath());
  }

  @Test public void testRelativePathDot2() throws IOException {
    final File f1 = fromPath(".");
    final File f2 = fromPath(".");

    assertEqualPaths(".",  Directory.relativePath(f1, f2, true).getPath());
  }
}
