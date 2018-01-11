// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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

package net.grinder.statistics;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.grinder.util.Serialiser;


/**
 * Factory for {@link StatisticsSet} objects.
 *
 * @author Philip Aston
 */
public final class StatisticsSetFactory {

  private final Serialiser m_serialiser = new Serialiser();
  private final StatisticsIndexMap m_statisticsIndexMap;

  StatisticsSetFactory(StatisticsIndexMap statisticsIndexMap) {
    m_statisticsIndexMap = statisticsIndexMap;
  }

  /**
   * Factory method.
   *
   * @return A new <code>StatisticsSet</code>.
   */
  public StatisticsSet create() {
    return new StatisticsSetImplementation(m_statisticsIndexMap);
  }

  void writeStatisticsExternal(ObjectOutput out,
                               StatisticsSetImplementation statistics)
    throws IOException {
    statistics.writeExternal(out, m_serialiser);
  }

  StatisticsSet readStatisticsExternal(ObjectInput in) throws IOException {
    return new StatisticsSetImplementation(m_statisticsIndexMap,
                                           in,
                                           m_serialiser);
  }
}
