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

package net.grinder.util;


/**
 * An immutable type-safe pair.
 *
 * @param <F> The type of the first item.
 * @param <S> The type of the first item.
 *
 * @author Philip Aston
 */
public class Pair<F, S> {
  private final F m_first;
  private final S m_second;

  /**
   * Type safe factory method.
   *
   * @param first The first item.
   * @param second The second item.
   * @return A pair.
   * @param <F> The type of the first item.
   * @param <S> The type of the first item.
   */
  public static <F, S> Pair<F, S> of(F first, S second) {
    return new Pair<F, S>(first, second);
  }

  /**
   * Constructor for Pair.
   *
   * @param first The first item.
   * @param second The second item.
   */
  protected Pair(F first, S second) {
    m_first = first;
    m_second = second;
  }

  /**
   * Accessor for the first item.
   *
   * @return The item.
   */
  public F getFirst() {
    return m_first;
  }

  /**
   * Accessor for the second item.
   *
   * @return The item.
   */
  public S getSecond() {
    return m_second;
  }

  /**
   * Hash code.
   *
   * @return The hash code.
   */
  @Override public int hashCode() {
    int result = 1;

    result = 31 * result + ((m_first == null) ? 0 : m_first.hashCode());
    result = 31 * result + ((m_second == null) ? 0 : m_second.hashCode());

    return result;
  }

  /**
   * Equality.
   *
   * @param o Object to compare.
   * @return {@code true} if and only if this object equals {@code o}.
   */
  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null) {
      return false;
    }

    if (getClass() != o.getClass()) {
      return false;
    }

    final Pair<?, ?> other = (Pair<?, ?>) o;

    if (m_first == null && other.m_first != null) {
      return false;
    }

    if (m_second == null && other.m_second != null) {
      return false;
    }

    return m_first.equals(other.m_first) &&
           m_second.equals(other.m_second);
  }

  /**
   * Describe ourself.
   *
   * @return The description.
   */
  @Override public String toString() {
    return "(" + m_first + ", " + m_second + ")";
  }
}
