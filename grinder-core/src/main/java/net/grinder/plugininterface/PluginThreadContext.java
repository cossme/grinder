// Copyright (C) 2000 Paco Gomez
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

package net.grinder.plugininterface;


/**
 * <p>This class is used to share thread information between the
 * Grinder and the plug-in. </p>
 *
 * @author Paco Gomez
 * @author Philip Aston
 */
public interface PluginThreadContext {

  /**
   * Return the thread number.
   *
   * @return The thread number.
   */
  int getThreadNumber();

  /**
   * Return the current run number.
   *
   * @return The current run number.
   */
  int getRunNumber();

  /**
   * The time taken between invocations of {@link #pauseClock()} and
   * {@link #resumeClock} is not included in the recorded time for a test. This
   * allows plug-ins to discount the cost of expensive pre or post processing.
   *
   * <p>
   * Has no effect if called when the is no dispatch in progress.
   * </p>
   */
  void pauseClock();

  /**
   * @see #pauseClock()
   */
  void resumeClock();
}
