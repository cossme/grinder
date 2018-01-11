// Copyright (C) 2005 - 2011 Philip Aston
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.grinder.util.ListenerSupport;


/**
 * Passive {@link Sender}class that dispatches incoming messages to the
 * appropriate handler.
 *
 * @author Philip Aston
 */
public final class MessageDispatchSender
  implements Sender, MessageDispatchRegistry {

  /* Guarded by m_handlers. */
  private final Map<Class<? extends Message>, Handler<Message>> m_handlers =
    Collections.synchronizedMap(
      new HashMap<Class<? extends Message>, Handler<Message>>());

  /* Guarded by m_responders. */
  private final Map<Class<? extends Message>, BlockingHandler<Message>>
    m_responders =
      Collections.synchronizedMap(
        new HashMap<Class<? extends Message>, BlockingHandler<Message>>());

  private final ListenerSupport<Handler<Message>> m_fallbackHandlers =
    new ListenerSupport<Handler<Message>>();

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override public <T extends Message, S extends T>
    Handler<T> set(Class<S> messageType, Handler<T> messageHandler) {

    return (Handler<T>)
      m_handlers.put(messageType, (Handler<Message>) messageHandler);
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override public <T extends Message, S extends T>
  BlockingHandler<T>
    set(Class<S> messageType, BlockingHandler<T> responder) {
    return (BlockingHandler<T>)
      m_responders.put(messageType, (BlockingHandler<Message>)responder);
  }

  /**
   * Register a message handler that is called if no other handler or responder
   * is registered for the message type.
   *
   * @param messageHandler The sender.
   */
  public void addFallback(Handler<Message> messageHandler) {
    m_fallbackHandlers.add(messageHandler);
  }

  /**
   * Sends a message to each handler until one claims to have handled the
   * message.
   *
   * @param message The message.
   * @throws CommunicationException If one of the handlers failed.
   */
  public void send(final Message message) throws CommunicationException {

    if (message instanceof MessageRequiringResponse) {
      final MessageRequiringResponse messageRequringResponse =
        (MessageRequiringResponse)message;

      final Message requestMessage = messageRequringResponse.getMessage();

      final BlockingHandler<Message> responder =
        m_responders.get(requestMessage.getClass());

      if (responder != null) {
        messageRequringResponse.sendResponse(
          responder.blockingSend(requestMessage));
        return;
      }
    }
    else {
      final Handler<Message> handler = m_handlers.get(message.getClass());

      if (handler != null) {
        handler.handle(message);
        return;
      }
    }

    final CommunicationException[] exception = new CommunicationException[1];

    m_fallbackHandlers.apply(new ListenerSupport.Informer<Handler<Message>>() {
        public void inform(Handler<Message> handler) {
          try {
            handler.handle(message);
          }
          catch (CommunicationException e) {
            exception[0] = e;
          }
        }
      });

    if (message instanceof MessageRequiringResponse) {
      final MessageRequiringResponse messageRequringResponse =
        (MessageRequiringResponse)message;

      if (!messageRequringResponse.isResponseSent()) {
        // No one responded.
        messageRequringResponse.sendResponse(new NoResponseMessage());
      }
    }

    if (exception[0] != null) {
      throw exception[0];
    }
  }

 /**
  * Shutdown all our handlers.
  */
  public void shutdown() {
    final List<Handler<? extends Message>> handlers;

    synchronized (m_handlers) {
      handlers = new ArrayList<Handler<? extends Message>>(m_handlers.values());
    }

    for (Handler<? extends Message> handler : handlers) {
      handler.shutdown();
    }

    final List<BlockingHandler<? extends Message>> responders;

    synchronized (m_responders) {
      responders =
        new ArrayList<BlockingHandler<? extends Message>>(
            m_responders.values());
    }

    for (BlockingHandler<? extends Message> responder : responders) {
      responder.shutdown();
    }

    m_fallbackHandlers.apply(new ListenerSupport.Informer<Handler<Message>>() {
      public void inform(Handler<Message> handler) { handler.shutdown(); }
    });
  }
}
