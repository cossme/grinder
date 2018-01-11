// Copyright (C) 2008 - 2011 Philip Aston
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

package net.grinder.console.synchronisation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.synchronisation.BarrierGroup;
import net.grinder.synchronisation.messages.BarrierIdentity;
import net.grinder.synchronisation.messages.OpenBarrierMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@link ConsoleBarrierGroups}.
 *
 * @author Philip Aston
 */
public class TestConsoleBarrierGroups {

  @Mock private MessageDispatchRegistry m_messageDispatchRegistry;
  @Mock private ConsoleCommunication m_consoleCommunication;

  @Captor private ArgumentCaptor<Message> m_messageCaptor;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(m_consoleCommunication.getMessageDispatchRegistry())
      .thenReturn(m_messageDispatchRegistry);
  }

  @Test public void testConsoleBarrierGroups() throws Exception {

    final ConsoleBarrierGroups barrierGroups =
      new ConsoleBarrierGroups(m_consoleCommunication);

    final BarrierGroup bg = barrierGroups.createBarrierGroup("foo");

    bg.addBarrier();
    verifyNoMoreInteractions(m_consoleCommunication);

    bg.addWaiter(mock(BarrierIdentity.class));
    verify(m_consoleCommunication).sendToAgents(m_messageCaptor.capture());

    assertEquals("foo",
                 ((OpenBarrierMessage)m_messageCaptor.getValue()).getName());
  }
}
