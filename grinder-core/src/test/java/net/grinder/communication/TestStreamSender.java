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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;

import junit.framework.TestCase;


/**
 *  Unit test case for <code>StreamSender</code>.
 *
 * @author Philip Aston
 */
public class TestStreamSender extends TestCase {

  public TestStreamSender(String name) {
    super(name);
  }

  public void testSend() throws Exception {

    final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

    final StreamSender streamSender = new StreamSender(byteOutputStream);

    final SimpleMessage message1 = new SimpleMessage();
    final SimpleMessage message2 = new SimpleMessage();

    streamSender.send(message1);
    streamSender.send(message2);

    final ByteArrayInputStream byteInputStream =
      new ByteArrayInputStream(byteOutputStream.toByteArray());

    // Need an ObjectInputStream for every message. See note in
    // StreamSender.writeMessage.
    final ObjectInputStream inputStream1 =
      new ObjectInputStream(byteInputStream);
    final Object o1 = inputStream1.readObject();

    final ObjectInputStream inputStream2 =
      new ObjectInputStream(byteInputStream);
    final Object o2 = inputStream2.readObject();

    assertEquals(message1, o1);
    assertEquals(message2, o2);

    assertEquals(0, byteInputStream.available());
  }

  public void testShutdown() throws Exception {

    final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

    final StreamSender streamSender = new StreamSender(byteOutputStream);

    final Message message = new SimpleMessage();

    streamSender.send(message);

    streamSender.shutdown();

    try {
      streamSender.send(message);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    final ByteArrayInputStream byteInputStream =
      new ByteArrayInputStream(byteOutputStream.toByteArray());

    final ObjectInputStream inputStream1 =
      new ObjectInputStream(byteInputStream);
    final Object o1 = inputStream1.readObject();
    assertNotNull(o1);

    final ObjectInputStream inputStream2 =
      new ObjectInputStream(byteInputStream);
    final Object o2 = inputStream2.readObject();

    assertTrue(o2 instanceof CloseCommunicationMessage);
  }
}
