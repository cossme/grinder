// Copyright (C) 2001 - 2012 Philip Aston
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

package net.grinder.engine.communication;

import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.MessageDispatchRegistry.Handler;
import net.grinder.messages.agent.ResetGrinderMessage;
import net.grinder.messages.agent.StartGrinderMessage;
import net.grinder.messages.agent.StopGrinderMessage;
import net.grinder.util.thread.Condition;

import org.slf4j.Logger;


/**
 * Process console messages and allows them to be asynchronously queried.
 *
 * @author Philip Aston
 */
public final class ConsoleListener {

  /**
   * Constant that represents start message.
   * @see #received
   */
  public static final int START = 1 << 0;

  /**
   * Constant that represents a a reset message.
   * @see #received
   */
  public static final int RESET = 1 << 1;

  /**
   * Constant that represents a stop message.
   * @see #received
   */
  public static final int STOP =  1 << 2;

  /**
   * Constant that represents a communication shutdown.
   * @see #received
   */
  public static final int SHUTDOWN =  1 << 3;

  /**
   * Constant that represent any message.
   * @see #received
   */
  public static final int ANY = START | RESET | STOP | SHUTDOWN;

  private final Condition m_notifyOnMessage;
  private final Logger m_logger;
  private int m_messagesReceived = 0;
  private int m_lastMessagesReceived = 0;
  private StartGrinderMessage m_lastStartGrinderMessage;

  /**
   * Constructor.
   *
   * @param notifyOnMessage
   *          An {@code  Object} to notify when a message arrives.
   * @param logger
   *          A {@code Logger} to which received event messages are logged.
   */
  public ConsoleListener(final Condition notifyOnMessage, final Logger logger) {
    m_notifyOnMessage = notifyOnMessage;
    m_logger = logger;
  }

  /**
   * Shut down.
   */
  public void shutdown() {
    setReceived(SHUTDOWN);
  }

  /**
   * Wait until any message is received.
   *
   * <p>After calling this method, the actual messages can be
   * determined using {@link #received}.</p>
   *
   */
  public void waitForMessage() {
    while (!checkForMessage(ConsoleListener.ANY)) {
      synchronized (m_notifyOnMessage) {
        m_notifyOnMessage.waitNoInterrruptException();
      }
    }
  }

  /**
   * Check for messages matching the given mask.
   *
   * <p>After calling this method, the actual messages can be
   * determined using {@link #received}.</p>
   *
   * @param mask The messages to check for.
   * @return {@code true} if at least one message matches the
   * {@code mask} parameter has been received since the last time
   * the message was checked for, or if communications have been
   * shutdown. {@code false} otherwise.
   */
  public boolean checkForMessage(final int mask) {
    synchronized (this) {
      final int intersection = m_messagesReceived & mask;

      try {
        m_lastMessagesReceived = intersection;
      }
      finally {
        // Clear all messages except shutdown.
        m_messagesReceived &= ~intersection | SHUTDOWN;
      }
    }

    return received(mask | SHUTDOWN);
  }

  /**
   * Discard pending messages that match the given mask.
   *
   * @param mask The messages to discard.
   */
  public void discardMessages(final int mask) {
    synchronized (this) {
      m_lastMessagesReceived &= ~mask;
      m_messagesReceived &= ~mask;
    }
  }

  /**
   * Query the messages set up by the last {@link #checkForMessage} or
   * {@link #waitForMessage} call.
   *
   * @param mask The messages to check for.
   * @return {@code true} if one or more of the received
   * messages matches {@code mask}.
   */
  public synchronized boolean received(final int mask) {
    return (m_lastMessagesReceived & mask) != 0;
  }

  private void setReceived(final int message) {
    synchronized (this) {
      m_messagesReceived |= message;
    }

    synchronized (m_notifyOnMessage) {
      m_notifyOnMessage.notifyAll();
    }
  }

  /**
   * Registers message handlers with a dispatcher.
   *
   * @param messageDispatcher The dispatcher.
   */

  public void registerMessageHandlers(
    final MessageDispatchRegistry messageDispatcher) {

    messageDispatcher.set(
      StartGrinderMessage.class,
      new AbstractMessageHandler<StartGrinderMessage>() {
        @Override
        public void handle(final StartGrinderMessage message) {
          m_logger.info("received a start message");
          m_lastStartGrinderMessage = message;
          setReceived(START);
        }
      });

    messageDispatcher.set(
      StopGrinderMessage.class,
      new AbstractMessageHandler<StopGrinderMessage>() {
        @Override
        public void handle(final StopGrinderMessage message) {
          m_logger.info("received a stop message");
          setReceived(STOP);
        }
      });

    messageDispatcher.set(
      ResetGrinderMessage.class,
      new AbstractMessageHandler<ResetGrinderMessage>() {
        @Override
        public void handle(final ResetGrinderMessage message) {
          m_logger.info("received a reset message");
          setReceived(RESET);
        }
      });
  }

  /**
   * Return the last {@link StartGrinderMessage} received.
   *
   * @return The message.
   */
  public StartGrinderMessage getLastStartGrinderMessage() {
    return m_lastStartGrinderMessage;
  }

  private abstract class AbstractMessageHandler<T extends Message>
    implements Handler<T> {

    @Override
    public void shutdown() {
      final boolean shutdown;

      synchronized (ConsoleListener.this) {
        shutdown = (m_messagesReceived & SHUTDOWN) == 0;
      }

      if (shutdown) {
        m_logger.info("console connection shut down");
        setReceived(SHUTDOWN);
      }
    }
  }
}
