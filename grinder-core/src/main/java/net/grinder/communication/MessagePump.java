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

import java.util.concurrent.ExecutorService;

import net.grinder.util.thread.ExecutorFactory;
import net.grinder.util.thread.InterruptibleRunnable;
import net.grinder.util.thread.InterruptibleRunnableAdapter;


/**
 * Active object that copies messages from a {@link Receiver} to a
 * {@link Sender}.
 *
 * @author Philip Aston
 */
public final class MessagePump {

  private final ExecutorService m_executor;
  private final Receiver m_receiver;
  private final Sender m_sender;
  private final int m_numberOfThreads;

  private volatile boolean m_shutdownTriggered = false;

  /**
     * Constructor.
     *
     * @param receiver Receiver to read messages from.
     * @param sender Sender to send messages to.
     * @param numberOfThreads Number of worker threads to use. Order
     * is not guaranteed if more than one thread is used.
     */
  public MessagePump(Receiver receiver, Sender sender, int numberOfThreads) {

    m_receiver = receiver;
    m_sender = sender;
    m_numberOfThreads = numberOfThreads;

    m_executor = ExecutorFactory.createThreadPool("Message pump",
                                                  numberOfThreads);
  }

  /**
   * Start the pump.
   */
  public void start() {
    for (int i = 0; i < m_numberOfThreads; ++i) {
      m_executor.submit(
        new InterruptibleRunnableAdapter(new MessagePumpRunnable()));
    }
  }

  /**
   * Shut down the MessagePump.
   *
   */
  public void shutdown() {

    if (!m_shutdownTriggered) {
      // Guard against repeat invocations due to a shutdown action
      // triggering a CommunicationException.
      m_shutdownTriggered = true;

      m_receiver.shutdown();
      m_sender.shutdown();

      // Now wait for the thread pool to finish.
      m_executor.shutdownNow();
    }
  }

  private class MessagePumpRunnable implements InterruptibleRunnable {
    public void interruptibleRun() {
      try {
        while (!m_executor.isShutdown()) {
          final Message message = m_receiver.waitForMessage();

          if (message == null) {
            shutdown();
          }
          else {
            m_sender.send(message);
          }
        }
      }
      catch (CommunicationException e) {
        // Shutting down.
      }
      finally {
        shutdown();
      }
    }
  }
}
