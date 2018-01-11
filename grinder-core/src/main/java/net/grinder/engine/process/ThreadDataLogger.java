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

package net.grinder.engine.process;

import net.grinder.common.Test;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticExpression;
import net.grinder.statistics.StatisticsSet;

import org.slf4j.Logger;


/**
 * Writes lines to the data log on behalf of a particular thread.
 *
 * @author Philip Aston
 */
class ThreadDataLogger {
  private static final String SEPARATOR = ", ";

  private final Logger m_dataLog;
  private final ExpressionView[] m_expressionViews;
  private final int m_threadNumber;

  private final StringBuilder m_buffer = new StringBuilder();
  private final int m_bufferAfterThreadIDIndex;

  private int m_bufferAfterRunNumberIndex = -1;
  private int m_lastRunNumber = -1;

  public ThreadDataLogger(Logger dataLog,
                          ExpressionView[] expressionViews,
                          int threadNumber) {
    m_dataLog = dataLog;
    m_expressionViews = expressionViews;
    m_threadNumber = threadNumber;

    m_buffer.append(threadNumber);
    m_buffer.append(SEPARATOR);
    m_bufferAfterThreadIDIndex = m_buffer.length();
  }

  public void report(int runNumber,
                     Test test,
                     long timeSinceExecutionStart,
                     StatisticsSet statistics) {

    if (runNumber == m_lastRunNumber && m_lastRunNumber != -1) {
      m_buffer.setLength(m_bufferAfterRunNumberIndex);
    }
    else {
      m_lastRunNumber = runNumber;

      m_buffer.setLength(m_bufferAfterThreadIDIndex);
      m_buffer.append(runNumber);
      m_buffer.append(SEPARATOR);
      m_bufferAfterRunNumberIndex = m_buffer.length();
    }

    m_buffer.append(test.getNumber());

    m_buffer.append(SEPARATOR);
    m_buffer.append(timeSinceExecutionStart);

    for (ExpressionView expressionView : m_expressionViews) {
      m_buffer.append(SEPARATOR);

      final StatisticExpression expression = expressionView.getExpression();

      if (expression.isDouble()) {
        m_buffer.append(expression.getDoubleValue(statistics));
      }
      else {
        m_buffer.append(expression.getLongValue(statistics));
      }
    }

    m_dataLog.info(m_buffer.toString(),
                   new DataLogArguments(m_threadNumber,
                                        runNumber,
                                        test,
                                        timeSinceExecutionStart,
                                        statistics));
  }
}
