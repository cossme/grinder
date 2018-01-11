// Copyright (C) 2000 - 2008 Philip Aston
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

package net.grinder.messages.console;

import net.grinder.communication.Message;
import net.grinder.statistics.TestStatisticsMap;


/**
 * Message used to report test statistics to the console.
 *
 * @author Philip Aston
 */
public final class ReportStatisticsMessage implements Message {

  private static final long serialVersionUID = 5659643598627115683L;

  private final TestStatisticsMap m_statisticsDelta;

  /**
   * Constructor.
   *
   * @param statisticsDelta The test statistics.
   */
  public ReportStatisticsMessage(TestStatisticsMap statisticsDelta) {
    m_statisticsDelta = statisticsDelta;
  }

  /**
   * Get the test statistics.
   *
   * @return The test statistics.
   */
  public TestStatisticsMap getStatisticsDelta() {
    return m_statisticsDelta;
  }
}
