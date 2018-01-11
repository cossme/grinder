// Copyright (C) 2000 - 2012 Philip Aston
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

package net.grinder.communication;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import net.grinder.common.Closer;


/**
 * Class that manages the sending of messages to a server.
 *
 * @author Philip Aston
 **/
public class StreamSender extends AbstractSender {

  private final OutputStream m_outputStream;
  private final Object m_streamLock;

  /**
   * Constructor.
   *
   * @param outputStream The output stream to write to.
   */
  public StreamSender(OutputStream outputStream) {
    this(outputStream, outputStream);
  }

  /**
   * Constructor.
   *
   * @param outputStream The output stream to write to.
   * @param streamLock Lock on this object around all stream operations.
   */
  private StreamSender(OutputStream outputStream, Object streamLock) {
    m_outputStream = new BufferedOutputStream(outputStream);
    m_streamLock = streamLock;
  }

  /**
   * Send a message.
   *
   * @param message The message.
   * @throws IOException If an error occurs.
   */
  @Override
  protected final void writeMessage(Message message) throws IOException {
    synchronized (m_streamLock) {
      writeMessageToStream(message, m_outputStream);
    }
  }

  /**
   * Cleanly shutdown the <code>Sender</code>. Ignore most errors,
   * connection has probably been reset by peer.
   */
  @Override public void shutdown() {

    super.shutdown();

    Closer.close(m_outputStream);
  }
}

