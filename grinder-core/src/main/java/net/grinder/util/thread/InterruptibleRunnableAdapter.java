// Copyright (C) 2006 Philip Aston
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

package net.grinder.util.thread;

import net.grinder.common.UncheckedInterruptedException;


/**
 * Adapt a {@link InterruptibleRunnable} to a {@link Runnable}.
 *
 * <p>
 * {@link #run()} exits quietly if an {UncheckedInterruptedException} is thrown
 * from the delegate.
 * </p>
 *
 * @author Philip Aston
 */
public final class InterruptibleRunnableAdapter implements Runnable {

  private final InterruptibleRunnable m_interuptibleRunnable;

  /**
   * Constructor for InterruptibleRunnableAdapter.
   *
   * @param interruptibleRunnable The delegate.
   */
  public InterruptibleRunnableAdapter(
    InterruptibleRunnable interruptibleRunnable) {

    m_interuptibleRunnable = interruptibleRunnable;
  }


  /**
   * Implement {@link Runnable}.
   */
  public void run() {
    try {
      m_interuptibleRunnable.interruptibleRun();
    }
    catch (UncheckedInterruptedException e) {
      // Ignore, and exit quietly.
    }
  }
}
