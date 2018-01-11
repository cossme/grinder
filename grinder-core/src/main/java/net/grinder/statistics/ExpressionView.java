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


/**
 * <p>
 * Associate a statistic expression with display information.
 * </p>
 *
 * <p>
 * Statistic expressions are composed of statistic names (see
 * {@link StatisticsIndexMap}) in a simple post-fix format using the symbols
 * <code>+</code>, <code>-</code>, <code>/</code> and <code>*</code>,
 * which have their usual meanings, in conjunction with simple statistic names
 * or sub-expressions. Precedence can be controlled by grouping expressions in
 * parentheses. For example, the error rate is
 * <code>(* (/ errors period) 1000)</code> errors per second.
 * </p>
 *
 * <p>
 * Sample statistics, such as <em>timedTests</em>, must be introduced with
 * one of <code>sum</code>, <code>count</code>, or <code>variance</code>,
 * depending on the attribute of interest.
 * </p>
 *
 * <p>
 * For example, the statistic expression <code>(/ (sum timedTests)
 * (count timedTests))</code>
 * represents the mean test time in milliseconds.
 *
 * @author Philip Aston
 */
public final class ExpressionView {

  private static int s_creationOrder;

  private final String m_displayName;
  private final String m_expressionString;
  private final boolean m_showForCompositeStatistics;
  private final int m_hashCode;
  private final int m_creationOrder;

  private final StatisticExpression m_expression;

  ExpressionView(String displayName,
                 String expressionString,
                 StatisticExpression expression,
                 boolean showForCompositeStatistics) {
    m_displayName = displayName;
    m_expressionString = expressionString;
    m_showForCompositeStatistics = showForCompositeStatistics;
    m_expression = expression;

    m_hashCode =
      m_displayName.hashCode() ^
      (expressionString != null ? m_expressionString.hashCode() : 0);

    // Code outside this package can only obtain ExpressionViews through a
    // StatisticExpressionFactory instance, and in turn this factory must be
    // obtained from the singleton StatisticServices instance.
    // StatisticsServicesImplementation creates the common statistics views in
    // a static block, so we can rely on creation order to ensure that the
    // standard views are ordered before script defined views.
    synchronized (ExpressionView.class) {
      m_creationOrder = s_creationOrder++;
    }
  }

  /**
   * Get the common display name.
   *
   * @return The display name.
   */
  public String getDisplayName() {
    return m_displayName;
  }

  /**
   * Return the {@link StatisticExpression}.
   *
   * @return The {@link StatisticExpression}.
   */
  public StatisticExpression getExpression() {
    return m_expression;
  }

  /**
   * Return the expression string.
   *
   * @return The string, or <code>null</code> if this view was built directly
   *         from a {@link StatisticExpression}.
   */
  public String getExpressionString() {
    return m_expressionString;
  }

  /**
   * Return whether this view is relevant for totals of composite statistics.
   * Many views (particularly those representing aggregate time, or averages)
   * are ambiguous for composite statistics, so we don't show them.
   *
   * @return <code>true</code> => show this view for composite statistics.
   */
  public boolean getShowForCompositeStatistics() {
    return m_showForCompositeStatistics;
  }

  /**
   * Value based equality.
   *
   * @param other An <code>Object</code> to compare.
   * @return <code>true</code> => <code>other</code> is equal to this object.
   */
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }

    if (other == null || other.getClass() != ExpressionView.class) {
      return false;
    }

    final ExpressionView otherView = (ExpressionView)other;

    return
      m_hashCode == otherView.m_hashCode &&
      m_displayName.equals(otherView.m_displayName) &&

      // If either expression string is null, one of the views
      // is not externalisable. If so, we only compare on the
      // display names.
      (m_expressionString == null ||
       otherView.m_expressionString == null ||
       m_expressionString.equals(otherView.m_expressionString));
  }

  /**
   * Implement {@link Object#hashCode}.
   *
   * @return an <code>int</code> value
   */
  public int hashCode() {
    return m_hashCode;
  }

  /**
   * Return a <code>String</code> representation of this
   * <code>ExpressionView</code>.
   *
   * @return The <code>String</code>
   */
  public String toString() {
    final StringBuilder result = new StringBuilder(32);
    result.append("ExpressionView(");
    result.append(m_displayName);

    if (m_expressionString != null) {
      result.append(", ");
      result.append(m_expressionString);
    }

    result.append(")");

    return result.toString();
  }

  int getCreationOrder() {
    return m_creationOrder;
  }
}
