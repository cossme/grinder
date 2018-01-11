// Copyright (C) 2004 - 2013 Philip Aston
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

package net.grinder.console.swingui;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.CallData;
import net.grinder.testutility.DelegatingStubFactory;
import net.grinder.testutility.RandomStubFactory;

import org.junit.Test;

/**
 * Unit tests for {@link CompositeTreeModel}.
 *
 * @author Philip Aston
 */
public class TestCompositeTreeModel {

  @Test
  public void testConstruction() throws Exception {
    final CompositeTreeModel compositeTreeModel = new CompositeTreeModel();
    assertNotNull(compositeTreeModel.getRoot());
    assertEquals(compositeTreeModel.getRoot(), compositeTreeModel.getRoot());

    final CompositeTreeModel compositeTreeModel2 = new CompositeTreeModel();
    AssertUtilities.assertNotEquals(compositeTreeModel.getRoot(),
      compositeTreeModel2.getRoot());
  }

  @Test
  public void testGetChildMethods() throws Exception {
    final CompositeTreeModel compositeTreeModel = new CompositeTreeModel();

    final Object root = compositeTreeModel.getRoot();

    assertNull(compositeTreeModel.getChild(root, -10));
    assertNull(compositeTreeModel.getChild(root, 0));
    assertNull(compositeTreeModel.getChild(new Object(), 0));
    assertEquals(0, compositeTreeModel.getChildCount(root));
    assertEquals(0, compositeTreeModel.getChildCount(new Object()));
    assertEquals(-1, compositeTreeModel.getIndexOfChild(root, null));
    assertEquals(-1, compositeTreeModel.getIndexOfChild(null, root));
    assertEquals(-1, compositeTreeModel.getIndexOfChild(root, new Object()));
    assertEquals(-1, compositeTreeModel.getIndexOfChild(new Object(), root));
    assertFalse(compositeTreeModel.isLeaf(root));
    assertFalse(compositeTreeModel.isLeaf(new Object()));

    final TreeModel delegateModel1 = createTreeModel();
    final DelegatingStubFactory<DefaultTreeModel> delegateModelStubFactory1 =
        DelegatingStubFactory.create(createTreeModel());
    final TreeModel instrumentedDelegateModel1 =
        delegateModelStubFactory1.getStub();

    compositeTreeModel.addTreeModel(instrumentedDelegateModel1, true);

    assertSame(root, compositeTreeModel.getRoot());
    assertNull(compositeTreeModel.getChild(root, -10));

    final Object rootOfFirstTree = compositeTreeModel.getChild(root, 0);
    assertFalse(compositeTreeModel.isLeaf(rootOfFirstTree));
    // Identity not equal because of wrapping: compare node text
    // instead.
    assertEquals("Root", rootOfFirstTree.toString());
    assertNull(compositeTreeModel.getChild(root, 1));
    delegateModelStubFactory1.assertSuccess("getRoot");
    delegateModelStubFactory1.assertSuccess("isLeaf", rootOfFirstTree);

    final Object delegateRoot1 = delegateModel1.getRoot();

    assertNull(compositeTreeModel.getChild(root, 1));
    assertEquals("Child2",
      compositeTreeModel.getChild(delegateRoot1, 1).toString());
    delegateModelStubFactory1.assertSuccess("getChild", delegateRoot1,
      new Integer(1));

    assertEquals(1, compositeTreeModel.getChildCount(root));
    assertEquals(2, compositeTreeModel.getChildCount(delegateRoot1));
    delegateModelStubFactory1.assertSuccess("getChildCount", delegateRoot1);
    delegateModelStubFactory1.assertNoMoreCalls();

    assertEquals(-1, compositeTreeModel.getIndexOfChild(root, delegateRoot1));

    final Object child1 = delegateModel1.getChild(delegateRoot1, 0);
    assertEquals(0, compositeTreeModel.getIndexOfChild(delegateRoot1, child1));

    final Object grandChild = delegateModel1.getChild(child1, 0);
    assertEquals(0, compositeTreeModel.getIndexOfChild(child1, grandChild));
    assertTrue(compositeTreeModel.isLeaf(grandChild));

    delegateModelStubFactory1.resetCallHistory();

    final TreeModel delegateModel2 = createTreeModel();
    final Object delegateRoot2 = delegateModel2.getRoot();

    compositeTreeModel.addTreeModel(delegateModel2, false);
    assertEquals(3, compositeTreeModel.getChildCount(root));

    assertEquals("Child1",
      compositeTreeModel.getChild(delegateRoot1, 0).toString());
    delegateModelStubFactory1.assertSuccess("getChild", delegateRoot1,
      new Integer(0));
    assertEquals("Child1", compositeTreeModel.getChild(root, 1).toString());
    assertEquals("Child2", compositeTreeModel.getChild(root, 2).toString());

    final Object otherChild2 = delegateModel2.getChild(delegateRoot2, 1);

    assertEquals(1, delegateModel2.getIndexOfChild(delegateRoot2,
      otherChild2));
    assertEquals(2, compositeTreeModel.getIndexOfChild(root, otherChild2));
  }

