// Copyright (C) 2005 - 2009 Philip Aston
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

package net.grinder.engine.agent;

import net.grinder.common.processidentity.AgentIdentity;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.testutility.Serializer;
import junit.framework.TestCase;

/**
 * TestAgentIdentityImplementation.
 *
 * @author Philip Aston
 */
public class TestAgentIdentityImplementation extends TestCase {

  public void testAgentIdentityImplementation() throws Exception {

    final AgentIdentityImplementation a1 =
      new AgentIdentityImplementation("foo");
    final AgentIdentity a2 =new AgentIdentityImplementation("foo");

    assertEquals(a1, a1);
    assertTrue(!a1.equals(null));
    assertTrue(!a1.equals(this));
    assertTrue(!a1.equals(a2));

    final AgentIdentity a1Copy = Serializer.serialize(a1);

    assertEquals("foo", a1.getName());
    a1.setName("bah");
    assertEquals("bah", a1.getName());
    assertEquals(a1, a1Copy);
    assertTrue(!a1Copy.getName().equals(a1.getName()));

    assertTrue(!a1Copy.toString().equals(a1.toString()));
    assertTrue(!a1Copy.toString().equals(a2.toString()));

    a1.setNumber(10);
    assertEquals(10, a1.getNumber());
    assertEquals(-1, a1Copy.getNumber());
    assertEquals(a1, a1Copy);

    final WorkerIdentity w1 = a1.createWorkerIdentity();
    final WorkerIdentity w2 = a1.createWorkerIdentity();

    assertEquals(w1, w1);
    assertTrue(!w1.equals(null));
    assertTrue(!w1.equals(a1));
    assertTrue(!w1.equals(w2));

    assertTrue(!w1.getName().equals(w2.getName()));

    final WorkerIdentity w1Copy = Serializer.serialize(w1);

    assertEquals(w1, w1Copy);
    assertEquals(w1.toString(), w1Copy.toString());
  }
}
