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

package net.grinder.scriptengine.jython.instrumentation.dcr;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.grinder.script.NonInstrumentableTypeException;
import net.grinder.script.Test.InstrumentationFilter;
import net.grinder.scriptengine.AbstractDCRInstrumenter;
import net.grinder.scriptengine.DCRContext;
import net.grinder.scriptengine.Recorder;
import net.grinder.util.weave.ClassSource;
import net.grinder.util.weave.ParameterSource;
import net.grinder.util.weave.WeavingException;

import org.python.core.PyClass;
import org.python.core.PyFunction;
import org.python.core.PyInstance;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyProxy;
import org.python.core.PyReflectedConstructor;
import org.python.core.PyReflectedFunction;


/**
 * Common code used by the Jython DCR instrumenters.
 *
 * @author Philip Aston
 */
abstract class AbstractJythonDCRInstrumenter extends AbstractDCRInstrumenter {

  private Field m_pyMethodFunc;
  private Field m_pyMethodSelf;


  /**
   * Constructor.
   *
   * @param context The DCR context.
   * @throws WeavingException If the available version of Jython is
   *  incompatible.
   */
  protected AbstractJythonDCRInstrumenter(final DCRContext context)
      throws WeavingException {
    super(context);

    try {
      //  Jython 2.2, 2.5.
      m_pyMethodFunc = reflectField(PyMethod.class, "im_func");
      m_pyMethodSelf = reflectField(PyMethod.class, "im_self");
    }
    catch (final NoSuchFieldException e) {
      try {
        // Jython 2.7.
        m_pyMethodFunc = reflectField(PyMethod.class, "__func__");
        m_pyMethodSelf = reflectField(PyMethod.class, "__self__");
      }
      catch (final NoSuchFieldException e2) {
        throw new WeavingException(
            "Failed to reflect PyMethod - unsupported Jython version", e2);
      }
    }
  }

  private Field reflectField(final Class<?> clazz, final String fieldName)
      throws WeavingException, NoSuchFieldException {
    try {
      return clazz.getField(fieldName);
    }
    catch (final SecurityException e) {
      throw new WeavingException(
         "Failed to reflect Jython implementation - security manager?", e);
    }
  }

  /**
   * Extract and instrument the underlying Java methods for instrumentation.
   *
   * @param <T>
   *          Constructor<?> or Method
   * @param pyReflectedFunction
   *          The Jython object.
   * @return A list of Java methods or constructors.
   * @throws NonInstrumentableTypeException
   *           If the methods could not be extracted.
   */
  @SuppressWarnings("unchecked")
  private <T extends Member> List<T>
    extractJavaMethods(final PyReflectedFunction pyReflectedFunction)
    throws NonInstrumentableTypeException {

    // ReflectedArgs is package scope in Jython 2.2.1; use reflection
    // to avoid compilation issues.

    final Object[] argsList = pyReflectedFunction.argslist;
    final int nargs = pyReflectedFunction.nargs;

    final List<T> result = new ArrayList<T>(nargs);

    for (int i = 0; i < nargs; ++i) {
      final Object argument = argsList[i];

      try {
        final Field dataField = argument.getClass().getField("data");
        dataField.setAccessible(true);
        result.add((T)dataField.get(argument));
      }
      catch (final Exception e) {
        throw new NonInstrumentableTypeException(
          e.getMessage() + " [" + pyReflectedFunction + "]",
          e);
      }
    }

    return result;
  }

  protected static final Object reflectField(final Field field, final Object o)
      throws NonInstrumentableTypeException {

    try {
      return field.get(o);
    }
    catch (final IllegalArgumentException e) {
      throw new NonInstrumentableTypeException("Reflect " + o.getClass(), e);
    }
    catch (final IllegalAccessException e) {
      throw new NonInstrumentableTypeException("Reflect " + o.getClass(), e);
    }
  }

  protected final PyObject func(final PyMethod method)
    throws NonInstrumentableTypeException {

    return (PyObject) reflectField(m_pyMethodFunc, method);
  }

  protected final PyObject self(final PyMethod method)
      throws NonInstrumentableTypeException {

    return (PyObject) reflectField(m_pyMethodSelf, method);
  }

  @Override protected boolean instrument(final Object target,
                                         final Recorder recorder,
                                         final InstrumentationFilter filter)
    throws NonInstrumentableTypeException {

    if (target instanceof PyObject) {
      disallowSelectiveFilter(filter);

      // Jython object.
      if (target instanceof PyInstance) {
        transform(recorder, (PyInstance)target);
      }
      else if (target instanceof PyFunction) {
        transform(recorder, (PyFunction)target);
      }
      else if (target instanceof PyMethod) {
        final PyMethod pyMethod = (PyMethod)target;

        // PyMethod is used for bound and unbound Python methods, and
        // bound Java methods.

        final PyObject theFunc = func(pyMethod);

        if (theFunc instanceof PyReflectedFunction) {

          // Its Java.

          // Its possible im_func might be an unbound Java method or a Java
          // constructor, but I can't find a way to trigger this. We always
          // receive a PyReflectedMethod or PyReflectedConstructor directly.
          // Here, we defensively cope with unbound methods, but not
          // constructors.
          transform(recorder,
                    (PyReflectedFunction) theFunc,
                    self(pyMethod).__tojava__(Object.class));
        }
        else {
          transform(recorder, pyMethod);
        }
      }
      else if (target instanceof PyClass) {
        transform(recorder, (PyClass)target);
      }
      else if (target instanceof PyReflectedConstructor) {
        transform(recorder, (PyReflectedConstructor)target);
      }
      else if (target instanceof PyReflectedFunction) {
        transform(recorder, (PyReflectedFunction)target, null);
      }
      else {
        // Fail, rather than guess a generic approach.

        // We should never be called with a PyType, since it will be
        // converted to a PyClass or Java class by the implicit __tojava__()
        // calls as part of dispatching to the record() implementation.

        // Similarly PyObjectDerived will be converted to a Java class.

        throw new NonInstrumentableTypeException("Unknown PyObject:" +
                                                 target.getClass());
      }
    }
    else if (target instanceof PyProxy) {
      disallowSelectiveFilter(filter);

      transform(recorder, (PyProxy)target);
    }
    else {
      // Let the Java instrumenter have a go.
      return false;
    }

    return true;
  }

