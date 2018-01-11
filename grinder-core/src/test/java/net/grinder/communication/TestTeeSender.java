// Copyright (C) 2004 - 2011 Philip Aston
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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 *  Unit tests for {@link TeeSender}.
 *
 * @author Philip Aston
 */
public class TestTeeSender {

  @Mock private Sender m_sender1;
  @Mock private Sender m_sender2;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test public void testWithGoodSenders() throws Exception {

    final TeeSender teeSender = new TeeSender(m_sender1, m_sender2);

    final Message m1 = new SimpleMessage();
    final Message m2 = new SimpleMessage();

    teeSender.send(m1);
    teeSender.send(m2);
    teeSender.handle(m2);
    teeSender.shutdown();

    verify(m_sender1).send(m1);
    verify(m_sender1, times(2)).send(m2);
    verify(m_sender1).shutdown();

    verify(m_sender2).send(m1);
    verify(m_sender2, times(2)).send(m2);
    verify(m_sender2).shutdown();

    verifyNoMoreInteractions(m_sender1, m_sender2);
  }

  @Test public void testWithABadSender() throws Exception {

    final CommunicationException exceptionToThrowFromSend =
      new CommunicationException("Foo");

    final RuntimeException exceptionToThrowFromShutdown =
      new RuntimeException();

    doThrow(exceptionToThrowFromSend).when(m_sender2).send(isA(Message.class));
    doThrow(exceptionToThrowFromShutdown).when(m_sender2).shutdown();

    // m_sender1 is first, so should be invoked before m_sender2 fails.
    final TeeSender teeSender1 = new TeeSender(m_sender1, m_sender2);

    final Message m = new SimpleMessage();

    try {
      teeSender1.send(m);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
      assertSame(exceptionToThrowFromSend, e);
    }

    verify(m_sender1).send(m);

    try {
      teeSender1.shutdown();
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
      assertSame(exceptionToThrowFromShutdown, e);
    }

    verify(m_sender1).shutdown();

    // goodSender is second, so will never be invoked.
    final TeeSender teeSender2 = new TeeSender(m_sender2, m_sender1);

    try {
      teeSender2.send(m);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
      assertSame(exceptionToThrowFromSend, e);
    }

    try {
      teeSender2.shutdown();
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
      assertSame(exceptionToThrowFromShutdown, e);
    }

    verifyNoMoreInteractions(m_sender1);
  }
}
