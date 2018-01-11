// Copyright (C) 2003 - 2011 Philip Aston
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

import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import net.grinder.common.Closer;
import net.grinder.common.UncheckedInterruptedException;


/**
 * Manages receipt of messages from a server over a stream.
 *
 * @author Philip Aston
 */
public class StreamReceiver implements Receiver {

  private final InputStream m_inputStream;
  private final Object m_streamLock;

  // Guarded by m_streamLock.
  private boolean m_shutdown = false;

  /**
   * Constructor.
   *
   * @param inputStream The input stream to read from.
   */
  public StreamReceiver(InputStream inputStream) {
    this(inputStream, inputStream);
  }

  /**
   * Constructor.
   *
   * @param inputStream The input stream to read from.
   * @param streamLock Lock on this object around all stream operations.
   */
  private StreamReceiver(InputStream inputStream, Object streamLock) {
    m_inputStream = inputStream;
    m_streamLock = streamLock;
  }

  /**
   * Block until a message is available. Typically called from a
   * message dispatch loop.
   *
   * @return The message or <code>null</code> if shut down.
   * @throws CommunicationException If an error occurred receiving a message.
   */
  public final Message waitForMessage() throws CommunicationException {

    try {
      final Message message;

      // This blocks holding the lock, by design.
      synchronized (m_streamLock) {
        if (m_shutdown) {
          return null;
        }

        final ObjectInputStream objectStream =
          new ObjectInputStream(m_inputStream);

        message = (Message)objectStream.readObject();

        if (message instanceof CloseCommunicationMessage) {
          shutdown();
          return null;
        }
      }

      return message;
    }
    catch (IOException e) {
      UncheckedInterruptedException.ioException(e);
      throw new CommunicationException("Failed to read message", e);
    }
    catch (ClassNotFoundException e) {
      throw new CommunicationException("Failed to read message", e);
    }
  }

  /**
   * Cleanly shut down the <code>Receiver</code>. Ignore errors,
   * connection has probably been reset by peer.
   */
  public void shutdown() {

    synchronized (m_streamLock) {
      m_shutdown = true;
    }

    Closer.close(m_inputStream);
  }
}
