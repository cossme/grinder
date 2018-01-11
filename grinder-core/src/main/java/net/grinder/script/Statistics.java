// Copyright (C) 2003 - 2013 Philip Aston
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

package net.grinder.script;

import net.grinder.common.GrinderException;


/**
 * Script statistics query and reporting API.
 *
 * <p>
 * An implementation of this interface can be obtained by calling {@link
 * Grinder.ScriptContext#getStatistics getStatistics} on the
 * {@link Grinder#grinder grinder} context object.
 * </p>
 *
 * <h2><a name="#statistics">Statistics</a></h2>
 *
 * <p>
 * The following table lists the statistics provided by The Grinder. Each
 * statistic has a unique name. Basic statistics either hold <em>long</em>
 * integer values or <em>double</em> floating-point values. Sample statistics
 * are a special type of statistic that hold aggregate information about a
 * series of long or double sample values; specifically <em>count</em> (number
 * of samples), <em>sum</em> (total of all sample values), and sample
 * <em>variance</em>.
 * </p>
 *
 * <blockquote> <table class="table">
 * <tr>
 * <th>Name</th>
 * <th>Type</th>
 * <th>Description</th>
 * </tr>
 *
 * <tr>
 * <td><em>errors</em></td>
 * <td>basic&nbsp;long</td>
 * <td>The number of tests performed that resulted in an error.</td>
 * </tr>
 *
 * <tr>
 * <td><em>timedTests</em></td>
 * <td>sample&nbsp;long</td>
 * <td>Sample statistic that records successful tests.
 * A test is considered successful if it is not marked as an error.</td>
 * </tr>
 *
 * <tr>
 * <td><em>userLong0</em>, <em>userLong1</em>, <em>userLong2</em>,
 * <em>userLong3</em>, <em>userLong4</em></td>
 * <td>basic&nbsp;long</td>
 * <td>Statistics that scripts and custom plug-ins can use for their own
 * purposes.</td>
 * </tr>
 *
 * <tr>
 * <td><em>userDouble0</em>, <em>userDouble1</em>, <em>userDouble2</em>,
 * <em>userDouble3</em>, <em>userDouble4</em></td>
 * <td>basic&nbsp;double</td>
 * <td>Statistics that scripts and custom plug-ins can use for their own
 * purposes.</td>
 * </tr>
 *
 * <tr>
 * <td><em>untimedTests</em></td>
 * <td>basic&nbsp;long</td>
 * <td>The number of successful tests performed that have no timing
 * information.
 * <br/>This statistic only used when reporting statistics to the console,
 * it has no meaning in a worker process.
 * </td>
 * </tr>
 *
 * <tr>
 * <td><em>period</em></td>
 * <td>basic&nbsp;long</td>
 * <td>The sampling period duration, in milliseconds. <br/>This statistic is
 * used to define views for evaluation in the console and for the overall
 * time used to calculate rates in the Totals line in the final summary
 * table.</td>
 * </tr>
 *
 * <tr>
 * <td><em>peakTPS</em></td>
 * <td>basic&nbsp;double</td>
 * <td>The highest Tests Per Second figure in the current sampling period.
 * <br/>This statistic is used to define views for evaluation in the
 * console and has no meaning in a worker process.</td>
 * </tr>
 *
 * </table> </blockquote>
 *
 * <p>
 * The {@link #getForCurrentTest()} and {@link #getForLastTest()} methods allow
 * scripts to query or update statistics for a single test. Consequentially,
 * the statistics should be interpreted as follows when using this interface.
 * </p>
 *
 * <blockquote> <table class="table">
 * <tr>
 * <th>Name</th>
 * <th>Type</th>
 * <th>Meaning for a single test</th>
 * </tr>
 *
 *
 * <tr>
 * <td><em>errors</em></td>
 * <td>basic&nbsp;long</td>
 * <td><code>1</code> if the test resulted in an error, otherwise
 * <code>0</code>.</td>
 * </tr>
 *
 * <tr>
 * <td><em>timedTests</em></td>
 * <td>sample&nbsp;long</td>
 * <td>If the test was successful, the count is <code>1</code> and the sum is
 * the test time in milliseconds, otherwise the sum and the count are zero.
 * The variance is always <code>0</code>.
 * </td>
 * </tr>
 *
 * <tr>
 * <td><em>userLong0</em>, <em>userLong1</em>, <em>userLong2</em>,
 * <em>userLong3</em>, <em>userLong4</em></td>
 * <td>basic&nbsp;long</td>
 * <td>Depends on how the script or custom plug-in chooses to use the
 * statistic.</td>
 * </tr>
 *
 * <tr>
 * <td><em>userDouble0</em>, <em>userDouble1</em>, <em>userDouble2</em>,
 * <em>userDouble3</em>, <em>userDouble4</em></td>
 * <td>basic&nbsp;double</td>
 * <td>Depends on how the script or custom plug-in chooses to use the
 * statistic.</td>
 * </tr>
 *
 * <tr>
 * <td><em>untimedTests</em></td>
 * <td>basic&nbsp;long</td>
 * <td>Not relevant.</td>
 * </tr>
 *
 * <tr>
 * <td><em>period</em></td>
 * <td>basic&nbsp;long</td>
 * <td>Not relevant.</td>
 * </tr>
 *
 * <tr>
 * <td><em>peakTPS</em></td>
 * <td>basic&nbsp;double</td>
 * <td>Not relevant.</td>
 * </tr>
 *
 * </table> </blockquote>
 *
 * <p>
 * The Grinder updates the statistics for each test just before it is recorded
 * as follows:
 * </p>
 *
 * <ul>
 * <li>If <em>errors</em> is <code>0</code>, the elapsed time of the test is
 * added to the <em>timedTests</em> sample statistic.</li>
 * <li>If <em>errors</em> is not <code>0</code>, the <em>timedTests</em>
 * and <em>untimedTests</em> statistics are reset to zero, and <em>errors</em>
 * is set to <code>1</code>.
 * </ul>
 *
 * <p>If the <code>grinder.reportTimesToConsole</code> property (see <a
 * href="http://grinder.sourceforge.net/g3/properties.html">The Grinder manual
 * </a>) is <code>false</code>, the statistics sent to the console are further
 * modified by setting <em>untimedTests</em> to the count of the
 * <em>timedTests</em> statistic, and resetting <em>timedTests</em>.</p>
 *
 * <h4>HTTP Plug-in Statistics</h4>
 *
 * <p>
 * The HTTP plug-in adds a number of basic long statistics. Although these will
 * be updated for a single test, many HTTP requests might be wrapped in that
 * test. Where there is more than one HTTP request, the statistic values will
 * reflect the sum for all requests unless otherwise stated.
 * </p>
 *
 * <blockquote> <table class="table">
 * <tr>
 * <th>Name</th>
 * <th>Type</th>
 * <th>Description</th>
 * </tr>
 *
 * <tr>
 * <td><em>httpplugin.responseStatus</em></td>
 * <td>basic&nbsp;long</td>
 * <td>The HTTP status code of the response to the last HTTP request wrapped in
 * the test.</td>
 * </tr>
 *
 * <tr>
 * <td><em>httpplugin.responseLength</em></td>
 * <td>basic&nbsp;long</td>
 * <td>The length of the HTTP response in bytes.</td>
 * </tr>
 *
 * <tr>
 * <td><em>httpplugin.responseErrors</em></td>
 * <td>basic&nbsp;long</td>
 * <td><code>1</code> if the HTTP response status code was greater or equal
 * to 400, otherwise <code>0</code>.</td>
 * </tr>
 *
 * <tr>
 * <td><em>httpplugin.dnsTime</em></td>
 * <td>basic&nbsp;long</td>
 * <td>The time taken to resolve the host name in milliseconds.</td>
 * </tr>
 *
 * <tr>
 * <td><em>httpplugin.connectTime</em></td>
 * <td>basic&nbsp;long</td>
 * <td>The time taken to establish the HTTP connection in milliseconds. (This
 * includes time to resolve the host name).</td>
 * </tr>
 *
 * <tr>
 * <td><em>httpplugin.firstByteTime</em></td>
 * <td>basic&nbsp;long</td>
 * <td>The time taken to receive the first response byte in milliseconds. (This
 * includes time to resolve the host name and establish the connection).</td>
 * </tr>
 *
 * </table> </blockquote>
 *
 *
 * <h2>Querying statistics</h2>
 *
 * <p>
 * {@link StatisticsForTest#getDouble(String)} and
 * {@link StatisticsForTest#getLong(String)} provide basic
 * statistics about the current or last test performed by the calling worker
 * thread. There is also {@link StatisticsForTest#getSuccess()}, which is
 * equivalent to <code>getLong("errors") != 0</code>.
 * </p>
 *
 * <p>
 * Example of querying the result of a test:
 * </p>
 *
 * <blockquote>
 *
 * <pre>
 * result1 = test1.doSomething()
 *
 * statisticsForTest = grinder.statistics.forLastTest
 *
 * if statisticsForTest.success and \
 *   statisticsForTest.getLong(&quot;httpplugin.responseStatus&quot;) == 200:
 *   # ...
 * </pre>
 *
 * </blockquote>
 *
 * <p>
 * Calling query methods on the result of {@link #getForLastTest} provides
 * information about the last test performed by the calling worker thread.
 * </p>
 *
 * <p>
 * Calling query methods on the result of {@link #getForCurrentTest} from within
 * code wrapped by a {@link Test} proxy provides information about the test in
 * progress. This information may be partially complete.
 * </p>
 *
 * <p>
 * There are no general methods to access the count, sum, and variance of sample
 * statistics; these terms aren't that meaningful for a single test. Instead,
 * there are specific methods which influence the only sample statistic used
 * by The Grinder - the <em>timedTests</em> statistic. The time of the last test
 * can be obtained with {@link StatisticsForTest#getTime()} (or the elapsed time
 * of the current test when used with {@link #getForCurrentTest()}}, and
 * {@link StatisticsForTest#getSuccess()} returns whether the test was
 * successful or not.
 *
 * <p>
 * There's a subtle difference between the sum of <em>timedTests</em> and the
 * result of {@link StatisticsForTest#getTime()}.
 * {@link StatisticsForTest#getTime()} always returns the time
 * taken by the test, even if the test was an error and the time will not be
 * added to <em>timedTests</em>. This allows the script to access the time
 * taken by a failed test, even though it's not recorded anywhere else.
 * </p>
 *
 * <h2>Updating statistics</h2>
 *
 * <p>
 * The following methods allow basic statistics to be updated.
 * </p>
 *
 * <ul>
 * <li>{@link StatisticsForTest#setSuccess(boolean)}</li>
 * <li>{@link StatisticsForTest#setLong(String, long)}</li>
 * <li>{@link StatisticsForTest#setDouble(String, double)}</li>
 * <li>{@link StatisticsForTest#addLong(String, long)}
 * <li>{@link StatisticsForTest#addDouble(String, double)}</li>
 * </ul>
 *
 * <p>
 * The only way to influence the <em>timedTests</em> sample statistic through
 * this interface is to mark the test as an error.
 * </p>
 *
 * <p>
 * By default, test statistics reports are automatically sent to the console and
 * data log when the test proxy call completes, so the script cannot modify the
 * test statistics after the call. Scripts can turn off this automatic reporting
 * for the current worker thread by using {@link #setDelayReports}. Having done
 * so, the script can modify or set the statistics before they are sent to the
 * log and the console. The statistics reports are sent at a later time as
 * described in {@link #setDelayReports}. For example:
 *
 * <blockquote>
 *
 * <pre>
 * grinder.statistics.delayReports = 1
 *
 * result1 = test1.doSomething()
 *
 * if isFailed(result1):
 *   # Mark test as failure. The appropriate failure detection
 *   # depends on the type of test.
 *   grinder.statistics.forLastTest.success = 0
 * </pre>
 *
 * </blockquote>
 *
 * <p>
 * It is also possible to set the statistics from within test implementation
 * itself using {@link #getForCurrentTest()}. Changes to the standard statistics
 * will be modified by The Grinder engine when the test finishes as described
 * above.
 * </p>
 *
 * <h2>Registering new expressions</h2>
 *
 * New statistics expressions for the console and the data log can be registered
 * using {@link #registerSummaryExpression(String, String)} and
 * {@link #registerDataLogExpression(String, String)}.
 *
 *
 * @author Philip Aston
 */
