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

package net.grinder.console.editor;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.editor.BufferImplementation.Listener;


/**
 * Buffer state.
 *
 * @author Philip Aston
 */
public interface Buffer {

  /**
   * Buffer type.
   */
  enum Type {

    /** Buffer type constant. */
    JAVA_BUFFER("Java", "text/java", "java"),

    /** Buffer type constant. */
    CLOJURE_BUFFER("Clojure", "text/clojure", "clj"),

    /** Buffer type constant. */
    PYTHON_BUFFER("Python", "text/python", "py"),

    /** Buffer type constant. */
    SHELL_BUFFER("Shell", "text/bash", "sh", "bash", "csh", "ksh"),

    /** Buffer type constant. */
    HTML_BUFFER("HTML", "text/html", "html", "htm"),

    /** Buffer type constant. */
    MSDOS_BATCH_BUFFER("MSDOS Batch", "text/dosbatch", "bat", "cmd"),

    /** Buffer type constant. */
    XML_BUFFER("XML", "text/xml", "xml"),

    /** Buffer type constant. */
    PROPERTIES_BUFFER("Unknown", "text/properties", "properties"),

    /** Buffer type constant. */
    TEXT_BUFFER("Unknown", "text/text", "text", "txt");

    private final String m_name;
    private final String m_contentType;
    private Set<String> m_extensions;

    private Type(String name, String contentType, String... extensions) {
      m_name = name;
      m_contentType = contentType;
      m_extensions = new HashSet<String>(asList(extensions));
    }

    public String getName() {
      return m_name;
    }

    public String getContentType() {
      return m_contentType;
    }

    public static Type forExtension(String extension) {
      for (Type t : Type.values()) {
        if (t.m_extensions.contains(extension)) {
          return t;
        }
      }

      return TEXT_BUFFER;
    }
  }


  /**
   * Return the buffer's {@link TextSource}.
   *
   * @return The text source.
   */
  TextSource getTextSource();

  /**
   * Update the text source from the file.
   *
   * @exception DisplayMessageConsoleException If the file could not
   * be read from.
   * @exception EditorException If an unexpected problem occurs.
   */
  void load() throws DisplayMessageConsoleException, EditorException;

  /**
   * Update the buffer's file from the text source.
   *
   * @exception DisplayMessageConsoleException If the file could not
   * be written to.
   * @exception EditorException If an unexpected problem occurs.
   */
  void save() throws DisplayMessageConsoleException, EditorException;

  /**
   * Update a file from the text source and, if successful, associate
   * the buffer with the new file.
   *
   * @param file The file.
   * @exception DisplayMessageConsoleException If the file could not
   * be written to.
   */
  void save(File file) throws DisplayMessageConsoleException;

  /**
   * Return whether the buffer's text has been changed since the last
   * save.
   *
   * @return <code>true</code> => the text has changed.
   */
  boolean isDirty();

  /**
   * Return the buffer's associated file.
   *
   * @return The file. <code>null</code> if there is no associated file.
   */
  File getFile();

  /**
   * Return whether the file has been independently modified since the
   * last save.
   *
   * @return <code>true</code> => the file has changed independently
   * of the buffer.
   */
  boolean isUpToDate();

  /**
   * Get the type of the buffer.
   *
   * @return The buffer's type.
   */
  Type getType();

  /**
   * Return display name of buffer.
   *
   * @return The buffer's name.
   */
  String getDisplayName();

  /**
   * Useful for debugging.
   *
   * @return Description of the Buffer.
   */
  String toString();

  /**
   * Add a new listener.
   *
   * @param listener The listener.
   */
  void addListener(Listener listener);
}
