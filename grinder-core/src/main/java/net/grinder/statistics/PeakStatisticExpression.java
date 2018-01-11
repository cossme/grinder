// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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


/**
 * A {@link StatisticExpression} that tracks the peak value of another
 * {@link StatisticExpression}. The monitored {@link
 * StatisticExpression} is specified when the {@link
 * PeakStatisticExpression} is created, see {@link
 * StatisticExpressionFactoryImplementation}.
 *
 * @author Philip Aston
 * @see StatisticExpressionFactoryImplementation
 **/
public interface PeakStatisticExpression extends StatisticExpression {
  /**
   * When called, the peak value of monitored expression applied to
   * <code>monitoredStatistics</code> is calculated and stored in the
   * given <code>peakStorageStatistics</code>.
   *
   * @param monitoredStatistics The monitored <code>StatisticsSet</code>.
   * @param peakStorageStatistics The <code>StatisticsSet</code> in
   * which to store the result.
   */
  void update(StatisticsSet monitoredStatistics,
              StatisticsSet peakStorageStatistics);
}
