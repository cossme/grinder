// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2012 Philip Aston
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

package net.grinder.console;

import static net.grinder.util.ClassLoaderUtilities.loadRegisteredImplementations;

import java.io.File;
import java.io.PrintWriter;
import java.util.Timer;

import net.grinder.common.GrinderException;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.MessageDispatchRegistry.AbstractHandler;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.ErrorQueue;
import net.grinder.console.common.Resources;
import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.console.communication.ConsoleCommunicationImplementation;
import net.grinder.console.communication.DistributionControlImplementation;
import net.grinder.console.communication.ProcessControlImplementation;
import net.grinder.console.communication.server.DispatchClientCommands;
import net.grinder.console.distribution.FileDistributionImplementation;
import net.grinder.console.distribution.WireFileDistribution;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.SampleModel;
import net.grinder.console.model.SampleModelImplementation;
import net.grinder.console.model.SampleModelViews;
import net.grinder.console.model.SampleModelViewsImplementation;
import net.grinder.console.synchronisation.WireDistributedBarriers;
import net.grinder.console.textui.TextUI;
import net.grinder.messages.console.RegisterExpressionViewMessage;
import net.grinder.messages.console.RegisterTestsMessage;
import net.grinder.messages.console.ReportStatisticsMessage;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.util.StandardTimeAuthority;

import org.picocontainer.ComponentMonitor;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.LifecycleStrategy;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.Parameter;
import org.picocontainer.behaviors.Caching;
import org.picocontainer.lifecycle.CompositeLifecycleStrategy;
import org.picocontainer.lifecycle.JavaEE5LifecycleStrategy;
import org.picocontainer.lifecycle.StartableLifecycleStrategy;
import org.picocontainer.monitors.NullComponentMonitor;
import org.picocontainer.monitors.WriterComponentMonitor;
import org.picocontainer.parameters.ComponentParameter;
import org.picocontainer.parameters.ConstantParameter;
import org.slf4j.Logger;


/**
 * This is the entry point of The Grinder console.
 *
 * @author Paco Gomez
 * @author Philip Aston
 **/
public final class ConsoleFoundation {

  private static final String DYNAMIC_COMPONENT_RESOURCE_NAME =
      "META-INF/net.grinder.console";

  private final MutablePicoContainer m_container;
  private final Timer m_timer;

  /**
   * Constructor. Locates the console properties in the user's home directory.
   *
   * @param resources Console resources
   * @param logger Logger.
   * @param headless {@code true} => run with the text UI.
   *
   * @exception GrinderException If an error occurs.
   */
  public ConsoleFoundation(Resources resources, Logger logger, boolean headless)
    throws GrinderException {

    this(resources,
         logger,
         headless,
         new Timer(true),
         new ConsoleProperties(
            resources,
            // Some platforms do not have user home directories, fall back
            // to java.home.
            new File(System.getProperty("user.home",
                       System.getProperty("java.home")),
                     ".grinder_console")));
  }

