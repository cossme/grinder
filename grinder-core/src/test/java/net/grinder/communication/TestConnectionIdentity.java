// Copyright (C) 2004 Philip Aston
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

import junit.framework.TestCase;

import java.net.InetAddress;


/**
 *  Unit test case for <code>ConnectionIdentity</code>.
 *
 * @author Philip Aston
 */
public class TestConnectionIdentity extends TestCase {

  public void testEquality() throws Exception {

    final InetAddress host = InetAddress.getLocalHost();

    final ConnectionIdentity connection1 = 
      new ConnectionIdentity(host, 1234, 999l);

    assertTrue(!connection1.equals(null));
    assertTrue(!connection1.equals(this));
    assertEquals(connection1, connection1);

    final ConnectionIdentity connection2 = 
      new ConnectionIdentity(host, 1234, 999l);
    
    assertEquals(connection1.hashCode(), connection2.hashCode());
    assertEquals(connection1, connection2);

    final ConnectionIdentity connection3 = 
      new ConnectionIdentity(host, 1234, 92199l);

    assertTrue(!connection1.equals(connection3));
  }
}
