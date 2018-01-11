// Copyright (C) 2000 - 2008 Philip Aston
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;


/**
 * FileWriter that doesn't create a file until a write occurs.
 *
 * <p>
 * <strong>Unsynchronised</strong>. In practice, each
 * <code>DelayedCreationFileWriter</code> is wrapped in a
 * {@link java.io.PrintWriter} that provides adequate synchronisation.
 * </p>
 *
 * @author Philip Aston
 */
public final class DelayedCreationFileWriter extends Writer {

  private final File m_file;
  private final boolean m_append;

  private Writer m_delegate = null;

  /**
   * Constructor.
   *
   * @param file The file.
   * @param append Whether to append to the file, or overwrite.
   */
  public DelayedCreationFileWriter(File file, boolean append) {
    m_file = file;
    m_append = append;

    if (!append) {
      // Delete the old file. Well it would get trashed anyway
      // if you used a standard FileWriter, so stop
      // complaining, OK?
      m_file.delete();
    }
  }

  /**
   * Close the file.
   *
   * @exception IOException If an error occurs.
   */
  public void close() throws IOException {
    if (m_delegate == null) {
      return;
    }

    m_delegate.close();
  }

  /**
   * Flush the file.
   *
   * @exception IOException If an error occurs.
   */
  public void flush() throws IOException {
    if (m_delegate == null) {
      return;
    }

    m_delegate.flush();
  }

  /**
   * Write many bytes to the file.
   *
   * @param bytes Byte array.
   * @param offset Offset into <code>bytes</code>.
   * @param length How many bytes to write.
   * @exception IOException If an error occurs.
   */
  public void write(char[] bytes, int offset, int length) throws IOException {
    if (m_delegate == null) {
      m_delegate = new FileWriter(m_file.getPath(), m_append);
    }

    m_delegate.write(bytes, offset, length);
  }
}
