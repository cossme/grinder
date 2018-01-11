// Copyright (C) 2005 - 2012 Philip Aston
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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import net.grinder.communication.MessageDispatchRegistry.AbstractBlockingHandler;
import net.grinder.communication.MessageDispatchRegistry.AbstractHandler;
import net.grinder.communication.MessageDispatchRegistry.BlockingHandler;
import net.grinder.communication.MessageDispatchRegistry.Handler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@link MessageDispatchSender}.
 *
 * @author Philip Aston
 */
public class TestMessageDispatchSender {

  @Mock private Handler<Message> m_fallBackHandler;
  @Mock private Handler<Message> m_handler;
  @Mock private Handler<Message> m_handler2;
  @Mock private BlockingHandler<OtherMessage> m_responder;
  @Mock private Sender m_sender;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test public void testSend() throws Exception {
    final MessageDispatchSender messageDispatchSender =
      new MessageDispatchSender();

    messageDispatchSender.send(new SimpleMessage());

    messageDispatchSender.addFallback(m_fallBackHandler);

    final Message m1 = new SimpleMessage();
    final Message m2 = new SimpleMessage();

    messageDispatchSender.send(m1);
    messageDispatchSender.send(m2);

    verify(m_fallBackHandler).handle(m1);
    verify(m_fallBackHandler).handle(m2);

    final Handler<Message> previousHandler =
      messageDispatchSender.set(SimpleMessage.class, m_handler);
    assertNull(previousHandler);

    final Handler<Message> previousHandler2 =
      messageDispatchSender.set(OtherMessage.class, m_handler2);
    assertNull(previousHandler2);

    messageDispatchSender.send(m1);
    messageDispatchSender.send(m2);

    verify(m_handler).handle(m1);
    verify(m_handler).handle(m2);

    final OtherMessage m3 = new OtherMessage();
    messageDispatchSender.send(m3);

    verify(m_handler2).handle(m3);

    final CommunicationException e = new CommunicationException("");
    doThrow(e).when(m_handler).handle(m1);

    try {
      messageDispatchSender.send(m1);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e2) {
      assertSame(e, e2);
    }

    verify(m_handler, times(2)).handle(m1);

    verifyNoMoreInteractions(m_handler, m_handler2, m_fallBackHandler);
  }

  @Test public void testWithMessageRequiringResponse() throws Exception {
    final MessageDispatchSender messageDispatchSender =
      new MessageDispatchSender();

    final Message message = new SimpleMessage();
    final MessageRequiringResponse messageRequiringResponse =
      new MessageRequiringResponse(message);

    try {
      messageDispatchSender.send(messageRequiringResponse);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    messageRequiringResponse.setResponder(m_sender);

    messageDispatchSender.send(messageRequiringResponse);
    verify(m_sender).send(isA(NoResponseMessage.class));

    // Now check a handler can send a response.
    final Message responseMessage = new SimpleMessage();

    messageDispatchSender.set(
      SimpleMessage.class,
      new MessageDispatchRegistry.AbstractBlockingHandler<SimpleMessage>() {
        public Message blockingSend(SimpleMessage someMessage)  {
          return responseMessage;
        }
      });

    final MessageRequiringResponse messageRequiringResponse2 =
      new MessageRequiringResponse(message);
    messageRequiringResponse2.setResponder(m_sender);

    messageDispatchSender.send(messageRequiringResponse2);
    verify(m_sender).send(responseMessage);

    // Finally, check that fallback handler can handle response.
    final Message responseMessage2 = new SimpleMessage();

    messageDispatchSender.addFallback(
      new AbstractHandler<Message>() {
        public void handle(Message theMessage) throws CommunicationException {
          if (theMessage instanceof MessageRequiringResponse) {
            final MessageRequiringResponse m =
              (MessageRequiringResponse) theMessage;
            m.sendResponse(responseMessage2);
          }
        }
      });

    final MessageRequiringResponse messageRequiringResponse3 =
      new MessageRequiringResponse(new OtherMessage());
    messageRequiringResponse3.setResponder(m_sender);

    messageDispatchSender.send(messageRequiringResponse3);
    verify(m_sender).send(responseMessage2);

    verifyNoMoreInteractions(m_sender);
  }

  @Test public void testWithBadHandlers() throws Exception {
    final MessageDispatchSender messageDispatchSender =
      new MessageDispatchSender();

    final Message message = new SimpleMessage();

    final CommunicationException communicationException =
      new CommunicationException("");
    doThrow(communicationException).when(m_handler).handle(message);

    messageDispatchSender.addFallback(m_handler);

    try {
      messageDispatchSender.send(message);
    }
    catch (CommunicationException e) {
      assertSame(communicationException, e);
    }

    messageDispatchSender.set(SimpleMessage.class, m_handler);

    try {
      messageDispatchSender.send(message);
    }
    catch (CommunicationException e) {
      assertSame(communicationException, e);
    }

    verify(m_handler, times(2)).handle(message);

    verifyNoMoreInteractions(m_handler);
  }

  @Test public void testShutdown() throws Exception {
    final MessageDispatchSender messageDispatchSender =
      new MessageDispatchSender();

    messageDispatchSender.shutdown();

    messageDispatchSender.set(SimpleMessage.class, m_handler);

    messageDispatchSender.shutdown();

    verify(m_handler).shutdown();

    messageDispatchSender.addFallback(m_handler2);
    messageDispatchSender.addFallback(m_handler2);

    messageDispatchSender.set(OtherMessage.class, m_responder);

    final BlockingHandler<Message> blockingHandler2 =
      new AbstractBlockingHandler<Message>() {
        public Message blockingSend(Message message)
          throws CommunicationException {
            return null;
        }};
    messageDispatchSender.set(Message.class, blockingHandler2);

    messageDispatchSender.shutdown();

    verify(m_handler, times(2)).shutdown();

    verify(m_handler2, times(2)).shutdown();

    verify(m_responder).shutdown();

    verifyNoMoreInteractions(m_handler, m_handler2, m_responder);
  }

  public static class OtherMessage implements Message {
  }
}
