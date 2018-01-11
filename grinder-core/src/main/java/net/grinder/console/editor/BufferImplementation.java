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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.EventListener;

import net.grinder.common.Closer;
import net.grinder.common.UncheckedInterruptedException;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.translation.Translations;
import net.grinder.util.ListenerSupport;


/**
 * Implementation of {@link Buffer}.
 *
 * @author Philip Aston
 */
final class BufferImplementation implements Buffer {

  private final Translations m_translations;
  private final TextSource m_textSource;

  private final ListenerSupport<Listener> m_listeners =
    new ListenerSupport<Listener>();

  private String m_name;
  private File m_file;

  private long m_lastModified = -1;

  /**
   * Constructor for buffers with no associated file.
   *
   * @param translations Translation service.
   * @param textSource The text editor.
   */
  BufferImplementation(final Translations translations,
                       final TextSource textSource,
                       final String name) {
    m_translations = translations;
    m_textSource = textSource;
    m_file = null;
    m_name = name;
  }

  /**
   * Constructor for buffers with an associated file.
   *
   * @param resources Console resources.
   * @param textSource The text editor.
   * @param file The file.
   */
  BufferImplementation(
    final Translations translations,
    final TextSource textSource,
    final File file) {

    m_translations = translations;
    m_textSource = textSource;
    setFile(file);
  }

  /**
   * Return the buffer's {@link TextSource}.
   *
   * @return The text source.
   */
  @Override
  public TextSource getTextSource() {
    return m_textSource;
  }

  /**
   * Update the text source from the file.
   *
   * @exception DisplayMessageConsoleException If the file could not
   * be read from.
   * @exception EditorException If an unexpected problem occurs.
   */
  @Override
  public void load() throws DisplayMessageConsoleException, EditorException {
    // Should never be called if there is no associated file, but
    // check anyway.
    if (m_file == null) {
      throw new EditorException(
        "Can't load a buffer that has no associated file");
    }

    final StringWriter stringWriter = new StringWriter();
    BufferedReader reader = null;

    try {
      // We use a BufferedReader to canonicalise line endings
      reader = new BufferedReader(new FileReader(m_file));

      while (true) {
        final String line = reader.readLine();

        if (line == null) {
          break;
        }

        stringWriter.write(line);
        stringWriter.write('\n');
      }
    }
    catch (final IOException e) {
      UncheckedInterruptedException.ioException(e);

      throw new DisplayMessageConsoleException(
        m_translations.translate("console.phrase/file-read-error",
          m_file,
          ".\n(" + extractReasonFromIOException(e) + ")"),
        e);
    }
    finally {
      Closer.close(reader);
    }

    m_textSource.setText(stringWriter.toString());

    m_lastModified = m_file.lastModified();
  }

  /**
   * Update the buffer's file from the text source.
   *
   * @exception DisplayMessageConsoleException If the file could not
   * be written to.
   * @exception EditorException If an unexpected problem occurs.
   */
  @Override
  public void save() throws DisplayMessageConsoleException, EditorException {
    // The UI should never call save if there is no associated file,
    // but check anyway.
    if (m_file == null) {
      throw new EditorException(
        "Can't save a buffer that has no associated file");
    }

    save(m_file);
  }

  /**
   * Update a file from the text source and, if successful, associate
   * the buffer with the new file.
   *
   * @param file The file.
   * @exception DisplayMessageConsoleException If the file could not
   * be written to.
   */
  @Override
  public void save(final File file) throws DisplayMessageConsoleException {
    final File oldFile = getFile();

    Writer fileWriter = null;

    try {
      // Calling getText() causes the text source to be set to "clean"
      // and a buffer changed event to be fired by the EditorModel.
      final String text = m_textSource.getText();

      // Line-oriented output using the platform line ending. In the future,
      // we may try to preserve the predominant line ending of the original
      // input.
      final String[] lines = text.split("\n", -1);

      fileWriter = new FileWriter(file);
      final PrintWriter printWriter = new PrintWriter(fileWriter);

      for (final String line : lines) {
        printWriter.println(line);
      }

      setFile(file);
      printWriter.close();      // Close necessary to ensure last
                                // modified time is updated?
      m_lastModified = m_file.lastModified();

      m_listeners.apply(
        new ListenerSupport.Informer<Listener>() {
          @Override
          public void inform(final Listener l) {
            l.bufferSaved(BufferImplementation.this, oldFile);
          }
        });
    }
    catch (final IOException e) {
      UncheckedInterruptedException.ioException(e);

      throw new DisplayMessageConsoleException(
        m_translations.translate("console.phrase/file-write-error",
          m_file,
          ".\n(" + extractReasonFromIOException(e) + ")"),
        e);
    }
    finally {
      Closer.close(fileWriter);
    }
  }

  /**
   * Return whether the buffer's text has been changed since the last
   * save.
   *
   * @return <code>true</code> => the text has changed.
   */
  @Override
  public boolean isDirty() {
    return m_textSource.isDirty();
  }

  private void setFile(final File file) {
    m_file = file;
    m_name = file.getName();
  }

  /**
   * Return the buffer's associated file.
   *
   * @return The file. <code>null</code> if there is no associated file.
   */
  @Override
  public File getFile() {
    return m_file;
  }

  /**
   * Return whether the file has been independently modified since the
   * last save.
   *
   * @return <code>true</code> => the file has changed independently
   * of the buffer.
   */
  @Override
  public boolean isUpToDate() {
    return m_file == null || m_lastModified == m_file.lastModified();
  }

  /**
   * Get the type of the buffer.
   *
   * @return The buffer's type.
   */
  @Override
  public Type getType() {

    if (m_file != null) {
      final String name = m_file.getName();
      final int lastDot = name.lastIndexOf('.');

      if (lastDot >= 0) {
        final String extension = name.substring(lastDot + 1);
        return Type.forExtension(extension);
      }
    }

    return Type.TEXT_BUFFER;
  }

  /**
   * Return display name of buffer.
   *
   * @return The buffer's name.
   */
  @Override
  public String getDisplayName() {
    return m_name;
  }

    /**
     * Useful for debugging.
     *
     * @return Description of the Buffer.
     */
  @Override public String toString() {
    return "<Buffer " + hashCode() + " '" + getDisplayName() + "'>";
  }

  /**
   * Add a new listener.
   *
   * @param listener The listener.
   */
  @Override
  public void addListener(final Listener listener) {
    m_listeners.add(listener);
  }

  /**
   * Interface for listeners.
   */
  public interface Listener extends EventListener {

    /**
     * Called when a buffer has been saved.
     *
     * @param buffer The buffer.
     * @param oldFile The File the buffer was previously associated with.
     */
    void bufferSaved(Buffer buffer, File oldFile);
  }

  /**
   * Protected for unit tests.
   */
  static String extractReasonFromIOException(final IOException e) {
    if (e instanceof FileNotFoundException) {
      final String message = e.getMessage();

      final int firstParenthesis = message.indexOf('(');
      final int secondsParenthesis = message.indexOf(')', firstParenthesis);

      if (firstParenthesis >= 0 && secondsParenthesis > firstParenthesis + 1) {
        return message.substring(firstParenthesis + 1, secondsParenthesis);
      }
    }

    return "";
  }
}
