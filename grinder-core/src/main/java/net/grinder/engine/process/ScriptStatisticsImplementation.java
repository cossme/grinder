// Copyright (C) 2003 - 2011 Philip Aston
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

import net.grinder.common.GrinderException;
import net.grinder.communication.Sender;
import net.grinder.messages.console.RegisterExpressionViewMessage;
import net.grinder.script.InvalidContextException;
import net.grinder.script.Statistics;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;


/**
 * Implement the script statistics interface.
 *
 * <p>Package scope.
 *
 * @author Philip Aston
 */
final class ScriptStatisticsImplementation implements Statistics {

  private final ThreadContextLocator m_threadContextLocator;
  private final StatisticsServices m_statisticsServices;
  private final Sender m_consoleSender;

  public ScriptStatisticsImplementation(
    ThreadContextLocator threadContextLocator,
    StatisticsServices statisticsServices,
    Sender consoleSender) {

    m_threadContextLocator = threadContextLocator;
    m_statisticsServices = statisticsServices;
    m_consoleSender = consoleSender;
  }

  public void setDelayReports(boolean b) throws InvalidContextException {
    getThreadContext().setDelayReports(b);
  }

  public void report() throws InvalidContextException {
    getThreadContext().reportPendingDispatchContext();
  }

  private ThreadContext getThreadContext()
    throws InvalidContextException {
    final ThreadContext threadContext = m_threadContextLocator.get();

    if (threadContext == null) {
      throw new InvalidContextException(
        "Statistics interface is only supported for worker threads.");
    }

    return threadContext;
  }

  public boolean isTestInProgress() {
    final ThreadContext threadContext = m_threadContextLocator.get();

    return
      threadContext != null &&
      threadContext.getStatisticsForCurrentTest() != null;
  }

  public void registerSummaryExpression(String displayName, String expression)
    throws GrinderException {

    final ExpressionView expressionView =
      StatisticsServicesImplementation.getInstance()
      .getStatisticExpressionFactory()
      .createExpressionView(displayName, expression, false);

    m_statisticsServices.getSummaryStatisticsView().add(expressionView);

    // Queue up, will get flushed with next process status or
    // statistics report.
    m_consoleSender.send(new RegisterExpressionViewMessage(expressionView));
  }

  public void registerDataLogExpression(String displayName, String expression)
    throws GrinderException {

    if (m_threadContextLocator.get() != null) {
      throw new InvalidContextException(
        "registerDataLogExpression() is not supported from worker threads");
    }

    m_statisticsServices.getDetailStatisticsView().add(
      StatisticsServicesImplementation.getInstance()
      .getStatisticExpressionFactory()
      .createExpressionView(displayName, expression, false));
  }

  public StatisticsForTest getForCurrentTest() throws InvalidContextException {
    final StatisticsForTest statisticsForCurrentTest =
      getThreadContext().getStatisticsForCurrentTest();

    if (statisticsForCurrentTest == null) {
      throw new InvalidContextException("There is no test in progress.");
    }

    return statisticsForCurrentTest;
  }

  public StatisticsForTest getForLastTest() throws InvalidContextException {
    final StatisticsForTest statisticsForLastTest =
      getThreadContext().getStatisticsForLastTest();

    if (statisticsForLastTest == null) {
      throw new InvalidContextException(
        "No tests have been performed by this thread.");
    }

    return statisticsForLastTest;
  }
}
