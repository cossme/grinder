// Copyright (C) 2001 - 2011 Philip Aston
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

package net.grinder.common;


/**
 * Base class which provides equality and ordering semantics for
 * {@link Test} implementations.
 *
 * @author Philip Aston
 */
public abstract class AbstractTestSemantics implements Test {

  /**
   * Define ordering.
   *
   * @param o <code>Object</code> to compare.
   * @return <code>-1</code> if <code>o</code> is less, <code>0</code>
   * if its equal, <code>1</code> if its greater.
   */
  @Override public final int compareTo(Test o) {
    final int ours = getNumber();
    final int others = ((AbstractTestSemantics)o).getNumber();
    return ours < others ? -1 : (ours == others ? 0 : 1);
  }

  /**
   * Define hash semantics. The test number is used as the hash code.
   * I wondered whether it was worth distributing the hash codes more
   * evenly across the range of an int, but using the value is good
   * enough for <code>java.lang.Integer</code> so its good enough for
   * us.
   *
   * @return The hash code.
   */
  @Override public final int hashCode() {
    return getNumber();
  }

  /**
   * {@inheritDoc}
   */
  @Override public final boolean equals(Object o) {

    if (o == this) {
      return true;
    }

    // instanceof does not break symmetry since equals() is final
    // and all Tests inherit from AbstractTestSemantics.
    if (!(o instanceof Test)) {
      return false;
    }

    return getNumber() == ((Test)o).getNumber();
  }

  /**
   * {@inheritDoc}
   */
  @Override public final String toString() {

    final String description = getDescription();

    if (description == null) {
      return "Test " + getNumber();
    }
    else {
      return "Test " + getNumber() + " (" + description + ')';
    }
  }
}
