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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import net.grinder.common.GrinderProperties;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchSender;
import net.grinder.messages.agent.ResetGrinderMessage;
import net.grinder.messages.agent.StartGrinderMessage;
import net.grinder.messages.agent.StopGrinderMessage;
import net.grinder.util.thread.Condition;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;


/**
 * Unit tests for {@code ConsoleListener}.
 *
 * @author Philip Aston
 */
public class TestConsoleListener {

  @Mock private Logger m_logger;

  @Before public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test public void testConstruction() throws Exception {
    final Condition myCondition = new Condition();

    new ConsoleListener(myCondition, m_logger);

    verifyNoMoreInteractions(m_logger);
  }

  @Test public void testSendNotification() throws Exception {
    final Condition myCondition = new Condition();
    final ConsoleListener listener = new ConsoleListener(myCondition, m_logger);

    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();
    listener.registerMessageHandlers(messageDispatcher);

    final WaitForNotification notified = new WaitForNotification(myCondition);

    messageDispatcher.send(new StopGrinderMessage());

    assertTrue(notified.wasNotified());
  }

  @Test public void testCheckForMessageAndReceive() throws Exception {

    final Condition myCondition = new Condition();
    final ConsoleListener listener = new ConsoleListener(myCondition, m_logger);

    assertFalse(listener.checkForMessage(ConsoleListener.ANY));
    assertFalse(listener.checkForMessage(ConsoleListener.RESET |
                                         ConsoleListener.SHUTDOWN));
    assertFalse(listener.checkForMessage(ConsoleListener.SHUTDOWN));

    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();
    listener.registerMessageHandlers(messageDispatcher);

    messageDispatcher.send(
      new StartGrinderMessage(new GrinderProperties(), -1));
    messageDispatcher.send(new MyMessage());
    messageDispatcher.send(new ResetGrinderMessage());

    verify(m_logger).info("received a start message");
    verify(m_logger).info("received a reset message");
    verifyNoMoreInteractions(m_logger);

    assertFalse(listener.checkForMessage(ConsoleListener.ANY ^
                                         (ConsoleListener.START |
                                          ConsoleListener.RESET)));
    assertTrue(listener.checkForMessage(ConsoleListener.START |
                                        ConsoleListener.STOP));
    assertTrue(listener.received(ConsoleListener.START));
    assertFalse(listener.received(ConsoleListener.STOP));
    assertTrue(listener.received(ConsoleListener.ANY));
    assertFalse(listener.received(ConsoleListener.STOP |
                                 ConsoleListener.RESET));
    assertFalse(listener.received(ConsoleListener.STOP));
    assertFalse(listener.received(ConsoleListener.SHUTDOWN));
    assertFalse(listener.received(ConsoleListener.RESET));

    assertFalse(listener.checkForMessage(ConsoleListener.START));
    assertFalse(listener.received(ConsoleListener.ANY));
    assertFalse(listener.received(ConsoleListener.START));

    assertTrue(listener.checkForMessage(ConsoleListener.RESET));
    assertTrue(listener.received(ConsoleListener.RESET));
    assertTrue(listener.received(ConsoleListener.RESET));

    assertFalse(listener.checkForMessage(ConsoleListener.RESET));
    assertFalse(listener.received(ConsoleListener.RESET));

    reset(m_logger);
    messageDispatcher.send(
      new StartGrinderMessage(new GrinderProperties(), -1));
    messageDispatcher.send(new ResetGrinderMessage());

    verify(m_logger).info("received a start message");
    verify(m_logger).info("received a reset message");
    verifyNoMoreInteractions(m_logger);

    assertTrue(listener.checkForMessage(ConsoleListener.RESET |
                                        ConsoleListener.START));

    reset(m_logger);
    messageDispatcher.send(new ResetGrinderMessage());

    verify(m_logger).info("received a reset message");
    verifyNoMoreInteractions(m_logger);

    assertTrue(listener.checkForMessage(ConsoleListener.RESET |
                                        ConsoleListener.START));
    assertTrue(listener.received(ConsoleListener.RESET));
    assertFalse(listener.checkForMessage(ConsoleListener.RESET |
                                         ConsoleListener.START));
    assertFalse(listener.received(ConsoleListener.START));

    messageDispatcher.shutdown();

    assertTrue(listener.checkForMessage(ConsoleListener.SHUTDOWN));
    assertTrue(listener.received(ConsoleListener.SHUTDOWN));
    assertTrue(listener.received(ConsoleListener.SHUTDOWN));
  }

