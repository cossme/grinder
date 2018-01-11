// Copyright (C) 2000 - 2012 Philip Aston
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 *  Abstract unit test cases for {@link Sender} and
 *  {@link Receiver} implementations.
 *
 * @author Philip Aston
 */
public abstract class AbstractSenderAndReceiverTests {

  protected volatile Receiver m_receiver;
  protected Sender m_sender;

  private ExecuteThread m_executeThread;

  protected void initialise(Receiver receiver, Sender sender) {
    m_receiver = receiver;
    m_sender = sender;
  }

  @Before public void startExecuteThread() throws Exception {
    m_executeThread = new ExecuteThread();
    m_executeThread.start();
  }

  @After public void stopThreads() throws Exception {
    m_executeThread.shutdown();

    m_receiver.shutdown();
    m_sender.shutdown();
  }

  @Test public void testSendSimpleMessage() throws Exception {

    final SimpleMessage sentMessage = new SimpleMessage();
    m_sender.send(sentMessage);

    final Message receivedMessage = m_executeThread.waitForMessage();
    assertEquals(sentMessage, receivedMessage);
    assertTrue(sentMessage != receivedMessage);
  }

  @Test public void testSendManyMessages() throws Exception {

    for (int i=1; i<=10; ++i) {
      final SimpleMessage[] sentMessages = new SimpleMessage[i];

      for (int j=0; j<i; ++j) {
        sentMessages[j] = new SimpleMessage(i);
        m_sender.send(sentMessages[j]);
      }

      for (int j=0; j<i; ++j) {
        final SimpleMessage receivedMessage =
          (SimpleMessage) m_executeThread.waitForMessage();

        assertEquals(sentMessages[j], receivedMessage);
        assertTrue(sentMessages[j] != receivedMessage);
      }
    }
  }

  @Test public void testSendLargeMessage() throws Exception {
    // This causes a message size of about 38K. Should be limited by
    // the buffer size in Receiver.
    final SimpleMessage sentMessage = new SimpleMessage(8000);
    m_sender.send(sentMessage);

    final SimpleMessage receivedMessage =
      (SimpleMessage) m_executeThread.waitForMessage();

    assertEquals(sentMessage, receivedMessage);
    assertTrue(sentMessage != receivedMessage);
  }

  @Test public void testShutdownReceiver() throws Exception {
    m_receiver.shutdown();
    assertNull(m_executeThread.waitForMessage());
  }

  @Test public void testQueueAndFlush() throws Exception {

    final QueuedSender sender = new QueuedSenderDecorator(m_sender);

    final SimpleMessage[] messages = new SimpleMessage[25];

    for (int i=0; i<messages.length; ++i) {
      messages[i] = new SimpleMessage();
      sender.send(messages[i]);
    }

    sender.flush();

    for (int i=0; i<messages.length; ++i) {
      final Message receivedMessage = m_executeThread.waitForMessage();

      assertEquals(messages[i], receivedMessage);
      assertTrue(messages[i] != receivedMessage);
    }
  }

  @Test public void testQueueAndSend() throws Exception {

    final QueuedSender sender = new QueuedSenderDecorator(m_sender);

    final SimpleMessage[] messages = new SimpleMessage[25];

    for (int i=0; i<messages.length; ++i) {
      messages[i] = new SimpleMessage();
      sender.send(messages[i]);
    }

    final SimpleMessage finalMessage = new SimpleMessage();
    sender.send(finalMessage);
    sender.flush();

    for (int i=0; i<messages.length; ++i) {
      final Message receivedMessage = m_executeThread.waitForMessage();

      assertEquals(messages[i], receivedMessage);
      assertTrue(messages[i] != receivedMessage);
    }

    final Message receivedFinalMessage = m_executeThread.waitForMessage();

    assertEquals(finalMessage, receivedFinalMessage);
    assertTrue(finalMessage != receivedFinalMessage);
  }

  /**
   * Pico-kernel! Need a long running thread because of the half-baked
   * PipedInputStream/PipedOutputStream thread checking.
   */
  private final class ExecuteThread extends Thread {

    private Action m_action;

    public ExecuteThread() {
      super("ExecuteThread");
    }

    public synchronized void run() {

      try {
        while (true) {
          while (m_action == null) {
            wait();
          }

          m_action.run();
          m_action = null;

          notifyAll();
        }
      }
      catch (InterruptedException e) {
      }
    }

    private synchronized Object execute(Action action) throws Exception {

      m_action = action;
      notifyAll();

      while (!action.getHasRun()) {
        wait();
      }

      return action.getResult();
    }

    public Message waitForMessage() throws Exception {
      return (Message) execute(
        new Action() {
          public Object doAction() throws Exception {
            return m_receiver.waitForMessage();
          }
        }
        );
    }

    public void shutdown() throws Exception {
      execute(
        new Action() {
          public Object doAction() throws Exception {
            throw new InterruptedException();
          }
        }
        );
    }

    private abstract class Action {

      private Object m_result;
      private Exception m_exception;
      private boolean m_hasRun = false;

      public void run() throws InterruptedException {
        try {
          m_result = doAction();
        }
        catch (InterruptedException e) {
          throw e;
        }
        catch (Exception e) {
          m_exception = e;
        }
        finally {
          m_hasRun = true;
        }
      }

      public Object getResult() throws Exception {
        if (m_exception != null) {
          throw m_exception;
        }

        return m_result;
      }

      public boolean getHasRun() {
        return m_hasRun;
      }

      protected abstract Object doAction() throws Exception;
    }
  }

  static final class BigBufferPipedInputStream extends PipedInputStream {
    public BigBufferPipedInputStream(PipedOutputStream src)
      throws IOException {
      super(src);
      // JDK, I laugh at your puny buffer.
      buffer = new byte[32768];
    }
  }
}
