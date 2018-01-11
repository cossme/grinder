// Copyright (C) 2000 - 2011 Philip Aston
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


/**
 * QueuedSender implementation.
 *
 * @author Philip Aston
 */
public final class QueuedSenderDecorator implements QueuedSender {

  private final Sender m_delegate;
  private final MessageQueue m_messageQueue = new MessageQueue(false);

  /**
   * Constructor.
   *
   * @param delegate Sender to decorate.
   */
  public QueuedSenderDecorator(Sender delegate) {
    m_delegate = delegate;
  }

  /**
   * Queue the given message for later sending.
   *
   * @param message A {@link Message}.
   * @throws CommunicationException If an error occurs.
   **/
  public void send(Message message) throws CommunicationException {
    m_messageQueue.queue(message);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void flush() throws CommunicationException {

    for (Message message : m_messageQueue.drainMessages()) {
      m_delegate.send(message);
    }
  }

  /**
   * Cleanly shutdown the <code>Sender</code>.
   *
   * <p>Any queued messages are discarded.</p>
   */
  public void shutdown() {
    m_messageQueue.shutdown();
    m_delegate.shutdown();
  }
}
