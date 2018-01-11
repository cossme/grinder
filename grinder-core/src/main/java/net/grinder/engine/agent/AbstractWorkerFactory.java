// Copyright (C) 2005 - 2011 Philip Aston
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
import net.grinder.common.UncheckedGrinderException;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.FanOutStreamSender;
import net.grinder.communication.StreamSender;
import net.grinder.engine.agent.AgentIdentityImplementation.WorkerIdentityImplementation;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.engine.messages.InitialiseGrinderMessage;


/**
 * Core implementation of {@link WorkerFactory}.
 *
 * @author Philip Aston
 */
abstract class AbstractWorkerFactory implements WorkerFactory {
  private final AgentIdentityImplementation m_agentIdentity;
  private final FanOutStreamSender m_fanOutStreamSender;
  private final boolean m_reportToConsole;
  private final ScriptLocation m_script;
  private final GrinderProperties m_properties;
  private WorkerIdentity m_firstWorkerIdentity;

  protected AbstractWorkerFactory(AgentIdentityImplementation agentIdentity,
                                  FanOutStreamSender fanOutStreamSender,
                                  boolean reportToConsole,
                                  ScriptLocation script,
                                  GrinderProperties properties) {
    m_agentIdentity = agentIdentity;
    m_fanOutStreamSender = fanOutStreamSender;
    m_reportToConsole = reportToConsole;
    m_script = script;
    m_properties = properties;
  }

  public Worker create(
    OutputStream outputStream, OutputStream errorStream)
    throws EngineException {

    final WorkerIdentityImplementation workerIdentity =
      m_agentIdentity.createWorkerIdentity();

    if (m_firstWorkerIdentity == null) {
      m_firstWorkerIdentity = workerIdentity;
    }

    final Worker worker = createWorker(workerIdentity,
                                       outputStream,
                                       errorStream);

    final OutputStream processStdin = worker.getCommunicationStream();

    try {
      final InitialiseGrinderMessage initialisationMessage =
        new InitialiseGrinderMessage(workerIdentity,
                                     m_firstWorkerIdentity,
                                     m_reportToConsole,
                                     m_script,
                                     m_properties);

      new StreamSender(processStdin).send(initialisationMessage);
    }
    catch (CommunicationException e) {
      worker.destroy();
      throw new EngineException("Failed to send initialisation message", e);
    }
    catch (UncheckedGrinderException e) {
      worker.destroy();
      throw e;
    }

    m_fanOutStreamSender.add(processStdin);

    return worker;
  }

  protected abstract Worker createWorker(
    WorkerIdentityImplementation workerIdentity,
    OutputStream outputStream,
    OutputStream errorStream) throws EngineException;
}
