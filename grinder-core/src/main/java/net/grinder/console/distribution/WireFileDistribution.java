package net.grinder.console.distribution;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import net.grinder.console.model.ConsoleProperties;
import net.grinder.util.Directory;


/**
 * Factory that wires up the FileDistribution. As far as I can see, Pico
 * forces us to use a constructor. Would be nicer if we could say
 * <pre>
 *    container.call(MyFactory.class, "myMethod");
 * </pre>
 *
 * <p>Must be public for PicoContainer.</p>
 *
 * @author Philip Aston
 */
public class WireFileDistribution {

  /**
   * Constructor for WireFileDistribution.
   *
   * @param fileDistribution A file distribution.
   * @param properties The console properties.
   * @param timer A timer.
   */
  public WireFileDistribution(final FileDistribution fileDistribution,
                              ConsoleProperties properties,
                              Timer timer) {

    timer.schedule(new TimerTask() {
        public void run() {
          fileDistribution.scanDistributionFiles();
        }
      },
      properties.getScanDistributionFilesPeriod(),
      properties.getScanDistributionFilesPeriod());


    properties.addPropertyChangeListener(
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
          final String propertyName = e.getPropertyName();

          if (propertyName.equals(
            ConsoleProperties.DISTRIBUTION_DIRECTORY_PROPERTY)) {
            fileDistribution.setDirectory((Directory)e.getNewValue());
          }
          else if (propertyName.equals(
            ConsoleProperties.DISTRIBUTION_FILE_FILTER_EXPRESSION_PROPERTY)) {
            fileDistribution.setFileFilterPattern((Pattern) e.getNewValue());
          }
        }
      });
  }
}
