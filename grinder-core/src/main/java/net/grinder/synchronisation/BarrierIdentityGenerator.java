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
package net.grinder.synchronisation;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import net.grinder.synchronisation.messages.BarrierIdentity;


/**
 * Identity generator.
 *
 * @author Philip Aston
 */
public final class BarrierIdentityGenerator
  implements BarrierIdentity.Factory {

  private final AtomicInteger m_next = new AtomicInteger();
  private final Serializable m_scope;

  /**
   * Constructor.
   *
   * @param scope The scope of the generated identities.
   */
  public BarrierIdentityGenerator(Serializable scope) {
    m_scope = scope;
  }

  /**
   * {@inheritDoc}
   */
  @Override public BarrierIdentity next() {
    return new BarrierIdentityImplementation(m_scope,
                                             m_next.getAndIncrement());
  }

  /**
   * Simple barrier identity implementation.
   */
  static final class BarrierIdentityImplementation implements BarrierIdentity {

    private static final long serialVersionUID = 1L;

    private final Serializable m_scope;
    private final int m_value;

    /**
     * Constructor.
     *
     * @param scope
     *          Scope.
     * @param value
     *          Guaranteed to be unique for each instance with the same {@code
     *          scope}.
     */
    public BarrierIdentityImplementation(Serializable scope, int value) {
      m_scope = scope;
      m_value = value;
    }

    @Override public int hashCode() {
      return m_value * 17 + m_scope.hashCode();
    }

    @Override public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      final BarrierIdentityImplementation other =
        (BarrierIdentityImplementation) o;

      return m_value == other.m_value && m_scope.equals(other.m_scope);
    }

    @Override public String toString() {
      return "BarrierIdentity[" + m_scope + ", " + m_value + "]";
    }
  }
}
