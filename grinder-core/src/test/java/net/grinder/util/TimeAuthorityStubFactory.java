// Copyright (C) 2006 - 2013 Philip Aston
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

package net.grinder.util;

import java.util.ArrayList;
import java.util.List;

import net.grinder.common.TimeAuthority;
import net.grinder.testutility.RandomStubFactory;


public class TimeAuthorityStubFactory extends RandomStubFactory<TimeAuthority> {

  public TimeAuthorityStubFactory() {
    super(TimeAuthority.class);
  }

  private long m_lastTime;
  private final List<Long> m_nextTimes = new ArrayList<Long>();

  public void nextTime(final long time) {
    m_nextTimes.add(new Long(time));
  }

  public long override_getTimeInMilliseconds(final Object proxy) {

    if (m_nextTimes.size() != 0) {
      m_lastTime = m_nextTimes.remove(0).longValue();
    }

    return m_lastTime;
  }
}
