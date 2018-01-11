// Copyright (C) 2005 Philip Aston
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
 * Statistics services.
 *
 * @author Philip Aston
 * @see StatisticsServicesImplementation
 */
public interface StatisticsServices {

  /**
   * Get the common detail {@link StatisticsView}.
   *
   * @return The {@link StatisticsView}.
   */
  StatisticsView getDetailStatisticsView();

  /**
   * Get the common summary {@link StatisticsView}.
   *
   * @return The {@link StatisticsView}.
   */
  StatisticsView getSummaryStatisticsView();

  /**
   * Return a {@link StatisticExpression} factory.
   *
   * @return A {@link StatisticExpressionFactoryImplementation}.
   */
  StatisticExpressionFactory getStatisticExpressionFactory();

  /**
   * Return a {@link StatisticsSet} factory.
   *
   * @return A {@link StatisticExpressionFactoryImplementation}.
   */
  StatisticsSetFactory getStatisticsSetFactory();

  /**
   * Return the {@link StatisticsIndexMap} for the current process.
   *
   * @return The {@link StatisticsIndexMap}.
   */
  StatisticsIndexMap getStatisticsIndexMap();

  /**
   * Return an object allowing access to common functions of test statistics.
   *
   * @return The {@link TestStatisticsQueries}.
   */
  TestStatisticsQueries getTestStatisticsQueries();

  /**
   * Return the TPS expression.
   *
   * @return The TPS expression.
   */
  StatisticExpression getTPSExpression();
}
