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

package net.grinder.engine.process;

import net.grinder.common.UncheckedGrinderException;


/**
 * Records time.
 *
 * <p>
 * Strict state model, the {@link StopWatch} is either <em>running</em> or
 * <em>not running</em>. If <em>running</em> then the only valid method is
 * {@link #stop}. If <em>not running</em> then {@link #start},
 * {@link #getTime}, {@link #add}, and {@link #reset} are all valid, but
 * {@link #stop} is not.
 * </p>
 *
 * <p>
 * Calling a method that is not valid for the current state results in an
 * unchecked {@link StopWatchStateException}; this is effectively an assertion.
 * </p>
 *
 * @author Philip Aston
 */
interface StopWatch {

  void start() throws StopWatchRunningException;

  void stop() throws StopWatchNotRunningException;

  void reset() throws StopWatchRunningException;

  long getTime() throws StopWatchRunningException;

  boolean isRunning();

  void add(StopWatch watch) throws StopWatchRunningException;

  /**
   * {@link StopWatch} was in an invalid state for the method called.
   */
  abstract class StopWatchStateException extends UncheckedGrinderException {
    public StopWatchStateException(String s) {
      super(s);
    }
  }

  /**
   * {@link StopWatch} was running.
   */
  class StopWatchRunningException extends StopWatchStateException {
    public StopWatchRunningException(String s) {
      super(s);
    }
  }

  /**
   * {@link StopWatch} was not running.
   */
  class StopWatchNotRunningException extends StopWatchStateException {
    public StopWatchNotRunningException(String s) {
      super(s);
    }
  }
}
