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

package net.grinder.testutility;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.grinder.util.BlockingClassLoader;

import org.junit.After;
import org.junit.Before;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

/**
 * A JUnit test class runner that sets up the context for different Jython
 * installations.
 *
 * @author Philip Aston
 */
public abstract class JythonVersionRunner extends Suite {
  /**
   * Jython has global state that can't be reset (Jython bug 2053). Currently a
   * separate classloader, hence a fresh interpreter, is used for every test
   * class. Applying this annotation to a test ensures that a fresh interpreter
   * will be used for the test . We don't do this for every test because 1.
   * efficiency, 2. Jython has classloader leaks (e.g. Jython bug 2054), so we
   * run out of perm space.
   *
   * <p>
   * We may re-visit sharing of interpreters across test classes further to
   * reduce the footprint.
   *
   * @author Philip Aston
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public static @interface UseSeparateInterpreter {

  }

  protected static List<String> getHomes(final String... homesProperties) {
    final List<String> homes = new ArrayList<String>();

    for (final String property : homesProperties) {
      final String pythonHome = System.getProperty(property);

      if (pythonHome == null) {
        System.err.println("***** " + property +
            " not set, skipping tests for Jython version.");
      }
      else {
        final File f = new File(pythonHome);

        final String description = property +
            " property is set, but value [" + f + "]";

        assertTrue(description + " is not a directory", f.isDirectory());
        assertTrue(description + " is not readable", f.canRead());

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
      new HashSet<String>(asList(
        "net.grinder.util.weave.agent.*",
        "org.python.google.*"));

  public JythonVersionRunner(final Class<?> testClass,
                             final List<String> pythonHomes)
      throws ClassNotFoundException,
      InitializationError,
      MalformedURLException {

    super(testClass, createRunners(testClass, pythonHomes));
  }

  private static List<Runner> createRunners(
    final Class<?> testClass,
    final List<String> pythonHomes)
      throws ClassNotFoundException,
      InitializationError,
      MalformedURLException {

    final List<Runner> runners = new ArrayList<Runner>();

    for (final String pythonHome : pythonHomes) {
      runners.add(new PythonRunner(testClass, pythonHome));
    }

    if (runners.size() == 0) {
      throw new InitializationError("No python home properties set");
    }

    return runners;
  }

  private static class PythonRunner extends BlockJUnit4ClassRunner {

    private final String m_pythonHome;

    private final URL m_jythonJarURL;

    private ClassLoader m_lastLoader;

    public PythonRunner(final Class<?> klass, final String pythonHome)
        throws InitializationError, MalformedURLException {
      super(klass);

      m_pythonHome = pythonHome;
      m_jythonJarURL = new URL("file://" + pythonHome + "/jython.jar");
    }

    @Override
    public Description getDescription() {
      final Class<?> javaClass = getTestClass().getJavaClass();

      final Description result =
          Description.createSuiteDescription(
            javaClass.getSimpleName() + " [" +
                m_pythonHome + "]",
            javaClass.getAnnotations());

      for (final Description child : super.getDescription().getChildren()) {
        result.addChild(child);
      }

      return result;
    }

    @Override
    protected String testName(final FrameworkMethod method) {
      return super.testName(method) + " [" + m_pythonHome + "]";
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Statement methodBlock(final FrameworkMethod method) {
      final ClassLoader loader;

      if (m_lastLoader == null ||
          method.getAnnotation(UseSeparateInterpreter.class) != null) {
        loader = new BlockingClassLoader(Arrays.asList(m_jythonJarURL),
          Collections.<String> emptySet(),
          ISOLATED_CLASSES,
          SHARED_CLASSES,
          true);
        m_lastLoader = loader;
      }
      else {
        loader = m_lastLoader;
      }

      final Statement isolatedBlock;

      try {
        final Method testMethod = method.getMethod();
        final Class<?> isolatedClass =
            loader.loadClass(getTestClass().getName());
        final TestClass isolatedTestClass = new TestClass(isolatedClass);

        final FrameworkMethod isolatedMethod = new FrameworkMethod(
          isolatedClass.getMethod(
            testMethod.getName(),
            testMethod.getParameterTypes()));

        final Object test =
            isolatedTestClass.getOnlyConstructor().newInstance();

        Statement statement = methodInvoker(isolatedMethod, test);
        statement =
            possiblyExpectingExceptions(isolatedMethod, test, statement);
        statement = withPotentialTimeout(isolatedMethod, test, statement);

        final List<FrameworkMethod> befores =
            isolatedTestClass.getAnnotatedMethods(Before.class);
        statement = new RunBefores(statement, befores, test);

        final List<FrameworkMethod> afters =
            isolatedTestClass.getAnnotatedMethods(After.class);
        statement = new RunAfters(statement, afters, test);

        isolatedBlock = statement;
      }
      catch (final Exception e) {
        return new Fail(e);
      }

      return new PythonHomeDecorator(m_pythonHome, isolatedBlock);
    }

    private static final class PythonHomeDecorator extends Statement {
      private final String m_pythonHome;

      private final Statement m_delegate;

      private PythonHomeDecorator(final String pythonHome,
                                  final Statement delegate) {
        m_pythonHome = pythonHome;
        m_delegate = delegate;
      }

      @Override
      public void evaluate() throws Throwable {
        final String oldPythonHome = System.getProperty("python.home");

        System.setProperty("python.home", m_pythonHome);

        try {
          m_delegate.evaluate();
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
}
