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

import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import net.grinder.common.UncheckedInterruptedException;
import net.grinder.testutility.IsolatedObjectFactory;

import junit.framework.TestCase;


/**
 *  Unit test case for <code>StreamReceiver</code>.
 *
 * @author Philip Aston
 */
public class TestStreamReceiver extends TestCase {

  public TestStreamReceiver(String name) {
    super(name);
  }

  public void testReceive() throws Exception {

    final PipedOutputStream outputStream = new PipedOutputStream();
    final InputStream inputStream = new PipedInputStream(outputStream);

    final StreamReceiver streamReceiver = new StreamReceiver(inputStream);

    final SimpleMessage message1 = new SimpleMessage();

    final ObjectOutputStream objectStream1 =
      new ObjectOutputStream(outputStream);
    objectStream1.writeObject(message1);
    objectStream1.flush();

    // Message that we can't read using the standard class loaders.
    final SimpleMessage message2 = new SimpleMessage();
    message2.setPayload(IsolatedObjectFactory.getIsolatedObject());

    final ObjectOutputStream objectStream2 =
      new ObjectOutputStream(outputStream);
    objectStream2.writeObject(message2);
    objectStream2.flush();

    final SimpleMessage message3 = new SimpleMessage();

    final ObjectOutputStream objectStream3 =
      new ObjectOutputStream(outputStream);
    objectStream3.writeObject(message3);
    objectStream3.flush();

    final Message receivedMessage1 = streamReceiver.waitForMessage();

    try {
      streamReceiver.waitForMessage();
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    final Message receivedMessage2 = streamReceiver.waitForMessage();

    assertEquals(message1, receivedMessage1);
    assertEquals(message3, receivedMessage2);

    assertEquals(
      UncheckedInterruptedException.class,
      new BlockingActionThread() {
        protected void blockingAction() throws CommunicationException {
          streamReceiver.waitForMessage();
        }
      }.getException().getClass());


    outputStream.close();

    try {
      streamReceiver.waitForMessage();
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }
  }

  public void testShutdown() throws Exception {

    final PipedOutputStream outputStream = new PipedOutputStream();
    final InputStream inputStream = new PipedInputStream(outputStream);

    final StreamReceiver streamReceiver = new StreamReceiver(inputStream);

    final SimpleMessage message = new SimpleMessage();

    final ObjectOutputStream objectStream =
      new ObjectOutputStream(outputStream);
    objectStream.writeObject(message);
    objectStream.flush();

    final Message receivedMessage = streamReceiver.waitForMessage();
    assertNotNull(receivedMessage);

    streamReceiver.shutdown();

    assertNull(streamReceiver.waitForMessage());
  }

  public void testCloseCommunicationMessage() throws Exception {

    final PipedOutputStream outputStream = new PipedOutputStream();
    final InputStream inputStream = new PipedInputStream(outputStream);

    final StreamReceiver streamReceiver = new StreamReceiver(inputStream);

    final SimpleMessage message = new SimpleMessage();

    final ObjectOutputStream objectStream1 =
      new ObjectOutputStream(outputStream);
    objectStream1.writeObject(message);
    objectStream1.flush();

    final Message receivedMessage = streamReceiver.waitForMessage();
    assertNotNull(receivedMessage);

    final Message closeCommunicationMessage = new CloseCommunicationMessage();

    final ObjectOutputStream objectStream2 =
      new ObjectOutputStream(outputStream);
    objectStream2.writeObject(closeCommunicationMessage);
    objectStream2.flush();

    assertNull(streamReceiver.waitForMessage());
  }
}

