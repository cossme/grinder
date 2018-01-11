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

package net.grinder.console.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.PrintWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Unit test case for {@link ResourcesImplementation}.
 *
 * @author Philip Aston
 */
public class TestResources {

  private final ResourcesImplementation resources =
      new ResourcesImplementation(getClass().getName());

  @Mock
  private PrintWriter m_errorWriter;

  @Before
  public void setUp() {
    initMocks(this);
    resources.setErrorWriter(m_errorWriter);
  }

  @After
  public void postConditions() {
    verifyNoMoreInteractions(m_errorWriter);
  }

  @Test
  public void testResources() throws Exception {
    final ResourcesImplementation resources2 =
        new ResourcesImplementation("TestResources");

    resources2.setErrorWriter(m_errorWriter);

    assertEquals("file1", resources.getString("resourceFile"));
    assertEquals("file2", resources2.getString("resourceFile"));
  }

  @Test
  public void testGetString() throws Exception {
    assertEquals("A property value", resources.getString("key"));
  }

  @Test
  public void testGetStringMissing() throws Exception {
    assertEquals("", resources.getString("notthere"));
    verify(m_errorWriter).println(isA(String.class));
  }

  @Test
  public void testGetImageIcon() throws Exception {
    assertNotNull(resources.getImageIcon("image"));
  }

  @Test
  public void testGetImageIconMissing() throws Exception {
    assertNull(resources.getImageIcon("notthere"));
  }
}
