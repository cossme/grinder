// Copyright (C) 2005 - 2013 Philip Aston
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import net.grinder.util.ListenerSupport.HandlingInformer;
import net.grinder.util.ListenerSupport.Informer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

/**
 * Unit tests for {@link ListenerSupport}.
 *
 * @author Philip Aston
 */
public class TestListenerSupport {

  @Mock
  private Informer<Object> informer; // a licky boom boom down

  @Mock
  private HandlingInformer<Object> handlingInformer;

  private final ListenerSupport<Object> listenerSupport =
      new ListenerSupport<Object>();

  private final Object listener1 = new Object();

  private final Object listener2 = new Object();

  private final Object listener3 = new Object();

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void testWithInformer() throws Exception {

    listenerSupport.add(listener1);
    listenerSupport.add(listener2);
    listenerSupport.add(listener3);
    listenerSupport.add(listener1);
    verifyNoMoreInteractions(informer);

    listenerSupport.apply(informer);

    final InOrder inOrder = inOrder(informer);
    inOrder.verify(informer).inform(listener1);
    inOrder.verify(informer).inform(listener2);
    inOrder.verify(informer).inform(listener3);
    inOrder.verify(informer).inform(listener1);
    verifyNoMoreInteractions(informer);
  }

  @Test
  public void testWithHandlingInformerNotHandled() {

    assertFalse(listenerSupport.apply(handlingInformer));

    listenerSupport.add(listener1);
    listenerSupport.add(listener2);
    listenerSupport.add(listener1);
    verifyNoMoreInteractions(handlingInformer);

    assertFalse(listenerSupport.apply(handlingInformer));

    final InOrder inOrder = inOrder(handlingInformer);
    inOrder.verify(handlingInformer).inform(listener1);
    inOrder.verify(handlingInformer).inform(listener2);
    inOrder.verify(handlingInformer).inform(listener1);
    verifyNoMoreInteractions(handlingInformer);
  }

  @Test
  public void testWithHandlingInformer() {

    when(handlingInformer.inform(listener2)).thenReturn(true);

    assertFalse(listenerSupport.apply(handlingInformer));

    listenerSupport.add(listener1);
    listenerSupport.add(listener2);
    listenerSupport.add(listener1);
    listenerSupport.add(listener2);
    verifyNoMoreInteractions(handlingInformer);

    assertTrue(listenerSupport.apply(handlingInformer));

    final InOrder inOrder = inOrder(handlingInformer);
    inOrder.verify(handlingInformer).inform(listener1);
    inOrder.verify(handlingInformer).inform(listener2);
    verifyNoMoreInteractions(handlingInformer);
  }

  @Test
  public void testRemove() {
    listenerSupport.add(listener1);
    listenerSupport.add(listener2);
    listenerSupport.add(listener3);
    listenerSupport.add(listener1);

    listenerSupport.remove(listener2);

    listenerSupport.apply(informer);

    final InOrder inOrder = inOrder(informer);
    inOrder.verify(informer).inform(listener1);
    inOrder.verify(informer).inform(listener3);
    inOrder.verify(informer).inform(listener1);
    verifyNoMoreInteractions(informer);

    listenerSupport.remove(listener1);

    listenerSupport.apply(informer);

    inOrder.verify(informer).inform(listener3);
    verifyNoMoreInteractions(informer);

    listenerSupport.remove(listener1);

    listenerSupport.apply(informer);
    inOrder.verify(informer).inform(listener3);
    verifyNoMoreInteractions(informer);
  }
}
