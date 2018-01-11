// Copyright (C) 2009 - 2013 Philip Aston
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

import static java.util.Arrays.asList;
import static java.util.Collections.enumeration;
import static java.util.Collections.list;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Class loader that selectively ignores its parents, allowing alternative
 * implementations of classes to be loaded.
 *
 * @author Philip Aston
 */
public class BlockingClassLoader extends URLClassLoader {

  private final boolean m_respectGrandparents;
  private final Classes m_blocked;
  private final Classes m_isolated;
  private final Classes m_shared;

  private static URL[] join(final List<URL> additionalClassPath,
                            final URL[] urls) {
    final List<URL> classPath = new ArrayList<URL>(additionalClassPath);
    classPath.addAll(asList(urls));
    return classPath.toArray(new URL[classPath.size()]);
  }

  /**
   * Constructor.
   *
   * <p>
   * The behaviour is determined by three sets of class names. Each set
   * contains fully qualified class names, or fully qualified prefixes ending in
   * "*", that identify the classes beginning with the package prefix.
   * </p>
   *
   * <p>
   * Resource names may also be specified, fully qualified with '/' separators
   * as necessary. Wild card package names also filter resources; the '.'
   * separators are translated internally to '/'s.
   * </p>
   *
   * <p>
   * If multiple sets refer to the same class, {@code shared} has priority
   * over {@code blocked}, and both have priority over {@code isolated},
   * whether or not the class is specified explicitly or using a package prefix.
   * </p>
   *
   * @param parent
   *          Parent classloader.
   * @param additionalClassPath
   *          Prepended to the parent's URLs to calculate the classpath for this
   *          class loader.
   * @param blocked
   *          Classes to block, whether or not the parent class loader has its
   *          own copies.
   * @param isolated
   *          Classes to load in this class loader, whether or not the parent
   *          class loader has its own copies.
   * @param shared
   *          Classes that should be loaded by the parent class loader,
   *          overriding {@code blocked} and {@code isolated}.
   * @param respectGrandparents
   *          Only block or isolate classes from the parent class loader.
   */
  public BlockingClassLoader(final URLClassLoader parent,
                             final List<URL> additionalClassPath,
                             final Set<String> blocked,
                             final Set<String> isolated,
                             final Set<String> shared,
                             final boolean respectGrandparents) {
    super(join(additionalClassPath, parent.getURLs()), parent);

    m_blocked = new Classes(blocked);
    m_isolated = new Classes(isolated);
    m_shared = new Classes(shared);

    m_respectGrandparents = respectGrandparents;
  }

  /**
   * Simplified constructor that uses standard application classloader as
   * the parent.
   *
   * @param additionalClassPath
   *          Prepended to the parent's URLs to calculate the classpath for this
   *          class loader.
   * @param blocked
   *          Classes to block, whether or not the parent class loader has its
   *          own copies.
   * @param isolated
   *          Classes to load in this class loader, whether or not the parent
   *          class loader has its own copies.
   * @param shared
   *          Classes that should be loaded by the parent class loader,
   *          overriding {@code blocked} and {@code isolated}.
   * @param respectGrandparents
   *          Only block or isolate classes from the parent class loader.
   */
  public BlockingClassLoader(final List<URL> additionalClassPath,
                             final Set<String> blocked,
                             final Set<String> isolated,
                             final Set<String> shared,
                             final boolean respectGrandparents) {
    this((URLClassLoader)BlockingClassLoader.class.getClassLoader(),
         additionalClassPath,
         blocked,
         isolated,
         shared,
         respectGrandparents);
  }


  /**
   * Simplified constructor without {@code additionalClassPath}.
   *
   * @param blocked
   *          Classes to block, whether or not the parent class loader has its
   *          own copies.
   * @param isolated
   *          Classes to load in this class loader, whether or not the parent
   *          class loader has its own copies.
   * @param shared
   *          Classes that should be loaded by the parent class loader,
   *          overriding {@code blocked} and {@code isolated}.
   * @param respectGrandparents
   *          Only block or isolate classes from the parent class loader.
   */
  public BlockingClassLoader(final Set<String> blocked,
                             final Set<String> isolated,
                             final Set<String> shared,
                             final boolean respectGrandparents) {
    this(Collections.<URL>emptyList(),
         blocked,
         isolated,
         shared,
         respectGrandparents);
  }

  /**
   * Override only to check parent ClassLoader if not blocked.
   *
   * {@inheritDoc}
   */
  @Override protected Class<?> loadClass(final String name,
                                         final boolean resolve)
    throws ClassNotFoundException  {

    if (!m_shared.matches(name, false)) {

      if (m_respectGrandparents) {
        try {
          // We always have a grandparent classloader.
          return Class.forName(name, resolve, getParent().getParent());
        }
        catch (final ClassNotFoundException e) {
          // Grandparent knows nothing.
        }
      }

      if (m_blocked.matches(name, false)) {
        throw new ClassNotFoundException();
      }

      if (m_isolated.matches(name, false)) {
        synchronized (this) {
          Class<?> c = findLoadedClass(name);

          if (c == null) {
            c = findClass(name);
          }

          if (resolve) {
            resolveClass(c);
          }

          return c;
        }
      }
    }

    return super.loadClass(name, resolve);
  }

  /**
   * Override only to check parent ClassLoader if not blocked.
   *
   * {@inheritDoc}
   */
  @Override public URL getResource(final String name) {

    if (!m_shared.matches(name, true)) {

      if (m_respectGrandparents) {
        // We always have a grandparent classloader.
        final URL grandParentResult = getParent().getParent().getResource(name);

        if (grandParentResult != null) {
          return grandParentResult;
        }
      }

      if (m_blocked.matches(name, true)) {
        return null;
      }

      if (m_isolated.matches(name, true)) {
        return findResource(name);
      }
    }

    return super.getResource(name);
  }

  /**
   * Override only to check parent ClassLoader if not blocked.
   *
   * {@inheritDoc}
   */
  @Override
  public Enumeration<URL> getResources(final String name) throws IOException {

    if (!m_shared.matches(name, true)) {

      final List<URL> result = new ArrayList<URL>();

      if (m_respectGrandparents) {
        // We always have a grandparent classloader.
        result.addAll(list(getParent().getParent().getResources(name)));
      }

      if (m_blocked.matches(name, true)) {
        return enumeration(result);
      }

      if (m_isolated.matches(name, true)) {
        result.addAll(list(findResources(name)));

        return enumeration(result);
      }
    }

    return super.getResources(name);
  }

  private static class Classes {
    private final Set<String> m_classNames = new HashSet<String>();
    private final Set<String> m_prefixes = new HashSet<String>();

    public Classes(final Set<String> wildcardNames) {

      for (final String name : wildcardNames) {
        final int index = name.indexOf('*');

        if (index >= 0) {
          m_prefixes.add(name.substring(0, index));
        }
        else {
          m_classNames.add(name);
        }
      }
    }

    public boolean matches(final String name, final boolean isResource) {
      final String packageName = isResource ? name.replace('/', '.') : name;

      if (m_classNames.contains(name)) {
        return true;
      }

      for (final String prefix : m_prefixes) {
        if (packageName.startsWith(prefix)) {
          return true;
        }
      }

      return false;
    }
  }
}
