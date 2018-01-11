// Copyright (C) 2003 - 2012 Philip Aston
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import net.grinder.common.UncheckedInterruptedException;
import net.grinder.communication.ResourcePool.Resource;
import net.grinder.util.thread.InterruptibleRunnable;
import net.grinder.util.thread.InterruptibleRunnableAdapter;


/**
 * Manages the sending of messages to many Receivers.
 *
 * @author Philip Aston
 */
abstract class AbstractFanOutSender extends AbstractSender {

  private final ExecutorService m_executor;
  private final ResourcePool m_resourcePool;

  /**
   * Constructor.
   *
   * @param executor Executor to use.
   * @param resourcePool Pool of resources from which the output
   * streams can be reserved.
   */
  protected AbstractFanOutSender(ExecutorService executor,
                                 ResourcePool resourcePool) {
    m_executor = executor;
    m_resourcePool = resourcePool;
  }

  /**
   * Send a message.
   *
   * @param message The message.
   * @exception IOException If an error occurs.
   */
  @Override protected final void writeMessage(final Message message)
    throws CommunicationException {
    writeAddressedMessage(new SendToEveryoneAddress(), message);
  }

  /**
   * Send a message.
   *
   * @param message The message.
   * @exception IOException If an error occurs.
   */
  protected final void writeAddressedMessage(Address address, Message message)
    throws CommunicationException {

    // We reserve all the resources here and hand off the
    // reservations to WriteMessageToStream instances. This
    // guarantees order of messages to a given resource for this
    // AbstractFanOutSender.
    for (ResourcePool.Reservation reservation : m_resourcePool.reserveAll()) {
      final Resource resource = reservation.getResource();

      if (!address.includes(getAddress(resource))) {
        reservation.free();
        continue;
      }

      // We don't need to synchronise access to the stream; access is
      // protected through the socket set and only we hold the reservation.
      m_executor.execute(
        new InterruptibleRunnableAdapter(
          new WriteMessageToStream(message,
                                   resourceToOutputStream(resource),
                                   reservation)));
    }
  }

  /**
   * Subclasses must implement this to return an output stream from a
   * resource.
   *
   * @param resource The resource.
   * @return The output stream.
   * @throws CommunicationException If the output stream could not be
   * obtained from the resource.
   */
  protected abstract OutputStream
    resourceToOutputStream(ResourcePool.Resource resource)
    throws CommunicationException;

  /**
   * Subclasses must implement this to return the address associated with
   * a resource.
   *
   * @param resource The resource.
   * @return The address, or <code>null</code> if the resource has no address.
   */
  protected abstract Address getAddress(Resource resource);

  /**
   * Allow subclasses to access the resource pool.
   *
   * @return The resource pool.
   */
  protected final ResourcePool getResourcePool() {
    return m_resourcePool;
  }

  /**
   * Shut down this sender.
   */
  @Override public void shutdown() {
    super.shutdown();

    m_executor.shutdown();

    try {
      final boolean terminated =
        m_executor.awaitTermination(10, TimeUnit.SECONDS);

      if (!terminated) {
        throw new AssertionError("Failed to terminate tasks");
      }
    }
    catch (InterruptedException e) {
      throw new UncheckedInterruptedException(e);
    }
  }

  private static final class SendToEveryoneAddress implements Address {
    private static final long serialVersionUID = 1L;

    public boolean includes(Address address) {
      return true;
    }
  }

  private static final class WriteMessageToStream
    implements InterruptibleRunnable {

    private final Message m_message;
    private final OutputStream m_outputStream;
    private final ResourcePool.Reservation m_reservation;

    public WriteMessageToStream(Message message,
                                OutputStream outputStream,
                                ResourcePool.Reservation reservation) {
      m_message = message;
      m_outputStream = outputStream;
      m_reservation = reservation;
    }

    public void interruptibleRun() {
      try {
        writeMessageToStream(m_message, m_outputStream);
      }
      catch (IOException e) {
        // InterruptedIOExceptions take this path.
        m_reservation.close();
      }
      finally {
        m_reservation.free();
      }
    }
  }
}
