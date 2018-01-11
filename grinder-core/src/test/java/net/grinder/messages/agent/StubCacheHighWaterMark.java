// Copyright (C) 2008 Philip Aston
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

package net.grinder.messages.agent;

import net.grinder.messages.agent.CacheHighWaterMark;


/**
 * Stub implementation of {@link CacheHighWaterMark}.
 *
 * @author Philip Aston
 */
public class StubCacheHighWaterMark implements CacheHighWaterMark {
  private final String m_cacheID;
  private final long m_time;

  public StubCacheHighWaterMark(String cacheID, long height) {
    m_cacheID = cacheID;
    m_time = height;
  }

  public boolean isLater(CacheHighWaterMark other) {
    return m_time >= ((StubCacheHighWaterMark)other).m_time;
  }

  public int hashCode() {
    return (int)m_time;
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (o == null || o.getClass() != getClass()) {
      return false;
    }

    return m_time == ((StubCacheHighWaterMark)o).m_time;
  }

  public long getTime() {
    return m_time;
  }

  public boolean isForSameCache(CacheHighWaterMark other) {
    return other != null &&
           other instanceof StubCacheHighWaterMark &&
           m_cacheID.equals(((StubCacheHighWaterMark)other).m_cacheID);
  }
}