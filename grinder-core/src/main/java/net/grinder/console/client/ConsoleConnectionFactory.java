// Copyright (C) 2007 - 2012 Philip Aston
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

package net.grinder.console.client;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.grinder.communication.ClientSender;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.Connector;
import net.grinder.util.thread.ExecutorFactory;


/**
 * Something that can create {@link ConsoleConnection} instances.
 *
 * @author Philip Aston
 */
public class ConsoleConnectionFactory {

  private final ScheduledExecutorService m_executor;

  /**
   * Constructor.
   */
  public ConsoleConnectionFactory() {
    this(ExecutorFactory.getUtilityScheduledExecutor());
  }

  /**
   * Constructor.
   *
   * @param executor Scheduled executor used to send keep alive messages.
   */
  protected ConsoleConnectionFactory(ScheduledExecutorService executor) {
    m_executor = executor;
  }

  /**
   * Create a {@link ConsoleConnection}.
   *
   * @param host Console host.
   * @param port Console port.
   * @return The {@link ConsoleConnection}.
   * @throws ConsoleConnectionException Failed to establish a connection.
   */
  public ConsoleConnection connect(String host, int port)
    throws ConsoleConnectionException {

    try {
      final ClientSender sender = ClientSender.connect(
        new Connector(host, port, ConnectionType.CONSOLE_CLIENT), null);

      final Runnable keepAlive = new Runnable() {
        @Override
        public void run() {
          try {
            sender.sendKeepAlive();
          }
          catch (CommunicationException e) {
            // Connection is dead, terminate task.
            throw new RuntimeException(e);
          }
        }
      };

      m_executor.scheduleWithFixedDelay(keepAlive,
                                        0,
                                        1,
                                        TimeUnit.SECONDS);

      return new ConsoleConnectionImplementation(sender);
    }
    catch (CommunicationException e) {
      throw new ConsoleConnectionException("Failed to connect", e);
    }
  }
}
