// Copyright (C) 2011 Philip Aston
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

package net.grinder.synchronisation.messages;

import static java.util.Arrays.asList;
import static net.grinder.testutility.Serializer.serialize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.Set;

import net.grinder.common.processidentity.AgentIdentity;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.communication.CommunicationException;
import net.grinder.messages.console.AgentAddress;
import net.grinder.messages.console.WorkerAddress;
import net.grinder.synchronisation.BarrierIdentityGenerator;

import org.junit.Test;


/**
 * Unit tests for barrier group messages.
 *
 * @author Philip Aston
 */
public class TestBarrierGroupMessages {

  private final BarrierIdentity.Factory m_identityFactory =
    new BarrierIdentityGenerator(new Integer(1));

  @Test public void testOpenBarrierMessage() throws Exception {

    final Set<BarrierIdentity> waiters =
      new HashSet<BarrierIdentity>(asList(m_identityFactory.next(),
                                          m_identityFactory.next()));

    final OpenBarrierMessage message = new OpenBarrierMessage("abc",
                                                              waiters);

    final OpenBarrierMessage serialized = serialize(message);

    assertEquals("abc", serialized.getName());
    assertEquals(waiters, message.getWaiters());
  }

  @Test public void testAddWaiterMessage() throws Exception {
    final BarrierIdentity identity = m_identityFactory.next();

    final AddWaiterMessage message = new AddWaiterMessage("abc", identity);

    final AddWaiterMessage serialized = serialize(message);

    assertEquals("abc", serialized.getName());
    assertEquals(identity, serialized.getBarrierIdentity());
  }

  @Test public void testAddressAwareMessage() throws Exception {

    final WorkerIdentity identity = mock(WorkerIdentity.class);

    final AddWaiterMessage message =
      new AddWaiterMessage("abc", m_identityFactory.next());

    message.setAddress(new WorkerAddress(identity));

    assertSame(identity, message.getProcessIdentity());
  }

  @Test public void testAddressAwareMessageBadAddress() throws Exception {

    final AddWaiterMessage message =
      new AddWaiterMessage("abc", m_identityFactory.next());

    try {
      message.setAddress(new AgentAddress(mock(AgentIdentity.class)));
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }
  }
}
