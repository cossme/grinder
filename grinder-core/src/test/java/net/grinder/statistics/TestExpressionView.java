// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2013 Philip Aston
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;



/**
 * Unit test case for {@link ExpressionView}
 *
 * @author Philip Aston
 */
public class TestExpressionView  {

  @Test
  public void testConstruction() throws Exception {
    final StatisticExpressionFactory statisticExpressionFactory =
      StatisticsServicesImplementation.getInstance()
      .getStatisticExpressionFactory();

    final ExpressionView view =
      statisticExpressionFactory.createExpressionView(
        "My view", "(+ userLong0 userLong1)", false);

    assertEquals("My view", view.getDisplayName());
    assertTrue(view.getExpression() != null);

    final ExpressionView view2 =
      statisticExpressionFactory
        .createExpressionView("My view2", statisticExpressionFactory.createExpression(
           "userLong0"));

    assertEquals("My view2", view2.getDisplayName());
    assertTrue(view.getExpression() != null);
  }

  @Test
  public void testEquality() throws Exception {
    final StatisticExpressionFactory statisticExpressionFactory =
      StatisticsServicesImplementation.getInstance()
      .getStatisticExpressionFactory();

    final ExpressionView[] views = {
      statisticExpressionFactory.createExpressionView("My view", "(+ userLong0 userLong1)", false),
      statisticExpressionFactory.createExpressionView("My view", "(+ userLong0 userLong1)", false),
      statisticExpressionFactory.createExpressionView("My view", "(+ userLong0 userLong2)", false),
      statisticExpressionFactory.createExpressionView("My View", "(+ userLong0 userLong1)", false),
    };

    assertEquals(views[0], views[0]);
    assertEquals(views[0], views[1]);
    assertEquals(views[1], views[0]);
    assertTrue(!views[0].equals(views[2]));
    assertTrue(!views[1].equals(views[3]));

    assertTrue(!views[0].equals(new Object()));
  }

  @Test
  public void testToString() throws Exception {
    final String displayName = "My view";
    final String expressionString = "(+ userLong0 userLong1)";

    final StatisticExpressionFactory statisticExpressionFactory =
      StatisticsServicesImplementation.getInstance()
      .getStatisticExpressionFactory();

    final ExpressionView expressionView =
      statisticExpressionFactory
      .createExpressionView(displayName, expressionString, false);

    final String string = expressionView.toString();

    assertTrue(string.indexOf(displayName) >= 0);
    assertTrue(string.indexOf(expressionString) >= 0);

    final ExpressionView view2 =
      statisticExpressionFactory
        .createExpressionView("My view2",
          statisticExpressionFactory.createExpression("userLong0"));

    final String string2 = view2.toString();
    assertTrue(string2.indexOf(displayName) >= 0);
  }

  @Test
  public void testTranslatable() throws Exception {
    final String displayName = "My view";
    final String expressionString = "(+ userLong0 userLong1)";

    final StatisticExpressionFactory statisticExpressionFactory =
      StatisticsServicesImplementation.getInstance()
      .getStatisticExpressionFactory();

    final ExpressionView expressionView =
      statisticExpressionFactory
      .createExpressionView(displayName, expressionString, false);

    assertEquals("console.statistic/My-view",
                 expressionView.getTranslationKey());
  }
}
