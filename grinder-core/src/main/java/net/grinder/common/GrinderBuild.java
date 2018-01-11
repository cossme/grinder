// Copyright (C) 2003 - 2011 Philip Aston
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

package net.grinder.common;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;


/**
 * Accessor for build version information.
 *
 * @author Philip Aston
 */
public final class GrinderBuild {

  private static final String s_versionString;

  static {
    try {
      final InputStream buildPropertiesStream =
        GrinderBuild.class.getResourceAsStream(
          "resources/build.properties");

        if (buildPropertiesStream == null) {
          throw new IOException("Could not find build.properties");
        }

      try {
        final Properties properties = new Properties();
        properties.load(buildPropertiesStream);

        s_versionString = properties.getProperty("version");
      }
      finally {
        buildPropertiesStream.close();
      }
    }
    catch (IOException e) {
      UncheckedInterruptedException.ioException(e);
      throw new ExceptionInInitializerError(e);
    }
  }

  /** Disabled constructor. Package scope for unit tests. */
  GrinderBuild() {
    throw new UnsupportedOperationException();
  }

  /**
   * Return the public name of this build.
   *
   * @return The name as a String.
   */
  public static String getName() {
    return "The Grinder " + getVersionString();
  }

  /**
   * Return the build version.
   *
   * @return The build version as a String.
   */
  public static String getVersionString() {
    return s_versionString;
  }
}
