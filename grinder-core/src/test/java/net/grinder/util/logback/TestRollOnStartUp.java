// Copyright (C) 2011 Philip Aston
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

package net.grinder.util.logback;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import net.grinder.testutility.AbstractJUnit4FileTestCase;

import org.junit.Test;


/**
 * Unit tests for {@link RollOnStartUp}.
 *
 * @author Philip Aston
 */
public class TestRollOnStartUp extends AbstractJUnit4FileTestCase {

  @Test public void testRollOnStartUp() throws IOException {
    final File f = new File(getDirectory(), "foo");
    final FileWriter out = new FileWriter(f);
    out.write("foo");
    out.close();

    final RollOnStartUp<Object> r1 = new RollOnStartUp<Object>();
    assertTrue(r1.isTriggeringEvent(f, null));
    assertFalse(r1.isTriggeringEvent(f, null));
    assertFalse(r1.isTriggeringEvent(f, null));

    assertTrue(new RollOnStartUp<Object>().isTriggeringEvent(f, null));
    assertFalse(r1.isTriggeringEvent(f, null));
  }

  @Test public void testRollOnStartUpNoFile() throws IOException {
    final File f = new File(getDirectory(), "foo");

    final RollOnStartUp<Object> r1 = new RollOnStartUp<Object>();
    assertFalse(r1.isTriggeringEvent(f, null));
  }

  @Test public void testRollOnStartUpEmptyFile() throws IOException {
    final File f = new File(getDirectory(), "foo");
    f.createNewFile();

    final RollOnStartUp<Object> r1 = new RollOnStartUp<Object>();
    assertFalse(r1.isTriggeringEvent(f, null));
  }
}
