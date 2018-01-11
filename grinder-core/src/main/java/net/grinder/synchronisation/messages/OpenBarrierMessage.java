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

import java.util.Set;

import net.grinder.communication.Message;


/**
 * Barrier group message sent to agents when a barrier is opened.
 *
 * @author Philip Aston
 */
public class OpenBarrierMessage implements Message {

  private static final long serialVersionUID = 1L;

  private final String m_name;
  private final Set<BarrierIdentity> m_waiters;

  /**
   * Constructor.
   *
   * @param name
   *          Barrier name.
   * @param waiters
   *          Waiters to wake.
   */
  public OpenBarrierMessage(String name, Set<BarrierIdentity> waiters) {
    m_name = name;
    m_waiters = waiters;
  }

  /**
   * Barrier name.
   *
   * @return The barrier name.
   */
  public String getName() {
    return m_name;
  }

  /**
   * Waiters to wake.
   *
   * @return The waiters.
   */
  public Set<BarrierIdentity> getWaiters() {
    return m_waiters;
  }
}
