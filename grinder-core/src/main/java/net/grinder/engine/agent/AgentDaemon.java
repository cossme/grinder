// Copyright (C) 2008 - 2011 Philip Aston
// Copyright (C) 2008 Pawel Lacinski
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

import net.grinder.common.GrinderException;
import net.grinder.util.Sleeper;
import net.grinder.util.Sleeper.ShutdownException;
import net.grinder.util.SleeperImplementation;
import net.grinder.util.StandardTimeAuthority;

import org.slf4j.Logger;


/**
 * Agent Daemon. This is a wrapper for an {@link Agent}. It runs like a daemon
 * and if Agent finishes its work it sleeps and runs it again.
 *
 * @author Pawel Lacinski
 * @author Philip Aston
 */
public final class AgentDaemon implements Agent {

  private final Logger m_logger;
  private final long m_sleepTime;
  private final Agent m_delegateAgent;
  private final Sleeper m_sleeper;
  private final Thread m_shutdownHook;

  /**
   * Constructor for AgentDaemon.
   *
   * @param logger A Logger.
   * @param sleepTime Time in milliseconds between connection attempts.
   * @param agent Delegate Agent that we want to run.
   */
  public AgentDaemon(Logger logger, long sleepTime,  Agent agent) {
    this(logger,
         sleepTime,
         agent,
         new SleeperImplementation(new StandardTimeAuthority(), logger, 1, 0));
  }

  /**
   * Package scope for unit tests.
   */
  AgentDaemon(Logger logger, long sleepTime, Agent agent, Sleeper sleeper) {
    m_logger = logger;
    m_delegateAgent = agent;
    m_sleepTime = sleepTime;
    m_sleeper = sleeper;
    m_shutdownHook = new Thread(new ShutdownHook());
  }

  /**
   * Start the agent.
   *
   * @throws GrinderException If an error occurs.
   */
  public void run() throws GrinderException {

    Runtime.getRuntime().addShutdownHook(m_shutdownHook);

    try {
      while (true) {
        m_delegateAgent.run();

        m_logger.info("agent finished");

        m_sleeper.sleepNormal(m_sleepTime);
      }
    }
    catch (ShutdownException e) {
      // Exiting.
    }
  }

  /**
   * Shut down the agent.
   */
  public void shutdown() {
    m_sleeper.shutdown();

    m_delegateAgent.shutdown();
  }

  /**
   * For unit tests.
   *
   * @return The shutdown hook.
   */
  Thread getShutdownHook() {
    return m_shutdownHook;
  }

  private class ShutdownHook implements Runnable {
    public void run() {
      shutdown();
    }
  }
}
