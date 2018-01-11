// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2012 Philip Aston
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

package net.grinder;

import java.io.File;

import net.grinder.common.GrinderException;
import net.grinder.engine.agent.Agent;
import net.grinder.engine.agent.AgentDaemon;
import net.grinder.engine.agent.AgentImplementation;
import net.grinder.util.AbstractMainClass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is the entry point of The Grinder agent process.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @author Pawel Lacinski
 */
public final class Grinder extends AbstractMainClass {

  private static final String USAGE =
    "  java " + Grinder.class.getName() +
    " <options> [alternatePropertiesFilename]" +
    "\n\n" +
    "Options:" +
    "\n  [-daemon [n]]                Run agent in deamon mode; try to" +
    "\n                               reconnect every n seconds (default 60)." +
    "\n\n";

  /**
   * The Grinder agent process entry point.
   *
   * @param args Command line arguments.
   */
  public static void main(String[] args) {

    final Logger logger = LoggerFactory.getLogger("agent");

    try {
      final Grinder grinder = new Grinder(args, logger);
      grinder.run();
    }
    catch (LoggedInitialisationException e) {
      System.exit(1);
    }
    catch (Throwable e) {
      logger.error(e.getMessage(), e);
      System.exit(2);
    }

    System.exit(0);
  }

  private final Agent m_agent;

  private Grinder(String[] args, Logger logger) throws GrinderException {
    super(logger, USAGE);

    File propertiesFile = null;
    long daemonPeriod = -1;

    for (int i = 0; i < args.length; ++i) {
      if ("-daemon".equalsIgnoreCase(args[i])) {
        daemonPeriod = 60000;

        try {
          daemonPeriod = Integer.parseInt(args[i + 1]) * 1000L;
          ++i;
        }
        catch (IndexOutOfBoundsException e) {
          // Ignore.
        }
        catch (NumberFormatException e) {
          // Ignore.
        }
      }
      else if (i == args.length - 1 && !args[i].startsWith("-")) {
        propertiesFile = new File(args[i]);
      }
      else {
        throw barfUsage();
      }
    }

    if (daemonPeriod > -1) {
      m_agent =
        new AgentDaemon(
          logger,
          daemonPeriod,
          new AgentImplementation(logger, propertiesFile, false));
    }
    else {
      m_agent = new AgentImplementation(logger, propertiesFile, true);
    }
  }

  private void run() throws GrinderException {
    m_agent.run();
    m_agent.shutdown();
  }
}
