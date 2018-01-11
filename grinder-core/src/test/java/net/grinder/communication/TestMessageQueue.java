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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import net.grinder.communication.MessageQueue.ShutdownException;

import org.junit.Test;


/**
 *  Unit test case for {@code MessageQueue}.
 *
 * @author Philip Aston
 **/
public class TestMessageQueue {

  private final MessageQueue m_queue = new MessageQueue(false);

  @Test public void testWithOneThread() throws Exception {
    final Message[] messages = {
      new SimpleMessage(10),
      new SimpleMessage(0),
      new SimpleMessage(999),
    };

    for (int i=0; i<messages.length; ++i) {
      m_queue.queue(messages[i]);
    }

    for (int i=0; i<messages.length; ++i) {
      assertSame(messages[i], m_queue.dequeue(false));
    }

    assertNull(m_queue.dequeue(false));
  }

  @Test public void testWithActiveDequeuer() throws Exception {
    final Message[] messages = {
      new SimpleMessage(10),
      new SimpleMessage(0),
      new SimpleMessage(999),
    };

    final DequeuerThread dequeuerThread = new DequeuerThread(messages.length);
    dequeuerThread.start();

    for (int i=0; i<messages.length; ++i) {
      m_queue.queue(messages[i]);
    }

    dequeuerThread.join();

    final List<Message> receivedMessages = dequeuerThread.getMessages();

    assertEquals(messages.length, receivedMessages.size());

    for (int i=0; i<messages.length; ++i) {
      assertSame(messages[i], receivedMessages.get(i));
    }
  }

  @Test public void testShutdownReciever() throws Exception {

    final DequeuerThread dequeuerThread = new DequeuerThread(1);
    dequeuerThread.start();
    m_queue.shutdown();

    dequeuerThread.join();
    assertTrue(dequeuerThread.getException() instanceof ShutdownException);

    try {
      m_queue.queue(new SimpleMessage(0));
      fail("Expected a ShutdownException");
    }
    catch (ShutdownException e) {
    }

    try {
      m_queue.dequeue(true);
      fail("Expected a ShutdownException");
    }
    catch (ShutdownException e) {
    }
  }

  @Test public void testManyQueuersAndDequeuers() throws Exception {

    final Thread[] queuers = new Thread[6];
    final Thread[] dequeuers = new Thread[3];
    final Random random = new Random();

    for (int i=0; i<queuers.length; ++i) {
      queuers[i] = new Thread() {
          public void run() {
            for (int j=0; j<10; ++j) {
              try {
                m_queue.queue(new SimpleMessage(0));
              }
              catch (ShutdownException e) {
                fail("Unexpected ShutdownException");
              }

              try {
                Thread.sleep(random.nextInt(10));
              }
              catch (InterruptedException e) {
              }
            }
          }
        };

      queuers[i].start();
    }

    for (int i=0; i<dequeuers.length; ++i) {
      dequeuers[i] = new DequeuerThread(20);
      dequeuers[i].start();
    }

    for (int i=0; i<dequeuers.length; ++i) {
      dequeuers[i].join();
    }

    assertNull(m_queue.dequeue(false));
  }

  @Test public void testExceptionPropagation() throws Exception {

    // m_queue does not allow exceptions to be queued.
    try {
      m_queue.queue(new CommunicationException(""));
      fail("Expected an Assertion");
    }
    catch (AssertionError e) {
    }

    final MessageQueue exceptionQueue = new MessageQueue(true);

    final CommunicationException[] exceptions = {
      new CommunicationException("Exception 1"),
      new CommunicationException("Exception 2"),
    };

    final Message[] messages = {
      new SimpleMessage(0),
      new SimpleMessage(999),
    };

    for (int i=0; i<messages.length; ++i) {
      exceptionQueue.queue(exceptions[i]);
      exceptionQueue.queue(messages[i]);
    }

    for (int i=0; i<messages.length; ++i) {
      try {
        exceptionQueue.dequeue(false);
        fail("Expected a CommunicationException");
      }
      catch (CommunicationException e) {
        assertSame(exceptions[i], e.getCause());
      }

      assertSame(messages[i], exceptionQueue.dequeue(false));
    }

    assertNull(m_queue.dequeue(false));
  }

  private class DequeuerThread extends Thread {

    private List<Message> m_messages = new LinkedList<Message>();
    private Exception m_exception;
    private int m_howMany;

    public DequeuerThread(int howMany) {
      m_howMany = howMany;
    }

    public List<Message> getMessages() {
      return m_messages;
    }

    public Exception getException() {
      return m_exception;
    }

    public void run() {

      m_exception = null;

      try {
        while (m_howMany-- > 0) {
          m_messages.add(m_queue.dequeue(true));
        }
      }
      catch (Exception e) {
        m_exception = e;
      }
    }
  }
}
