// Copyright (C) 2005 - 2010 Philip Aston
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

package net.grinder.console.distribution;

import net.grinder.util.FileContents.FileContentsException;


/**
 * Something that can handle the distribution of files.
 *
 * @author Philip Aston
 */
public interface FileDistributionHandler {

  /**
   * Result of sending a file.
   */
  public interface Result {

    /**
     * Progress through the file distribution set.
     *
     * @return A number between 0 and 99.
     */
    int getProgressInCents();

    /**
     * The file name of file just distributed.
     *
     * @return The file name.
     */
    String getFileName();
  }

  /**
   * Send the next file.
   *
   * @return A {@link Result} or <code>null</code> if there are more
   * files to process.
   * @throws FileContentsException If an error occurs
   * sending the file.
   */
  Result sendNextFile() throws FileContentsException;
}
