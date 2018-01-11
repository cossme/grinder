// Copyright (C) 2008 - 2012 Philip Aston
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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;

import org.junit.Test;


/**
 * Unit tests for {@link Closer}.
 *
 * @author Philip Aston
 */
public class TestCloser {
  private final IOException[] m_ioexception = new IOException[1];

  @Test public void testCloseReader() throws Exception {
    Closer.close((Reader)null);

    final Reader reader = new Reader() {
      public void close() throws IOException {
        TestCloser.this.close();
      }

      public int read(char[] cbuf, int off, int len) {
        return 0;
      }
    };

    Closer.close(reader);

    m_ioexception[0] = new IOException();
    Closer.close(reader);

    m_ioexception[0] = new InterruptedIOException();

    try {
      Closer.close(reader);
      fail("Expected UncheckedInterruptedException");
    }
    catch (UncheckedInterruptedException e) {
      assertSame(m_ioexception[0], e.getCause());
    }
  }

  @Test public void testCloseWriter() throws Exception {
    Closer.close((Writer)null);

    final Writer writer = new Writer() {
      public void close() throws IOException {
        TestCloser.this.close();
      }

      public void flush() throws IOException {
      }

      public void write(char[] cbuf, int off, int len) throws IOException {
      }
    };

    Closer.close(writer);

    m_ioexception[0] = new IOException();
    Closer.close(writer);

    m_ioexception[0] = new InterruptedIOException();

    try {
      Closer.close(writer);
      fail("Expected UncheckedInterruptedException");
    }
    catch (UncheckedInterruptedException e) {
      assertSame(m_ioexception[0], e.getCause());
    }
  }

  @Test public void testCloseInputStream() throws Exception {
    Closer.close((InputStream)null);

    final InputStream in = new InputStream() {
      public void close() throws IOException {
        TestCloser.this.close();
      }

      public int read() throws IOException {
        return 0;
      }
    };

    Closer.close(in);

    m_ioexception[0] = new IOException();
    Closer.close(in);

    m_ioexception[0] = new InterruptedIOException();

    try {
      Closer.close(in);
      fail("Expected UncheckedInterruptedException");
    }
    catch (UncheckedInterruptedException e) {
      assertSame(m_ioexception[0], e.getCause());
    }
  }

  @Test public void testCloseOutputStream() throws Exception {
    Closer.close((OutputStream)null);

    final OutputStream outputStream = new OutputStream() {
      public void close() throws IOException {
        TestCloser.this.close();
      }

      public void flush() throws IOException {
      }

      public void write(int b) throws IOException {
      }
    };

    Closer.close(outputStream);

    m_ioexception[0] = new IOException();
    Closer.close(outputStream);

    m_ioexception[0] = new InterruptedIOException();

    try {
      Closer.close(outputStream);
      fail("Expected UncheckedInterruptedException");
    }
    catch (UncheckedInterruptedException e) {
      assertSame(m_ioexception[0], e.getCause());
    }
  }

  @Test public void testCloser() throws Exception {
    Closer.close((Socket)null);

    final Socket socket = new Socket() {
      public synchronized void close() throws IOException {
        TestCloser.this.close();
      }
    };

    Closer.close(socket);

    m_ioexception[0] = new IOException();
    Closer.close(socket);

    m_ioexception[0] = new InterruptedIOException();

    try {
      Closer.close(socket);
      fail("Expected UncheckedInterruptedException");
    }
    catch (UncheckedInterruptedException e) {
      assertSame(m_ioexception[0], e.getCause());
    }
  }

  private void close() throws IOException {
    if (m_ioexception[0] != null) {
      throw m_ioexception[0];
    }
  }

  @Test(expected=UnsupportedOperationException.class)
  public void coverConstructor() throws Exception {
    new Closer();
  }
}
