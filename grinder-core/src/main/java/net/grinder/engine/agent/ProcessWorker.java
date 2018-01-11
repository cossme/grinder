// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2011 Philip Aston
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

package net.grinder.engine.agent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.grinder.common.UncheckedInterruptedException;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.engine.common.EngineException;
import net.grinder.util.StreamCopier;


/**
 * This class knows how to start a child process. It redirects the
 * child process standard output and error streams to our streams.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @see net.grinder.engine.process.WorkerProcessEntryPoint
 *
 */
final class ProcessWorker implements Worker {

  private final WorkerIdentity m_workerIdentity;
  private final Process m_process;
  private final Redirector m_stdoutRedirector;
  private final Redirector m_stderrRedirector;

  /**
   * Constructor.
   *
   * @param workerIdentity The process identity.
   * @param commandLine Command line arguments and working directory.
   * @param outputStream Output stream to which child process stdout
   * should be redirected. Will not be closed by this class.
   * @param errorStream Output stream to which child process stderr
   * should be redirected. Will not be closed by this class.
   * @throws EngineException If an error occurs.
   */
  public ProcessWorker(WorkerIdentity workerIdentity,
                       CommandLine commandLine,
                       OutputStream outputStream,
                       OutputStream errorStream)
    throws EngineException {

    m_workerIdentity = workerIdentity;

    final ProcessBuilder processBuilder =
      new ProcessBuilder(commandLine.getCommandList());

    processBuilder.directory(commandLine.getWorkingDirectory().getFile());

    try {
      m_process = processBuilder.start();
    }
    catch (IOException e) {
      UncheckedInterruptedException.ioException(e);
      throw new EngineException("Could not start process", e);
    }

    m_stdoutRedirector =
      new Redirector(m_process.getInputStream(),
                     outputStream,
                     m_process.toString());

    m_stderrRedirector =
      new Redirector(m_process.getErrorStream(),
                     errorStream,
                     m_process.toString());
  }

  /**
   * Return the worker name.
   *
   * @return The worker name.
   */
  public WorkerIdentity getIdentity() {
    return m_workerIdentity;
  }

  /**
   * Return an output stream connected to the input stream for the
   * child process.
   *
   * @return The stream.
   */
  public OutputStream getCommunicationStream() {
    return m_process.getOutputStream();
  }

  /**
   * Wait until the worker has completed. Return the exit status.
   *
   * @return See {@link net.grinder.engine.process.WorkerProcessEntryPoint} for
   * valid values.
   */
  public int waitFor() {
    try {
      m_process.waitFor();
    }
    catch (InterruptedException e) {
      throw new UncheckedInterruptedException(e);
    }
    finally {
      m_stdoutRedirector.stop();
      m_stderrRedirector.stop();
    }

    return m_process.exitValue();
  }

  /**
   * Destroy the worker.
   */
  public void destroy() {
    // Experimentation shows we can't interrupt threads blocked waiting
    // on a live process stream, nor can we close that stream.
    //m_stdoutRedirector.stop();
    //m_stderrRedirector.stop();

    // Calling destroy sometimes stoves W2K in such a way that some types
    // of new process can't be launched. (Including Java, Cygwin processes).
    // Replicated with: JRockit 1.4.2_05, and SUN JRE's 1.4.2_05 and 1.5.0_03.
    // A brief sleep appears to be a workaround.

    try {
      Thread.sleep(100);
    }
    catch (InterruptedException e) {
      throw new UncheckedInterruptedException(e);
    }

    m_process.destroy();
  }

  private static class Redirector {

    private final Thread m_thread;

    public Redirector(InputStream inputStream,
                      OutputStream outputStream,
                      String processName) {
      m_thread =
        new Thread(new StreamCopier(4096, false).getRunnable(inputStream,
                                                             outputStream),
                   "Stream redirector for process " + processName);
      m_thread.setDaemon(true);
      m_thread.start();
    }

    public void stop() {
      // We used to interrupt our thread, but that's a dumb idea since there
      // may be pending output to flush.

      while (m_thread.isAlive()) {
        try {
          m_thread.join();
        }
        catch (InterruptedException e) {
          throw new UncheckedInterruptedException(e);
        }
      }
    }
  }
}
