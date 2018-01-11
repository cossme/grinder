// Copyright (C) 2011 Philip Aston
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

package net.grinder.testutility;

import static java.util.Arrays.asList;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.grinder.util.BlockingClassLoader;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;


/**
 * A JUnit test class runner that sets up the context for different Jython
 * installations.
 *
 * @author Philip Aston
 */
public abstract class JythonVersionRunner extends Suite {

  protected static List<String> getHomes(String... homesProperties) {
    final List<String> homes = new ArrayList<String>();

    for (String property : homesProperties) {
      final String pythonHome = System.getProperty(property);

      if (pythonHome == null) {
        System.err.println("***** " + property +
                           " not set, skipping tests for Jython version.");
      }
      else {
        homes.add(pythonHome);
      }
    }

    return homes;
  }

  private static final Set<String> ISOLATED_CLASSES =
    new HashSet<String>(asList("net.grinder.*",
                               "org.python.*",
                               "grinder.test.*",
                               "org.mockito.*"));

  private static final Set<String> SHARED_CLASSES =
    new HashSet<String>(asList("net.grinder.util.weave.agent.*"));

  public JythonVersionRunner(Class<?> testClass,
                             List<String> pythonHomes)
    throws ClassNotFoundException, InitializationError, MalformedURLException{

    super(testClass, createRunners(testClass, pythonHomes));
  }

  private static List<Runner> createRunners(Class<?> testClass,
                                            List<String> pythonHomes)
   throws ClassNotFoundException, InitializationError, MalformedURLException {

    final List<Runner> runners = new ArrayList<Runner>();

    for (String pythonHome : pythonHomes) {
      final URL jythonJarURL =
        new URL("file://" + pythonHome + "/jython.jar");

      final ClassLoader loader =
        new BlockingClassLoader(Arrays.asList(jythonJarURL),
                                Collections.<String>emptySet(),
                                ISOLATED_CLASSES,
                                SHARED_CLASSES,
                                true);

      final Class<?> isolatedClass = loader.loadClass(testClass.getName());

      runners.add(new PythonHomeRunner(isolatedClass, pythonHome));
    }

    if (runners.size() == 0) {
      throw new InitializationError("No python home properties set");
    }

    return runners;
  }

  private static class PythonHomeRunner extends BlockJUnit4ClassRunner {

    private final String m_pythonHome;

    public PythonHomeRunner(Class<?> klass, String pythonHome)
      throws InitializationError {
      super(klass);

      m_pythonHome = pythonHome;
    }

    @Override public Description getDescription() {
      final Class<?> javaClass = getTestClass().getJavaClass();

      final Description result =
        Description.createSuiteDescription(
          javaClass.getSimpleName() + " [" + m_pythonHome + "]",
          javaClass.getAnnotations());

      for (Description child : super.getDescription().getChildren()) {
        result.addChild(child);
      }

      return result;
    }

    @Override protected String testName(FrameworkMethod method) {
      return super.testName(method) + " [" + m_pythonHome + "]";
    }

    @Override protected void runChild(FrameworkMethod method,
                                      RunNotifier notifier) {
      final String oldPythonHome = System.getProperty("python.home");

      System.setProperty("python.home", m_pythonHome);

      try {
        super.runChild(method, notifier);
      }
      finally {
        if (oldPythonHome != null) {
          System.setProperty("python.home", oldPythonHome);
        }
        else {
          System.clearProperty("python.home");
        }
      }
    }
  }
}
