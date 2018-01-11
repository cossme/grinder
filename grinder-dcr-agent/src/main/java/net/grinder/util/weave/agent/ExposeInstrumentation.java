// Copyright (C) 2009 Philip Aston
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

package net.grinder.util.weave.agent;

import java.lang.instrument.Instrumentation;


/**
 * Java agent that provides the main code base with access to class redefinition
 * and transformation functions.
 *
 * @author Philip Aston
 */
public final class ExposeInstrumentation {
  private static Instrumentation s_instrumentation;

  /**
   * Remember the {@link Instrumentation}.
   *
   * @param arguments Arguments passed to the agent.
   * @param instrumentation The JRE supplies this.
   */
  public static void premain(String arguments,
                             Instrumentation instrumentation) {
    s_instrumentation = instrumentation;
  }

  /**
   * Provide access to the {@link Instrumentation}.
   *
   * @return The {@link Instrumentation}.
   */
  public static Instrumentation getInstrumentation() {
    return s_instrumentation;
  }
}
