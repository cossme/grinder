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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


/**
 * Unit tests for {@link QueuedSenderDecorator}.
 *
 * @author Philip Aston
 */
public class TestQueuedSenderDecorator {

  private final static class StubSender implements Sender {

    private List<Message> m_messagesReceived = new ArrayList<Message>();
    private boolean m_shutdown;

    public void send(Message message) throws CommunicationException {
      m_messagesReceived.add(message);
    }

    public void shutdown() {
      m_shutdown = true;
    }

    public Message[] getMessagesReceived() {
      try {
        return m_messagesReceived.toArray(new Message[0]);
      }
      finally {
        m_messagesReceived.clear();
      }
    }

    public boolean getIsShutdown() {
      return m_shutdown;
    }
  }

  @Test public void testConstructor() throws Exception {
    final StubSender sender = new StubSender();

    new QueuedSenderDecorator(sender);
  }

  @Test public void testSend() throws Exception {
    final StubSender sender = new StubSender();

    final QueuedSender queuedSender = new QueuedSenderDecorator(sender);

    final Message message1 = new SimpleMessage();
    final Message message2 = new SimpleMessage();

    queuedSender.send(message1);
    queuedSender.send(message2);
    queuedSender.flush();

    final Message[] messagesReceived = sender.getMessagesReceived();

    assertEquals(2, messagesReceived.length);
    assertSame(message1, messagesReceived[0]);
    assertSame(message2, messagesReceived[1]);
  }

  @Test public void testQueueAndFlush() throws Exception {
    final StubSender sender = new StubSender();

    final QueuedSender queuedSender = new QueuedSenderDecorator(sender);

    final Message message1 = new SimpleMessage();
    final Message message2 = new SimpleMessage();
    final Message message3 = new SimpleMessage();
    final Message message4 = new SimpleMessage();

    queuedSender.send(message1);

    assertEquals(0, sender.getMessagesReceived().length);

    queuedSender.flush();

    final Message[] messagesReceived = sender.getMessagesReceived();

    assertEquals(1, messagesReceived.length);
    assertSame(message1, messagesReceived[0]);

    queuedSender.send(message1);
    queuedSender.send(message2);
    queuedSender.send(message3);
    queuedSender.flush();
    queuedSender.send(message4);

    final Message[] messagesReceived2 = sender.getMessagesReceived();

    assertEquals(3, messagesReceived2.length);
    assertSame(message1, messagesReceived2[0]);
    assertSame(message2, messagesReceived2[1]);
    assertSame(message3, messagesReceived2[2]);

    queuedSender.flush();

    final Message[] messagesReceived3 = sender.getMessagesReceived();

    assertEquals(1, messagesReceived3.length);
    assertSame(message4, messagesReceived3[0]);
  }

  @Test public void testShutdown() throws Exception {
    final StubSender sender = new StubSender();

    final QueuedSender queuedSender = new QueuedSenderDecorator(sender);

    assertTrue(!sender.getIsShutdown());

    queuedSender.shutdown();

    assertTrue(sender.getIsShutdown());

    try {
      queuedSender.send(new SimpleMessage());
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    try {
      queuedSender.flush();
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }
  }
}
