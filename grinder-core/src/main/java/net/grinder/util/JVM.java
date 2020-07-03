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
