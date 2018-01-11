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

import java.io.OutputStream;
import java.util.concurrent.ExecutorService;

import net.grinder.communication.ResourcePool.Resource;
import net.grinder.util.thread.ExecutorFactory;


/**
 * Manages the sending of messages to many TCP clients.
 *
 * @author Philip Aston
 */
public final class FanOutServerSender extends AbstractFanOutSender {

  /**
   * Constructor.
   *
   * @param acceptor Acceptor.
   * @param connectionType Connection type.
   * @param numberOfThreads Number of sender threads to use.
   * @throws Acceptor.ShutdownException If the acceptor has been shutdown.
   */
  public FanOutServerSender(Acceptor acceptor,
                            ConnectionType connectionType,
                            int numberOfThreads)
    throws Acceptor.ShutdownException {

    this(acceptor.getSocketSet(connectionType),
         ExecutorFactory.createThreadPool(
           "FanOutServerSender for " + connectionType, numberOfThreads));
  }

  /**
   * Constructor.
   *
   * @param acceptedSockets Socket set.
   * @param executor Executor service to use.
   * @throws CommunicationException If server socket could not be
   * bound.
   */
  private FanOutServerSender(ResourcePool acceptedSockets,
                             ExecutorService executor) {

    super(executor, acceptedSockets);
  }

  /**
   * Send a message to a particular address.
   *
   * @param address Address to send message to.
   * @param message The message.
   * @exception CommunicationException If an error occurs.
   */
  public void send(Address address, Message message)
    throws CommunicationException {

    if (isShutdown()) {
      throw new CommunicationException("Shut down");
    }

    writeAddressedMessage(address, message);
  }

  /**
   * Return an output stream from a socket resource.
   *
   * @param resource The resource.
   * @return The output stream.
   * @throws CommunicationException If the output stream could not be
   * obtained from the socket.
   */
  @Override protected OutputStream resourceToOutputStream(
    ResourcePool.Resource resource) throws CommunicationException {

    // We don't need to synchronise access to the SocketWrapper;
    // access is protected through the socket set and only we hold
    // the reservation.
    return ((SocketWrapper)resource).getOutputStream();
  }

  /**
   * Return the address of a socket.
   *
   * @param resource The resource.
   * @return The address, or <code>null</code> if the socket has no address.
   */
  @Override protected Address getAddress(Resource resource) {

    // We don't need to synchronise access to the SocketWrapper;
    // access is protected through the socket set and only we hold
    // the reservation.
    return ((SocketWrapper)resource).getAddress();
  }
}
