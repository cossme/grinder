// Copyright (C) 2000 - 2011 Philip Aston
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

import java.util.ArrayList;
import java.util.List;

import net.grinder.statistics.StatisticsIndexMap.DoubleIndex;
import net.grinder.statistics.StatisticsIndexMap.DoubleSampleIndex;
import net.grinder.statistics.StatisticsIndexMap.LongIndex;
import net.grinder.statistics.StatisticsIndexMap.LongSampleIndex;


/**
 * Factory for StatisticExpressions.
 *
 * @author Philip Aston
 */
final class StatisticExpressionFactoryImplementation
  implements StatisticExpressionFactory {

  private final StatisticsIndexMap m_indexMap;

  StatisticExpressionFactoryImplementation(
    StatisticsIndexMap statisticsIndexMap) {
    m_indexMap = statisticsIndexMap;
  }

  /**
   * Apply standard formatting to an expression.
   *
   * @param expression The expression.
   * @return The formatted expression.
   * @exception StatisticsException If the expression is invalid.
   */
  public String normaliseExpressionString(String expression)
    throws StatisticsException {
    final ParseContext parseContext = new ParseContext(expression);
    final StringBuilder result = new StringBuilder(expression.length());

    normaliseExpressionString(parseContext, result);

    if (parseContext.hasMoreCharacters()) {
      throw parseContext.createParseException("Additional characters found");
    }

    return result.toString();
  }

  private void normaliseExpressionString(ParseContext parseContext,
                                         StringBuilder result)
    throws StatisticsException {
    if (parseContext.peekCharacter() == '(') {
      // Compound expression.
      result.append(parseContext.readCharacter());
      result.append(parseContext.readToken());

      while (parseContext.peekCharacter() != ')') {
        result.append(' ');
        normaliseExpressionString(parseContext, result);
      }

      result.append(parseContext.readCharacter());
    }
    else {
      result.append(parseContext.readToken());
    }
  }

  /**
   * Parse an expression.
   *
   * @param expression The expression.
   * @return The parsed expression.
   * @exception StatisticsException If the expression is invalid.
   */
  public StatisticExpression createExpression(String expression)
    throws StatisticsException {

    final ParseContext parseContext = new ParseContext(expression);

    final StatisticExpression result = readExpression(parseContext);

    if (parseContext.hasMoreCharacters()) {
      throw parseContext.createParseException("Additional characters found");
    }

    return result;
  }

  private StatisticExpression readExpression(ParseContext parseContext)
    throws ParseContext.ParseException {

    if (parseContext.peekCharacter() == '(') {
      parseContext.readCharacter();

      final String operation = parseContext.readToken();
      final StatisticExpression result;

      if ("+".equals(operation)) {
        result = createSum(readOperands(parseContext));
      }
      else if ("-".equals(operation)) {

        final StatisticExpression firstOperand = readExpression(parseContext);
        final StatisticExpression[] others = readOperands(parseContext);

        if (others.length == 0) {
          result = createNegation(firstOperand);
        }
        else {
          result = createMinus(firstOperand, others);
        }
      }
      else if ("*".equals(operation)) {
        result = createProduct(readOperands(parseContext));
      }
      else if ("/".equals(operation)) {
        result = createDivision(readExpression(parseContext),
                                readExpression(parseContext));
      }
      else if ("sum".equals(operation)) {
        result = createSampleSum(parseContext);
      }
      else if ("count".equals(operation)) {
        result = createSampleCount(parseContext);
      }
      else if ("variance".equals(operation)) {
        result = createSampleVariance(parseContext);
      }
      else if ("sqrt".equals(operation)) {
        result = createSquareRoot(readExpression(parseContext));
      }
      else {
        throw parseContext.createParseException(
          "Unknown operation '" + operation + "'");
      }

      if (parseContext.readCharacter() != ')') {
        throw parseContext.createParseException("Expecting ')'");
      }

      return result;
    }
    else {
      final String token = parseContext.readToken();

      try {
        return createConstant(Long.parseLong(token));
      }
      catch (NumberFormatException e) {
        try {
          return createConstant(Double.parseDouble(token));
        }
        catch (NumberFormatException e2) {
          final LongIndex longIndex = m_indexMap.getLongIndex(token);

          if (longIndex != null) {
            return createPrimitive(longIndex);
          }

          final DoubleIndex doubleIndex = m_indexMap.getDoubleIndex(token);

          if (doubleIndex != null) {
            return createPrimitive(doubleIndex);
          }
        }
      }

      throw parseContext.createParseException("Unknown token '" + token + "'");
    }
  }

  /**
   * Create a constant long expression.
   *
   * @param value The value.
   * @return The <code>StatisticExpression</code>.
   */
  public StatisticExpression createConstant(final long value) {
    return new LongStatistic() {
        public long getValue(StatisticsSet statisticsSet) {
          return value;
        }
      };
  }

  /**
   *  Create a constant float expression.
   *
   * @param value The value.
   * @return The <code>StatisticExpression</code>.
   */
  public StatisticExpression createConstant(final double value) {
    return new DoubleStatistic() {
        public double getValue(StatisticsSet statisticsSet) {
          return value;
        }
      };
  }

  /**
   * Create a primitive double expression.
   *
   * @param index The expression index.
   * @return The <code>StatisticExpression</code>.
   */
  public StatisticExpression createPrimitive(DoubleIndex index) {
    return new PrimitiveDoubleStatistic(index);
  }

  /**
   * Create a primitive long expression.
   *
   * @param index The expression index.
   * @return The <code>StatisticExpression</code>.
   */
  public StatisticExpression createPrimitive(LongIndex index) {
    return new PrimitiveLongStatistic(index);
  }

  /**
   * Create a sum.
   *
   * @param operands The things to add.
   * @return The resulting expression.
   */
  public StatisticExpression createSum(final StatisticExpression[] operands) {

    return new FoldArgumentsExpressionFactory(0, operands) {
        public double doDoubleOperation(
          double result, StatisticExpression operand,
          StatisticsSet statisticsSet) {
          return result + operand.getDoubleValue(statisticsSet);
        }

        public long doLongOperation(
          long result, StatisticExpression operand,
          StatisticsSet statisticsSet) {
          return result + operand.getLongValue(statisticsSet);
        }
      }
      .getExpression();
  }

  /**
   * Create a negation.
   *
   * @param operand The thing to negate.
   * @return The resulting expression.
   */
  public StatisticExpression createNegation(final StatisticExpression operand) {

    if (operand.isDouble()) {
      return new DoubleStatistic() {
        protected double getValue(StatisticsSet statisticsSet) {
          return -operand.getDoubleValue(statisticsSet);
        }
      };
    }
    else {
      return new LongStatistic() {
        protected long getValue(StatisticsSet statisticsSet) {
          return -operand.getLongValue(statisticsSet);
        }
      };
    }
  }

  /**
   * Create a minus expression. The result is the first argument less the
   * sum of the remaining arguments.
   *
   * @param firstOperand The first argument.
   * @param otherOperands The remaining arguments.
   * @return The resulting expression.
   */
  public StatisticExpression createMinus(
    final StatisticExpression firstOperand,
    final StatisticExpression[] otherOperands) {

    return new FoldArgumentsExpressionFactory(firstOperand, otherOperands) {
      public double doDoubleOperation(double result,
                                      StatisticExpression operand,
                                      StatisticsSet statisticsSet) {
        return result - operand.getDoubleValue(statisticsSet);
      }

      public long doLongOperation(long result,
                                  StatisticExpression operand,
                                  StatisticsSet statisticsSet) {
        return result - operand.getLongValue(statisticsSet);
      }
    }
    .getExpression();
  }


  /**
   * Create a product.
   *
   * @param operands The things to multiply.
   * @return The resulting expression.
   */
  public StatisticExpression
    createProduct(final StatisticExpression[] operands) {

    return new FoldArgumentsExpressionFactory(1, operands) {
        public double doDoubleOperation(
          double result, StatisticExpression operand,
          StatisticsSet statisticsSet) {
          return result * operand.getDoubleValue(statisticsSet);
        }

        public long doLongOperation(
          long result, StatisticExpression operand,
          StatisticsSet statisticsSet) {
          return result * operand.getLongValue(statisticsSet);
        }
      }
      .getExpression();
  }

  /**
   * Create a division.
   *
   * @param numerator The numerator.
   * @param denominator The denominator.
   * @return The resulting expression.
   */
  public StatisticExpression
    createDivision(final StatisticExpression numerator,
                   final StatisticExpression denominator) {

    return new DoubleStatistic() {
        public double getValue(StatisticsSet statisticsSet) {
          return
            numerator.getDoubleValue(statisticsSet) /
            denominator.getDoubleValue(statisticsSet);
        }
      };
  }

  /**
   * Create an accessor for a sample's sum attribute.
   *
   * <p>
   * It might look like there's an easy abstraction across createSampleSum(),
   * createSampleCount(), and createSampleVariance(); but it isn't so because of
   * type issues. The count is a long for both DoubleSampleStatistics and
   * LongSampleStatistics; the variance is a double, and the sum is a double for
   * a DoubleStatistics and a long for a LongSampleStatistics.
   * </p>
   *
   * @param parseContext
   *            The parse context.
   * @return The resulting expression.
   * @throws ParseException
   *             If the parse failed.
   */
  private StatisticExpression createSampleSum(ParseContext parseContext)
    throws ParseContext.ParseException {

    final StatisticExpression result;

    final String token = parseContext.readToken();

    final DoubleSampleIndex doubleSampleIndex =
      m_indexMap.getDoubleSampleIndex(token);

    if (doubleSampleIndex != null) {
      result = createPrimitive(doubleSampleIndex.getSumIndex());
    }
    else {
      final LongSampleIndex longSampleIndex =
        m_indexMap.getLongSampleIndex(token);

      if (longSampleIndex != null) {
        result = createPrimitive(longSampleIndex.getSumIndex());
      }
      else {
        throw parseContext.createParseException(
          "Can't apply sum to unknown sample index '" + token + "'");
      }
    }

    return result;
  }

  /**
   * Create an accessor for a sample's count attribute.
   *
   * @param parseContext The parse context.
   * @return The resulting expression.
   * @throws ParseException If the parse failed.
   */
  private StatisticExpression createSampleCount(ParseContext parseContext)
    throws ParseContext.ParseException {

    final StatisticExpression result;

    final String token = parseContext.readToken();

    final DoubleSampleIndex doubleSampleIndex =
      m_indexMap.getDoubleSampleIndex(token);

    if (doubleSampleIndex != null) {
      result = createPrimitive(doubleSampleIndex.getCountIndex());
    }
    else {
      final LongSampleIndex longSampleIndex =
        m_indexMap.getLongSampleIndex(token);

      if (longSampleIndex != null) {
        result = createPrimitive(longSampleIndex.getCountIndex());
      }
      else {
        throw parseContext.createParseException(
          "Can't apply count to unknown sample index '" + token + "'");
      }
    }

    return result;
  }

  /**
   * Create an accessor for a sample's variance attribute.
   *
   * @param parseContext The parse context.
   * @return The resulting expression.
   * @throws ParseException If the parse failed.
   */
  private StatisticExpression createSampleVariance(ParseContext parseContext)
    throws ParseContext.ParseException {

    final StatisticExpression result;

    final String token = parseContext.readToken();

    final StatisticsIndexMap.DoubleSampleIndex doubleSampleIndex =
      m_indexMap.getDoubleSampleIndex(token);

    if (doubleSampleIndex != null) {
      result = createPrimitive(doubleSampleIndex.getVarianceIndex());
    }
    else {
      final StatisticsIndexMap.LongSampleIndex longSampleIndex =
        m_indexMap.getLongSampleIndex(token);

      if (longSampleIndex != null) {
        result = createPrimitive(longSampleIndex.getVarianceIndex());
      }
      else {
        throw parseContext.createParseException(
            "Can't apply variance to unknown sample index '" + token + "'");
      }
    }

    return result;
  }

  /**
   * Create a square root.
   *
   * @param operand The operand.
   * @return The resulting expression.
   */
  public StatisticExpression
    createSquareRoot(final StatisticExpression operand) {

    return new DoubleStatistic() {
        public double getValue(StatisticsSet statisticsSet) {
          return Math.sqrt(operand.getDoubleValue(statisticsSet));
        }
      };
  }

  /**
   * Create a peak double statistic.
   *
   * @param peakIndex Index of a slot to store peak information in.
   * @param monitoredStatistic Statistic to monitor.
   * @return The resulting expression.
   */
  public PeakStatisticExpression
    createPeak(DoubleIndex peakIndex,
               StatisticExpression monitoredStatistic) {
    return new PeakDoubleStatistic(peakIndex, monitoredStatistic);
  }

  /**
   * Create a peak long statistic.
   *
   * @param peakIndex Index of a slot to store peak information in.
   * @param monitoredStatistic Statistic to monitor.
   * @return The resulting expression.
   */
  public PeakStatisticExpression
    createPeak(LongIndex peakIndex,
               StatisticExpression monitoredStatistic) {
    return new PeakLongStatistic(peakIndex, monitoredStatistic);
  }

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
   * @exception StatisticsException
   *              If the expression is invalid.
   * @return The ExpressionView.
   */
  public ExpressionView createExpressionView(String displayName,
                                             String expressionString,
                                             boolean showForCompositeStatistics)
    throws StatisticsException {

    return new ExpressionView(displayName,
                              normaliseExpressionString(expressionString),
                              createExpression(expressionString),
                              showForCompositeStatistics);
  }

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
  public ExpressionView createExpressionView(String displayName,
                                             StatisticExpression expression) {
    return new ExpressionView(displayName, null, expression, false);
  }

  private StatisticExpression[] readOperands(ParseContext parseContext)
    throws ParseContext.ParseException {
    final List<StatisticExpression> arrayList =
      new ArrayList<StatisticExpression>();

    while (parseContext.peekCharacter() != ')') {
      arrayList.add(readExpression(parseContext));
    }

    return arrayList.toArray(new StatisticExpression[arrayList.size()]);
  }

  private abstract static class DoubleStatistic
    implements StatisticExpression {
    public final double getDoubleValue(StatisticsSet statisticsSet) {
      return getValue(statisticsSet);
    }

    public final long getLongValue(StatisticsSet statisticsSet) {
      return (long)getValue(statisticsSet);
    }

    public final boolean isDouble() {
      return true;
    }

    protected abstract double getValue(StatisticsSet statisticsSet);
  }

  private static class PrimitiveDoubleStatistic extends DoubleStatistic {

    private final DoubleIndex m_index;

    public PrimitiveDoubleStatistic(DoubleIndex index) {
      m_index = index;
    }

    public final double getValue(StatisticsSet statisticsSet) {
      return statisticsSet.getValue(m_index);
    }

    protected final void setValue(StatisticsSet statisticsSet, double value) {
      statisticsSet.setValue(m_index, value);
    }
  }

  private static class PeakDoubleStatistic
    extends PrimitiveDoubleStatistic implements PeakStatisticExpression {

    private final StatisticExpression m_monitoredStatistic;

    public PeakDoubleStatistic(DoubleIndex peakIndex,
                               StatisticExpression monitoredStatistic) {
      super(peakIndex);
      m_monitoredStatistic = monitoredStatistic;
    }

    public void update(StatisticsSet monitoredStatistics,
                       StatisticsSet peakStorageStatistics) {
      setValue(peakStorageStatistics,
               Math.max(getValue(peakStorageStatistics),
                        m_monitoredStatistic.getDoubleValue(
                          monitoredStatistics)));
    }
  }

  private abstract static class LongStatistic implements StatisticExpression {

    public final double getDoubleValue(StatisticsSet statisticsSet) {
      return getValue(statisticsSet);
    }

    public final long getLongValue(StatisticsSet statisticsSet) {
      return getValue(statisticsSet);
    }

    public final boolean isDouble() {
      return false;
    }

    protected abstract long getValue(StatisticsSet statisticsSet);
  }

  private static class PrimitiveLongStatistic extends LongStatistic {

    private final LongIndex m_index;

    public PrimitiveLongStatistic(LongIndex index) {
      m_index = index;
    }

    public final long getValue(StatisticsSet statisticsSet) {
      return statisticsSet.getValue(m_index);
    }

    protected final void setValue(StatisticsSet statisticsSet, long value) {
      statisticsSet.setValue(m_index, value);
    }
  }

  private static class PeakLongStatistic
    extends PrimitiveLongStatistic implements PeakStatisticExpression {
    private final StatisticExpression m_monitoredStatistic;

    public PeakLongStatistic(LongIndex peakIndex,
                             StatisticExpression monitoredStatistic) {
      super(peakIndex);
      m_monitoredStatistic = monitoredStatistic;
    }

    public void update(StatisticsSet monitoredStatistics,
                       StatisticsSet peakStorageStatistics) {
      setValue(peakStorageStatistics,
               Math.max(getValue(peakStorageStatistics),
                        m_monitoredStatistic.getLongValue(
                          monitoredStatistics)));
    }
  }

  private abstract class FoldArgumentsExpressionFactory {

    private final StatisticExpression m_expression;

    public FoldArgumentsExpressionFactory(
      double initialValue, final StatisticExpression[] operands) {
      this(createConstant(initialValue), operands);
    }

    public FoldArgumentsExpressionFactory(
      final StatisticExpression initialValue,
      final StatisticExpression[] operands) {

      boolean doubleResult = false;

      for (int i = 0; i < operands.length && !doubleResult; ++i) {
        if (operands[i].isDouble()) {
          doubleResult = true;
        }
      }

      if (doubleResult) {
        m_expression = new DoubleStatistic() {
            public final double getValue(
              StatisticsSet statisticsSet) {
              double result = initialValue.getDoubleValue(statisticsSet);

              for (int i = 0; i < operands.length; ++i) {
                result = doDoubleOperation(result, operands[i], statisticsSet);
              }

              return result;
            }
          };
      }
      else {
        m_expression = new LongStatistic() {
            public final long getValue(
              StatisticsSet statisticsSet) {
              long result = initialValue.getLongValue(statisticsSet);

              for (int i = 0; i < operands.length; ++i) {
                result = doLongOperation(result, operands[i], statisticsSet);
              }

              return result;
            }
          };
      }
    }

    protected abstract double
      doDoubleOperation(double result, StatisticExpression operand,
                        StatisticsSet statisticsSet);

    protected abstract long
      doLongOperation(long result, StatisticExpression operand,
                      StatisticsSet statisticsSet);

    final StatisticExpression getExpression() {
      return m_expression;
    }
  }

  /**
   * Package scope for unit tests.
   */
  static final class ParseContext {

    private static final char EOS_SENTINEL = 0;
    private final char[] m_expression;
    private int m_index;

    public ParseContext(String expression) {
      m_expression = expression.toCharArray();
      m_index = 0;
    }

    public boolean hasMoreCharacters() {
      eatWhiteSpace();
      return m_index < m_expression.length;
    }

    public char peekCharacter() {
      eatWhiteSpace();
      return peekCharacterNoEat();
    }

    private char peekCharacterNoEat() {
      if (m_index >= m_expression.length) {
        return EOS_SENTINEL;
      }

      return m_expression[m_index];
    }

    public char readCharacter() {
      final char result = peekCharacter();

      if (result != EOS_SENTINEL) {
        ++m_index;
      }

      return result;
    }

    public String readToken() throws ParseException {
      eatWhiteSpace();

      final int start = m_index;

      while (isTokenCharacter(peekCharacterNoEat())) {
        ++m_index;
      }

      final int stringLength = m_index - start;

      if (stringLength == 0) {
        throw createParseException("Expected a token", start);
      }

      return new String(m_expression, start, stringLength);
    }

    private boolean isTokenCharacter(char c) {
      return
        c != EOS_SENTINEL &&
        c != '(' &&
        c != ')' &&
        !Character.isWhitespace(c);
    }

    private void eatWhiteSpace() {
      while (Character.isWhitespace(peekCharacterNoEat())) {
        ++m_index;
      }
    }

    private ParseException createParseException(String message) {
      return createParseException(message, m_index);
    }

    private ParseException createParseException(String message, int where) {
      return new ParseException(message, new String(m_expression), m_index);
    }

    /**
     * Exception representing a failure to parse an expression.
     */
    static final class ParseException extends StatisticsException {
      public ParseException(String message, String expression, int where) {
        super("Parse exception: " + message + ", at character " + where +
              " of '" + expression + "'");
      }
    }
  }
}
