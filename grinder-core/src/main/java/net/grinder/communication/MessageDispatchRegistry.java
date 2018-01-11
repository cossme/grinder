// Copyright (C) 2006 - 2011 Philip Aston
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

/**
 * Register of message handlers, keyed by message type.
 *
 * <p>
 * The current implementation of this interface ({@link MessageDispatchSender})
 * does not interpret the message type polymorphicly. That is, a message is only
 * passed to a handler if the handler was registered for the message's class;
 * handlers registered for super classes are not invoked.
 * </p>
 *
 * @author Philip Aston
 */
public interface MessageDispatchRegistry {

  /**
   * Handler interface.
   */
  interface Handler<T extends Message> {

    /**
     * Handle the given message.
     *
     * @param message A {@link Message}.
     * @throws CommunicationException If an error occurs.
     */
    void handle(T message) throws CommunicationException;

    /**
     * Cleanly shut down the handler.
     */
    void shutdown();
  }

  /**
   * Handler interface.
   */
  interface BlockingHandler<T extends Message> {

    /**
     * Handle the given message.
     *
     * @param message A {@link Message}.
     * @return The response message.
     * @throws CommunicationException If an error occurs.
     */
    Message blockingSend(T message) throws CommunicationException;

    /**
     * Cleanly shut down the handler.
     */
    void shutdown();
  }

  /**
   * Register a message handler.
   *
   * @param messageType
   *          Messages of this type will be routed to the handler.
   * @param messageHandler
   *          The message handler.
   * @param <S> Class of messageType.
   * @param <T> Constrains messageHandler to be able to handle messageType.
   *
   * @return The previous message handler registered for
   *         <code>messageType</code> or <code>null</code>.
   */
  <T extends Message, S extends T>
    Handler<T> set(Class<S> messageType, Handler<T> messageHandler);

  /**
   * Register a message responder.
   *
   * @param messageType
   *          Messages of this type will be routed to the handler.
   * @param responder The message responder.
   * @param <S> Class of messageType.
   * @param <T> Constrains messageHandler to be able to handle messageType.
   *
   * @return The previous message handler registered for
   *         <code>messageType</code> or <code>null</code>.
   */
  <T extends Message, S extends T>
    BlockingHandler<T> set(Class<S> messageType, BlockingHandler<T> responder);

  /**
   * Register a message handler that is called if no other handler or responder
   * is registered for the message type. There can be multiple such handlers.
   *
   * @param messageHandler The sender.
   */
  void addFallback(Handler<Message> messageHandler);

  /**
   * Most handlers ignore the shutdown event, so provide this as a convenient
   * base for anonymous classes.
   *
   * @param <T> Message type.
   */
  public abstract static class AbstractHandler<T extends Message>
    implements Handler<T> {

    /**
     * Ignore shutdown events.
     */
    public void shutdown() {
    }
  }

  /**
   * Most handlers ignore the shutdown event, so provide this as a convenient
   * base for anonymous classes.
   *
   * @param <T> Request message type.
   */
  public abstract static class AbstractBlockingHandler<T extends Message>
    implements BlockingHandler<T> {

    /**
     * Ignore shutdown events.
     */
    public void shutdown() {
    }
  }
}
