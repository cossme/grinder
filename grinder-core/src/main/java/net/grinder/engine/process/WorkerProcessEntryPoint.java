// Copyright (C) 2005 - 2013 Philip Aston
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

package net.grinder.engine.process;

import java.io.InputStream;

import net.grinder.communication.StreamReceiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Entry point for processes launched by the agent.
 *
 * @author Philip Aston
 */
public class WorkerProcessEntryPoint {

  /**
   * Main method.
   *
   * <p>
   * This is not intended to be used directly; you should always start The
   * Grinder by starting an agent process. If you're debugging, you might want
   * to use the single threaded mode if you want the worker "process" to be
   * launched in the same JVM as the agent. See the grinder.debug.singleprocess
   * property.
   * </p>
   *
   * @param args
   *          Command line arguments.
   */
  // CHECKSTYLE.OFF: Regexp - System.err
  public static void main(final String[] args) {
    if (args.length > 1) {
      System.err.println("Usage: java " + GrinderProcess.class.getName());
      System.exit(-1);
    }

    final int exitCode = new WorkerProcessEntryPoint().run(System.in);

    System.exit(exitCode);
  }
  // CHECKSTYLE.ON: Regexp

  /**
   * Create and run a process.
   *
   * @param agentCommunicationStream The agent communication stream.
   * @return Process exit code.
   */
  public int run(final InputStream agentCommunicationStream) {

    final Logger logger = LoggerFactory.getLogger("worker-bootstrap");

    final GrinderProcess grinderProcess;

    try {
      grinderProcess =
        new GrinderProcess(new StreamReceiver(agentCommunicationStream));
    }
    catch (final Exception e) {
      logger.error("Error initialising worker process", e);
      return -2;
    }

    try {
      grinderProcess.run();
      return 0;
    }
    catch (final Exception e) {
      logger.error("Error running worker process", e);
      return -3;
    }
    catch (final Error t) {
      logger.error("Error running worker process", t);
      throw t;
    }
    finally {
      grinderProcess.shutdown(agentCommunicationStream == System.in);
    }
  }
}