  @Test public void testDiscardMessages() throws Exception {
    final Condition myCondition = new Condition();
    final ConsoleListener listener = new ConsoleListener(myCondition, m_logger);

    assertFalse(listener.checkForMessage(ConsoleListener.ANY));
    assertFalse(listener.checkForMessage(ConsoleListener.RESET |
                                         ConsoleListener.SHUTDOWN));
    assertFalse(listener.checkForMessage(ConsoleListener.SHUTDOWN));

    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();
    listener.registerMessageHandlers(messageDispatcher);

    listener.discardMessages(ConsoleListener.ANY);

    messageDispatcher.send(
      new StartGrinderMessage(new GrinderProperties(), -1));
    messageDispatcher.send(new MyMessage());
    messageDispatcher.send(new ResetGrinderMessage());

    assertTrue(listener.checkForMessage(ConsoleListener.START |
                                        ConsoleListener.STOP));
    assertTrue(listener.received(ConsoleListener.START));
    assertFalse(listener.received(ConsoleListener.RESET));

    listener.discardMessages(ConsoleListener.RESET);

    assertFalse(listener.checkForMessage(ConsoleListener.RESET));
    assertFalse(listener.received(ConsoleListener.RESET));

    messageDispatcher.send(new ResetGrinderMessage());

    assertTrue(listener.checkForMessage(ConsoleListener.RESET));

    listener.discardMessages(ConsoleListener.RESET);

    assertFalse(listener.received(ConsoleListener.RESET));

    listener.discardMessages(ConsoleListener.RESET);

    assertFalse(listener.received(ConsoleListener.RESET));

    messageDispatcher.shutdown();

    assertTrue(listener.checkForMessage(ConsoleListener.SHUTDOWN));
    assertTrue(listener.received(ConsoleListener.SHUTDOWN));
    listener.discardMessages(ConsoleListener.SHUTDOWN);
    assertFalse(listener.received(ConsoleListener.SHUTDOWN));
  }

  @Test public void testWaitForMessage() throws Exception {
    final Condition myCondition = new Condition();
    final ConsoleListener listener = new ConsoleListener(myCondition, m_logger);
    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();
    listener.registerMessageHandlers(messageDispatcher);

    final Thread t = new Thread() {
        @Override
        public void run() {
          // We synchronise to ensure main thread is blocked in
          // waitForMessage();
          synchronized (myCondition) {
            try {
              messageDispatcher.send(
                new StartGrinderMessage(new GrinderProperties(), -1));
            }
            catch (final CommunicationException e) {
              e.printStackTrace();
            }
          }
        }
    };

    synchronized (myCondition) {
      t.start();
      listener.waitForMessage();
    }

    assertTrue(listener.received(ConsoleListener.START));
  }

  private static final class MyMessage implements Message {
  }

  @Test public void testDispatcherShutdown() throws Exception {

    final Condition myCondition = new Condition();
    final ConsoleListener listener = new ConsoleListener(myCondition, m_logger);
    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();
    listener.registerMessageHandlers(messageDispatcher);

    final WaitForNotification notified = new WaitForNotification(myCondition);

    messageDispatcher.shutdown();

    assertTrue(notified.wasNotified());

    verify(m_logger).info("console connection shut down");
    verifyNoMoreInteractions(m_logger);

    assertFalse(listener.checkForMessage(ConsoleListener.ANY ^
                                          ConsoleListener.SHUTDOWN));
    assertTrue(listener.checkForMessage(ConsoleListener.SHUTDOWN));
    assertTrue(listener.received(ConsoleListener.SHUTDOWN));
    assertTrue(listener.checkForMessage(ConsoleListener.SHUTDOWN));
    assertTrue(listener.received(ConsoleListener.SHUTDOWN));
  }

  @Test public void testShutdown() throws Exception {

    final Condition myCondition = new Condition();
    final ConsoleListener listener = new ConsoleListener(myCondition, m_logger);

    final WaitForNotification notified = new WaitForNotification(myCondition);

    listener.shutdown();

    assertTrue(notified.wasNotified());

    verifyNoMoreInteractions(m_logger);

    assertFalse(listener.checkForMessage(ConsoleListener.ANY ^
                                          ConsoleListener.SHUTDOWN));
    assertTrue(listener.checkForMessage(ConsoleListener.SHUTDOWN));
    assertTrue(listener.received(ConsoleListener.SHUTDOWN));
    assertTrue(listener.checkForMessage(ConsoleListener.SHUTDOWN));
    assertTrue(listener.received(ConsoleListener.SHUTDOWN));
  }


  private static class WaitForNotification implements Runnable {
    private final Thread m_thread;
    private final Object m_condition;
    private boolean m_started = false;
    private boolean m_notified = false;

    public WaitForNotification(final Object condition) throws InterruptedException {
      m_condition = condition;

      m_thread = new Thread(this);
      m_thread.start();

      synchronized (m_condition) {
        while (!m_started) {
          m_condition.wait();
        }
      }
    }

    public boolean wasNotified() throws InterruptedException {
      m_thread.join();

      return m_notified;
    }

    @Override
    public final void run() {
      synchronized(m_condition) {
        final long startTime = System.currentTimeMillis();
        final long maximumTime = 10000;
        m_started = true;
        m_condition.notifyAll();

        try {
          m_condition.wait(maximumTime);

          if (System.currentTimeMillis() - startTime < maximumTime) {
            m_notified = true;
          }
        }
        catch (final InterruptedException e) {
        }
      }
    }
  }

  @Test public void testGetLastStartGrinderMessage() throws Exception {

    final ConsoleListener listener =
      new ConsoleListener(new Condition(), m_logger);

    final Message m1 = new StartGrinderMessage(new GrinderProperties(), -1);
    final Message m2 = new StartGrinderMessage(new GrinderProperties(), -1);
    final Message m3 = new MyMessage();

    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();
    listener.registerMessageHandlers(messageDispatcher);

    assertNull(listener.getLastStartGrinderMessage());

    messageDispatcher.send(m1);
    assertEquals(m1, listener.getLastStartGrinderMessage());

    messageDispatcher.send(m3);
    assertEquals(m1, listener.getLastStartGrinderMessage());

    messageDispatcher.send(m2);
    assertEquals(m2, listener.getLastStartGrinderMessage());
  }
}

