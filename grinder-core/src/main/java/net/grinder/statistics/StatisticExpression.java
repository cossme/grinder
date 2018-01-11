// Copyright (C) 2000 - 2006 Philip Aston
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
 * Interface to apply a statistics calculation to a {@link
 * StatisticsSet}.
 *
 * @author Philip Aston
 * @see StatisticExpressionFactoryImplementation
 **/
public interface StatisticExpression {

  /**
   * Apply this {@link StatisticExpression} to the given {@link
   * StatisticsSet} and return the result as a <code>double</code>.
   *
   * @param statisticsSet A <code>StatisticsSet</code> value.
   * @return The result.
   **/
  double getDoubleValue(StatisticsSet statisticsSet);

  /**
   * Apply this {@link StatisticExpression} to the given {@link
   * StatisticsSet} and return the result as a <code>long</code>,
   * rounding as necessary.
   *
   * @param statisticsSet A <code>StatisticsSet</code> value.
   * @return The result.
   **/
  long getLongValue(StatisticsSet statisticsSet);

  /**
   * Returns <code>true</code> if the type of this {@link
   * StatisticExpression} is non integral. Callers might use this to
   * decide which accessor to call to ensure that information is not
   * lost, or how to format the result.
   *
   * @return a <code>boolean</code> value
   **/
  boolean isDouble();
}
