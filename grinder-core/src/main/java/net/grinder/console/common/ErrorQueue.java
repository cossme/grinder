// Copyright (C) 2004 - 2011 Philip Aston
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

package net.grinder.console.common;

import java.util.LinkedList;


/**
 * {@link ErrorHandler} that queues up its errors when a delegate
 * <code>ErrorHandler</code> is not available, and passes the errors
 * on when a delegate is available.
 *
 * @author Philip Aston
 */
public final class ErrorQueue implements ErrorHandler {

  private ErrorHandler m_delegate = null;
  private final LinkedList<DelayedError> m_queue =
    new LinkedList<DelayedError>();

  /**
   * Set the delegate error handler. Any queued up errors will be
   * reported to the delegate immediately.
   *
   * @param errorHandler Where to report errors.
   */
  public void setErrorHandler(ErrorHandler errorHandler) {
    synchronized (this) {
      m_delegate = errorHandler;

      if (m_delegate != null) {
        synchronized (m_queue) {
          while (m_queue.size() > 0) {
            m_queue.removeFirst().apply(m_delegate);
          }
        }
      }
    }
  }

  private interface DelayedError {
    void apply(ErrorHandler errorHandler);
  }

  private void queue(DelayedError delayedError) {
    synchronized (this) {
      if (m_delegate != null) {
        delayedError.apply(m_delegate);
      }
      else {
        synchronized (m_queue) {
          m_queue.add(delayedError);
        }
      }
    }
  }

  /**
   * Method that handles error messages.
   *
   * @param errorMessage The error message.
   */
  public void handleErrorMessage(final String errorMessage) {
    queue(new DelayedError() {
        public void apply(ErrorHandler errorHandler) {
          errorHandler.handleErrorMessage(errorMessage);
        }
      });
  }

  /**
   * Method that handles error messages.
   *
   * @param errorMessage The error message.
   * @param title A title to use.
   */
  public void handleErrorMessage(final String errorMessage,
                                 final String title) {
    queue(new DelayedError() {
        public void apply(ErrorHandler errorHandler) {
          errorHandler.handleErrorMessage(errorMessage, title);
        }
      });
  }

  /**
   * Method that handles exceptions.
   *
   * @param throwable The exception.
   */
  public void handleException(final Throwable throwable) {
    queue(new DelayedError() {
        public void apply(ErrorHandler errorHandler) {
          errorHandler.handleException(throwable);
        }
      });
  }

  /**
   * Method that handles exceptions.
   *
   * @param throwable The exception.
   * @param title A title to use.
   */
  public void handleException(final Throwable throwable, final String title) {
    queue(new DelayedError() {
        public void apply(ErrorHandler errorHandler) {
          errorHandler.handleException(throwable, title);
        }
      });
  }

  /**
   * Method that handles information messages.
   *
   * @param informationMessage The information message.
   */
  public void handleInformationMessage(final String informationMessage) {
    queue(new DelayedError() {
      public void apply(ErrorHandler errorHandler) {
        errorHandler.handleInformationMessage(informationMessage);
      }
    });
  }
}
