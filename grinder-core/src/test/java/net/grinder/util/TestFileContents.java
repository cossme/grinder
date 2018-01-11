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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Random;

import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.Serializer;

import org.junit.Test;

/**
 * Unit test case for {@link FileContents}.
 *
 * @author Philip Aston
 */
public class TestFileContents extends AbstractJUnit4FileTestCase {
  private static Random s_random = new Random();

  @Test
  public void testConstruction() throws Exception {

    final String[] files = {
                            "file1",
                            "another/file",
    };

    for (final String file : files) {
      final File relativePath = new File(file);
      final File fullPath = new File(getDirectory(), relativePath.getPath());

      fullPath.getParentFile().mkdirs();
      final OutputStream outputStream = new FileOutputStream(fullPath);
      final byte[] bytes = new byte[500];
      s_random.nextBytes(bytes);
      outputStream.write(bytes);
      outputStream.close();

      final FileContents fileContents =
          new FileContents(getDirectory(), relativePath);

      assertEquals(relativePath, fileContents.getFilename());
      assertArrayEquals(bytes, fileContents.getContents());

      final FileContents fileContents2 = Serializer.serialize(fileContents);

      assertEquals(relativePath, fileContents2.getFilename());
      assertArrayEquals(bytes, fileContents2.getContents());

      final String s = fileContents.toString();
      assertTrue(s.indexOf(relativePath.getPath()) >= 0);
      assertTrue(s.indexOf(Integer.toString(bytes.length)) >= 0);
    }
  }

  @Test
  public void testBadConstruction() throws Exception {

    try {
      new FileContents(getDirectory(), getDirectory());
      fail("Expected FileContentsException");
    }
    catch (final FileContents.FileContentsException e) {
    }

    try {
      new FileContents(new File("non existing"), new File("file"));
      fail("Expected FileContentsException");
    }
    catch (final FileContents.FileContentsException e) {
    }
  }

  @Test
  public void testCreate() throws Exception {

    final String[] files = { "file1", "another/file", };

    for (final String file : files) {
      final File relativePath = new File(file);
      final File fullPath = new File(getDirectory(), relativePath.getPath());

      createRandomFile(fullPath);

      final FileContents fileContents =
          new FileContents(getDirectory(), relativePath);

      final File outputDirectory = new File(getDirectory(), "output");
      outputDirectory.mkdir();

      fileContents.create(new Directory(outputDirectory));

      final FileContents fileContents2 =
          new FileContents(outputDirectory, relativePath);

      assertEquals(fileContents.getFilename(), fileContents2.getFilename());
      assertArrayEquals(fileContents.getContents(),
        fileContents2.getContents());
    }
  }
}
