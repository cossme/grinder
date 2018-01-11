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

package net.grinder.engine.agent;

import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import net.grinder.communication.StreamReceiver;
import net.grinder.engine.messages.InitialiseGrinderMessage;



/**
 * Simple process for <code>TestChildProcess</code> to play with.
 *
 * @author Philip Aston
 */
public class ReadMessageEchoClass {

  public static void main(String arguments[]) throws Exception {

    final StreamReceiver receiver = new StreamReceiver(System.in);

    final InitialiseGrinderMessage message =
      (InitialiseGrinderMessage)receiver.waitForMessage();

    // Echo the initialisation message, followed by the arguments.
    final ObjectOutput objectOutput = new ObjectOutputStream(System.out);
    objectOutput.writeObject(message);

    for (int i=0; i<arguments.length; ++i) {
      System.out.print(arguments[i]);
    }

    // Testing shows that flushing our streams on exit is necessary to
    // prevent lossage on win32, and maybe elsewhere.
    System.out.flush();
    System.err.flush();
  }
}

