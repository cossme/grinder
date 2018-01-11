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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import net.grinder.common.UncheckedInterruptedException;


/**
 * Thread-safe queue of {@link Message}s.
 *
 * @author Philip Aston
 */
final class MessageQueue {

  private final BlockingQueue<Serializable> m_queue =
    new LinkedBlockingQueue<Serializable>();

  private final boolean m_passExceptions;

  private volatile boolean m_shutdown;

  private static final Serializable SHUTDOWN_MESSAGE = new Serializable() { };

  /**
   * Creates a new {@code MessageQueue} instance.
   *
   * @param passExceptions
   *          {@code true} => allow exceptions to be inserted into the queue and
   *          re-thrown to callers of {@link #dequeue}.
   */
  public MessageQueue(boolean passExceptions) {
    m_passExceptions = passExceptions;
  }

  /**
   * Queue the given message.
   *
   * @param message
   *          A {@link Message}.
   * @throws ShutdownException
   *           If the queue has been shut down.
   * @see #shutdown
   */
  public void queue(Message message) throws ShutdownException {

    checkIfShutdown();

    m_queue.add(message);
  }

  /**
   * Queue the given exception.
   *
   * @param exception
   *          An exception.
   * @throws AssertionError
   *           If the queue does not allow exceptions to be propagated..
   * @throws ShutdownException
   *           If the queue has been shut down.
   * @see #shutdown
   */
  public void queue(Exception exception) throws ShutdownException {

    if (!m_passExceptions) {
      throw new AssertionError(
        "This MessageQueue does not allow Exceptions to be queued");
    }

    checkIfShutdown();

    m_queue.add(exception);
  }

  /**
   * Dequeue a message.
   *
   * @param block
   *          {@code true} => block until message is available, {@code false}
   *          return {@code null} if no message is available.
   * @throws CommunicationException
   *           If the queue allows exceptions to be propagated, queued
   *           CommunicationExceptions are re-thrown to callers of this method.
   * @throws ShutdownException
   *           If the queue has been shut down.
   * @see #shutdown
   */
  public Message dequeue(boolean block) throws CommunicationException {

    checkIfShutdown();

    final Serializable result;

    if (block) {
      try {
        result = m_queue.take();
      }
      catch (InterruptedException e) {
        throw new UncheckedInterruptedException(e);
      }
    }
    else {
      result = m_queue.poll();
    }

    if (result == SHUTDOWN_MESSAGE) {
      // Handle race.
      shutdown(); // Enqueue another shut down message.
      checkIfShutdown();
    }

    if (m_passExceptions && result instanceof Exception) {
      final Exception e = (Exception) result;
      throw new CommunicationException(e.getMessage(), e);
    }

    return (Message) result;
  }

  /**
   * Shut down the <code>MessageQueue</code>. Any {@link Message}s in
   * the queue are discarded.
   */
  public void shutdown() {

    m_shutdown = true;
    m_queue.clear();

    m_queue.offer(SHUTDOWN_MESSAGE);
  }

  /**
   * Throw a ShutdownException if we are shut down.
   *
   * @throws ShutdownException
   *           Thrown if the {@code MessageQueue} is shut down.
   */
  public void checkIfShutdown() throws ShutdownException {

    if (m_shutdown) {
      throw new ShutdownException("ThreadSafeQueue shutdown");
    }
  }

  /**
   * Drain and return the messages in the queue.
   *
   * @return The messages.
   * @throws ShutdownException
   *           If the queue has been shut down.
   * @see #shutdown
   */
  public List<Message> drainMessages() throws ShutdownException {

    checkIfShutdown();

    final List<Serializable> contents =
      new ArrayList<Serializable>(m_queue.size());

    m_queue.drainTo(contents);

    final List<Message> result = new ArrayList<Message>(contents.size());

    for (Serializable c : contents) {
      if (c == SHUTDOWN_MESSAGE) {
        // Handle race.
        shutdown(); // Enqueue another shut down message.
        checkIfShutdown();
      }

      if (c instanceof Message) {
        result.add((Message)c);
      }
    }

    return result;
  }

  /**
   * Exception that indicates {@code MessageQueue} has been shut down.
   */
  public static final class ShutdownException extends CommunicationException {
    ShutdownException(String s) {
      super(s);
    }
  }
}
