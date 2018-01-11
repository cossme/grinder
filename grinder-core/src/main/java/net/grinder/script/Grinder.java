// Copyright (C) 2001 - 2013 Philip Aston
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
import net.grinder.common.GrinderProperties;
import net.grinder.common.TimeAuthority;

import org.slf4j.Logger;


/**
 * Name space for {@code grinder} script context object.
 *
 * @author Philip Aston
 */
public class Grinder {

  /**
   * Object that provides context services to scripts.
   */
  // CHECKSTYLE.OFF: StaticVariableName|VisibilityModifier
  public static InternalScriptContext grinder;
  // CHECKSTYLE.ON

  /**
   * Scripts can get contextual information and access services
   * through a global {@code net.grinder.script.Grinder.grinder}
   * object that supports this interface.
   *
   * @author Philip Aston
   */
  public interface ScriptContext {

    /**
     * Return the agent number. The console allocates a unique number to that
     * each connected agent, and the agent passes this on to the worker process.
     *
     * <p>
     * The lowest possible number is allocated. When an agent disconnects, its
     * number will be reused. Script authors can assume that the agent number
     * lies between {@code 0} and the number of currently connected
     * agents.
     * </p>
     *
     * @return The agent number, or {@code 1} if not launched from the
     *         console.
     * @see #getProcessNumber()
     * @see #getThreadNumber()
     */
    int getAgentNumber();

    /**
     * Return the process number. The agent allocates a unique number to that
     * each worker process it starts.
     *
     * @return The process number.
     * @see #getAgentNumber()
     * @see #getThreadNumber()
     * @see #getFirstProcessNumber()
     */
    int getProcessNumber();

    /**
     * Get a unique name for this worker process.
     *
     * @return The process name.
     * @see #getProcessNumber()
     */
    String getProcessName();

    /**
     * Return the process number of the first worker process. The first process
     * is one our agent started first upon receiving a start signal from the
     * console.
     *
     * <p>Process numbers are assigned incrementally, so the expression
     * {@code (grinder.getProcessNumber() - grinder.getFirstProcessNumber())}
     * can be used to as a zero based index for this process against an array
     * of test data.</p>
     *
     * @return The first process number.
     * @see #getProcessNumber()
     */
    int getFirstProcessNumber();

    /**
     * Return the thread number, or {@code -1} if not called from a
     * worker thread.
     *
     * @return The thread number.
     * @see #getAgentNumber()
     * @see #getProcessNumber()
     */
    int getThreadNumber();

    /**
     * Return the current run number, or {@code -1} if not called from a
     * worker thread.
     *
     * @return An {@code int} value.
     */
    int getRunNumber();

    /**
     * Get a {@link org.slf4j.Logger}.
     *
     * @return A {@code Logger}.
     */
    Logger getLogger();

    /**
     * Sleep for a time based on the meanTime parameter. The actual
     * time may be greater or less than meanTime, and is distributed
     * according to a pseudo normal distribution.
     *
     * @param meanTime Mean time in milliseconds.
     * @throws GrinderException If the sleep failed.
     */
    void sleep(long meanTime) throws GrinderException;

    /**
     * Sleep for a time based on the meanTime parameter. The actual
     * time may be greater or less than meanTime, and is distributed
     * according to a pseudo normal distribution.
     *
     * @param meanTime Mean time in milliseconds.
     * @param sigma The standard deviation, in milliseconds.
     * @exception GrinderException If the sleep failed.
     */
    void sleep(long meanTime, long sigma) throws GrinderException;

    /**
     * Start a new worker thread. The script's {@code TestRunner} class
     * will be used to create new test runner instance for the worker thread.
     *
     * @return The thread number of the new worker thread.
     * @throws InvalidContextException If the main thread has not yet
     *  initialised the script engine, or all other threads have shut down.
     *  Typically, you should only call {@code startWorkerThread()} from
     *  another worker thread.
     * @throws GrinderException If the new worker thread could not be started.
     */
    int startWorkerThread() throws GrinderException;

