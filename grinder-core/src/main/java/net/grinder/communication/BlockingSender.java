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
 * Interface for classes that manage the sending of messages.
 *
 * @author Philip Aston
 */
public interface BlockingSender {

  /**
   * Send the given message and await a response.
   *
   * <p>
   * The input stream is implementation dependent. This should only be used
   * where the sender can guarantee that the input stream will be free for
   * exclusive use.
   * </p>
   *
   * @param message
   *          A {@link Message}.
   * @return The response message.
   * @throws CommunicationException
   *           If an error occurs.
   */
  Message blockingSend(Message message) throws CommunicationException;

  /**
   * Cleanly shut down the <code>Sender</code>.
   */
  void shutdown();

  /**
   * Exception indicating that the server chose to send no response back
   * to a {@link BlockingSender#blockingSend(Message)}.
   *
   * @author Philip Aston
   */
  class NoResponseException extends CommunicationException {

    NoResponseException(String s) {
      super(s);
    }
  }
}
