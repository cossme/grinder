// Copyright (C) 2005 - 2009 Philip Aston
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

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;


/**
 * Unit test case for {@link UniqueIdentityGenerator}.
 *
 * @author Philip Aston
 */
public class TestUniqueIdentityGenerator extends TestCase {
  public void testUniqueIdentityGenerator() {
    final UniqueIdentityGenerator uniqueIdentityGenerator =
      new UniqueIdentityGenerator();

    final Set<String> set = new HashSet<String>();

    final int numberOfIdentities = 100;

    for (int i = 0; i < numberOfIdentities; ++i) {
      set.add(uniqueIdentityGenerator.createUniqueString("foo"));
    }

    assertEquals(numberOfIdentities, set.size());
  }
}