public interface Statistics  {

  /**
   * Access to the statistics for the current test.
   *
   * <p>
   * This is only valid when called from code wrapped within a Test. If this is
   * not the case, {@link InvalidContextException} will be thrown. E.g.
   * </p>
   *
   * <pre>
   * def foo():
   *   statistics = grinder.statistics.getForCurrentTest()
   *   t = statistics.time      # Time since foo() was called.
   *   statistics.success = 0   # Mark test as bad.
   *
   * instrumentedFoo = Test(1, &quot;My Test&quot;).wrap(foo)
   *
   * instrumentedFoo()              # OK.
   * foo()                          # The statistics.getForCurrentTest() call in
   *                                # foo() will throw exception as there is no
   *                                # current test.
   * statistics.getForCurrentTest() # Will throw exception, no current test.
   * </pre>
   *
   * @return The statistics for the current test.
   * @throws InvalidContextException
   *           If not called from a worker thread.
   * @throws InvalidContextException
   *           If there is no test in progress.
   * @see #getForLastTest()
   * @see #isTestInProgress()
   */
  StatisticsForTest getForCurrentTest() throws InvalidContextException;

  /**
   * Access the statistics for the last completed test. These can only
   * be updated if {@link #setDelayReports(boolean) reporting has been
   * delayed}.
   *
   * @return The statistics for the last test.
   * @throws InvalidContextException
   *           If not called from a worker thread.
   * @throws InvalidContextException
   *           If called before the first test has started.
   * @see #getForCurrentTest
   */
  StatisticsForTest getForLastTest() throws InvalidContextException;

