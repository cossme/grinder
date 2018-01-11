// Copyright (C) 2011 - 2013 Philip Aston
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

package net.grinder.scriptengine;

import java.util.List;

import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;

/**
 * Service interface that script engines should implement.
 *
 * <p>
 * The Grinder discovers script engine implementations from the {@code
 * META-INF/net.grinder.scriptengines} resource files. Each engine should
 * register its class name as a line in such a resource file. The order is
 * important, since it determines priority. The engines are ordered first by
 * the class path search order used to load the resource files, and then the
 * order of lines in the resource files. Earlier engines have higher priority.
 * </p>
 *
 * <p>
 * Each script engine is injected with framework services using PicoContainer.
 * The use of PicoContainer should be transparent; implementations simply need
 * to declare the services they require as constructor parameters.
 * </p>
 *
 * <p>
 * Available services include:
 * </p>
 *
 * <ul>
 * <li>{@link net.grinder.common.Logger}</li>
 * <li>{@link net.grinder.common.GrinderProperties}</li>
 * <li>{@link net.grinder.script.ScriptContext}</li>
 * <li>{@link net.grinder.scriptengine.DCRContext}</li>
 * </ul>
 *
 * <p>
 * A {@code DCRContext} will be provided only if DCR is available. Engines that
 * use DCR should have two constructors, one of them requiring a {@code
 * DCRContext}, and one of them not. The latter constructor will be used if DCR
 * is unavailable.
 * </p>
 *
 * @author Philip Aston
 */
public interface ScriptEngineService {

  /**
   * All resources with this name are loaded to discover implementations.
   */
  String RESOURCE_NAME = "META-INF/net.grinder.scriptengine";

  /**
   * If the script engine service can handle the given script, it should return
   * a suitable implementation.
   *
   * <p>
   * Implementations typically will execute the script and perform any
   * process level initialisation.
   * </p>
   *
   * @param script
   *          The script.
   * @return The script engine, or {@code null}.
   * @throws EngineException
   *           If an implementation could not be created.
   */
  ScriptEngine createScriptEngine(ScriptLocation script) throws EngineException;

  /**
   * Initialises script engine instrumentation.
   *
   * <p>
   * Each script engine can provide instrumenters, irrespective of the engine
   * used to execute the script. The instrumenters provided by each engine are
   * consulted according to service registration order in the META-INF file.
   * </p>
   *
   * @return Additional instrumenters to use. Engines that do not provide
   *         instrumentation should return an empty list.
   * @throws EngineException
   *           If a problem occurred creating instrumenters.
   */
  List<? extends Instrumenter> createInstrumenters() throws EngineException;

  /**
   * Handler for a particular type of script.
   */
  interface ScriptEngine {

    /**
     * Create a {@link WorkerRunnable} that will be used to run the work
     * for one worker thread. The {@link WorkerRunnable} will forward to
     * a new instance of the script's {@code TestRunner} class.
     *
     * @return The runnable.
     * @throws EngineException If the runnable could not be created.
     */
    WorkerRunnable createWorkerRunnable() throws EngineException;

    /**
     * Create a {@link WorkerRunnable} that will be used to run the work
     * for one worker thread. The {@link WorkerRunnable} will forward to
     * a the supplied {@code TestRunner}.
     *
     * @param testRunner An existing script instance that is callable.
     * @return The runnable.
     * @throws EngineException If the runnable could not be created.
     */
    WorkerRunnable createWorkerRunnable(Object testRunner)
      throws EngineException;

    /**
     * Shut down the engine.
     *
     * @throws EngineException If the engine could not be shut down.
     */
    void shutdown() throws EngineException;

    /**
     * Returns a description of the script engine for the log.
     *
     * @return The description.
     */
    String getDescription();
  }

  /**
   * Interface to the runnable script object for a particular worker thread.
   */
  interface WorkerRunnable {
    void run() throws ScriptExecutionException;

    void shutdown() throws ScriptExecutionException;
  }
}
