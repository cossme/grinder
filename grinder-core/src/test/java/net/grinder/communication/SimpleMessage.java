// Copyright (C) 2003, 2004, 2005 Philip Aston
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

import java.util.Random;


/**
 *  Simple Message implementation.
 *
 * @author Philip Aston
 */
public final class SimpleMessage implements Message {

  private static Random s_random = new Random();

  private final String m_text = "Some message";
  private final int m_random = s_random.nextInt();
  private final int[] m_padding;
  private Object m_payload;

  public SimpleMessage() {
    this(30);
  }

  public SimpleMessage(int paddingSize) {

    m_padding = new int[paddingSize];

    for (int i=0; i<paddingSize; i++) {
      m_padding[i] = i;
    }
  }

  public void setPayload(Object payload) {
    m_payload = payload;
  }

  public Object getPayload() {
    return m_payload;
  }

  public String toString() {
    return "(" + m_text + ", " + m_random + ")";
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof SimpleMessage)) {
      return false;
    }

    final SimpleMessage other = (SimpleMessage)o;

    return m_text.equals(other.m_text) && m_random == other.m_random;
  }

  public int hashCode() {
    return m_text.hashCode() ^ m_random;
  }
}

