// Copyright (C) 2004 - 2012 Philip Aston
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

package net.grinder.util;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.slf4j.Logger;

import net.grinder.common.GrinderBuild;
import net.grinder.common.GrinderException;


/**
 * Utility class for querying JVM environment.
 *
 * @author Philip Aston
 */
public final class JVM {

  /**
   * Get a JVM instance that represents the current JVM.
   *
   * @return The instance.
   */
  public static JVM getInstance() {
    return new JVM();
  }

  private JVM() {
  }

  /**
   * Check the JVM is the right version, has the right optional components
   * installed, and so on. If there are problems, report them to the logger and
   * return {@code false}.
   *
   * @param logger
   *          Where to report any problems.
   * @return {@code true} => we have everything.
   * @exception VersionException
   *              If the JVM's version could not be parsed.
   */
  public boolean haveRequisites(Logger logger) throws VersionException {

    final String name = "The Grinder " + GrinderBuild.getVersionString();

    if (!isAtLeastVersion(1, 6)) {
      logger.error("Fatal Error - incompatible version of Java ({})%n" +
                   "{} requires at least Java SE 6.",
                   this,
                   name);
      return false;
    }

    return true;
  }

  /**
   * Check whether the JVM is of given version or later.
   *
   * @param minimumMajor
   *          Major version number.
   * @param minimumMinor
   *          Minor version number.
   * @return {@code true} => the JVM is at least the requested version.
   * @exception VersionException
   *              If the JVM's version could not be parsed.
   */
  public boolean isAtLeastVersion(int minimumMajor, int minimumMinor)
    throws VersionException {

    final String version = System.getProperty("java.version");
    final StringTokenizer versionTokenizer = new StringTokenizer(version, ".");

    try {
      final int major = Integer.parseInt(versionTokenizer.nextToken());
      final int minor = Integer.parseInt(versionTokenizer.nextToken());

      return
        major >= minimumMajor &&
        minor >= minimumMinor;
    }
    catch (NoSuchElementException e) {
      throw new VersionException("Could not parse JVM version " + version);
    }
    catch (NumberFormatException e) {
      throw new VersionException("Could not parse JVM version " + version);
    }
  }

  /**
   * Return a description of the JVM.
   *
   * @return The description.
   */
  public String toString()  {
    return System.getProperty("java.runtime.name") + " " +
      System.getProperty("java.runtime.version") + ": " +
      System.getProperty("java.vm.name") + " (" +
      System.getProperty("java.vm.version") + ", " +
      System.getProperty("java.vm.info") +
      ") on " + System.getProperty("os.name") + " " +
      System.getProperty("os.arch") + " " +
      System.getProperty("os.version");
  }

  /**
   * Represents problems in determining JVM versions.
   */
  public static final class VersionException extends GrinderException {

    /**
     * Constructor.
     *
     * @param reason A message.
     */
    public VersionException(String reason) {
      super(reason);
    }
  }
}
