// Copyright (C) 2012 Philip Aston
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

package net.grinder.util.logback;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.status.ErrorStatus;


/**
 * A version of {@link EchoEncoder} that logs only the message and is buffered.
 *
 * @author Philip Aston
 */
public final class BufferedEchoMessageEncoder
  extends EchoEncoder<ILoggingEvent> {

  private static final byte[] LINE_SEPARATOR_BYTES =
    CoreConstants.LINE_SEPARATOR.getBytes();

  private int m_bufferSize = 65536;

  /**
   * {@inheritDoc}
   */
  @Override
  public void init(OutputStream os) throws IOException {
    super.init(new BufferedOutputStream(os, getBufferSize()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void doEncode(ILoggingEvent event) throws IOException {
    outputStream.write(event.getMessage().getBytes());
    outputStream.write(LINE_SEPARATOR_BYTES);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException {
    super.close();

    try {
      outputStream.flush();
    }
    catch (IOException e) {
      addStatus(new ErrorStatus("Failed to flush buffer", this, e));
    }
  }

  /**
   * Get the buffer size.
   *
   * @return The buffer size, in bytes.
   */
  public int getBufferSize() {
    return m_bufferSize;
  }

  /**
   * Set the buffer size.
   *
   * @param bufferSize
   *          The buffer size, in bytes.
   */
  public void setBufferSize(int bufferSize) {
    if (outputStream != null) {
      throw new IllegalStateException("Already initialised");
    }

    this.m_bufferSize = bufferSize;
  }
}