  /**
   * Returns whether there is a test in progress.
   *
   * @return <code>true</code => {@link #getForCurrentTest} will
   * not throw {@link InvalidContextException}.
   */
  boolean isTestInProgress();

  /**
   * Use to delay reporting of the last test statistics to the log and the
   * console so that the script can modify them. Normally test statistics are
   * reported automatically when the code wrapped by the test completes.
   *
   * <p>
   * With this set to <code>true</code> the statistics for a completed test
   * will be reported at the following times:
   * <ol>
   * <li>When the next test begins.</li>
   * <li>When an enclosing test ends.</li>
   * <li>When the current run completes.</li>
   * <li>When the script calls {@link #report}.</li>
   * <li>When the script calls <code>setDelayReports(false)</code>.</li>
   * </ol>
   * </p>
   *
   * <p>
   * This method only changes reporting for the calling worker thread.
   * </p>
   *
   * @param b
   *          <code>false</code> => enable automatic reporting when the code
   *          wrapped by a test completes (the default behaviour);
   *          <code>true</code> => delay reporting.
   * @throws InvalidContextException
   *           If not called from a worker thread.
   * @see #report
   */
  void setDelayReports(boolean b) throws InvalidContextException;

  /**
   * Send any pending statistics for the last completed test to the data log and
   * the console.
   *
   * <p>
   * Calling this does nothing if there are no pending statistics to report.
   * This will be the case if {@link #setDelayReports} has not been called to
   * delay reporting.
   * </p>
   *
   * @throws InvalidContextException
   *           If not called from a worker thread.
   */
  void report() throws InvalidContextException;

