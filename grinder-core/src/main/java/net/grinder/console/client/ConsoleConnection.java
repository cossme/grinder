// Copyright (C) 2006, 2007 Philip Aston
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

package net.grinder.console.client;

import net.grinder.common.GrinderProperties;


/**
 * Console API.
 *
 * <p>
 * <b>Warning: </b> This API is under development and not stable. It will
 * change.</p>
 *
 * @author Philip Aston
 */
public interface ConsoleConnection {

  /**
   * Close the connection.
   */
  void close();

  /**
   * Start the console recording.
   *
   * @throws ConsoleConnectionException If a communication error occurred.
   */
  void startRecording() throws ConsoleConnectionException;

  /**
   * Stop the console recording.
   *
   * @throws ConsoleConnectionException If a communication error occurred.
   */
  void stopRecording() throws ConsoleConnectionException;

  /**
   * Reset the console recording.
   *
   * @throws ConsoleConnectionException If a communication error occurred.
   */
  void resetRecording() throws ConsoleConnectionException;

  /**
   * How many agents are connected?
   *
   * @return The number of agents.
   * @throws ConsoleConnectionException If a communication error occurred.
   */
  int getNumberOfAgents() throws ConsoleConnectionException;

  /**
   * Stop all agent processes.
   *
   * @throws ConsoleConnectionException
   *           If a communication error occurred.
   */
  void stopAgents() throws ConsoleConnectionException;

  /**
   * Start all the worker processes.
   *
   * @param properties
   *            Properties that override the agents' local properties.
   * @throws ConsoleConnectionException
   *           If a communication error occurred.
   */
  void startWorkerProcesses(GrinderProperties properties)
    throws ConsoleConnectionException;

  /**
   * Reset all the worker processes.
   *
   * @throws ConsoleConnectionException
   *           If a communication error occurred.
   */
  void resetWorkerProcesses() throws ConsoleConnectionException;
}
