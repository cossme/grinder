// Copyright (C) 2006 - 2009 Philip Aston
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

import net.grinder.testutility.RandomStubFactory;
import junit.framework.TestCase;


/**
 * Unit tests for <code>TestResponseSender</code>.
 *
 * @author Philip Aston
 */
public class TestResponseSender extends TestCase {

  public void testResponseSenderConstruction() throws Exception {
    final Message message = new SimpleMessage();

    final MessageRequiringResponse responseSender = new MessageRequiringResponse(message);

    assertSame(message, responseSender.getMessage());

    assertFalse(responseSender.isResponseSent());
  }

  public void testSendingResponse() throws Exception {

    final Message message = new SimpleMessage();

    final MessageRequiringResponse responseSender = new MessageRequiringResponse(message);

    final SimpleMessage responseMessage = new SimpleMessage();

    try {
      responseSender.sendResponse(responseMessage);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    final RandomStubFactory<Sender> senderStubFactory =
      RandomStubFactory.create(Sender.class);

    responseSender.setResponder(senderStubFactory.getStub());

    responseSender.sendResponse(responseMessage);
    senderStubFactory.assertSuccess("send", responseMessage);
    senderStubFactory.assertNoMoreCalls();

    assertTrue(responseSender.isResponseSent());

    try {
      responseSender.sendResponse(responseMessage);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    senderStubFactory.assertNoMoreCalls();
  }
}