  /**
   * Register a new "summary" statistic expression. This expression will appear
   * in the worker process output log summaries and the console.
   *
   * <p>
   * Statistic expressions are composed of statistic names (see
   * {@link Statistics}) in a simple post-fix format using the symbols
   * <code>+</code>, <code>-</code>, <code>/</code> and <code>*</code>,
   * which have their usual meanings, in conjunction with simple statistic names
   * or sub-expressions. Precedence is controlled by grouping expressions in
   * parentheses. For example, the error rate is
   * <code>(* (/ errors period) 1000)</code> errors per second. The symbol
   * <code>sqrt</code> can be used to calculate the square root of an
   * expression.</p>
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
   * </p>
   *
   * @param displayName
   *          A display name. In the console, this is converted to a key for an
   *          internationalised resource bundle look up by prefixing the string
   *          with <code>statistic.</code> and replacing any whitespace with
   *          underscores.
   * @param expression
   *          The expression string.
   * @throws GrinderException
   *           If the expression could not be registered.
   */
  void registerSummaryExpression(String displayName, String expression)
    throws GrinderException;

  /**
   * Register a new "detail" statistic expression. This expression will appear
   * in the worker process data log. Each test invocation will have an value
   * displayed for the detail statistic expression.
   *
   * <p>
   * You should call <code>registerDataLogExpression</code> from the top level
   * of your script. It cannot be called from a worker thread - the data logs
   * are initialised by the time the worker threads start.
   * </p>
   *
   * @param displayName
   *          A display name.
   * @param expression
   *          The expression string.
   * @throws GrinderException
   *           If the expression could not be registered.
   * @throws InvalidContextException
   *           If called from a worker thread.
   * @see #registerSummaryExpression(String, String) for details of the
   *      expression format.
   */
  void registerDataLogExpression(String displayName, String expression)
     throws GrinderException, InvalidContextException;

