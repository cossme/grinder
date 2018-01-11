// Copyright (C) 2007 - 2009 Philip Aston
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

package net.grinder.engine.common;

import java.io.File;

import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.Serializer;
import net.grinder.util.Directory;


/**
 * Unit tests for {@link  ScriptLocation}.
 *
 * @author Philip Aston
 */
public class TestScriptLocation extends AbstractFileTestCase {

  public void testScriptLocation() throws Exception {

    final Directory directory = new Directory(new File("abc"));
    final File file1 = new File("def");
    final File file2 = new File("blah");
    final File file3 = new File("lah/dah");

    final ScriptLocation sl1 = new ScriptLocation(directory, file1);
    final ScriptLocation sl2 = new ScriptLocation(directory, file1);
    final ScriptLocation sl3 = new ScriptLocation(directory, file2);

    assertEquals(sl1, sl1);
    assertEquals(sl1, sl2);
    assertTrue(!sl1.equals(sl3));
    assertTrue(!sl1.equals(this));
    assertEquals(sl1.hashCode(), sl2.hashCode());
    assertTrue(sl1.hashCode() != sl3.hashCode());

    final ScriptLocation sl4 = Serializer.serialize(sl1);
    assertEquals(sl1, sl4);

    final ScriptLocation sl5 = new ScriptLocation(file1);
    assertEquals(new File("."),
                 sl5.getDirectory().getFile());
    assertEquals(new File(".", file1.getPath()), sl5.getFile());

    final ScriptLocation sl6 = new ScriptLocation(file3);
    assertEquals(new File("."),
                 sl6.getDirectory().getFile());
    assertEquals(new File(".", file3.getPath()), sl6.getFile());
  }

  public void testNameShortening() throws Exception {
    final Directory directory = new Directory(getDirectory());
    final File existentFile = new File(getDirectory(), "hello");
    assertTrue(existentFile.createNewFile());
    final File nonExistentFile = new File(getDirectory(), "world");

    final ScriptLocation s1 = new ScriptLocation(directory, existentFile);
    assertEquals("hello", s1.toString());

    final ScriptLocation s2 = new ScriptLocation(directory, nonExistentFile);
    assertEquals("world", s2.toString());
  }
}
