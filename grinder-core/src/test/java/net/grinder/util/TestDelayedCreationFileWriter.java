// Copyright (C) 2005 - 2012 Philip Aston
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import net.grinder.testutility.AbstractFileTestCase;


/**
 * Unit tests for {@link DelayedCreationFileWriter}.
 *
 * @author Philip Aston
 */
public final class TestDelayedCreationFileWriter extends AbstractFileTestCase {

  public void testConstructor() throws Exception {
    final File file = new File(getDirectory(), "blah");

    final Writer w0 = new DelayedCreationFileWriter(file, false);
    assertFalse(file.exists());

    createRandomFile(file);
    assertTrue(file.exists());

    final Writer w1 = new DelayedCreationFileWriter(file, true);
    assertTrue(file.exists());

    // Creating with append=false deletes existing file.
    final Writer w2 = new DelayedCreationFileWriter(file, false);
    assertFalse(file.exists());

    w0.close();
    w1.close();
    w2.close();
    assertFalse(file.exists());
  }

  public void testWriteAndFlush() throws Exception {

    final File file = new File(getDirectory(), "blah");

    final Writer w0 = new DelayedCreationFileWriter(file, false);
    w0.flush();
    assertFalse(file.exists());

    final String string0 = "Those evil chemicals.";
    final String string1 = "Egg & Chips";

    w0.write(string0);
    assertTrue(file.exists());

    w0.write(string1);

    w0.flush();
    w0.close();

    final String string2 = "The light at the end of the tunnel";
    final Writer w1 = new DelayedCreationFileWriter(file, true);
    w1.write(string2);
    w1.close();

    final Reader reader = new FileReader(file);
    final char[] chars = new char[100];
    final int n = reader.read(chars);
    assertEquals(string0 + string1 + string2, new String(chars, 0, n));
    reader.close();

    final File brokenFile = new File(file, "blah");
    final Writer w2 = new DelayedCreationFileWriter(brokenFile, false);

    try {
      w2.write("");
      fail("Expected IOException");
    }
    catch (IOException e) {
    }

    w2.close();
  }
}
