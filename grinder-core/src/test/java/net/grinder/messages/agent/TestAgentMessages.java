// Copyright (C) 2000 - 2011 Philip Aston
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

package net.grinder.messages.agent;

import java.io.File;

import net.grinder.common.GrinderProperties;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.Serializer;
import net.grinder.util.FileContents;

import org.junit.Test;


/**
 * Unit test case for messages that are sent to the agent processes.
 *
 * @author Philip Aston
 */
public class TestAgentMessages extends AbstractFileTestCase {

  @Test public void testResetGrinderMessage() throws Exception {
    Serializer.serialize(new ResetGrinderMessage());
  }

  @Test public void testStartGrinderMessage() throws Exception {
    final GrinderProperties properties = new GrinderProperties();
    properties.setProperty("foo", "bah");
    properties.setInt("lah", 123);

    final StartGrinderMessage received =
      Serializer.serialize(new StartGrinderMessage(properties, -1));

    assertEquals(properties, received.getProperties());
  }

  @Test public void testStopGrinderMessage() throws Exception {
    Serializer.serialize(new StopGrinderMessage());
  }

  @Test public void testDistributeFileMessage() throws Exception {
    final File file = new File("test");
    assertTrue(new File(getDirectory(), file.getPath()).createNewFile());

    final FileContents fileContents = new FileContents(getDirectory(), file);

    final DistributeFileMessage received =
      Serializer.serialize(new DistributeFileMessage(fileContents));

    assertEquals(fileContents.toString(),
                 received.getFileContents().toString());
  }

  @Test public void testClearCacheMessage() throws Exception {
    Serializer.serialize(new ClearCacheMessage());
  }
}
