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

package net.grinder.statistics;


/**
 * Factory for StatisticExpressions.
 *
 * @author Philip Aston
 */
public interface StatisticExpressionFactory {

  /**
   * Apply standard formatting to an expression.
   *
   * @param expression The expression.
   * @return The formatted expression.
   * @exception StatisticsException If the expression is invalid.
   */
  String normaliseExpressionString(String expression)
    throws StatisticsException;

  /**
   * Parse an expression.
   *
   * @param expression The expression.
   * @return The parsed expression.
   * @exception StatisticsException If the expression is invalid.
   */
  StatisticExpression createExpression(String expression)
    throws StatisticsException;

  /**
   * Create a constant long expression.
   *
   * @param value The value.
   * @return The <code>StatisticExpression</code>.
   */
  StatisticExpression createConstant(long value);

  /**
   *  Create a constant float expression.
   *
   * @param value The value.
   * @return The <code>StatisticExpression</code>.
   */
  StatisticExpression createConstant(double value);

  /**
   * Create a primitive double expression.
   *
   * @param index The expression index.
   * @return The <code>StatisticExpression</code>.
   */
  StatisticExpression createPrimitive(StatisticsIndexMap.DoubleIndex index);

  /**
   * Create a primitive long expression.
   *
   * @param index The expression index.
   * @return The <code>StatisticExpression</code>.
   */
  StatisticExpression createPrimitive(StatisticsIndexMap.LongIndex index);

  /**
   * Create a sum.
   *
   * @param operands The things to add.
   * @return The resulting expression.
   */
  StatisticExpression createSum(StatisticExpression[] operands);

  /**
   * Create a negation.
   *
   * @param operand The thing to negate.
   * @return The resulting expression.
   */
  StatisticExpression createNegation(StatisticExpression operand);

  /**
   * Create a minus expression. The result is the first argument less the
   * sum of the remaining arguments.
   *
   * @param firstOperand The first argument.
   * @param otherOperands The remaining arguments.
   * @return The resulting expression.
   */
  StatisticExpression createMinus(StatisticExpression firstOperand,
                                  StatisticExpression[] otherOperands);

  /**
   * Create a product.
   *
   * @param operands The things to multiply.
   * @return The resulting expression.
   */
  StatisticExpression createProduct(StatisticExpression[] operands);

  /**
   * Create a division.
   *
   * @param numerator The numerator.
   * @param denominator The denominator.
   * @return The resulting expression.
   */
  StatisticExpression createDivision(StatisticExpression numerator,
                                     StatisticExpression denominator);

  /**
   * Create a square root.
   *
   * @param operand The operand.
   * @return The resulting expression.
   */
  StatisticExpression createSquareRoot(StatisticExpression operand);

  /**
   * Create a peak double statistic.
   *
   * @param peakIndex Index of a slot to store peak information in.
   * @param monitoredStatistic Statistic to monitor.
   * @return The resulting expression.
   */
  PeakStatisticExpression createPeak(StatisticsIndexMap.DoubleIndex peakIndex,
                                     StatisticExpression monitoredStatistic);

  /**
   * Create a peak long statistic.
   *
   * @param peakIndex Index of a slot to store peak information in.
   * @param monitoredStatistic Statistic to monitor.
   * @return The resulting expression.
   */
  PeakStatisticExpression createPeak(StatisticsIndexMap.LongIndex peakIndex,
                                     StatisticExpression monitoredStatistic);

  /**
   * Creates a new <code>ExpressionView</code> instance.
   *
   * @param displayName
   *          A display name. In the console, this is converted to a key for an
   *          internationalised resource bundle look up by prefixing the string
   *          with "statistic." and replacing any whitespace with underscores.
   * @param expressionString
   *          An expression string, used to create the
   *          {@link StatisticExpression} for the <code>ExpressionView</code>.
   * @param showForCompositeStatistics
   *          Whether the expression applies to composite statistics.
   * @exception StatisticsException
   *              If the expression is invalid.
   * @return The ExpressionView.
   */
  ExpressionView createExpressionView(String displayName,
                                      String expressionString,
                                      boolean showForCompositeStatistics)
    throws StatisticsException;

  /**
   * Creates a new <code>ExpressionView</code> instance.
   *
   * <p>
   * This method takes a {@link StatisticExpression}, and is used to by
   * the console to construct a view around expressions that have no string
   * representation (namely, those involving peak statistics).
   * </p>
   *
   * @param displayName
   *          A common display name.
   * @param expression
   *          A {@link StatisticExpression}.
   * @return The ExpressionView.
   */
  ExpressionView createExpressionView(String displayName,
                                      StatisticExpression expression);
}
