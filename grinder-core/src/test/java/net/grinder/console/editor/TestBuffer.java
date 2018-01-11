// Copyright (C) 2004 - 2013 Philip Aston
// Copyright (C) 2005 Martin Wagner
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;

import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.translation.Translations;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;


/**
 * Unit test for {@link BufferImplementation}.
 *
 * @author Philip Aston
 */
public class TestBuffer extends AbstractJUnit4FileTestCase {

  private static String LINE_SEPARATOR = System.getProperty("line.separator");

  @Mock private Translations m_translations;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test public void testBufferWithNoFile() throws Exception {
    final String text = "Some text for testing with";

    final TextSource textSource = new StringTextSource(text);

    assertEquals(text, textSource.getText());

    final Buffer buffer =
        new BufferImplementation(m_translations, textSource, "My Buffer");

    assertNotNull(textSource.getText());
    assertEquals(text, textSource.getText());
    assertEquals("My Buffer", buffer.getDisplayName());
    assertTrue( buffer.toString().indexOf(buffer.getDisplayName()) >= 0);

    try {
      buffer.load();
      fail("Expected EditorException");
    }
    catch (final EditorException e) {
    }

    try {
      buffer.save();
      fail("Expected EditorException");
    }
    catch (final EditorException e) {
    }

    assertTrue(!buffer.isDirty());
    assertTrue(buffer.isUpToDate());
    assertNull(buffer.getFile());
    assertEquals(textSource, buffer.getTextSource());

    assertEquals(Buffer.Type.TEXT_BUFFER, buffer.getType());

    assertTrue(!buffer.isDirty());
    assertTrue(buffer.isUpToDate());
    assertNull(buffer.getFile());
    assertEquals(textSource, buffer.getTextSource());
  }

  private static final class Expectation {
    private final Buffer.Type m_type;
    private final File m_file;

    public Expectation(final Buffer.Type type, final String filename) {
      m_type = type;
      m_file = new File(filename);
    }

    public Buffer.Type getType() {
      return m_type;
    }

    public File getFile() {
      return m_file;
    }
  }

  @Test public void testGetType() throws Exception {
    final StringTextSource textSource = new StringTextSource("");

    final Expectation[] wordsOfExpectation = {
      new Expectation(Buffer.Type.HTML_BUFFER, "somefile/blah.htm"),
      new Expectation(Buffer.Type.HTML_BUFFER, "foo.html"),
      new Expectation(Buffer.Type.JAVA_BUFFER, "eieio.java"),
      new Expectation(Buffer.Type.MSDOS_BATCH_BUFFER, "eat/my.shorts.bat"),
      new Expectation(Buffer.Type.MSDOS_BATCH_BUFFER, "alpha.cmd"),
      new Expectation(Buffer.Type.PROPERTIES_BUFFER, "essential.properties"),
      new Expectation(Buffer.Type.PYTHON_BUFFER, "why/oh.py"),
      new Expectation(Buffer.Type.SHELL_BUFFER, "bishbosh.bash"),
      new Expectation(Buffer.Type.SHELL_BUFFER, "clishclosh.csh"),
      new Expectation(Buffer.Type.SHELL_BUFFER, "kkkkrassh.ksh"),
      new Expectation(Buffer.Type.SHELL_BUFFER, "be/quiet.sh"),
      new Expectation(Buffer.Type.TEXT_BUFFER, "tick.txt"),
      new Expectation(Buffer.Type.TEXT_BUFFER, "tech.text"),
      new Expectation(Buffer.Type.XML_BUFFER, "xplicitly.xml"),
      new Expectation(Buffer.Type.TEXT_BUFFER, "blurb/blah"),
      new Expectation(Buffer.Type.TEXT_BUFFER, "fidledly.foo"),
      new Expectation(Buffer.Type.TEXT_BUFFER, "bah/bah"),
      new Expectation(Buffer.Type.TEXT_BUFFER, "...."),
    };

    for (final Expectation expectation : wordsOfExpectation) {
      final Buffer buffer =
        new BufferImplementation(
          m_translations,
          textSource,
          expectation.getFile());

      assertEquals(expectation.getType(), buffer.getType());
      assertEquals(textSource, buffer.getTextSource());
    }
  }

