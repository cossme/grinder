// Copyright (C) 2004-2009 Philip Aston
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

package net.grinder.engine.agent;

import java.io.StringWriter;
import java.io.Writer;


/**
 * Simple process for <code>TestChildProcess</code> to play with.
 *
 * @author Philip Aston
 */
public class EchoClass {

  public static final String ECHO_ARGUMENTS = "echo arguments";
  public static final String ECHO_STREAMS = "echo streams";

  public static void main(String arguments[]) throws Exception {

    final Writer commandWriter = new StringWriter();

    while (true) {
      final int c = System.in.read();

      if (c == -1) {
        throw new Exception("Could not read command");
      }
      else if (c == '\n') {
        break;
      }
      else {
        commandWriter.write(c);
      }
    }

    final String command = commandWriter.toString();

    if (command.equals(ECHO_ARGUMENTS)) {
      for (int i=0; i<arguments.length; ++i) {
        System.out.print(arguments[i]);
      }
    }
    else if (command.equals(ECHO_STREAMS)) {
      while (true) {
        final int b = System.in.read();

        if (b == -1) {
          break;
        }

        System.out.write(b);
        System.err.write(b);
      }
    }

    // Testing shows that flushing our streams on exit is necessary to
    // prevent lossage on win32, and maybe elsewhere.
    System.out.flush();
    System.err.flush();
  }
}


