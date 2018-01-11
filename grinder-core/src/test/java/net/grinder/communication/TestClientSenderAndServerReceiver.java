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

import java.util.Random;

import org.junit.Before;


/**
 * Unit tests for {link ClientSender} and {@link ServerReceiver}.
 *
 * @author Philip Aston
 */
public class TestClientSenderAndServerReceiver
  extends AbstractSenderAndReceiverSocketTests {

  private final static Random s_random = new Random();

  @Before public void setUp() throws Exception {
    final ServerReceiver receiver = new ServerReceiver();

    receiver.receiveFrom(
      getAcceptor(),
      new ConnectionType[] { getConnectionType() },
      3, 10, 10000);

    initialise(receiver, ClientSender.connect(getConnector(), null));
  }

  static int s_numberOfMessages = 0;

  private class SenderThread extends Thread {
    public void run() {
      try {
        final Sender sender = ClientSender.connect(getConnector(), null);

        final int n = s_random.nextInt(10);

        for (int i=0; i<n; ++i) {
          sender.send(new SimpleMessage(1));
          sleep(s_random.nextInt(30));
        }

        synchronized(Sender.class) {
          s_numberOfMessages += n;
        }

        sender.shutdown();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void testManySenders() throws Exception {
    s_numberOfMessages = 0;

    final Thread[] senderThreads = new Thread[5];

    for (int i=0; i<senderThreads.length; ++i) {
      senderThreads[i] = new SenderThread();
      senderThreads[i].start();
    }

    for (int i=0; i<senderThreads.length; ++i) {
      senderThreads[i].join();
    }

    for (int i=0; i<s_numberOfMessages; ++i) {
      m_receiver.waitForMessage();
    }
  }
}
