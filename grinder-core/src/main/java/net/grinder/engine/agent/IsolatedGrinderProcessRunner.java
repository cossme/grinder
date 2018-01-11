// Copyright (C) 2005 - 2008 Philip Aston
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

import java.io.InputStream;

import net.grinder.engine.agent.DebugThreadWorker.IsolateGrinderProcessRunner;
import net.grinder.engine.process.WorkerProcessEntryPoint;


/**
 * Implementation of {@link DebugThreadWorker.IsolateGrinderProcessRunner} that
 * is loaded in separate {@link net.grinder.util.BlockingClassLoader}s by
 * {@link DebugThreadWorker}.
 *
 * @author Philip Aston
 */
public class IsolatedGrinderProcessRunner
  implements IsolateGrinderProcessRunner {

  /**
   * Create and run a
   * {@link net.grinder.engine.process.WorkerProcessEntryPoint}.
   *
   * @param agentInputStream
   *          {@link InputStream} used to listen to the agent.
   * @return Process exit code.
   */
  public int run(final InputStream agentInputStream) {

    final WorkerProcessEntryPoint workerProcessEntryPoint =
      new WorkerProcessEntryPoint();

    return workerProcessEntryPoint.run(agentInputStream);
  }
}
