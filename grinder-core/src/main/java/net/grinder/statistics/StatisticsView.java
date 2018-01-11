// Copyright (C) 2000 - 2009 Philip Aston
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

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * An ordered collection of {@link ExpressionView}s.
 *
 * @author Philip Aston
 * @see net.grinder.script.Statistics#registerDataLogExpression
 * @see net.grinder.script.Statistics#registerSummaryExpression
 */
public final class StatisticsView {

  /**
   * We define a <code>Comparator</code> for {@link ExpressionView}s
   * rather than having the <code>ExpressionView</code> implement
   * <code>Comparable</code> because our sort order is inconsistent with equals.
   */
  private static final Comparator<ExpressionView> s_expressionViewComparator =
    new CreationOrderComparator();

  /**
   * We use this set to ensure that new views are unique. We can't
   * do this with a SortedSet because our sort order is inconsistent
   * with equals.
   */
  private final Set<ExpressionView> m_unique = new HashSet<ExpressionView>();

  private final SortedSet<ExpressionView> m_columns;

  /**
   * Creates a new <code>StatisticsView</code> instance.
   */
  public StatisticsView() {
    m_columns = new TreeSet<ExpressionView>(s_expressionViewComparator);
  }

  /**
   * Add all the {@link ExpressionView}s in <code>other</code> to
   * this <code>StatisticsView</code>.
   *
   * @param other Another <code>StatisticsView</code>.
   */
  public synchronized void add(StatisticsView other) {
    for (ExpressionView expressionView : other.m_columns) {
      add(expressionView);
    }
  }

  /**
   * Add the specified {@link ExpressionView} to this
   * <code>StatisticsView</code>.
   *
   * @param statistic An {@link ExpressionView}.
   */
  public synchronized void add(ExpressionView statistic) {
    if (!m_unique.contains(statistic)) {
      m_unique.add(statistic);
      m_columns.add(statistic);
    }
  }

  /**
   * Return our {@link ExpressionView}s as an array.
   *
   * @return The {@link ExpressionView}s.
   */
  public synchronized ExpressionView[] getExpressionViews() {
    return m_columns.toArray(new ExpressionView[m_columns.size()]);
  }

  /**
   * Package scope for unit tests.
   */
  static final class CreationOrderComparator
    implements Comparator<ExpressionView> {

    public int compare(ExpressionView viewA, ExpressionView viewB) {

      if (viewA.getCreationOrder() < viewB.getCreationOrder()) {
        return -1;
      }
      else if (viewA.getCreationOrder() > viewB.getCreationOrder()) {
        return 1;
      }
      else {
        // Should assert ? Same creation order => same instance.
        return 0;
      }
    }
  }
}
