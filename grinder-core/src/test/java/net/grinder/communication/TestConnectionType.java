// Copyright (C) 2003 - 2011 Philip Aston
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import net.grinder.testutility.AssertUtilities;

import org.junit.Test;


/**
 *  Unit tests for {@code ConnectionType}.
 *
 * @author Philip Aston
 */
public class TestConnectionType {

  @Test public void testToString() {
    assertNotNull(ConnectionType.AGENT.toString());
    AssertUtilities.assertNotEquals(ConnectionType.AGENT.toString(),
                                    ConnectionType.CONSOLE_CLIENT.toString());
    AssertUtilities.assertNotEquals(ConnectionType.CONSOLE_CLIENT.toString(),
                                    ConnectionType.WORKER.toString());
    AssertUtilities.assertNotEquals(ConnectionType.WORKER.toString(),
                                    ConnectionType.AGENT.toString());  }

  @Test public void testEquality() throws Exception {
    assertEquals(ConnectionType.AGENT.hashCode(),
                 ConnectionType.AGENT.hashCode());

    assertEquals(ConnectionType.AGENT, ConnectionType.AGENT);

    assertEquals(ConnectionType.WORKER.hashCode(),
                 ConnectionType.WORKER.hashCode());

    assertEquals(ConnectionType.WORKER, ConnectionType.WORKER);

    assertTrue(!ConnectionType.AGENT.equals(ConnectionType.WORKER));
    assertTrue(!ConnectionType.WORKER.equals(ConnectionType.AGENT));
    assertTrue(!ConnectionType.CONSOLE_CLIENT.equals(ConnectionType.AGENT));

    assertTrue(!ConnectionType.WORKER.equals(new Object()));
  }
}