  @Test public void testBufferWithAssociatedFile() throws Exception {

    final String s0 =
      "A shield for your eyes\na beast in the well on your hand";

    final String s1 =
      "Catch the mean beast\nin the well in the hell on the back\n" +
      "Watch out! You've got no shield\n" +
      "Break up! He's got no peace";

    final StringTextSource textSource = new StringTextSource(s0);
    assertSame(s0, textSource.getText());

    final File file = new File(getDirectory(), "myfile.txt");

    final Buffer buffer =
        new BufferImplementation(m_translations, textSource, file);

    assertEquals(Buffer.Type.TEXT_BUFFER, buffer.getType());
    assertTrue(!buffer.isDirty());
    assertTrue(!buffer.isUpToDate());
    assertEquals(file, buffer.getFile());
    assertEquals(textSource, buffer.getTextSource());

    buffer.save();

    assertTrue(!buffer.isDirty());
    assertTrue(buffer.isUpToDate());

    assertSame(s0, textSource.getText());

    textSource.setText(s1);
    textSource.markDirty();

    assertTrue(buffer.isDirty());
    assertTrue(buffer.isUpToDate());
    assertSame(s1, textSource.getText());

    buffer.load();

    assertTrue(!buffer.isDirty());
    assertTrue(buffer.isUpToDate());
    assertEquals(canonicaliseLineEndings(s0), textSource.getText());
    assertNotSame(s0, textSource.getText());

    // Add an error margin, as Linux does not support setting the modification
    // date with millisecond precision.
    assertTrue(file.setLastModified(System.currentTimeMillis() + 1000));

    assertTrue(!buffer.isUpToDate());

    buffer.load();

    assertTrue(buffer.isUpToDate());
    assertEquals(textSource, buffer.getTextSource());
  }

  private static String canonicaliseLineEndings(final String s0) {
    return s0.replaceAll("\n", LINE_SEPARATOR) + LINE_SEPARATOR;
  }

  @Test public void testBufferWithLargeFile() throws Exception {
    final char[] chars = "0123456789abcdef".toCharArray();
    final char[] manyChars = new char[10000];

    for (int i=0; i<manyChars.length; ++i) {
      manyChars[i] = chars[i % chars.length];
    }

    final String s0 = new String(manyChars);
    final StringTextSource textSource = new StringTextSource(s0);
    assertSame(s0, textSource.getText());

    final File file = new File(getDirectory(), "myfile.txt");

    final Buffer buffer =
        new BufferImplementation(m_translations, textSource, file);

    assertEquals(Buffer.Type.TEXT_BUFFER, buffer.getType());
    assertTrue(!buffer.isDirty());
    assertTrue(!buffer.isUpToDate());
    assertEquals(file, buffer.getFile());
    assertEquals(textSource, buffer.getTextSource());

    buffer.save();

    assertTrue(!buffer.isDirty());
    assertTrue(buffer.isUpToDate());
    assertSame(s0, textSource.getText());

    buffer.load();

    assertTrue(!buffer.isDirty());
    assertTrue(buffer.isUpToDate());
    assertEquals(canonicaliseLineEndings(s0), textSource.getText());
    assertNotSame(s0, textSource.getText());
  }

  @Test public void testBufferWithBadAssociatedFile() throws Exception {

    final StringTextSource textSource = new StringTextSource("");

    final Buffer buffer =
        new BufferImplementation(m_translations, textSource, getDirectory());

    try {
      buffer.load();
      fail("Expected DisplayMessageConsoleException");
    }
    catch (final DisplayMessageConsoleException e) {
      assertTrue(e.getCause() instanceof IOException);
    }

    try {
      buffer.save();
      fail("Expected DisplayMessageConsoleException");
    }
    catch (final DisplayMessageConsoleException e) {
      assertTrue(e.getCause() instanceof IOException);
    }
  }

  private static final class ExtractReasonExpectation {
    private final IOException m_ioException;
    private final String m_reason;

    public ExtractReasonExpectation(final IOException ioException, final String reason) {
      m_ioException = ioException;
      m_reason = reason;
    }

    public IOException getIOException() {
      return m_ioException;
    }

    public String getReason() {
      return m_reason;
    }
  }

  @Test public void testExtractReasonFromIOException() throws Exception {
    final ExtractReasonExpectation[] wordsOfExpectation = {
      new ExtractReasonExpectation(new EOFException("Blah"), ""),
      new ExtractReasonExpectation(new IOException("Blah (foo)"), ""),
      new ExtractReasonExpectation(new UnknownHostException("Blah"), ""),
      new ExtractReasonExpectation(new FileNotFoundException("Blah"), ""),
      new ExtractReasonExpectation(
        new FileNotFoundException("Blah (Some info)"),
        "Some info"),
      new ExtractReasonExpectation(
        new FileNotFoundException("Blah (invalid"),
        ""),
      new ExtractReasonExpectation(
        new FileNotFoundException("Blah (a different message) (blah)"),
        "a different message"),
    };

    for (final ExtractReasonExpectation expectation : wordsOfExpectation) {
      final String reason =
        BufferImplementation.extractReasonFromIOException(expectation.getIOException());

      assertEquals(expectation.getReason(), reason);
    }
  }
}
