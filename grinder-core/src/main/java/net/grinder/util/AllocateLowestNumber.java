// Copyright (C) 2008 Philip Aston
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
 * A set that associates an number with each of member. Objects added to the set
 * are allocated the lowest available number, starting at 0.
 *
 * @author Philip Aston
 */
public interface AllocateLowestNumber {

  /**
   * Add a new object. If the object already belongs to the set, the existing
   * associated number is returned.
   *
   * @param o
   *            The object.
   * @return The associated number.
   */
  int add(Object o);

  /**
   * Remove an object from the set. The number previously associated with
   * the object (if any) is freed for re-use.
   *
   * @param o The object.
   */
  void remove(Object o);

  /**
   * Call <code>iteratorCallback</code> for each member of the set.
   *
   * @param iteratorCallback Called for each member of the set.
   */
  void forEach(IteratorCallback iteratorCallback);

  /**
   * Iteration callback, see {@link AllocateLowestNumber#forEach}.
   */
  interface IteratorCallback {
    /**
     * Called for a member of the set.
     *
     * @param object The object.
     * @param number The associated number.
     */
    void objectAndNumber(Object object, int number);
  }
}
