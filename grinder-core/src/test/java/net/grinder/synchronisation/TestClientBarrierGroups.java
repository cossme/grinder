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

package net.grinder.synchronisation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.Sender;
import net.grinder.communication.MessageDispatchRegistry.AbstractHandler;
import net.grinder.synchronisation.BarrierGroup.Listener;
import net.grinder.synchronisation.messages.AddBarrierMessage;
import net.grinder.synchronisation.messages.AddWaiterMessage;
import net.grinder.synchronisation.messages.BarrierIdentity;
import net.grinder.synchronisation.messages.CancelWaiterMessage;
import net.grinder.synchronisation.messages.OpenBarrierMessage;
import net.grinder.synchronisation.messages.RemoveBarriersMessage;
import net.grinder.testutility.MockingUtilities.TypedArgumentMatcher;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@link ClientBarrierGroups}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestClientBarrierGroups {

  private static final BarrierIdentity ID1 = new BarrierIdentity() {};
  private static final BarrierIdentity ID2 = new BarrierIdentity() {};

  @Mock private Sender m_sender;
  @Mock private MessageDispatchRegistry m_messageDispatch;
  @Captor
  private ArgumentCaptor<AbstractHandler<OpenBarrierMessage>> m_handlerCaptor;

  private int m_awakenCount = 0;

  private ClientBarrierGroups m_groups;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);

    m_groups = new ClientBarrierGroups(m_sender, m_messageDispatch);
  }

  @Test public void testCreateAndRetrieve() throws Exception {
    assertNull(m_groups.getExistingGroup("A"));

    final BarrierGroup a = m_groups.getGroup("A");
    assertEquals("A", a.getName());
    assertSame(a, m_groups.getGroup("A"));
    assertNotSame(a, m_groups.getGroup("B"));

    assertSame(a, m_groups.getExistingGroup("A"));

    a.addBarrier();

    a.removeBarriers(1); // Invalidate a.

    verify(m_sender).send(isA(AddBarrierMessage.class));
    verify(m_sender).send(isA(RemoveBarriersMessage.class));

    assertNotSame(a, m_groups.getGroup("A"));

    verifyNoMoreInteractions(m_sender);
  }

  @Test public void testMessageHandler() throws Exception {

    verify(m_messageDispatch).set(eq(OpenBarrierMessage.class),
                                  m_handlerCaptor.capture());

    final AbstractHandler<OpenBarrierMessage> handler =
      m_handlerCaptor.getValue();

    final OpenBarrierMessage message = mock(OpenBarrierMessage.class);
    when(message.getName()).thenReturn("A");

    handler.handle(message);
    assertNull(m_groups.getExistingGroup("A"));
    assertEquals(0, m_awakenCount);

    createBarrierGroup("A");
    assertEquals(0, m_awakenCount);
    handler.handle(message);
    assertEquals(1, m_awakenCount);

    verifyNoMoreInteractions(m_messageDispatch);
  }

  private BarrierGroup createBarrierGroup(String groupName) {
    final BarrierGroup bg = m_groups.getGroup(groupName);

    bg.addListener(new Listener() {
        public void awaken(Set<BarrierIdentity> waiters) {
          ++m_awakenCount;
        }
      });

    return bg;
  }

  @Test public void testBarrierGroup() {
    final BarrierGroup bg = createBarrierGroup("Foo");

    assertEquals("Foo", bg.getName());
    assertEquals(0, m_awakenCount);
  }

  @Test public void testBarrierGroupAddWaiter() throws Exception {
    final BarrierGroup bg = createBarrierGroup("Foo");

    bg.addBarrier();
    bg.addBarrier();

    verify(m_sender, times(2)).send(isA(AddBarrierMessage.class));

    bg.addWaiter(ID1);

    bg.addWaiter(ID2);

    verify(m_sender).send(argThat(new AddWaiterMessageMatcher(ID1)));
    verify(m_sender).send(argThat(new AddWaiterMessageMatcher(ID2)));

    assertEquals(0, m_awakenCount);

    verifyNoMoreInteractions(m_sender);
  }

  @Test public void testBarrierGroupAddTooManyWaiters() throws Exception {
    final BarrierGroup bg = createBarrierGroup("Foo");

    bg.addBarrier();
    bg.addWaiter(ID1);

    try {
      bg.addWaiter(ID2);
      fail("Expected AssertionError");
    }
    catch (AssertionError e) {
    }
  }

  @Test public void testRemoveBarriers() throws Exception {
    final BarrierGroup bg = createBarrierGroup("Foo");

    bg.addBarrier();
    bg.addBarrier();
    bg.addBarrier();

    verify(m_sender, times(3)).send(isA(AddBarrierMessage.class));

    bg.addWaiter(ID1);
    bg.addWaiter(ID2);

    verify(m_sender).send(argThat(new AddWaiterMessageMatcher(ID1)));
    verify(m_sender).send(argThat(new AddWaiterMessageMatcher(ID2)));

    bg.removeBarriers(1);

    verify(m_sender).send(argThat(new RemoveBarriersMessageMatcher(1)));

    assertEquals(0, m_awakenCount);

    verifyNoMoreInteractions(m_sender);
  }

  @Test public void testRemoveTooManyBarriers() throws Exception {
    final BarrierGroup bg = createBarrierGroup("Foo");

    bg.addBarrier();
    bg.addBarrier();
    bg.addBarrier();

    bg.removeBarriers(1);
    bg.removeBarriers(1);

    try {
      bg.removeBarriers(2);
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
    }

    assertEquals(0, m_awakenCount);
  }

  @Test public void testInvalidGroup() throws Exception {
    final BarrierGroup bg = createBarrierGroup("Foo");

    bg.addBarrier();
    bg.removeBarriers(1);

    try {
      bg.addWaiter(null);
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
    }

    try {
      bg.addBarrier();
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
    }

    try {
      bg.removeBarriers(1);
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
    }

    assertEquals(0, m_awakenCount);
  }

  @Test public void addMoreWaitersThanBarriers() throws Exception {
    final BarrierGroup bg = createBarrierGroup("Foo");

    try {
      bg.addWaiter(ID2);
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
    }
  }

  @Test public void testCancelWaiter() throws Exception {
    final BarrierGroup bg = createBarrierGroup("Foo");

    bg.addBarrier();
    bg.addBarrier();

    verify(m_sender, times(2)).send(isA(AddBarrierMessage.class));

    bg.addWaiter(ID1);
    verify(m_sender).send(argThat(new AddWaiterMessageMatcher(ID1)));

    bg.cancelWaiter(ID2); // noop
    verify(m_sender).send(argThat(new CancelWaiterMessageMatcher(ID2)));

    bg.cancelWaiter(ID1);
    verify(m_sender).send(argThat(new CancelWaiterMessageMatcher(ID1)));

    bg.addWaiter(ID2);
    verify(m_sender).send(argThat(new AddWaiterMessageMatcher(ID2)));

    bg.addWaiter(ID1);
    verify(m_sender, times(2)).send(argThat(new AddWaiterMessageMatcher(ID1)));

    assertEquals(0, m_awakenCount);

    verifyNoMoreInteractions(m_sender);
  }

  private static class RemoveBarriersMessageMatcher
    extends TypedArgumentMatcher<RemoveBarriersMessage> {

    private final int m_numberOfBarriers;

    RemoveBarriersMessageMatcher(int numberOfBarriers) {
      m_numberOfBarriers = numberOfBarriers;
    }

    @Override protected boolean argumentMatches(RemoveBarriersMessage t) {
      return t.getNumberOfBarriers() == m_numberOfBarriers;
    }
  }

  private static class AddWaiterMessageMatcher
    extends TypedArgumentMatcher<AddWaiterMessage> {

    private final BarrierIdentity m_barrierIdentity;

    AddWaiterMessageMatcher(BarrierIdentity barrierIdentity) {
      m_barrierIdentity = barrierIdentity;
    }

    @Override protected boolean argumentMatches(AddWaiterMessage t) {
      return t.getBarrierIdentity().equals(m_barrierIdentity);
    }
  }

  private static class CancelWaiterMessageMatcher
    extends TypedArgumentMatcher<CancelWaiterMessage> {

    private final BarrierIdentity m_barrierIdentity;

    CancelWaiterMessageMatcher(BarrierIdentity barrierIdentity) {
      m_barrierIdentity = barrierIdentity;
    }

    @Override protected boolean argumentMatches(CancelWaiterMessage t) {
      return t.getBarrierIdentity().equals(m_barrierIdentity);
    }
  }
}
