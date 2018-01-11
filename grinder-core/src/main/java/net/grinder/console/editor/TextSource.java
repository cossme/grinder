// Copyright (C) 2004 Philip Aston
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

package net.grinder.console.editor;

import java.util.EventListener;


/**
 * Something that can edit text.
 *
 * @author Philip Aston
 */
public interface TextSource {

  /**
   * Return the current text.
   *
   * @return The text.
   */
  String getText();

  /**
   * Set the text.
   *
   * @param text The new text.
   */
  void setText(String text);

  /**
   * Return whether the text has changed since the last call to {@link
   * #getText} or {@link #setText}.
   *
   * @return <code>true</code> => the text has changed.
   */
  boolean isDirty();

  /**
   * Listener registration.
   *
   * @param listener The listener.
   */
  void addListener(Listener listener);

  /**
   * Listener interface.
   */
  interface Listener extends EventListener {

    /**
     * Called when the {@link TextSource} has changed.
     *
     * @param dirtyStateChanged <code>true</code> iff the
     * <code>TextSource</code> changed from clean to dirty or vice
     * versa.
     */
    void textSourceChanged(boolean dirtyStateChanged);
  }

  /**
   * Factory interface.
   */
  interface Factory {
    TextSource create();
  }
}
