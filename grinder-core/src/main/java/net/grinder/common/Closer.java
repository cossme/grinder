// Copyright (C) 2008 Philip Aston
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

package net.grinder.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;


/**
 * Static utility methods to close resource handles.
 *
 * <p>
 * I considered a template method approach, but that's equally ugly.
 * </p>
 *
 * <p>
 * The methods are intended to be called from finally blocks, and so handle all
 * exceptions quietly; with the exception of {@link InterruptedException}s
 * which are always converted to {@link UncheckedInterruptedException}s.
 *
 * @author Philip Aston
 */
public final class Closer {

  /** Disabled constructor. Package scope for unit tests. */
  Closer() {
    throw new UnsupportedOperationException();
  }

  /**
   * Close the resource.
   *
   * @param reader The resource to close.
   */
  public static void close(Reader reader) {
    if (reader != null) {
      try {
        reader.close();
      }
      catch (IOException e) {
        UncheckedInterruptedException.ioException(e);
      }
    }
  }

  /**
   * Close the resource.
   *
   * @param writer The resource to close.
   */
  public static void close(Writer writer) {
    if (writer != null) {
      try {
        writer.close();
      }
      catch (IOException e) {
        UncheckedInterruptedException.ioException(e);
      }
    }
  }

  /**
   * Close the resource.
   *
   * @param inputStream The resource to close.
   */
  public static void close(InputStream inputStream) {
    if (inputStream != null) {
      try {
        inputStream.close();
      }
      catch (IOException e) {
        UncheckedInterruptedException.ioException(e);
      }
    }
  }

  /**
   * Close the resource.
   *
   * @param outputStream The resource to close.
   */
  public static void close(OutputStream outputStream) {
    if (outputStream != null) {
      try {
        outputStream.close();
      }
      catch (IOException e) {
        UncheckedInterruptedException.ioException(e);
      }
    }
  }

  /**
   * Close the resource.
   *
   * @param socket The resource to close.
   */
  public static void close(Socket socket) {
    if (socket != null) {
      try {
        socket.close();
      }
      catch (IOException e) {
        UncheckedInterruptedException.ioException(e);
      }
    }
  }
}
