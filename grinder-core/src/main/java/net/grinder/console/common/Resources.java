// Copyright (C) 2005 Philip Aston
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

import javax.swing.ImageIcon;

/**
 * Type safe interface to resource bundle.
 *
 * @author Philip Aston
 */
public interface Resources {

  /**
   * Overloaded version of {@link #getString(String, boolean)} which
   * writes out a waning if the resource is missing.
   * @param key The resource key.
   * @return The string.
   */
  String getString(String key);

  /**
   * Use key to look up resource which names image URL. Return the image.
   * @param key The resource key.
   * @param warnIfMissing true => write out an error message if the
   * resource is missing.
   * @return The string.
   */
  String getString(String key, boolean warnIfMissing);

  /**
   * Overloaded version of {@link #getImageIcon(String, boolean)}
   * which doesn't write out a waning if the resource is missing.
   *
   * @param key The resource key.
   * @return The image.
   */
  ImageIcon getImageIcon(String key);

  /**
   * Use key to look up resource which names image URL. Return the image.
   *
   * @param key The resource key.
   * @param warnIfMissing true => write out an error message if the
   * resource is missing.
   * @return The image
   */
  ImageIcon getImageIcon(String key, boolean warnIfMissing);

  /**
   * Use <code>key</code> to identify a file by URL. Return contents
   * of file as a String.
   *
   * @param key Resource key used to look up URL of file.
   * @param warnIfMissing true => write out an error message if the
   * resource is missing.
   * @return Contents of file.
   */
  String getStringFromFile(String key, boolean warnIfMissing);
}
