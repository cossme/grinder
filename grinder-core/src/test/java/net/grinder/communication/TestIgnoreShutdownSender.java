// Copyright (C) 2008 - 2009 Philip Aston
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

package net.grinder.communication;

import junit.framework.TestCase;

import net.grinder.testutility.RandomStubFactory;


/**
 * Unit tests for {@link IgnoreShutdownSender}.
 *
 * @author Philip Aston
 */
public class TestIgnoreShutdownSender extends TestCase {

  public void testIgnoreShudtonwSender() throws Exception {

    final RandomStubFactory<Sender> senderStubFactory =
      RandomStubFactory.create(Sender.class);

    final IgnoreShutdownSender ignoreShutdownSender =
      new IgnoreShutdownSender(senderStubFactory.getStub());

    final Message m1 = new SimpleMessage();
    final Message m2 = new SimpleMessage();

    ignoreShutdownSender.send(m1);
    ignoreShutdownSender.send(m2);
    ignoreShutdownSender.send(m2);
    ignoreShutdownSender.shutdown();

    senderStubFactory.assertSuccess("send", m1);
    senderStubFactory.assertSuccess("send", m2);
    senderStubFactory.assertSuccess("send", m2);
    senderStubFactory.assertNoMoreCalls();
  }
}
