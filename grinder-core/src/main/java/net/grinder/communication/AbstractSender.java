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

package net.grinder.communication;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import net.grinder.common.UncheckedInterruptedException;


/**
 * Abstract class that manages the sending of messages.
 *
 * @author Philip Aston
 */
abstract class AbstractSender implements Sender {

  private volatile boolean m_shutdown = false;

  /**
   * Send the given message.
   *
   * @param message A {@link Message}.
   * @exception CommunicationException If an error occurs.
   */
  public final void send(Message message) throws CommunicationException {

    if (m_shutdown) {
      throw new CommunicationException("Shut down");
    }

    try {
      writeMessage(message);
    }
    catch (IOException e) {
      UncheckedInterruptedException.ioException(e);
      throw new CommunicationException("Exception whilst sending message", e);
    }
  }

  /**
   * Template method for subclasses to implement the sending of a
   * message.
   */
  protected abstract void writeMessage(Message message)
    throws CommunicationException, IOException;

  protected static final void writeMessageToStream(Message message,
                                                   OutputStream stream)
    throws IOException {

    // I tried the model of using a single ObjectOutputStream for the
    // lifetime of the Sender and a single ObjectInputStream for each
    // Reader. However, the corresponding ObjectInputStream would get
    // occasional EOF's during readObject. Seems like voodoo to me,
    // but creating a new ObjectOutputStream for every message fixes
    // this.

    // Dr Heinz M. Kabutz's Java Specialists 2004-05-19 newsletter
    // (http://www.javaspecialists.co.za) may hold the answer.
    // ObjectOutputStream's cache based on object identity. The EOF
    // might be due to this, or at least ObjectOutputStream.reset()
    // may help. I can't get excited enough about the cost of creating
    // a new ObjectOutputStream() to try this as the bulk of what we
    // send are long[]'s so aren't cacheable, and it would break sends
    // that reuse Messages.

    final ObjectOutputStream objectStream = new ObjectOutputStream(stream);
    objectStream.writeObject(message);
    objectStream.flush();
  }

  /**
   * Cleanly shutdown the <code>Sender</code>.
   */
  public void shutdown() {
    try {
      send(new CloseCommunicationMessage());
    }
    catch (CommunicationException e) {
      // Ignore.
    }

    // Keep track of whether we've been closed. Can't rely on delegate
    // as some implementations don't do anything with close(), e.g.
    // ByteArrayOutputStream.
    m_shutdown = true;
  }

  /**
   * Return whether we are shutdown.
   *
   * @return <code>true</code> if and only if we are shut down.
   */
  public boolean isShutdown() {
    return m_shutdown;
  }
}
