// Copyright (C) 2007 - 2009 Philip Aston
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import net.grinder.common.GrinderException;
import net.grinder.console.distribution.AgentCacheState;

/**
 * Handles opening a file in external editor.
 *
 * @author Philip Aston
 */
class ExternalEditor {

  private static final ThreadGroup s_threadGroup =
    new ThreadGroup("ExternalEditor");

  static final ThreadGroup getThreadGroup() {
    return s_threadGroup;
  }

  private final AgentCacheState m_agentCacheState;
  private final EditorModel m_editorModel;
  private final File m_command;
  private final String m_arguments;

  public ExternalEditor(AgentCacheState agentCacheState,
                        EditorModel editorModel,
                        File command,
                        String arguments) {
    m_agentCacheState = agentCacheState;
    m_editorModel = editorModel;
    m_command = command;
    m_arguments = arguments;
  }


  String[] fileToCommandLine(File file) {
    final List<String> result = new ArrayList<String>();
    result.add(m_command.getAbsolutePath());

    boolean fileTemplateFound = false;

    if (m_arguments != null) {
      final StringTokenizer tokenizer = new StringTokenizer(m_arguments);

      while (tokenizer.hasMoreElements()) {
        final String token = tokenizer.nextToken();

        final String argument = token.replaceAll("%f", file.getAbsolutePath());
        result.add(argument);

        fileTemplateFound |= !argument.equals(token);
      }
    }

    if (!fileTemplateFound) {
      result.add(file.getAbsolutePath());
    }

    return result.toArray(new String[result.size()]);
  }

  public void open(final File file) throws IOException {
    final long originalModificationTime = file.lastModified();

    final Process exec = Runtime.getRuntime().exec(
      fileToCommandLine(file),
      null,
      file.getParentFile());

    final Runnable handleCompletion = new Runnable() {
      public void run() {
        try {
          // We ignore the process result when figuring out whether the
          // editor changed the file. I don't trust all editors to return
          // 0 on success.
          exec.waitFor();
        }
        catch (InterruptedException e) {
          // This thread has been interrupted, silently exit.
          return;
        }

        // If file no longer exists, lastModified will be 0.
        final long lastModified = file.lastModified();

        if (lastModified > originalModificationTime) {
          m_agentCacheState.setNewFileTime(lastModified);

          // If there is an existing, clean buffer refresh it from the file.
          final Buffer buffer = m_editorModel.getBufferForFile(file);

          if (buffer != null && !buffer.isDirty()) {
            try {
              buffer.load();
            }
            catch (GrinderException e) {
              // Ignore.
            }
          }
        }
      }
    };

    final Thread thread = new Thread(getThreadGroup(),
                                     handleCompletion,
                                     "External edit of " + file);
    thread.setDaemon(true);
    thread.start();
  }
}
