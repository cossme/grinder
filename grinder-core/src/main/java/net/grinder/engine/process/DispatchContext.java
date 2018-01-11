// Copyright (C) 2006 - 2008 Philip Aston
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

import net.grinder.common.Test;
import net.grinder.engine.common.EngineException;
import net.grinder.script.Statistics.StatisticsForTest;

import org.slf4j.Marker;


/**
 * Access state related to a particular dispatch of a test by a worker thread.
 *
 * @author Philip Aston
 */
interface DispatchContext {

  /**
   * The test.
   *
   * @return The test that this dispatch is for.
   */
  Test getTest();

  /**
   * Return an SLF4J marker indicating the test.
   *
   * @return The marker.
   */
  Marker getLogMarker();

  /**
   * Creates a {@link net.grinder.script.Statistics.StatisticsForTest} through
   * which the script can query and update the statistics. The statistics are
   * only mutable update until the next time {@link #report()} is called; after
   * that time the {@link net.grinder.script.Statistics.StatisticsForTest} is
   * disassociated from this {@link DispatchContext} and the statistics are
   * read-only.
   *
   * @return The handle the script uses to update the statistics, or
   *    <code>null</code> if there currently is no test in progress.
   */
  StatisticsForTest getStatisticsForTest();

  /**
   * Report any pending statistics.
   *
   * @throws DispatchStateException If there are no statistics to report.
   */
  void report() throws DispatchStateException;

  /**
   * Return a {@link StopWatch} which can be started and stopped around
   * code that should not contribute to the elapsed time measurement.
   *
   * @return The stop watch.
   */
  StopWatch getPauseTimer();

  /**
   * If there is a test in progress, return the elapsed time since the
   * start of the test. If a test has been completed, but not reported, return
   * the test time. Otherwise return -1.
   *
   * @return The elapsed time for the test.
   */
  long getElapsedTime();

  /**
   * Set that this <code>DispatchContext</code> has nested contexts.
   */
  void setHasNestedContexts();

  /**
   * Exception that indicates the dispatcher was in an invalid state for
   * the called method .
   */
  static class DispatchStateException extends EngineException {
    DispatchStateException(String message) {
      super(message);
    }
  }
}
