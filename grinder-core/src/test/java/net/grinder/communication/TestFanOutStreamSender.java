// Copyright (C) 2003, 2004, 2005 Philip Aston
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

import java.io.ObjectInputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.StreamCorruptedException;

import junit.framework.TestCase;


/**
 *  Unit tests for <code>FanOutStreamSender</code>.
 *
 * @author Philip Aston
 */
public class TestFanOutStreamSender extends TestCase {

  public TestFanOutStreamSender(String name) {
    super(name);
  }

  public void testAddAndSend() throws Exception {

    final FanOutStreamSender serverSender = new FanOutStreamSender(3);

    final PipedInputStream[] inputStreams = new PipedInputStream[10];
    final PipedOutputStream[] outputStreams =
      new PipedOutputStream[inputStreams.length];

    for (int i=0; i<outputStreams.length; ++i) {
      inputStreams[i] = new PipedInputStream();
      outputStreams[i] = new PipedOutputStream(inputStreams[i]);
      serverSender.add(outputStreams[i]);
    }

    final SimpleMessage message1 = new SimpleMessage();
    final SimpleMessage message2 = new SimpleMessage();

    serverSender.send(message1);
    serverSender.send(message2);

    for (int i=0; i<outputStreams.length; ++i) {
      final ObjectInputStream inputStream1 =
        new ObjectInputStream(inputStreams[i]);
      final Object o1 = inputStream1.readObject();

      final ObjectInputStream inputStream2 =
        new ObjectInputStream(inputStreams[i]);
      final Object o2 = inputStream2.readObject();

      assertEquals(message1, o1);
      assertEquals(message2, o2);

      assertEquals(0, inputStreams[i].available());
    }

    serverSender.shutdown();
  }

  public void testShutdown() throws Exception {

    final FanOutStreamSender serverSender = new FanOutStreamSender(3);

    final PipedInputStream inputStream = new PipedInputStream();
    final PipedOutputStream outputStream = new PipedOutputStream(inputStream);
    serverSender.add(outputStream);

    final Message message = new SimpleMessage();
    serverSender.send(message);

    final ObjectInputStream inputStream1 =
      new ObjectInputStream(inputStream);
    final Object o1 = inputStream1.readObject();
    assertNotNull(o1);

    serverSender.shutdown();

    try {
      serverSender.send(message);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    try {
      final ObjectInputStream inputStream2 =
        new ObjectInputStream(inputStream);
      final Object o2 = inputStream2.readObject();

      assertTrue(o2 instanceof CloseCommunicationMessage);
    }
    catch (StreamCorruptedException e) {
      // Occasionally this occurs because the connection is shutdown.
      // Whatever.
    }
  }
}