  private void disallowSelectiveFilter(final InstrumentationFilter filter)
    throws NonInstrumentableTypeException {

    if (filter != ALL_INSTRUMENTATION) {
      throw new NonInstrumentableTypeException(
        "The Jython instrumenters do not support selective instrumenters");
    }
  }

  protected abstract void transform(Recorder recorder, PyInstance target)
    throws NonInstrumentableTypeException;

  protected abstract void transform(Recorder recorder, PyFunction target)
    throws NonInstrumentableTypeException;

  protected abstract void transform(Recorder recorder, PyClass target)
    throws NonInstrumentableTypeException;

  protected abstract void transform(Recorder recorder, PyProxy target)
    throws NonInstrumentableTypeException;

  protected abstract void transform(Recorder recorder, PyMethod target)
    throws NonInstrumentableTypeException;

  protected final void transform(final Recorder recorder,
                                 final PyReflectedFunction target,
                                 final Object instance)
    throws NonInstrumentableTypeException {

    final List<Method> reflectedArguments = extractJavaMethods(target);

    if (instance != null) {
      for (final Method m : reflectedArguments) {

        Class<?> c = instance.getClass();

        // We want the instance's implementation, not the interface or
        // superclass method used by the call site.
        do {
          try {
            getContext().add(ParameterSource.FIRST_PARAMETER,
                             instance,
                             c.getDeclaredMethod(m.getName(),
                                                 m.getParameterTypes()),
                             recorder);
            break;
          }
          catch (final NoSuchMethodException e) {
            c = c.getSuperclass();
          }
        }
        while (c != null);
      }
    }
    else {
      for (final Method m : reflectedArguments) {
        getContext().add(ClassSource.CLASS,
                         m.getDeclaringClass(),
                         m,
                         recorder);
      }
    }
  }

  protected final void transform(final Recorder recorder,
                                 final PyReflectedConstructor target)
    throws NonInstrumentableTypeException {

    final List<Constructor<?>> reflectedArguments = extractJavaMethods(target);

    for (final Constructor<?> c : reflectedArguments) {
      getContext().add(c.getDeclaringClass(), c, recorder);
    }
  }

  protected final void instrumentPublicMethodsByName(
                                       final Object target,
                                       final String methodName,
                                       final Recorder recorder,
                                       final boolean includeSuperClassMethods)
    throws NonInstrumentableTypeException {
    instrumentPublicMethodsByName(target.getClass(),
                                  methodName,
                                  ParameterSource.FIRST_PARAMETER,
                                  target,
                                  recorder,
                                  includeSuperClassMethods);
  }

  protected final void instrumentPublicMethodsByName(
                                       final Class<?> targetClass,
                                       final String methodName,
                                       final ParameterSource targetSource,
                                       final Object target,
                                       final Recorder recorder,
                                       final boolean includeSuperClassMethods)
    throws NonInstrumentableTypeException {

    // getMethods() includes superclass methods.
    for (final Method method : targetClass.getMethods()) {
      if (!includeSuperClassMethods &&
          targetClass != method.getDeclaringClass()) {
        continue;
      }

      if (!method.getName().equals(methodName)) {
        continue;
      }

      if (!targetSource.canApply(method)) {
        continue;
      }

      getContext().add(targetSource, target, method, recorder);
    }
  }

  protected final void instrumentPublicMethodsByName(
                                       final Class<?> targetClass,
                                       final String methodName,
                                       final ParameterSource targetSource,
                                       final Object target,
                                       final ParameterSource target2Source,
                                       final Object target2,
                                       final Recorder recorder,
                                       final boolean includeSuperClassMethods)
    throws NonInstrumentableTypeException {

    // getMethods() includes superclass methods.
    for (final Method method : targetClass.getMethods()) {
      if (!includeSuperClassMethods &&
          targetClass != method.getDeclaringClass()) {
        continue;
      }

      if (!method.getName().equals(methodName)) {
        continue;
      }

      if (!targetSource.canApply(method)) {
        continue;
      }

      if (!target2Source.canApply(method)) {
        continue;
      }

      getContext().add(targetSource,
                       target,
                       target2Source,
                       target2,
                       method,
                       recorder);
    }
  }
}
