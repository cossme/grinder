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

package net.grinder.engine.agent;

import java.io.OutputStream;

import net.grinder.common.GrinderProperties;
import net.grinder.communication.FanOutStreamSender;
import net.grinder.engine.agent.AgentIdentityImplementation.WorkerIdentityImplementation;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;


/**
 * Class that starts workers as separate processes.
 *
 * @author Philip Aston
 */
final class ProcessWorkerFactory extends AbstractWorkerFactory {

  private final WorkerProcessCommandLine m_commandLine;

  public ProcessWorkerFactory(WorkerProcessCommandLine commandLine,
                              AgentIdentityImplementation agentIdentity,
                              FanOutStreamSender fanOutStreamSender,
                              boolean reportToConsole,
                              ScriptLocation script,
                              GrinderProperties properties) {
    super(agentIdentity,
          fanOutStreamSender,
          reportToConsole,
          script,
          properties);

    m_commandLine = commandLine;
  }

  @Override
  protected Worker createWorker(WorkerIdentityImplementation workerIdentity,
                                OutputStream outputStream,
                                OutputStream errorStream)
    throws EngineException {

    return new ProcessWorker(workerIdentity,
                             m_commandLine,
                             outputStream,
                             errorStream);
  }
}
