// Copyright (C) 2005 - 2009 Philip Aston
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

import java.util.List;
import java.util.ArrayList;

import junit.framework.TestCase;


/**
 * Unit test case for {@link ListenerSupport}.
 *
 * @author Philip Aston
 */
public class TestListenerSupport extends TestCase {

  public void testWithInformer() throws Exception {

    final ListenerSupport<Object> listenerSupport =
      new ListenerSupport<Object>();

    final Object listener1 = new Object();
    final Object listener2 = new Object();
    final Object listener3 = new Object();

    listenerSupport.add(listener1);
    listenerSupport.add(listener2);
    listenerSupport.add(listener3);
    listenerSupport.add(listener1);

    final List<Object> listeners = new ArrayList<Object>();

    listenerSupport.apply(new ListenerSupport.Informer<Object>() {
      public void inform(Object listener) {
        listeners.add(listener);
      }
    });

    final Object[] calledListeners = listeners.toArray();
    assertEquals(4, calledListeners.length);

    assertEquals(listener1, calledListeners[0]);
    assertEquals(listener2, calledListeners[1]);
    assertEquals(listener3, calledListeners[2]);
    assertEquals(listener1, calledListeners[3]);
  }

  public void testWithHandlingInformer() throws Exception {

    final ListenerSupport<Object> listenerSupport =
      new ListenerSupport<Object>();

    final Object listener1 = new Object();
    final Object listener2 = new Object();
    final Object listener3 = new Object();

    final List<Object> listeners = new ArrayList<Object>();

    final ListenerSupport.HandlingInformer<Object> informUpToListener3 =
      new ListenerSupport.HandlingInformer<Object>() {
        public boolean inform(Object listener) {
          if (listener == listener3) {
            return true;
          }

          listeners.add(listener);
          return false;
        }
      };

    assertFalse(listenerSupport.apply(informUpToListener3));

    listenerSupport.add(listener1);
    listenerSupport.add(listener2);
    listenerSupport.add(listener1);

    assertFalse(listenerSupport.apply(informUpToListener3));
    listeners.clear();

    listenerSupport.add(listener3);
    listenerSupport.add(listener2);

    assertTrue(listenerSupport.apply(informUpToListener3));

    final Object[] calledListeners = listeners.toArray();
    assertEquals(3, calledListeners.length);

    assertEquals(listener1, calledListeners[0]);
    assertEquals(listener2, calledListeners[1]);
    assertEquals(listener1, calledListeners[2]);

    listeners.clear();

    final ListenerSupport.HandlingInformer<Object> informAll =
      new ListenerSupport.HandlingInformer<Object>() {
        public boolean inform(Object listener) {
          listeners.add(listener);
          return false;
        }
      };

    assertFalse(listenerSupport.apply(informAll));

    final Object[] calledListeners2 = listeners.toArray();
    assertEquals(5, calledListeners2.length);

    assertEquals(listener1, calledListeners2[0]);
    assertEquals(listener2, calledListeners2[1]);
    assertEquals(listener1, calledListeners2[2]);
    assertEquals(listener3, calledListeners2[3]);
    assertEquals(listener2, calledListeners2[4]);
  }

  public void testRemove() {
    final ListenerSupport<Object> listenerSupport =
      new ListenerSupport<Object>();

    final Object listener1 = new Object();
    final Object listener2 = new Object();
    final Object listener3 = new Object();

    listenerSupport.add(listener1);
    listenerSupport.add(listener2);
    listenerSupport.add(listener3);
    listenerSupport.add(listener1);

    listenerSupport.remove(listener2);

    final List<Object> listeners = new ArrayList<Object>();

    final ListenerSupport.Informer<Object> informer =
      new ListenerSupport.Informer<Object>() {
        public void inform(Object listener) {
          listeners.add(listener);
        }
      };

    listenerSupport.apply(informer);

    final Object[] calledListeners = listeners.toArray();
    assertEquals(3, calledListeners.length);

    assertEquals(listener1, calledListeners[0]);
    assertEquals(listener3, calledListeners[1]);
    assertEquals(listener1, calledListeners[2]);

    listeners.clear();

    listenerSupport.remove(listener1);

    listenerSupport.apply(informer);
    assertEquals(1, listeners.size());
    assertEquals(listener3, listeners.get(0));

    listeners.clear();

    listenerSupport.remove(listener1);

    listenerSupport.apply(informer);
    assertEquals(1, listeners.size());
    assertEquals(listener3, listeners.get(0));
  }
}
