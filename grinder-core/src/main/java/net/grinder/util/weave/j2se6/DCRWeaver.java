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

package net.grinder.util.weave.j2se6;

import static java.util.Arrays.asList;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import net.grinder.util.Pair;
import net.grinder.util.weave.ClassSource;
import net.grinder.util.weave.Weaver;
import net.grinder.util.weave.WeavingException;


/**
 * {@link Weaver} that uses Java 6 dynamic class retransformation.
 *
 * @author Philip Aston
 */
public final class DCRWeaver implements Weaver {

  // Guarded by this.
  private final Set<Class<?>> m_pendingClasses = new HashSet<Class<?>>();

  private final PointCutRegistryImplementation m_pointCutRegistry =
    new PointCutRegistryImplementation();
  private final ClassFileTransformer m_transformer;

  private final Instrumentation m_instrumentation;

  /**
   * Constructor.
   *
   * @param transformerFactory Used to create the transformer.
   * @param instrumentation Access to the JVM instrumentation.
   */
  public DCRWeaver(final ClassFileTransformerFactory transformerFactory,
                   final Instrumentation instrumentation) {

    m_instrumentation = instrumentation;

    m_transformer = transformerFactory.create(m_pointCutRegistry);

    m_instrumentation.addTransformer(m_transformer, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override public String weave(final Constructor<?> constructor) {
    return m_pointCutRegistry.add(constructor);
  }

  /**
   * {@inheritDoc}
   */
  @Override public String weave(final Method method,
                                final TargetSource... targetSources)
    throws WeavingException {

    for (final TargetSource targetSource : targetSources) {
      if (!targetSource.canApply(method)) {
        throw new WeavingException("Insufficient parameters for " +
                                   targetSource + ": " + method.toString());
      }
    }

    return m_pointCutRegistry.add(method, targetSources);
  }

  /**
   * {@inheritDoc}
   */
  @Override public void applyChanges() throws WeavingException {
    synchronized (this) {
      if (m_pendingClasses.size() > 0) {
        try {
          m_instrumentation.retransformClasses(
            m_pendingClasses.toArray(new Class<?>[0]));
        }
        catch (final UnmodifiableClassException e) {
          throw new WeavingException("Failed to modify class", e);
        }
        catch (final ClassFormatError e) {
          throw new WeavingException(
             "Failed to modify class - wrong version of Java?", e);
        }

        m_pendingClasses.clear();
      }
    }
  }

  private static final String s_classLoaderIdentity =
    Integer.toHexString(DCRWeaver.class.hashCode()) + ":";
  private static final AtomicLong s_nextLocation = new AtomicLong();

  private static String generateLocationString() {
    return s_classLoaderIdentity + s_nextLocation.getAndIncrement();
  }


  /**
   * Factory that generates {@link ClassFileTransformer}s which perform
   * the weaving.
   */
  public interface ClassFileTransformerFactory {

    /**
     * Factory method.
     *
     * @param pointCutRegistry The point cut registry.
     * @return The transformer.
     */
    ClassFileTransformer create(PointCutRegistry pointCutRegistry);
  }

  private final class PointCutRegistryImplementation
    implements PointCutRegistry {
    // Guarded by this.
    private final Map<Pair<Member, List<TargetSource>>, String>
      m_wovenMembers =
        new HashMap<Pair<Member, List<TargetSource>>, String>();

    // Pre-calculated mapping of internal class name -> constructor -> weaving
    // details, for efficiency.
    // Guarded by this.
    private final Map<String, Map<Constructor<?>, List<WeavingDetails>>>
      m_internalClassNameToConstructorToLocation =
        new HashMap<String, Map<Constructor<?>, List<WeavingDetails>>>();

    // Pre-calculated mapping of internal class name -> method -> weaving
    // details, for efficiency.
    // Guarded by this.
    private final Map<String, Map<Method, List<WeavingDetails>>>
      m_internalClassNameToMethodToLocation =
        new HashMap<String, Map<Method, List<WeavingDetails>>>();

    @Override
    public Map<Constructor<?>, List<WeavingDetails>>
      getConstructorPointCutsForClass(final String className) {
        return m_internalClassNameToConstructorToLocation.get(className);
    }

    @Override
    public Map<Method, List<WeavingDetails>>
      getMethodPointCutsForClass(final String className) {
        return m_internalClassNameToMethodToLocation.get(className);
    }

    public String add(final Constructor<?> constructor) {
      return add(m_internalClassNameToConstructorToLocation,
                 constructor,
                 ClassSource.CLASS);
    }

    public String add(final Method method,
                      final TargetSource... targetSources) {
      return add(m_internalClassNameToMethodToLocation,
                 method,
                 targetSources);
    }

    private <T extends Member> String add(
      final Map<String, Map<T, List<WeavingDetails>>>
        internalClassNameToMethodToLocation,
      final T method,
      final TargetSource... targetSourceArray) {

      final List<TargetSource> targetSources = asList(targetSourceArray);

      final Pair<Member, List<TargetSource>> locationKey =
          Pair.of((Member) method, targetSources);

      synchronized (this) {
        final String alreadyWoven = m_wovenMembers.get(locationKey);

        if (alreadyWoven != null) {
          return alreadyWoven;
        }
      }

      final String className = method.getDeclaringClass().getName();
      final String internalClassName = className.replace('.', '/');
      final String location = generateLocationString();

      synchronized (this) {
        final Map<T, List<WeavingDetails>> memberToWeavingDetails;

        final Map<T, List<WeavingDetails>> existingMap =
          internalClassNameToMethodToLocation.get(internalClassName);

        if (existingMap != null) {
          memberToWeavingDetails = existingMap;
        }
        else {
          memberToWeavingDetails = new HashMap<T, List<WeavingDetails>>();
          internalClassNameToMethodToLocation.put(internalClassName,
                                          memberToWeavingDetails);
        }

        m_wovenMembers.put(locationKey, location);

        final List<WeavingDetails> weavingDetailsList;

        final List<WeavingDetails> existingList =
          memberToWeavingDetails.get(method);

        if (existingList != null) {
          weavingDetailsList = existingList;
        }
        else {
          weavingDetailsList = new ArrayList<WeavingDetails>();
          memberToWeavingDetails.put(method, weavingDetailsList);
        }

        weavingDetailsList.add(new WeavingDetails(location, targetSources));
      }

      synchronized (DCRWeaver.this) {
        m_pendingClasses.add(method.getDeclaringClass());
      }

      return location;
    }
  }
}
