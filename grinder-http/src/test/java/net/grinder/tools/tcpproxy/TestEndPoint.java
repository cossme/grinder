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

package net.grinder.tools.tcpproxy;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import junit.framework.TestCase;


/**
 * Unit test case for {@link EndPoint}.
 *
 * @author Philip Aston
 */
public class TestEndPoint extends TestCase {

  public void testAccessors() throws Exception {

    final Random random = new Random();

    for (int i=0; i<10; ++i) {
      final byte[] bytes = new byte[random.nextInt(30)];
      random.nextBytes(bytes);
      final String hostname = new String(bytes);
      final int port = random.nextInt(65536);

      final EndPoint endPoint = new EndPoint(hostname, port);
      assertEquals(hostname.toLowerCase(), endPoint.getHost());
      assertEquals(port, endPoint.getPort());
    }
  }

  public void testEquality() throws Exception   {
    final EndPoint[] endPoint = {
      new EndPoint("Abcdef", 55),
      new EndPoint("aBCDef", 55),
      new EndPoint("c", 55),
      new EndPoint("a", 5512),
      new EndPoint("a", 56),
    };

    assertEquals(endPoint[0], endPoint[0]);
    assertEquals(endPoint[0], endPoint[1]);
    assertEquals(endPoint[1], endPoint[0]);
    assertFalse(endPoint[0].equals(endPoint[2]));
    assertFalse(endPoint[1].equals(endPoint[3]));
    assertFalse(endPoint[1].equals(endPoint[4]));

    assertFalse(endPoint[0].equals(this));
  }

  public void testComparable() throws Exception {
    final EndPoint[] ordered = {
      new EndPoint("abc", 21331),
      new EndPoint("abc", 21431),
      new EndPoint("x", 131),
      new EndPoint("X", 132),
      new EndPoint("X", 91321),
    };

    for (int i=0; i<ordered.length; ++i) {
      for (int j=0; j<i; ++j) {
        assertTrue("EndPoint " + i + " is greater than EndPoint " + j,
                   ordered[i].compareTo(ordered[j]) > 0);
      }

      assertEquals(0, ordered[i].compareTo(ordered[i]));

      for (int j=i+1; j<ordered.length; ++j) {
        assertTrue("EndPoint " + i + " is less than EndPoint " + j,
                   ordered[i].compareTo(ordered[j]) < 0);
      }
    }

    // Check two different but equal EndPoint's.
    assertEquals(0, new EndPoint("blah", 999).compareTo(
                   new EndPoint("blah", 999)));
  }

  public void testServerEndPoint() throws Exception {
    final ServerSocket serverSocket = new ServerSocket(0);
    final EndPoint endPoint = EndPoint.serverEndPoint(serverSocket);
    assertNotNull(endPoint);
    serverSocket.close();
  }

  public void testClientEndPoint() throws Exception {
    final ServerSocket serverSocket = new ServerSocket(0);
    final Socket socket = new Socket(serverSocket.getInetAddress(),
                                     serverSocket.getLocalPort());
    final EndPoint endPoint = EndPoint.clientEndPoint(socket);
    assertNotNull(endPoint);
    socket.close();
    serverSocket.close();
  }
}
