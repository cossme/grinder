// Copyright (C) 2002 - 2011 Philip Aston
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

package net.grinder.script;

import java.io.Serializable;

import net.grinder.common.AbstractTestSemantics;


/**
 * Scripts create <code>Test</code> instances which can then be used
 * to {@link #wrap} other Jython objects.
 *
 * <p>To The Grinder, a <em>test</em> is a unit of work against which
 * statistics are recorded. Tests are uniquely defined by a <em>test
 * number</em> and may also have a <em>description</em>. Scripts can
 * report many different types of thing against the same test, The
 * Grinder will aggregate the results.</p>
 *
 * <p>Creating a <code>Test</code> will automatically update The
 * Grinder console with the test number and the description. If
 * two <code>Tests</code> are created with the same number but a
 * different description, the console will show the first
 * description.</p>
 *
 * @author Philip Aston
 */
public class Test extends AbstractTestSemantics implements Serializable {

  private static final long serialVersionUID = 1L;

  private final int m_number;
  private final String m_description;
  private final transient TestRegistry.RegisteredTest m_registeredTest;

  /**
   * Creates a new <code>Test</code> instance.
   *
   * @param number Test number.
   * @param description Test description.
   */
  public Test(int number, String description) {
    m_number = number;
    m_description = description;
    m_registeredTest = Grinder.grinder.getTestRegistry().register(this);
  }

  /**
   * Get the test number.
   *
   * @return The test number.
   */
  public final int getNumber() {
    return m_number;
  }

  /**
   * Get the test description.
   *
   * @return The test description.
   */
  public final String getDescription() {
    return m_description;
  }

  /**
   * Creates a proxy script object that has the same interface as
   * the passed object. The Grinder will delegate invocations on the
   * proxy object to the target object, timing and record the
   * success or failure of the invocation against the
   * <code>Test</code> statistics. This method can be called many
   * times, for many different targets.
   *
   * @param target Object to wrap.
   * @return The proxy.
   * @exception NotWrappableTypeException If the target object could not be
   * wrapped.
   */
  public final Object wrap(Object target) throws NotWrappableTypeException {
    return m_registeredTest.createProxy(target);
  }

  /**
   * Instrument the supplied {@code target} object. Subsequent calls to {@code
   * target} will be recorded against the statistics for this {@code Test}.
   *
   * @param target
   *          Object to instrument.
   * @throws NonInstrumentableTypeException
   *           If {@code target} could not be instrumented.
   */
  public final void record(Object target)
    throws NonInstrumentableTypeException {
    m_registeredTest.instrument(target);
  }

  /**
   * Selective instrumentation.
   *
   * @see Test#record(Object, InstrumentationFilter)
   */
  public interface InstrumentationFilter {

    /**
     * Filter the parts of an object.
     *
     * @param item
     *          Part to test. The type depends on the instrumenter.
     * @return {@code true} if the item should be instrumented.
     */
    boolean matches(Object item);
  }

  /**
   * Version of {@link #record(Object) record} that allows selective
   * instrumentation of an object.
   *
   * <p>
   * The instrumenter will pass candidate items for instrumentation to the
   * supplied {@code filter}. Only items for which the filter returns {@code
   * true} will be instrumented.
   * </p>
   *
   * <p>
   * The type of item passed to the filter depends upon the instrumenter, and in
   * turn this depends on the type of {@code target}. For example, the Java DCR
   * instrumenter will pass {@link java.lang.reflect.Method}s to the filter.
   * </p>
   *
   * <p>
   * Some instrumenters, including the Jython instrumenter, do not support
   * selective instrumentation. If an instrumenter can handle the {@code target}
   * , but does not support selective instrumentation, this method will throw
   * {@link NonInstrumentableTypeException}. The non-selective version of
   * {@link #record(Object)} should be used instead.
   * </p>
   *
   * @param target
   *          Object to instrument.
   * @param filter
   *          Filter that selects the parts of {@code target} to instrument.
   * @throws NonInstrumentableTypeException
   *           If {@code target} could not be instrumented.
   * @since 3.7
   */
  public final void record(Object target, InstrumentationFilter filter)
    throws NonInstrumentableTypeException {
    m_registeredTest.instrument(target, filter);
  }
}

