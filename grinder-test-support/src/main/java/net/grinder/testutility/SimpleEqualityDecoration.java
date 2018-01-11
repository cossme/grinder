// Copyright (C) 2004, 2005 Philip Aston
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

package net.grinder.testutility;


/**
 * Stub that provides simple equality and <code>Comparable</code>
 * semantics. For use with {@link OverrideInvocationHandlerDecorator}.
 *
 * @author    Philip Aston
 */
public final class SimpleEqualityDecoration {
  private final String m_name;

  public SimpleEqualityDecoration(String name) {
    m_name = name;
  }

  public String override_toString(Object proxy) {
    return m_name + ":" + System.identityHashCode(proxy);
  }

  public boolean override_equals(Object proxy, Object o) {
    return proxy == o;
  }

  public int override_hashCode(Object proxy) {
    return System.identityHashCode(proxy);
  }

  public int override_compareTo(Object proxy, Object o) {
    return System.identityHashCode(proxy) - System.identityHashCode(o);
  }
}
