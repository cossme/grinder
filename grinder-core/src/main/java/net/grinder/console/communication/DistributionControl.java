// Copyright (C) 2004 - 2008 Philip Aston
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

package net.grinder.console.communication;


import net.grinder.communication.Address;
import net.grinder.messages.agent.CacheHighWaterMark;
import net.grinder.util.FileContents;


/**
 * Interface for sending files to the agent process caches.
 *
 * @author Philip Aston
 */
public interface DistributionControl {

  /**
   * Signal agents matching the given address to clear their file caches.
   *
   * @param address
   *            The address of the agents.
   */
  void clearFileCaches(Address address);

  /**
   * Send a file to the agents matching the given address.
   *
   * @param address
   *            The address of the agents.
   * @param fileContents The file contents.
   */
  void sendFile(Address address, FileContents fileContents);

  /**
   * Inform agent processes of a checkpoint of the cache state. Each agent
   * should maintain this (perhaps persistently), and report it in status
   * reports.
   *
   * @param address
   *            The address of the agents.
   * @param highWaterMark
   *            A checkpoint of the cache state.
   */
  void setHighWaterMark(Address address, CacheHighWaterMark highWaterMark);
}
