// Copyright (C) 2012 Philip Aston
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

import java.lang.reflect.Method;

import net.grinder.util.weave.Weaver.TargetSource;


/**
 * {@link TargetSource} that specifies a single parameter.
 *
 * @author Philip Aston
 */
public enum ParameterSource implements TargetSource {

  /**
   * The first parameter is the target object. For non-static methods,
   * the first parameter is {@code this}.
   */
  FIRST_PARAMETER(0),

  /**
   * The second parameter is the target object.  For non-static methods,
   * the first parameter is {@code this}.
   */
  SECOND_PARAMETER(1),

  /**
   * The third parameter is the target object.  For non-static methods,
   * the first parameter is {@code this}.
   */
  THIRD_PARAMETER(2);

  private final int m_minimumParameters;

  ParameterSource(final int minimumParameters) {
    this.m_minimumParameters = minimumParameters;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canApply(final Method method) {
    return method.getParameterTypes().length >= m_minimumParameters;
  }
}
