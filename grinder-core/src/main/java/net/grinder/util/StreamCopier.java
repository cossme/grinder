// Copyright (C) 2000 Phil Dawes
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

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import net.grinder.common.Closer;
import net.grinder.common.UncheckedInterruptedException;
import net.grinder.util.thread.InterruptibleRunnable;
import net.grinder.util.thread.InterruptibleRunnableAdapter;


/**
 * Class that copies from <code>InputStream</code>s to
 * <code>OutputStream</code>s. Can be used in conjunction with an
 * active thread - see {@link #getInterruptibleRunnable}.
 *
 * @author Philip Aston
 */
public class StreamCopier {

  private final byte[] m_buffer;
  private final boolean m_closeStreams;

  /**
   * Constructor.
   *
   * @param bufferSize The buffer size.
   * @param closeStreams <code>true</code> => ensure the streams are
   * closed after copying.
   */
  public StreamCopier(int bufferSize, boolean closeStreams) {
    m_buffer = new byte[bufferSize];
    m_closeStreams = closeStreams;
  }

  /**
   * Copies from the input stream to the output stream until the input
   * stream is empty or one of the streams reports an error.
   *
   * <p>Not thread safe - use multiple <code>StreamCopier</code>
   * instances instead.
   *
   * @param in Input stream.
   * @param out Output stream.
   * @throws IOException If an IO problem occurred during the copy.
   */
  public void copy(InputStream in, OutputStream out) throws IOException {

    try {
      while (true) {
        final int bytesRead = in.read(m_buffer, 0, m_buffer.length);

        if (bytesRead ==  -1) {
          break;
        }

        out.write(m_buffer, 0, bytesRead);
        out.flush();
      }
    }
    finally {
      if (m_closeStreams) {
        Closer.close(out);
        Closer.close(in);
      }
    }
  }

  /**
   * Creates a <code>InterruptibleRunnable</code> that can be used to copy a
   * stream with an active Thread.
   *
   * <p>
   * Any exceptions that occur during processing are simply discarded.
   *
   * @param in
   *          Input stream.
   * @param out
   *          Output stream.
   * @return The <code>InterruptibleRunnable</code>.
   */
  public InterruptibleRunnable getInterruptibleRunnable(
    final InputStream in,
    final OutputStream out) {

    return new InterruptibleRunnable() {
        public void interruptibleRun() {
          try {
            copy(in, out);
          }
          catch (IOException e) {
            // Be silent about IOExceptions.
            UncheckedInterruptedException.ioException(e);
          }
        }
      };
  }

  /**
   * Convenience version of
   * {@link #getInterruptibleRunnable(InputStream, OutputStream)} that returns a
   * {@link Runnable}.
   *
   * @param in
   *          Input stream.
   * @param out
   *          Output stream.
   * @return The <code>InterruptibleRunnable</code>.
   */
  public Runnable getRunnable(InputStream in, OutputStream out) {
    return new InterruptibleRunnableAdapter(getInterruptibleRunnable(in, out));
  }
}
