// Copyright (C) 2000 - 2008 Philip Aston
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


/**
 * Interface for things that can deal with reporting errors and
 * exceptions.
 *
 * @author Philip Aston
 */
public interface ErrorHandler {

  /**
   * Method that handles error messages.
   *
   * @param errorMessage The error message.
   * @see #handleErrorMessage(String, String)
   * @see #handleInformationMessage(String)
   */
  void handleErrorMessage(String errorMessage);

  /**
   * Method that handles error messages.
   *
   * @param errorMessage The error message.
   * @param title A title to use.
   * @see #handleErrorMessage(String)
   */
  void handleErrorMessage(String errorMessage, String title);

  /**
   * Method that handles exceptions.
   *
   * @param throwable The exception.
   */
  void handleException(Throwable throwable);

  /**
   * Method that handles exceptions.
   *
   * @param throwable The exception.
   * @param title A title to use.
   */
  void handleException(Throwable throwable, String title);

  /**
   * Method that handles information messages. Information messages are lower
   * priority than error messages. In general, error messages should interrupt
   * the user (e.g. use a modal dialog), but information messages should just be
   * logged.
   *
   * @param informationMessage
   *            The information message.
   * @see #handleErrorMessage(String)
   */
  void handleInformationMessage(String informationMessage);
}
