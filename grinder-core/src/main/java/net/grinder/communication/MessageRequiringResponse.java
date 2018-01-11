// Copyright (C) 2006 Philip Aston
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
 * Pair a {@link Message} with a way to send a response.
 *
 * <p>Package scope</p>.
 *
 * @see ClientSender#blockingSend(Message)
 * @see MessageDispatchRegistry#set(Class, BlockingHandler)
 * @author Philip Aston
 */
final class MessageRequiringResponse implements Message {

  private static final long serialVersionUID = 1L;

  private final Message m_message;

  private transient Sender m_responder;
  private transient boolean m_responseSent;

  MessageRequiringResponse(Message message) {
    m_message = message;
  }

  /**
   * Provide access to the original message.
   *
   * @return The message.
   */
  public Message getMessage() {
    return m_message;
  }

  void setResponder(Sender sender) {
    m_responder = sender;
  }

  /**
   * Send the response.
   *
   * @param message A {@link Message}.
   * @throws CommunicationException If an error occurs.
   */
  public void sendResponse(Message message) throws CommunicationException {

    if (m_responder == null) {
      throw new CommunicationException("Response sender not set");
    }

    if (m_responseSent) {
      throw new CommunicationException("One response message only");
    }

    m_responseSent = true;
    m_responder.send(message);
  }

  /**
   * Query whether the response has been sent.
   *
   * @return <code>true</code> if and only if the response has been sent.
   */
  public boolean isResponseSent() {
    return m_responseSent;
  }
}
