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

package net.grinder.scriptengine;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import net.grinder.script.NonInstrumentableTypeException;
import net.grinder.util.weave.Weaver.TargetSource;
import net.grinder.util.weave.WeavingException;


/**
 * Provide access to DCR instrumentation.
 *
 * @author Philip Aston
 */
public interface DCRContext {

  /**
   * Register a constructor for instrumentation.
   *
   * @param target
   *          Target object.
   * @param constructor
   *          The constructor.
   * @param recorder
   *          The recorder to use.
   * @throws NonInstrumentableTypeException
   *           If the constructor belongs to a non-instrumentable type.
   */
  void add(Object target, Constructor<?> constructor, Recorder recorder)
    throws NonInstrumentableTypeException;

  /**
   * Register a method for instrumentation.
   *
   * @param targetSource
   *          The method parameter that identifies the target object.
   * @param target
   *          Target object.
   * @param method
   *          The method.
   * @param recorder
   *          The recorder to use.
   * @throws NonInstrumentableTypeException
   *           If the method belongs to a non-instrumentable type.
   */
  void add(TargetSource targetSource,
           Object target,
           Method method,
           Recorder recorder) throws NonInstrumentableTypeException;

  /**
   * Register a method for instrumentation.
   *
   * @param targetSource
   *          The method parameter that identifies the target object.
   * @param target
   *          Target object.
   * @param target2Source
   *          The method parameter that identifies the second target object.
   * @param target2
   *          Second target object.
   * @param method
   *          The method.
   * @param recorder
   *          The recorder to use.
   * @throws NonInstrumentableTypeException
   *           If the method belongs to a non-instrumentable type.
   */
  void add(TargetSource targetSource,
           Object target,
           TargetSource target2Source,
           Object target2,
           Method method,
           Recorder recorder) throws NonInstrumentableTypeException;

  /**
   * Test whether a class can be instrumented.
   *
   * @param targetClass The class to test.
   * @return {@code true} if the class can be instrumented.
   */
  boolean isInstrumentable(Class<?> targetClass);

  /**
   * Apply the changes queued with the {@code add} methods.
   *
   * @throws WeavingException
   *           If weaving failed.
   */
  void applyChanges() throws WeavingException;
}
