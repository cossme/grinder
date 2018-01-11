// Copyright (C) 2000 - 2012 Philip Aston
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

package net.grinder.scriptengine.jython;

import net.grinder.scriptengine.ScriptExecutionException;

import org.python.core.Py;
import org.python.core.PyClass;
import org.python.core.PyException;
import org.python.core.PyTraceback;


/**
 * Exception that wraps errors encountered when invoking Jython
 * scripts.
 *
 * @author Philip Aston
 */
final class JythonScriptExecutionException extends ScriptExecutionException {

  private final String m_message;
  private final String m_shortMessage;

  /**
   * Constructor for exceptions arising from a problem with the script that
   * did not arise from some other exception.
   *
   * @param message
   */
  public JythonScriptExecutionException(final String message) {
    super(message);
    m_message = message;
    m_shortMessage = message;
  }

  /**
   * Creates a new <code>JythonScriptExecutionException</code> instance.
   *
   * @param doingWhat What we were doing.
   * @param e <code>PyException</code> that we caught.
   */
  public JythonScriptExecutionException(final String doingWhat,
                                        final PyException e) {
    super("");
    setStackTrace(new StackTraceElement[0]);

    final Object javaError = e.value.__tojava__(Throwable.class);

    if (javaError == null || javaError == Py.NoConversion) {
      // Duplicate logic from the package scope Py.formatException().
      final StringBuilder pyExceptionMessage = new StringBuilder();

      if (e.type instanceof PyClass) {
        pyExceptionMessage.append(((PyClass) e.type).__name__);
      }
      else {
        pyExceptionMessage.append(e.type.__str__());
      }

      if (e.value != Py.None) {
        pyExceptionMessage.append(": ");
        // The original Py.formatException check's if e.value's type is
        // Py.SyntaxError and if so treats it as a tuple. This is clearly wrong,
        // it should check whether e.type is Py.SyntaxError. We do something
        // simple instead.
        pyExceptionMessage.append(e.value.__str__());
      }

      m_shortMessage =
        "Jython exception, " + pyExceptionMessage +
        " [" + doingWhat + "]";
      m_message =
        tracebackToMessage(pyExceptionMessage.toString(), e.traceback);
      initCause(null);
    }
    else {
      m_shortMessage = "Java exception " + doingWhat;
      m_message = tracebackToMessage(m_shortMessage, e.traceback);
      initCause((Throwable)javaError);
    }
  }

  /**
   * A short message, without the Jython stack trace. We override
   * {@link #getMessage} to include the Jython stack trace; sometimes we don't
   * want the stack trace.
   *
   * @return A short message, without the Jython stack trace.
   */
  @Override
  public String getShortMessage() {
    return m_shortMessage;
  }

  /**
   * The detail message string for this throwable.
   *
   * @return The message.
   */
  @Override
  public String getMessage() {
    return m_message;
  }

  /**
   * Remove the class name from stack traces.
   *
   * @return A string representation of this instance.
   */
  @Override
  public String toString() {
    return getLocalizedMessage();
  }

  /**
   * We fix various following problems with PyTraceback.dumpStack() to make it
   * more suitable for incorporation with a Java stack trace.
   * <ul>
   * <li>PyTraceback doesn't use platform specific line separators.</li>
   * <li>Stacks are printed with the innermost frame last.</li>
   * <li>The indentation style is different.</li>
   * </ul>
   */
  private static String tracebackToMessage(final String prefix,
                                           final PyTraceback traceback) {
    final StringBuilder result = new StringBuilder(prefix);

    if (traceback != null) {
      final String[] frames = traceback.dumpStack().split("\n");

      for (int i = frames.length - 1; i >= 1; --i) {
        result.append(System.getProperty("line.separator"));
        result.append("\t");
        result.append(frames[i].trim());
      }
    }

    return result.toString();
  }
}
