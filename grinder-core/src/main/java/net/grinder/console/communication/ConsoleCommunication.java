// Copyright (C) 2004 - 2011 Philip Aston
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

package net.grinder.console.communication;

import net.grinder.communication.Address;
import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchRegistry;


/**
 * Handles communication for the console.
 *
 * @author Philip Aston
 */
public interface ConsoleCommunication {

  /**
   * Returns the message dispatch registry which callers can use to register new
   * message handlers.
   *
   * @return The registry.
   */
  MessageDispatchRegistry getMessageDispatchRegistry();

  /**
   * Shut down communication.
   */
  void shutdown();

  /**
   * Wait to receive a message, then process it.
   *
   * @return <code>true</code> if we processed a message successfully;
   *         <code>false</code> if we've been shut down.
   * @see #shutdown()
   */
  boolean processOneMessage();

  /**
   * Send the given message to the agent processes (which may pass it on to
   * their workers).
   *
   * @param message The message to send.
   */
  void sendToAgents(Message message);

  /**
   * Send the given message to the given agent processes (which may pass it on
   * to its workers).
   *
   * @param address
   *            The address to which the message should be sent.
   * @param message
   *            The message to send.
   */
  void sendToAddressedAgents(Address address, Message message);
}
