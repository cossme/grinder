// Copyright (C) 2012 Philip Aston
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

package net.grinder.util.logback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.StatusManager;


/**
 * Unit tests for {@link BufferedEchoMessageEncoder}.
 *
 * @author Philip Aston
 */
public class TestBufferedEchoMessageEncoder {
  private final BufferedEchoMessageEncoder m_encoder =
    new BufferedEchoMessageEncoder();

  @Mock private ILoggingEvent m_event;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test public void testBufferSize() {

    assertTrue(m_encoder.getBufferSize() > 0);

    m_encoder.setBufferSize(1234);
    assertEquals(1234, m_encoder.getBufferSize());
  }

  @Test(expected=IllegalStateException.class)
  public void testCantSetBuferSizeAfterInitialised() throws Exception {
    m_encoder.init(null);

    m_encoder.setBufferSize(256);
  }

  @Test public void testDoEncode() throws Exception {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    m_encoder.init(baos);
    m_encoder.start();

    when(m_event.getMessage()).thenReturn("hello");
    m_encoder.doEncode(m_event);
    when(m_event.getMessage()).thenReturn("world");
    m_encoder.doEncode(m_event);
    m_encoder.close();

    assertEquals("hello" + CoreConstants.LINE_SEPARATOR +
                 "world" + CoreConstants.LINE_SEPARATOR, baos.toString());
  }

  @Test public void testBadClose() throws Exception {
    final Context context = mock(Context.class);
    final StatusManager statusManager = mock(StatusManager.class);

    when(context.getStatusManager()).thenReturn(statusManager);

    final OutputStream os = mock(OutputStream.class);

    doThrow(new IOException()).when(os).flush();

    m_encoder.setContext(context);
    m_encoder.init(os);
    m_encoder.start();
    m_encoder.close();

    verify(statusManager).add(isA(ErrorStatus.class));
  }

}
