// Copyright (C) 2004 - 2012 Philip Aston
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

package net.grinder.engine.agent;

import static java.util.Arrays.asList;
import static net.grinder.testutility.AssertUtilities.assertContainsPattern;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.grinder.common.GrinderProperties;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.util.Directory;

import org.junit.Test;


/**
 *  Unit tests for <code>WorkerProcessCommandLine</code>.
 *
 * @author Philip Aston
 */
public class TestWorkerProcessCommandLine extends AbstractJUnit4FileTestCase {

  @Test public void testConstructorWithEmptyProperties() throws Exception {

    final WorkerProcessCommandLine workerProcessCommandLine =
      new WorkerProcessCommandLine(new GrinderProperties(),
                                   new Properties(),
                                   null,
                                   new Directory());

    assertEquals(
      "java net.grinder.engine.process.WorkerProcessEntryPoint",
      workerProcessCommandLine.toString());
  }

  @Test public void testConstructorWithProperties() throws Exception {

    final GrinderProperties grinderProperties = new GrinderProperties() {{
      setProperty("grinder.jvm.arguments", "-server -Xmx1024M");
      setProperty("grinder.jvm.classpath", "abc;def");
    }};

    final Properties overrideProperties = new Properties();

    final WorkerProcessCommandLine workerProcessCommandLine =
      new WorkerProcessCommandLine(grinderProperties,
                                   overrideProperties,
                                   grinderProperties.getProperty("grinder.jvm.arguments"),
                                   new Directory());

    assertEquals("java -server '-Xmx1024M' -classpath 'abc;def' net.grinder.engine.process.WorkerProcessEntryPoint",
                 workerProcessCommandLine.toString());

    // Should work twice.
    assertEquals("java -server '-Xmx1024M' -classpath 'abc;def' net.grinder.engine.process.WorkerProcessEntryPoint",
                 workerProcessCommandLine.toString());
  }

  @Test public void testConstructorWithAgent() throws Exception {

    final GrinderProperties grinderProperties = new GrinderProperties();

    final Properties overrideProperties = new Properties();

    final File agentFile = new File(getDirectory(), "grinder-dcr-agent-1.jar");
    final File someJar = new File(getDirectory(), "some.jar");
    overrideProperties.put("java.class.path", someJar.getAbsolutePath());
    agentFile.createNewFile();

    final WorkerProcessCommandLine workerProcessCommandLine =
      new WorkerProcessCommandLine(grinderProperties,
                                   overrideProperties,
                                   grinderProperties.getProperty("grinder.jvm.arguments"),
                                   new Directory());

    assertContainsPattern(
      workerProcessCommandLine.toString(),
      "^java '-javaagent:[^\\s]*" + agentFile.getName() +
      "' -classpath '[^\\s]*" + someJar.getName() +
      "' net.grinder.engine.process.WorkerProcessEntryPoint$");
  }

  @Test public void testWithSystemProperties() throws Exception {

    final GrinderProperties grinderProperties = new GrinderProperties() {{
      setProperty("grinder.jvm.arguments", "-Xmx1024M");
      setProperty("grinder.jvm.classpath", "abc;def");
    }};

    final Properties systemProperties = new Properties() {{
      setProperty("grinder.processes", "99"); // Ignored
      setProperty("java.class.path", "jvd;vg;nc");
    }};

    final WorkerProcessCommandLine workerProcessCommandLine =
      new WorkerProcessCommandLine(grinderProperties,
                                   systemProperties,
                                   grinderProperties.getProperty("grinder.jvm.arguments"),
                                   new Directory());

    String commandLine = workerProcessCommandLine.toString();

    final String expectedPrefix = "java '-Xmx1024M' ";

    assertTrue(commandLine, commandLine.startsWith(expectedPrefix));

    commandLine = commandLine.substring(expectedPrefix.length());

    final String expectedSuffix =
      "-classpath 'abc;def" + File.pathSeparatorChar + "jvd;vg;nc' " +
      "net.grinder.engine.process.WorkerProcessEntryPoint";

    assertEquals(expectedSuffix, commandLine);
  }

  private static List<File> path(final String... strings) {
    final List<File> result = new ArrayList<File>(strings.length);

    for (final String s : strings) {
      result.add(new File(s));
    }

    return result;
  }

  @Test public void testIsAgentJarValidNames() {

    for (final String s : asList("grinder-dcr-agent-1.jar",
                                 "grinder-dcr-agent-3.11.jar",
                                 "grinder-dcr-agent-3.11-SNAPSHOT.jar")) {
      assertTrue(s, WorkerProcessCommandLine.isAgentJar(s));
    }
  }

  @Test public void testIsAgentJarInvalidNames() {

    for (final String s : asList("grinder-dcr-agent.jar",
                                 "grinder-dcr-agent-3.11.jar.asc",
                                 "grinder-dcr-agent-3.11-sources.jar",
                                 "xgrinder-dcr-agent-3.11.jar")) {
      assertFalse(s, WorkerProcessCommandLine.isAgentJar(s));
    }
  }

  @Test public void testFindAgentJarFile() throws Exception {
    assertNull(WorkerProcessCommandLine.findAgentJarFile(path("foo.jar")));

    assertNull(WorkerProcessCommandLine.findAgentJarFile(path("/foo.jar")));

    assertNull(
      WorkerProcessCommandLine.findAgentJarFile(path("/notthere/foo.jar")));

    assertNull(
      WorkerProcessCommandLine.findAgentJarFile(
        path("somewhere ", "somewhereelse")));

    final File directories = new File(getDirectory(), "a/b");
    directories.mkdirs();

    assertNull(
      WorkerProcessCommandLine.findAgentJarFile(path(directories.getPath()
                                                     + "/c.jar")));

    final File f = new File(directories, "grinder-dcr-agent-1.20.jar");
    f.createNewFile();

    assertNotNull(
      WorkerProcessCommandLine.findAgentJarFile(path(f.getAbsolutePath())));

    assertNull(
      WorkerProcessCommandLine.findAgentJarFile(
        asList(new File(getDirectory().getAbsoluteFile(), "c.jar"))));

    // I'd like also to test with relative paths, but this is impossible to
    // do in a platform independent manner.
  }
}