    /**
     * Start a new worker thread, specifying a <em>test runner</em> instance.
     *
     * <p>This is a more advanced version of {@link #startWorkerThread} that
     * allows a different test runner to be specified. The test runner should
     * be a function or a callable object.
     *
     * @param testRunner A function, or some other callable object.
     * @return The thread number of the new worker thread.
     * @throws InvalidContextException If the main thread has not yet
     *  initialised the script engine, or all other threads have shut down.
     *  Typically, you should only call {@code startWorkerThread()} from
     *  another worker thread.
     * @throws GrinderException If the new worker thread could not be started.
     */
    int startWorkerThread(Object testRunner) throws GrinderException;

    /**
     * Stop this worker thread immediately and cleanly.
     *
     * <p>This method works by throwing a special unchecked exception. If the
     * caller catches this exception, rather than letting it propagate, the
     * thread will not stop.</p>
     *
     * @throws InvalidContextException If called from a non-worker thread.
     */
    void stopThisWorkerThread() throws InvalidContextException;

    /**
     * Request a specific worker thread to stop. If the thread was running, it
     * will shut down be the next time it enters code instrumented by a
     * {@link Test}.
     *
     * <p>This method can be called from non-worker threads.</p>
     *
     * @param threadNumber The thread number of the worker thread to stop.
     * @return {@code true} if a thread existed with the given
     * {@code threadNumber}, otherwise {@code false}.
     */
    boolean stopWorkerThread(int threadNumber);

    /**
     * Stop this worker process cleanly.
     *
     * <p>All worker threads are requested to exit cleanly. Worker threads
     * will handle the request if they they call {{@link #sleep}; are
     * currently sleeping; or try to start new tests. If all threads fail to
     * do so in a timely manner ("timely" is currently hard coded to be 10
     * seconds), the process will exit anyway.</p>
     *
     * <p>This method can be called from non-worker threads. This allows it
     * to be scheduled in a timer thread, for example.</p>
     *
     * <p>If this method is called from a worker thread, the thread will
     * attempt to shut itself down as documented for
     * {@link #stopThisWorkerThread()}.</p>
     *
     * <p>Has no effect on other worker processes that may be running.</p>
     *
     * @since 3.12
     */
    void stopProcess();

    /**
     * Get the global properties for this agent/worker process set.
     *
     * @return The properties.
     */
    GrinderProperties getProperties();

    /**
     * Get a {@link Statistics} object that allows statistics to be queried
     * and updated.
     *
     * @return The statistics.
     */
    Statistics getStatistics();


    /**
     * Get an {@link SSLControl}. This can be used to create secure
     * sockets or to set the certificates that a worker thread should
     * use.
     *
     * @return The SSL control.
     */
    SSLControl getSSLControl();

    /**
     * Create a {@link Barrier} to coordinate worker thread actions across
     * running worker processes.
     *
     * <p>
     * Example script:
     * </p>
     *
     * <pre>
     * from net.grinder.script.Grinder import grinder
     *
     * class TestRunner:
     *   def __init__(self):
     *     # Each worker thread joins the barrier.
     *     self.phase1CompleteBarrier = grinder.barrier("Phase 1")
     *
     *   def __call__(self):
     *
     *     # ... Phase 1 actions.
     *
     *     # Wait for all worker threads to reach this point before proceeding.
     *     self.phase1CompleteBarrier.await()
     *
     *     # ... Further actions.
     * </pre>
     *
     * @param name
     *          The barrier name.
     * @return The barrier.
     * @throws GrinderException
     *           If the barrier could not be created due to a network problem.
     * @see Barrier
     * @since 3.6
     */
    Barrier barrier(String name) throws GrinderException;

    /**
     * Something that knows the time.
     *
     * @return The time authority.
     * @since 3.12
     */
    TimeAuthority getTimeAuthority();
  }
}
