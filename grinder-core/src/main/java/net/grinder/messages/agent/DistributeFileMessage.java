// Copyright (C) 2004 - 2008 Philip Aston
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

package net.grinder.messages.agent;

import net.grinder.communication.Message;
import net.grinder.util.FileContents;


/**
 * Message used to distribute a file from the console to the agent
 * processes.
 *
 * @author Philip Aston
 */
public final class DistributeFileMessage implements Message {

  private static final long serialVersionUID = -4338519775293350257L;

  private final FileContents m_fileContents;

  /**
   * Constructor.
   *
   * @param fileContents The file contents to distribute.
   */
  public DistributeFileMessage(FileContents fileContents) {
    m_fileContents = fileContents;
  }

  /**
   * Return the file contents.
   *
   * @return The file contents.
   */
  public FileContents getFileContents() {
    return m_fileContents;
  }
}