  /**
   * Query and update methods for the statistics relating to a particular call
   * of a test.
   *
   * @see Statistics#getForLastTest()
   * @see Statistics#getForCurrentTest()
   */
  interface StatisticsForTest {

    /**
     * Return the Test that the statistics are for.
     *
     * @return The test.
     */
    net.grinder.common.Test getTest();

    /**
     * Sets the long statistic <code>statisticName</code> to the specified
     * <code>value</code>.
     *
     * @param statisticName
     *          The statistic name. See {@link Statistics} for a list of valid
     *          names.
     * @param value
     *          The value.
     * @throws InvalidContextException
     *           If called when the statistics have already been sent for the
     *           last test performed by this thread - see
     *           {@link Statistics#setDelayReports}.
     * @throws NoSuchStatisticException
     *           If <code>statisticName</code> does not refer to a known basic
     *           long statistic.
     */
    void setLong(String statisticName, long value)
      throws InvalidContextException, NoSuchStatisticException;

    /**
     * Sets the double statistic <code>statisticName</code> to the specified
     * <code>value</code>.
     *
     * @param statisticName
     *          The statistic name. See {@link Statistics} for a list of valid
     *          names.
     * @param value
     *          The value.
     * @throws InvalidContextException
     *           If called when the statistics have already been sent for the
     *           last test performed by this thread - see
     *           {@link Statistics#setDelayReports(boolean)}.
     * @throws NoSuchStatisticException
     *           If <code>statisticName</code> does not refer to a known basic
     *           double statistic.
     */
    void setDouble(String statisticName, double value)
      throws InvalidContextException, NoSuchStatisticException;

