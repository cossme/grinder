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
 * Passive {@link Sender} class that delegates to two other {@link
 * Sender}s.
 *
 * @author Philip Aston
 */
public final class TeeSender
  implements Sender,  MessageDispatchRegistry.Handler<Message> {

  private final Sender m_delegate1;
  private final Sender m_delegate2;

  /**
   * Constructor.
   *
   * @param delegate1 The first <code>Sender</code>.
   * @param delegate2 The seconds <code>Sender</code>.
   */
  public TeeSender(Sender delegate1, Sender delegate2) {
    m_delegate1 = delegate1;
    m_delegate2 = delegate2;
  }

  /**
   * {@inheritDoc}
   */
  @Override public void send(Message message) throws CommunicationException {
    m_delegate1.send(message);
    m_delegate2.send(message);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void shutdown() {
    m_delegate1.shutdown();
    m_delegate2.shutdown();
  }

  /**
   * {@inheritDoc}
   */
  @Override public void handle(Message message) throws CommunicationException {
    send(message);
  }
}