  /**
   * Constructor. Allows properties to be specified.
   *
   * @param resources Console resources
   * @param logger Logger.
   * @param headless {@code true} => run with the text UI.
   * @param timer A timer.
   * @param properties The properties.
   *
   * @exception GrinderException If an error occurs.
   */
  @SuppressWarnings("unchecked")
  public ConsoleFoundation(Resources resources,
                           Logger logger,
                           boolean headless,
                           Timer timer,
                           ConsoleProperties properties)
    throws GrinderException {

    m_timer = timer;

    final ComponentMonitor monitor = new NullComponentMonitor();

    // Allow components to use Disposable/Startable interfaces or
    // JEE annotations.
    final LifecycleStrategy lifecycleStrategy =
        new CompositeLifecycleStrategy(
            new StartableLifecycleStrategy(monitor),
            new JavaEE5LifecycleStrategy(
                new WriterComponentMonitor(new PrintWriter(System.err, true))));

    m_container =
        new DefaultPicoContainer(new Caching(), lifecycleStrategy, null);

    m_container.addComponent(logger);
    m_container.addComponent(resources);
    m_container.addComponent(properties);
    m_container.addComponent(timer);
    m_container.addComponent(StatisticsServicesImplementation.getInstance());
    m_container.addComponent(new StandardTimeAuthority());

    m_container.addComponent(SampleModelImplementation.class);
    m_container.addComponent(SampleModelViewsImplementation.class);
    m_container.addComponent(ConsoleCommunicationImplementation.class);
    m_container.addComponent(DistributionControlImplementation.class);
    m_container.addComponent(ProcessControlImplementation.class);

    m_container.addComponent(
      FileDistributionImplementation.class,
      FileDistributionImplementation.class,
      new Parameter[] {
        new ComponentParameter(DistributionControlImplementation.class),
        new ComponentParameter(ProcessControlImplementation.class),
        new ConstantParameter(properties.getDistributionDirectory()),
        new ConstantParameter(properties.getDistributionFileFilterPattern()),
      });

    m_container.addComponent(DispatchClientCommands.class);

    m_container.addComponent(WireFileDistribution.class);

    m_container.addComponent(WireMessageDispatch.class);

    m_container.addComponent(ErrorQueue.class);

    m_container.addComponent(WireDistributedBarriers.class);

    // Dynamically load other component implementations found from
    // META-INF/net.grinder.console property files.
    final ClassLoader classLoader = getClass().getClassLoader();

    Class<? extends UI> uiClass = headless ? TextUI.class : null;

    for (Class<?> implementation :
      loadRegisteredImplementations(DYNAMIC_COMPONENT_RESOURCE_NAME,
                                    classLoader)) {

      if (uiClass == null && UI.class.isAssignableFrom(implementation)) {
        // First UI implementation wins, others are ignored.
        uiClass = (Class<? extends UI>) implementation;
      }
      else {
        // Implementations will be instantiated here if they are startable
        // - see http://picocontainer.org/lifecycle.html. Otherwise, they
        // are lazily created as other components require them.
        m_container.addComponent(implementation);
      }
    }

    if (uiClass == null) {
      uiClass = TextUI.class;
    }

    final UI ui =  m_container.addComponent(uiClass).getComponent(uiClass);

    final ErrorQueue errorQueue = m_container.getComponent(ErrorQueue.class);
    errorQueue.setErrorHandler(ui.getErrorHandler());
  }

  /**
   * Console message event loop. Dispatches communication messages
   * appropriately. Blocks until we are {@link #shutdown()}.
   */
  public void run() {
    m_container.start();

    // Request components, or they won't be instantiated.
    m_container.getComponent(WireMessageDispatch.class);
    m_container.getComponent(WireFileDistribution.class);
    m_container.getComponent(WireDistributedBarriers.class);

    final ConsoleCommunication communication =
      m_container.getComponent(ConsoleCommunication.class);

    while (communication.processOneMessage()) {
      // Process until communication is shut down.
    }
  }


  /**
   * Shut down the console.
   *
   * <p>Once stopped, the instance cannot be restarted.</p>
   */
  public void shutdown() {

    m_timer.cancel();

    m_container.dispose();
  }

  /**
   * Contract for user interfaces.
   *
   * @author Philip Aston
   */
  public interface UI {
    /**
     * Return an error handler to which errors should be reported.
     *
     * @return The error handler.
     */
    ErrorHandler getErrorHandler();
  }

  /**
   * Factory that wires up the message dispatch.
   *
   * <p>Must be public for PicoContainer.</p>
   *
   * @see WireFileDistribution
   */
  public static class WireMessageDispatch {

    /**
     * Constructor.
     *
     * @param communication Console communication.
     * @param model Console sample model.
     * @param sampleModelViews Console sample model views
     * @param dispatchClientCommands Client command dispatcher.
     */
    public WireMessageDispatch(ConsoleCommunication communication,
                               final SampleModel model,
                               final SampleModelViews sampleModelViews,
                               DispatchClientCommands dispatchClientCommands) {

      final MessageDispatchRegistry messageDispatchRegistry =
        communication.getMessageDispatchRegistry();

      messageDispatchRegistry.set(
        RegisterTestsMessage.class,
        new AbstractHandler<RegisterTestsMessage>() {
          public void handle(RegisterTestsMessage message) {
            model.registerTests(message.getTests());
          }
        });

      messageDispatchRegistry.set(
        ReportStatisticsMessage.class,
        new AbstractHandler<ReportStatisticsMessage>() {
          public void handle(ReportStatisticsMessage message) {
            model.addTestReport(message.getStatisticsDelta());
          }
        });

      messageDispatchRegistry.set(
        RegisterExpressionViewMessage.class,
        new AbstractHandler<RegisterExpressionViewMessage>() {
          public void handle(RegisterExpressionViewMessage message) {
            sampleModelViews.registerStatisticExpression(
              message.getExpressionView());
          }
        });

      dispatchClientCommands.registerMessageHandlers(messageDispatchRegistry);
    }
  }
}
