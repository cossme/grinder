// Copyright (C) 2009 - 2012 Philip Aston
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

package net.grinder.util.weave;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


/**
 * Something that can instrument classes.
 *
 * @author Philip Aston
 */
public interface Weaver {

  /**
   * Queue the given {@code constructor} for weaving, and return a unique
   * identifier that can be used by the advice to refer to the constructor
   * pointcut.
   *
   * <p>
   * Once {@link #weave} has been called for a constructor, subsequent calls are
   * no-ops that will return the identifier generated for the original call.
   * </p>
   *
   * <p>
   * The instrumentation will not be applied to the code until
   * {@link #applyChanges} is called.
   * </p>
   *
   * @param constructor
   *          The constructor.
   * @return String that uniquely identifies the pointcut.
   */
  String weave(Constructor<?> constructor);

  /**
   * Queue the given {@code method} for weaving, and return a unique identifier
   * that can be used by the advice to refer to the method pointcut.
   *
   * <p>
   * Once {@link #weave} has been called for a method, subsequent calls are
   * no-ops that will return the identifier generated for the original call.
   * </p>
   *
   * <p>
   * The instrumentation will not be applied to the code until
   * {@link #applyChanges} is called.
   * </p>
   *
   * @param method
   *          The method.
   * @param targetSources
   *          Which objects should be passed as the target to the advice.
   * @return String that uniquely identifies the pointcut.
   * @throws WeavingException
   *           If the {@code targetSource} is incompatible with the
   *           {@code method}.
   */
  String weave(Method method, TargetSource... targetSources)
    throws WeavingException;

  /**
   * Apply pending weaving that has been requested with {@link #weave}.
   *
   * @throws WeavingException A problem occurred with the weaving.
   */
  void applyChanges() throws WeavingException;

  /**
   * Source of the target objects that the weaving will pass on to the advice.
   */
  interface TargetSource {
    /**
     * Whether this target source can be used to instrument a given method.
     *
     * @param method The method to test.
     * @return {@code true} if the source can be used.
     */
    boolean canApply(Method method);
  }
}
