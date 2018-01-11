// Copyright (C) 2005, 2006, 2007 Philip Aston
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

import java.util.Random;


/**
 * Simple generator of unique Strings.
 *
 * @author Philip Aston
 */
public final class UniqueIdentityGenerator {

  private final String m_unique;
  private int m_nextNumber = 0;
  private static final Random s_random = new Random();

  /**
   * Constructor.
   */
  public UniqueIdentityGenerator() {
    m_unique = hashCode() + "|" +
               System.currentTimeMillis() + "|" +
               s_random.nextInt();
  }

  /**
   * Create a unique string.
   *
   * @param prefix Prefix for generated strings.
   * @return The unique string.
   */
  public synchronized String createUniqueString(String prefix) {
    return prefix + ":" + m_unique + ":" + m_nextNumber++;
  }
}