  @Test
  public void testListeners() throws Exception {
    final CompositeTreeModel compositeTreeModel = new CompositeTreeModel();
    final Object root = compositeTreeModel.getRoot();

    final RandomStubFactory<TreeModelListener> listener1StubFactory =
        RandomStubFactory.create(TreeModelListener.class);
    listener1StubFactory.setIgnoreObjectMethods();

    compositeTreeModel.addTreeModelListener(listener1StubFactory.getStub());

    compositeTreeModel.valueForPathChanged(null, null);
    listener1StubFactory.assertNoMoreCalls();

    final DefaultTreeModel delegateModel = createTreeModel();
    final DefaultMutableTreeNode delegateRoot =
        (DefaultMutableTreeNode) delegateModel.getRoot();

    compositeTreeModel.addTreeModel(delegateModel, false);

    final RandomStubFactory<TreeModelListener> listener2StubFactory =
        RandomStubFactory.create(TreeModelListener.class);
    listener2StubFactory.setIgnoreObjectMethods();

    compositeTreeModel.addTreeModelListener(listener2StubFactory.getStub());

    final DefaultMutableTreeNode child3 = new DefaultMutableTreeNode("Child3");
    delegateModel.insertNodeInto(child3, delegateRoot, 2);

    final CallData insertCallData =
        listener1StubFactory.assertSuccess("treeNodesInserted",
          TreeModelEvent.class);

    final TreeModelEvent insertEvent =
        (TreeModelEvent) insertCallData.getParameters()[0];

    assertArrayEquals(new Object[] { root, }, insertEvent.getPath());

    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertSuccess("treeNodesInserted",
      TreeModelEvent.class);
    listener2StubFactory.assertNoMoreCalls();

    compositeTreeModel.removeTreeModelListener(listener1StubFactory.getStub());

    final DefaultMutableTreeNode grandChild2 =
        new DefaultMutableTreeNode("Grandchild2");
    delegateModel.insertNodeInto(grandChild2, child3, 0);

    listener1StubFactory.assertNoMoreCalls();

    final CallData insertCallData2 =
        listener2StubFactory.assertSuccess("treeNodesInserted",
          TreeModelEvent.class);

    final TreeModelEvent insertEvent2 =
        (TreeModelEvent) insertCallData2.getParameters()[0];

    assertArrayEquals(
      new Object[] { root, child3 }, insertEvent2.getPath());

    // Removing twice should be a no-op.
    compositeTreeModel.removeTreeModelListener(listener1StubFactory.getStub());
  }

  private DefaultTreeModel createTreeModel() {
    final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
    final DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("Child1");
    final DefaultMutableTreeNode child2 = new DefaultMutableTreeNode("Child2");
    final DefaultMutableTreeNode grandChild =
        new DefaultMutableTreeNode("Grandchild");
    child1.add(grandChild);
    root.add(child1);
    root.add(child2);
    return new DefaultTreeModel(root);
  }
}
