// Copyright (C) 2002 - 2009 Philip Aston
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

package net.grinder.script;

import net.grinder.common.GrinderException;


/**
 * Thrown when an attempt is made to wrap a type that can not be instrumented.
 *
 * @author Philip Aston
 * @version $Revision: 4114 $
 */
public class NonInstrumentableTypeException extends GrinderException {
  /**
   * Creates a new <code>NotWrappableTypeException</code> instance.
   *
   * @param s Exception message.
   */
  public NonInstrumentableTypeException(String s) {
    super(s);
  }

  /**
   * Creates a new <code>NotWrappableTypeException</code> instance.
   *
   * @param s Exception message.
   * @param cause Cause.
   */
  public NonInstrumentableTypeException(String s, Throwable cause) {
    super(s, cause);
  }
}
