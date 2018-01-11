// Copyright (C) 2005 - 2008 Philip Aston
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

package net.grinder.console.distribution;

import java.beans.PropertyChangeListener;


/**
 * Simplistic model of remote file caches.
 *
 * <p>
 * This tracks the state of all the caches, so {@link #getOutOfDate()} will
 * return <code>true</code> if any one of the caches is out of date. For per
 * cache information, see {@link AgentSet#getAddressOfOutOfDateAgents}.
 * </p>
 *
 * @author Philip Aston
 */
public interface AgentCacheState {

  /**
   * Enquire whether one or more agent caches is out of date.
   *
   * @return <code>true</code> => at least one agent cache is out of date.
   */
  boolean getOutOfDate();

  /**
   * Inform that agent caches are out of date due to a change to a file. Called
   * whenever a new or modified file is found.
   *
   * @param time
   *            Caches need to be refreshed with files newer than this time
   *            (milliseconds since Epoch).
   */
  void setNewFileTime(long time);

  /**
   * Allow other parties to register their interest in changes to our state.
   *
   * @param listener Listener to notify on a state change.
   */
  void addListener(PropertyChangeListener listener);
}
