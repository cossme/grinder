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

package net.grinder.common;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;


/**
 * Make {@link InterruptedException}s and {@link InterruptedIOException}s
 * easier to propagate.
 *
 * <p>
 * Our policy on interrupt handling:
 *
 * <ul>
 * <li>{@link Thread#interrupt()} and {@link ThreadGroup#interrupt()} are used
 * in shut down code. We can't simply swallow {@link InterruptedException}s.
 * </li>
 *
 * <li>Whenever core code catches an {@link InterruptedException} which it
 * doesn't know how to handle, it rethrows it in an
 * {@link UncheckedInterruptedException}.</li>
 *
 * <li>{@link net.grinder.util.thread.InterruptibleRunnable#interruptibleRun()}
 * implementations are carefully reviewed to ensure that they do not ignore the
 * interrupt condition and will exit whenever {@link InterruptedException} and
 * {@link InterruptedIOException}s are received. We only interrupt code that
 * implements
 * {@link net.grinder.util.thread.InterruptibleRunnable#interruptibleRun()}.
 * </li>
 *
 * <li>The InterruptibleRunnable's are invoked by wrapping then in an
 * InterruptibleRunnableAdapter. This exits cleanly, silently handling
 * {@link UncheckedInterruptedException}s.</li>
 *
 * <li>Whenever core code outside an
 * {@link net.grinder.util.thread.InterruptibleRunnable} catches an
 * {@link IOException}, it calls {@link #ioException(IOException)}, which will
 * throw an {@link UncheckedInterruptedException} if necessary.</li>
 *
 * <li>Other code may exit cleanly or may ignore the interrupt condition due to
 * third-party libraries swallowing {@link InterruptedException}s. This doesn't
 * matter as we should never interrupt this code.</li>
 * </ul>
 * </p>
 *
 * @author Philip Aston
 */
public class UncheckedInterruptedException extends UncheckedGrinderException {

  /**
   * Constructor.
   *
   * @param e The original InterruptedException.
   */
  public UncheckedInterruptedException(InterruptedException e) {
    super("Thread interrupted", e);
  }


  private UncheckedInterruptedException(InterruptedIOException e) {
    super("Thread interrupted", e);
  }

  /**
   * {@link InterruptedIOException}s are a pain to handle as they extend
   * {@link IOException}. {@link IOException} handlers should call this, unless
   * they are part of an {@link net.grinder.util.thread.InterruptibleRunnable}
   * and know what they're doing.
   *
   * @param e
   *          An {@link IOException}.
   */
  public static void ioException(IOException e) {
    // SocketTimeoutException extends InterruptedIOException. One gets the
    // impression that JavaSoft was never serious about applications doing
    // anything other than ignoring interrupts.
    if (e instanceof InterruptedIOException &&
        !(e instanceof SocketTimeoutException)) {
      throw new UncheckedInterruptedException((InterruptedIOException)e);
    }
  }
}