    /**
     * Add <code>value</code> to the long statistic <code>statisticName</code>.
     *
     * @param statisticName
     *          The statistic name. See {@link Statistics} for a list of valid
     *          names.
     * @param value
     *          The value.
     * @throws InvalidContextException
     *           If called when the statistics have already been sent for the
     *           last test performed by this thread - see
     *           {@link Statistics#setDelayReports(boolean)}.
     * @throws NoSuchStatisticException
     *           If <code>statisticName</code> does not refer to a known basic
     *           long statistic.
     */
    void addLong(String statisticName, long value)
      throws InvalidContextException, NoSuchStatisticException;

    /**
     * Add <code>value</code> to the double statistic
     * <code>statisticName</code>.
     *
     * @param statisticName
     *          The statistic name. See {@link Statistics} for a list of valid
     *          names.
     * @param value
     *          The value.
     * @throws InvalidContextException
     *           If called when the statistics have already been sent for the
     *           last test performed by this thread - see
     *           {@link Statistics#setDelayReports(boolean)}.
     * @throws NoSuchStatisticException
     *           If <code>statisticName</code> does not refer to a known basic
     *           double statistic.
     */
    void addDouble(String statisticName, double value)
      throws InvalidContextException, NoSuchStatisticException;

    /**
     * Return the value of long statistic <code>statisticName</code>.
     *
     * @param statisticName
     *          The statistic name. See {@link Statistics} for a list of valid
     *          names.
     * @return The value.
     * @throws NoSuchStatisticException
     *           If <code>statisticName</code> does not refer to a known
     *           basic long statistic.
     */
    long getLong(String statisticName) throws NoSuchStatisticException;

    /**
     * Return the value of the double statistic <code>statisticName</code>.
     *
     * @param statisticName
     *          The statistic name. See {@link Statistics} for a list of valid
     *          names.
     * @return The value.
     * @throws NoSuchStatisticException
     *           If <code>statisticName</code> does not refer to a known basic
     *           double statistic.
     */
    double getDouble(String statisticName) throws NoSuchStatisticException;

    /**
     * Convenience method that sets whether the last test should be considered a
     * success or not.
     *
     * @param success
     *          If <code>true</code> <em>errors</em> is set to
     *          <code>0</code>, otherwise <em>errors</em> is set to
     *          <code>1</code>.
     * @throws InvalidContextException
     *           If called when the statistics have already been sent for the
     *           last test performed by this thread - see
     *           {@link Statistics#setDelayReports(boolean)}.
     */
    void setSuccess(boolean success) throws InvalidContextException;

    /**
     * Convenience method that returns whether the test was a success
     * (<em>errors</em> is zero) or not.
     *
     * @return Whether the test was a success.
     */
    boolean getSuccess();

    /**
     * Returns the elapsed time for the test.
     *
     * <p>If this {@link Statistics.StatisticsForTest StatisticsForTest} was
     * obtained with {@link Statistics#getForCurrentTest}, the result will be
     * the elapsed time since the test in progress was started. If it was
     * obtained with {@link Statistics#getForLastTest}, the result will be the
     * time taken by the last test.
     *
     * <p>
     * {@link #getTime()} always returns the time taken by the test, even if the
     * test was an error and the time will not be added to <em>timedTests</em>.
     * </p>
     *
     * @return The elapsed time for the test.
     */
    long getTime();


    /**
     * The time taken between invocations of {@link #pauseClock()} and
     * {@link #resumeClock} is not included in the recorded time for this test.
     *
     *
     * <p>This is an advanced API, primarily for the use of scripts and plug-ins
     * that wish to discount the cost of expensive pre or post processing.</p>
     *
     * @throws InvalidContextException If the test has completed. This method
     * can only be called when there is a test in progress, so will not work
     * if called on the result of {@link Statistics#getForLastTest()}.
     * @since 3.12
     */
    void pauseClock() throws InvalidContextException;

    /**
     * @see #pauseClock()
     * @throws InvalidContextException If the test has completed.
     * @since 3.12
     */
    void resumeClock() throws InvalidContextException;
  }
}
